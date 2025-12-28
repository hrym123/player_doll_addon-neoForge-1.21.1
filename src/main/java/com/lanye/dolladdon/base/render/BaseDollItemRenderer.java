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
        
        // 应用姿态的位置和大小
        float[] position = pose.getPosition();
        float[] scale = pose.getScale();
        if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
            poseStack.translate(position[0], position[1], position[2]);
        }
        if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
            poseStack.scale(scale[0], scale[1], scale[2]);
        }
        
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
        // 注意：身体的旋转通过 PoseStack 应用，不在这里设置，避免双重旋转
        playerModel.rightArm.setRotation(rightArmRotX, rightArmRotY, rightArmRotZ);
        playerModel.leftArm.setRotation(leftArmRotX, leftArmRotY, leftArmRotZ);
        playerModel.rightLeg.setRotation(rightLegRotX, rightLegRotY, rightLegRotZ);
        playerModel.leftLeg.setRotation(leftLegRotX, leftLegRotY, leftLegRotZ);
        
        // 同时设置外层部分的旋转，使它们跟随基础部分的动作
        // 注意：身体的旋转通过 PoseStack 应用，所以 jacket 的旋转也设为0
        DollRenderHelper.setOverlayPartsRotation(playerModel, 0, 0, 0, // 身体旋转通过 PoseStack 应用
                               leftArmRotX, leftArmRotY, leftArmRotZ,
                               rightArmRotX, rightArmRotY, rightArmRotZ,
                               leftLegRotX, leftLegRotY, leftLegRotZ,
                               rightLegRotX, rightLegRotY, rightLegRotZ);
        
        // 获取渲染类型
        var cutoutRenderType = net.minecraft.client.renderer.RenderType.entityCutoutNoCull(skinLocation);
        var translucentRenderType = net.minecraft.client.renderer.RenderType.entityTranslucent(skinLocation);
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        
        // 第一步：渲染基础层（base layer）
        var baseVertexConsumer = bufferSource.getBuffer(cutoutRenderType);
        
        // 如果有身体旋转，使用 PoseStack 在身体旋转中心应用旋转，然后渲染身体、头部、手臂和腿部
        if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
            poseStack.pushPose();
            // 移动到身体的旋转中心（身体和头连接处，Y坐标约为0.375）
            poseStack.translate(0.0, 0.375, 0.0);
            // 应用身体旋转（只在这里应用，不在 setRotation 中设置）
            poseStack.mulPose(Axis.XP.rotation(bodyRotX));
            poseStack.mulPose(Axis.YP.rotation(bodyRotY));
            poseStack.mulPose(Axis.ZP.rotation(bodyRotZ));
            // 移回
            poseStack.translate(0.0, -0.375, 0.0);
            // 在旋转后的坐标系中渲染身体、头部、手臂和腿部
            // 注意：头部和手臂的旋转值已经是相对于身体的，所以保持它们的旋转值
            playerModel.body.setRotation(0, 0, 0); // 确保身体不额外旋转（旋转已通过 PoseStack 应用）
            // 头部和手臂保持它们自己的相对旋转值（headRotX等已经在上面设置）
            playerModel.body.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.head.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.rightArm.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.leftArm.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.rightLeg.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.leftLeg.render(poseStack, baseVertexConsumer, packedLight, overlay);
            poseStack.popPose();
        } else {
            // 没有身体旋转时，正常渲染
            playerModel.body.setRotation(0, 0, 0);
            playerModel.body.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.head.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.rightArm.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.leftArm.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.rightLeg.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.leftLeg.render(poseStack, baseVertexConsumer, packedLight, overlay);
        }
        
        // 第二步：渲染外层（overlay layer）- 使用半透明渲染以正确显示多层皮肤
        var overlayVertexConsumer = bufferSource.getBuffer(translucentRenderType);
        
        // 如果有身体旋转，使用 PoseStack 在身体旋转中心应用旋转，然后渲染所有外层部分
        if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
            poseStack.pushPose();
            // 移动到身体的旋转中心（身体和头连接处，Y坐标约为0.375）
            poseStack.translate(0.0, 0.375, 0.0);
            // 应用身体旋转（只在这里应用，不在 setRotation 中设置）
            poseStack.mulPose(Axis.XP.rotation(bodyRotX));
            poseStack.mulPose(Axis.YP.rotation(bodyRotY));
            poseStack.mulPose(Axis.ZP.rotation(bodyRotZ));
            // 移回
            poseStack.translate(0.0, -0.375, 0.0);
            // 在旋转后的坐标系中渲染所有外层部分
            // hat层（头发外层）
            playerModel.hat.render(poseStack, overlayVertexConsumer, packedLight, overlay);
            // 手臂外层（保持它们自己的旋转值）
            renderArmOverlayParts(playerModel, poseStack, overlayVertexConsumer, packedLight, overlay);
            // 身体和腿部外层（jacket 的旋转设为0）
            setBodyOverlayRotation(playerModel, 0, 0, 0); // 确保身体外层不额外旋转
            renderBodyLegOverlayParts(playerModel, poseStack, overlayVertexConsumer, packedLight, overlay);
            poseStack.popPose();
        } else {
            // 没有身体旋转时，正常渲染
            playerModel.hat.render(poseStack, overlayVertexConsumer, packedLight, overlay);
            renderArmOverlayParts(playerModel, poseStack, overlayVertexConsumer, packedLight, overlay);
            setBodyOverlayRotation(playerModel, 0, 0, 0);
            renderBodyLegOverlayParts(playerModel, poseStack, overlayVertexConsumer, packedLight, overlay);
        }
        
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
            poseStack.translate(0.5, 0.6, 0.5);
            poseStack.scale(0.255F * modelScale, 0.25F * modelScale, 0.25F * modelScale);
        }
        
        // 翻转模型（玩家模型需要翻转才能正确显示）
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        
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
                // 如果无法获取，返回standing姿态
                return getDefaultPose();
            }
        }
        
        // 从custom_data组件读取NBT
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return getDefaultPose();
        }
        
        var dataTag = customData.copyTag();
        if (dataTag == null) {
            return getDefaultPose();
        }
        
        if (!dataTag.contains("EntityData")) {
            return getDefaultPose();
        }
        
        net.minecraft.nbt.CompoundTag entityTag = dataTag.getCompound("EntityData");
        
        // 优先检查是否有动作名称
        // 注意：对于物品渲染，动作应该显示第一帧（tick=0）的姿态
        // 因为动作是动态的，物品应该显示静态的姿态
        if (entityTag.contains("ActionName", net.minecraft.nbt.Tag.TAG_STRING)) {
            String actionName = entityTag.getString("ActionName");
            DollAction action = PoseActionManager.getAction(actionName);
            if (action != null) {
                // 物品渲染时，动作显示第一帧的姿态
                DollPose actionPose = action.getPoseAt(0);
                if (actionPose != null) {
                    return actionPose;
                }
            }
        }
        
        // 优先使用姿态名称（如果保存了）
        if (entityTag.contains("PoseName", net.minecraft.nbt.Tag.TAG_STRING)) {
            String poseName = entityTag.getString("PoseName");
            DollPose pose = PoseActionManager.getPose(poseName);
            if (pose != null) {
                return pose;
            }
        }
        
        // 如果没有姿态名称，尝试使用姿态索引（向后兼容）
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
        
        // 默认返回standing姿态
        return getDefaultPose();
    }
    
    /**
     * 获取默认姿态（standing，如果不存在则使用createDefaultStandingPose）
     */
    private DollPose getDefaultPose() {
        DollPose standingPose = PoseActionManager.getPose("standing");
        if (standingPose != null) {
            return standingPose;
        }
        // 如果standing姿态不存在，回退到createDefaultStandingPose
        return SimpleDollPose.createDefaultStandingPose();
    }
    
    /**
     * 渲染手臂外层部分（overlay layer）以支持多层皮肤
     * 
     * @param playerModel 玩家模型
     * @param poseStack 变换矩阵栈
     * @param overlayVertexConsumer 外层顶点消费者
     * @param packedLight 光照信息
     * @param overlay 覆盖纹理
     */
    private void renderArmOverlayParts(PlayerModel<?> playerModel,
                                      PoseStack poseStack,
                                      com.mojang.blaze3d.vertex.VertexConsumer overlayVertexConsumer,
                                      int packedLight,
                                      int overlay) {
        try {
            java.lang.reflect.Field leftSleeveField = PlayerModel.class.getDeclaredField("leftSleeve");
            java.lang.reflect.Field rightSleeveField = PlayerModel.class.getDeclaredField("rightSleeve");
            leftSleeveField.setAccessible(true);
            rightSleeveField.setAccessible(true);
            
            Object leftSleeve = leftSleeveField.get(playerModel);
            if (leftSleeve instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) leftSleeve).render(poseStack, overlayVertexConsumer, packedLight, overlay);
            }
            
            Object rightSleeve = rightSleeveField.get(playerModel);
            if (rightSleeve instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) rightSleeve).render(poseStack, overlayVertexConsumer, packedLight, overlay);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    /**
     * 设置身体外层（jacket）的旋转
     * 用于在渲染时临时设置，因为身体的旋转通过 PoseStack 应用
     */
    private void setBodyOverlayRotation(PlayerModel<?> playerModel, float bodyRotX, float bodyRotY, float bodyRotZ) {
        try {
            java.lang.reflect.Field jacketField = PlayerModel.class.getDeclaredField("jacket");
            jacketField.setAccessible(true);
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) jacket).setRotation(bodyRotX, bodyRotY, bodyRotZ);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    /**
     * 渲染身体和腿部外层部分（overlay layer）以支持多层皮肤
     * 
     * @param playerModel 玩家模型
     * @param poseStack 变换矩阵栈
     * @param overlayVertexConsumer 外层顶点消费者
     * @param packedLight 光照信息
     * @param overlay 覆盖纹理
     */
    private void renderBodyLegOverlayParts(PlayerModel<?> playerModel,
                                          PoseStack poseStack,
                                          com.mojang.blaze3d.vertex.VertexConsumer overlayVertexConsumer,
                                          int packedLight,
                                          int overlay) {
        try {
            java.lang.reflect.Field leftPantsField = PlayerModel.class.getDeclaredField("leftPants");
            java.lang.reflect.Field rightPantsField = PlayerModel.class.getDeclaredField("rightPants");
            java.lang.reflect.Field jacketField = PlayerModel.class.getDeclaredField("jacket");
            leftPantsField.setAccessible(true);
            rightPantsField.setAccessible(true);
            jacketField.setAccessible(true);
            
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) jacket).render(poseStack, overlayVertexConsumer, packedLight, overlay);
            }
            
            Object leftPants = leftPantsField.get(playerModel);
            if (leftPants instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) leftPants).render(poseStack, overlayVertexConsumer, packedLight, overlay);
            }
            
            Object rightPants = rightPantsField.get(playerModel);
            if (rightPants instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) rightPants).render(poseStack, overlayVertexConsumer, packedLight, overlay);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
}

