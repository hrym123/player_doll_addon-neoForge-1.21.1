package com.lanye.dolladdon.base.render;

import com.lanye.dolladdon.api.action.DollAction;
import com.lanye.dolladdon.api.pose.DollPose;
import com.lanye.dolladdon.api.pose.SimpleDollPose;
import com.lanye.dolladdon.util.PoseActionManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 玩偶物品渲染器基类
 * 提供所有玩偶物品渲染器的共同功能
 */
public abstract class BaseDollItemRenderer extends BlockEntityWithoutLevelRenderer {
    protected final PlayerModel<net.minecraft.world.entity.player.Player> playerModel;
    
    protected BaseDollItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet, PlayerModel<net.minecraft.world.entity.player.Player> playerModel) {
        super(dispatcher, modelSet);
        this.playerModel = playerModel;
    }
    
    /**
     * 获取皮肤资源位置
     * @return 皮肤资源位置
     */
    protected abstract ResourceLocation getSkinLocation();
    
    /**
     * 获取日志标签（用于调试）
     * @return 日志标签
     */
    protected abstract String getLogTag();
    
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        
        // 根据显示上下文调整模型的位置、缩放和旋转
        applyPlayerModelTransform(poseStack, transformType);
        
        // 获取皮肤位置（由子类实现）
        ResourceLocation skinLocation = getSkinLocation();
        
        // 从NBT读取动作或姿态
        DollPose pose = getPoseFromNBT(stack);
        
        // 从姿态获取旋转角度
        float[] headRot = pose.getHeadRotation();
        float[] hatRot = pose.getHatRotation();
        float[] bodyRot = pose.getBodyRotation();
        float[] rightArmRot = pose.getRightArmRotation();
        float[] leftArmRot = pose.getLeftArmRotation();
        float[] rightLegRot = pose.getRightLegRotation();
        float[] leftLegRot = pose.getLeftLegRotation();
        
        float headRotX = headRot[0], headRotY = headRot[1], headRotZ = headRot[2];
        float hatRotX = hatRot[0], hatRotY = hatRot[1], hatRotZ = hatRot[2];
        float bodyRotX = bodyRot[0], bodyRotY = bodyRot[1], bodyRotZ = bodyRot[2];
        float rightArmRotX = rightArmRot[0], rightArmRotY = rightArmRot[1], rightArmRotZ = rightArmRot[2];
        float leftArmRotX = leftArmRot[0], leftArmRotY = leftArmRot[1], leftArmRotZ = leftArmRot[2];
        float rightLegRotX = rightLegRot[0], rightLegRotY = rightLegRot[1], rightLegRotZ = rightLegRot[2];
        float leftLegRotX = leftLegRot[0], leftLegRotY = leftLegRot[1], leftLegRotZ = leftLegRot[2];
        
        playerModel.head.setRotation(headRotX, headRotY, headRotZ);
        playerModel.hat.setRotation(hatRotX, hatRotY, hatRotZ);
        playerModel.body.setRotation(bodyRotX, bodyRotY, bodyRotZ);
        playerModel.rightArm.setRotation(rightArmRotX, rightArmRotY, rightArmRotZ);
        playerModel.leftArm.setRotation(leftArmRotX, leftArmRotY, leftArmRotZ);
        playerModel.rightLeg.setRotation(rightLegRotX, rightLegRotY, rightLegRotZ);
        playerModel.leftLeg.setRotation(leftLegRotX, leftLegRotY, leftLegRotZ);
        
        // 同时设置外层部分的旋转，使它们跟随基础部分的动作
        setOverlayPartsRotation(bodyRotX, bodyRotY, bodyRotZ,
                               leftArmRotX, leftArmRotY, leftArmRotZ,
                               rightArmRotX, rightArmRotY, rightArmRotZ,
                               leftLegRotX, leftLegRotY, leftLegRotZ,
                               rightLegRotX, rightLegRotY, rightLegRotZ);
        
        // 获取渲染类型
        var cutoutRenderType = net.minecraft.client.renderer.RenderType.entityCutoutNoCull(skinLocation);
        var translucentRenderType = net.minecraft.client.renderer.RenderType.entityTranslucent(skinLocation);
        
        // 第一步：渲染基础层（base layer）- 所有基础部分
        var baseVertexConsumer = bufferSource.getBuffer(cutoutRenderType);
        
        // 渲染基础身体部分（不包括外层）
        playerModel.head.render(poseStack, baseVertexConsumer, packedLight, packedOverlay);
        playerModel.body.render(poseStack, baseVertexConsumer, packedLight, packedOverlay);
        playerModel.rightArm.render(poseStack, baseVertexConsumer, packedLight, packedOverlay);
        playerModel.leftArm.render(poseStack, baseVertexConsumer, packedLight, packedOverlay);
        playerModel.rightLeg.render(poseStack, baseVertexConsumer, packedLight, packedOverlay);
        playerModel.leftLeg.render(poseStack, baseVertexConsumer, packedLight, packedOverlay);
        
        // 第二步：渲染外层（overlay layer）- 使用半透明渲染以正确显示多层皮肤
        var overlayVertexConsumer = bufferSource.getBuffer(translucentRenderType);
        
        // 渲染hat层（头发外层）
        playerModel.hat.render(poseStack, overlayVertexConsumer, packedLight, packedOverlay);
        
        // 渲染外层部分（overlay layer）- 用于多层皮肤
        renderOverlayParts(poseStack, overlayVertexConsumer, packedLight, packedOverlay);
        
        poseStack.popPose();
    }
    
    /**
     * 根据显示上下文调整玩家模型的位置、缩放和旋转
     * 注意：玩家模型的原点在脚部（Y=0），模型高度约1.8，中心在Y=0.9处
     * 但是为了与实体渲染器保持一致，模型被缩放到高度1.125（缩放比例约0.625）
     * 
     * @param poseStack 变换矩阵栈
     * @param transformType 显示上下文类型
     */
    protected void applyPlayerModelTransform(PoseStack poseStack, ItemDisplayContext transformType) {
        // 模型缩放比例，与实体渲染器保持一致
        float modelScale = 1F;
        
        if (transformType == ItemDisplayContext.GUI) {
            // GUI 中：居中显示
            poseStack.translate(0.5, 0.75, 0.0);
            poseStack.scale(0.5F * modelScale, 0.5F * modelScale, 0.5F * modelScale);
            poseStack.mulPose(Axis.YP.rotationDegrees(-155.0F));
        } else if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || 
                   transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            poseStack.translate(0.5, 1, 0.5);
            poseStack.scale(0.5F * modelScale, 0.5F * modelScale, -0.5F * modelScale);
            // 旋转
            if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                // 第一人称左手
                poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
            } else{
                // 第一人称右手
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F));
            }
        } else if (transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || 
                   transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            // 第三人称：调整位置和大小
            poseStack.translate(0.5, 0.9, 0.5);
            // 缩放并前后反转（应用模型缩放）
            poseStack.scale(0.25F * modelScale, 0.25F * modelScale, -0.25F * modelScale);
        } else {
            // 其他情况（地面、框架等）
            poseStack.translate(0.0, 0.0, 0.0);
            poseStack.scale(0.5F * modelScale, 0.5F * modelScale, 0.5F * modelScale);
        }
        
        // 翻转模型（玩家模型需要翻转才能正确显示）
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        
    }
    
    /**
     * 设置外层部分的旋转，使它们跟随基础部分的动作
     * 
     * @param bodyRotX 身体的X旋转
     * @param bodyRotY 身体的Y旋转
     * @param bodyRotZ 身体的Z旋转
     * @param leftArmRotX 左臂的X旋转
     * @param leftArmRotY 左臂的Y旋转
     * @param leftArmRotZ 左臂的Z旋转
     * @param rightArmRotX 右臂的X旋转
     * @param rightArmRotY 右臂的Y旋转
     * @param rightArmRotZ 右臂的Z旋转
     * @param leftLegRotX 左腿的X旋转
     * @param leftLegRotY 左腿的Y旋转
     * @param leftLegRotZ 左腿的Z旋转
     * @param rightLegRotX 右腿的X旋转
     * @param rightLegRotY 右腿的Y旋转
     * @param rightLegRotZ 右腿的Z旋转
     */
    private void setOverlayPartsRotation(float bodyRotX, float bodyRotY, float bodyRotZ,
                                        float leftArmRotX, float leftArmRotY, float leftArmRotZ,
                                        float rightArmRotX, float rightArmRotY, float rightArmRotZ,
                                        float leftLegRotX, float leftLegRotY, float leftLegRotZ,
                                        float rightLegRotX, float rightLegRotY, float rightLegRotZ) {
        try {
            // 使用反射访问PlayerModel的外层部分并设置旋转
            java.lang.reflect.Field leftSleeveField = PlayerModel.class.getDeclaredField("leftSleeve");
            java.lang.reflect.Field rightSleeveField = PlayerModel.class.getDeclaredField("rightSleeve");
            java.lang.reflect.Field leftPantsField = PlayerModel.class.getDeclaredField("leftPants");
            java.lang.reflect.Field rightPantsField = PlayerModel.class.getDeclaredField("rightPants");
            java.lang.reflect.Field jacketField = PlayerModel.class.getDeclaredField("jacket");
            
            leftSleeveField.setAccessible(true);
            rightSleeveField.setAccessible(true);
            leftPantsField.setAccessible(true);
            rightPantsField.setAccessible(true);
            jacketField.setAccessible(true);
            
            // leftSleeve应该跟随leftArm的旋转
            Object leftSleeve = leftSleeveField.get(playerModel);
            if (leftSleeve instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) leftSleeve).setRotation(leftArmRotX, leftArmRotY, leftArmRotZ);
            }
            
            // rightSleeve应该跟随rightArm的旋转
            Object rightSleeve = rightSleeveField.get(playerModel);
            if (rightSleeve instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) rightSleeve).setRotation(rightArmRotX, rightArmRotY, rightArmRotZ);
            }
            
            // leftPants应该跟随leftLeg的旋转
            Object leftPants = leftPantsField.get(playerModel);
            if (leftPants instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) leftPants).setRotation(leftLegRotX, leftLegRotY, leftLegRotZ);
            }
            
            // rightPants应该跟随rightLeg的旋转
            Object rightPants = rightPantsField.get(playerModel);
            if (rightPants instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) rightPants).setRotation(rightLegRotX, rightLegRotY, rightLegRotZ);
            }
            
            // jacket应该跟随body的旋转
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) jacket).setRotation(bodyRotX, bodyRotY, bodyRotZ);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    /**
     * 渲染外层部分（overlay layer）以支持多层皮肤
     * 这些外层部分需要使用半透明渲染类型来正确显示叠加层
     * 
     * 注意：外层部分已经通过setOverlayPartsRotation方法设置了与基础部分相同的旋转，
     * 所以它们现在应该能够跟随基础部分的动作。
     * 
     * @param poseStack 变换矩阵栈
     * @param overlayVertexConsumer 外层顶点消费者
     * @param packedLight 光照信息
     * @param packedOverlay 覆盖纹理
     */
    private void renderOverlayParts(PoseStack poseStack, 
                                    com.mojang.blaze3d.vertex.VertexConsumer overlayVertexConsumer, 
                                    int packedLight, 
                                    int packedOverlay) {
        try {
            // 使用反射访问PlayerModel的外层部分（如果存在）
            java.lang.reflect.Field leftSleeveField = PlayerModel.class.getDeclaredField("leftSleeve");
            java.lang.reflect.Field rightSleeveField = PlayerModel.class.getDeclaredField("rightSleeve");
            java.lang.reflect.Field leftPantsField = PlayerModel.class.getDeclaredField("leftPants");
            java.lang.reflect.Field rightPantsField = PlayerModel.class.getDeclaredField("rightPants");
            java.lang.reflect.Field jacketField = PlayerModel.class.getDeclaredField("jacket");
            
            leftSleeveField.setAccessible(true);
            rightSleeveField.setAccessible(true);
            leftPantsField.setAccessible(true);
            rightPantsField.setAccessible(true);
            jacketField.setAccessible(true);
            
            // 渲染左袖子外层
            Object leftSleeve = leftSleeveField.get(playerModel);
            if (leftSleeve instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) leftSleeve).render(poseStack, overlayVertexConsumer, packedLight, packedOverlay);
            }
            
            // 渲染右袖子外层
            Object rightSleeve = rightSleeveField.get(playerModel);
            if (rightSleeve instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) rightSleeve).render(poseStack, overlayVertexConsumer, packedLight, packedOverlay);
            }
            
            // 渲染左腿外层
            Object leftPants = leftPantsField.get(playerModel);
            if (leftPants instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) leftPants).render(poseStack, overlayVertexConsumer, packedLight, packedOverlay);
            }
            
            // 渲染右腿外层
            Object rightPants = rightPantsField.get(playerModel);
            if (rightPants instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) rightPants).render(poseStack, overlayVertexConsumer, packedLight, packedOverlay);
            }
            
            // 渲染夹克外层（身体外层）
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) jacket).render(poseStack, overlayVertexConsumer, packedLight, packedOverlay);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    /**
     * 从物品堆的NBT标签中获取姿态
     * 支持从动作或姿态索引读取
     */
    private DollPose getPoseFromNBT(ItemStack stack) {
        // 使用 save 方法获取NBT数据
        net.minecraft.core.RegistryAccess registryAccess = null;
        if (Minecraft.getInstance().level != null) {
            registryAccess = Minecraft.getInstance().level.registryAccess();
        } else {
            // 如果没有世界，尝试使用服务器注册表访问（如果可用）
            try {
                if (Minecraft.getInstance().getConnection() != null && 
                    Minecraft.getInstance().getConnection().registryAccess() != null) {
                    registryAccess = Minecraft.getInstance().getConnection().registryAccess();
                }
            } catch (Exception e) {
                // 如果无法获取，返回默认姿态
                return SimpleDollPose.createDefaultStandingPose();
            }
        }
        
        if (registryAccess == null) {
            return SimpleDollPose.createDefaultStandingPose();
        }
        
        net.minecraft.nbt.Tag tag = stack.save(registryAccess);
        if (!(tag instanceof net.minecraft.nbt.CompoundTag)) {
            return SimpleDollPose.createDefaultStandingPose();
        }
        
        net.minecraft.nbt.CompoundTag itemTag = (net.minecraft.nbt.CompoundTag) tag;
        if (!itemTag.contains("EntityData")) {
            return SimpleDollPose.createDefaultStandingPose();
        }
        
        net.minecraft.nbt.CompoundTag entityTag = itemTag.getCompound("EntityData");
        
        // 优先检查是否有动作名称
        if (entityTag.contains("ActionName", net.minecraft.nbt.Tag.TAG_STRING)) {
            String actionName = entityTag.getString("ActionName");
            DollAction action = PoseActionManager.getAction(actionName);
            if (action != null) {
                // 计算动作的当前tick（基于游戏时间）
                long gameTime = 0;
                if (Minecraft.getInstance().level != null) {
                    gameTime = Minecraft.getInstance().level.getGameTime();
                } else {
                    // 如果没有世界，使用系统时间作为后备（每50ms = 1 tick）
                    gameTime = System.currentTimeMillis() / 50;
                }
                
                // 计算当前动作的tick（考虑循环）
                int actionTick;
                if (action.isLooping()) {
                    actionTick = (int)(gameTime % action.getDuration());
                } else {
                    // 非循环动作，只播放一次
                    actionTick = (int)Math.min(gameTime, action.getDuration() - 1);
                }
                
                DollPose actionPose = action.getPoseAt(actionTick);
                if (actionPose != null) {
                    return actionPose;
                }
            }
        }
        
        // 如果没有动作，检查姿态索引
        if (entityTag.contains("PoseIndex", net.minecraft.nbt.Tag.TAG_INT)) {
            int poseIndex = entityTag.getInt("PoseIndex");
            if (poseIndex >= 0) {
                java.util.List<String> poseNames = new java.util.ArrayList<>();
                java.util.Map<String, DollPose> allPoses = PoseActionManager.getAllPoses();
                poseNames.addAll(allPoses.keySet());
                poseNames.sort(String::compareTo);
                
                if (poseIndex < poseNames.size()) {
                    String poseName = poseNames.get(poseIndex);
                    DollPose pose = PoseActionManager.getPose(poseName);
                    if (pose != null) {
                        return pose;
                    }
                }
            }
        }
        
        // 默认返回站立姿态
        return SimpleDollPose.createDefaultStandingPose();
    }
}

