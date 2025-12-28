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
        DollRenderHelper.setOverlayPartsRotation(playerModel, bodyRotX, bodyRotY, bodyRotZ,
                               leftArmRotX, leftArmRotY, leftArmRotZ,
                               rightArmRotX, rightArmRotY, rightArmRotZ,
                               leftLegRotX, leftLegRotY, leftLegRotZ,
                               rightLegRotX, rightLegRotY, rightLegRotZ);
        
        // 渲染玩家模型（包括基础层和外层）
        DollRenderHelper.renderPlayerModel(playerModel, poseStack, bufferSource, skinLocation, packedLight, packedOverlay);
        
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
        com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[物品渲染] 开始从NBT读取姿态，物品: {}", stack.getItem());
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData == null) {
            com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[物品渲染] ❌ custom_data组件为空，使用默认姿态");
            return getDefaultPose();
        }
        
        var dataTag = customData.copyTag();
        if (dataTag == null) {
            com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[物品渲染] ❌ dataTag为空，使用默认姿态");
            return getDefaultPose();
        }
        
        com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[物品渲染] 读取到custom_data内容: {}", dataTag);
        
        if (!dataTag.contains("EntityData")) {
            com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[物品渲染] ❌ 未找到EntityData标签，使用默认姿态");
            return getDefaultPose();
        }
        
        net.minecraft.nbt.CompoundTag entityTag = dataTag.getCompound("EntityData");
        com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[物品渲染] 读取到EntityData内容: {}", entityTag);
        
        // 优先检查是否有动作名称
        // 注意：对于物品渲染，动作应该显示第一帧（tick=0）的姿态
        // 因为动作是动态的，物品应该显示静态的姿态
        if (entityTag.contains("ActionName", net.minecraft.nbt.Tag.TAG_STRING)) {
            String actionName = entityTag.getString("ActionName");
            com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[物品渲染] 检测到动作名称: {}", actionName);
            DollAction action = PoseActionManager.getAction(actionName);
            if (action != null) {
                // 物品渲染时，动作显示第一帧的姿态
                DollPose actionPose = action.getPoseAt(0);
                if (actionPose != null) {
                    com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[物品渲染] ✅ 使用动作的第一帧姿态: {}", actionName);
                    return actionPose;
                } else {
                    com.lanye.dolladdon.PlayerDollAddon.LOGGER.warn("[物品渲染] ⚠️ 动作的第一帧姿态为空: {}", actionName);
                }
            } else {
                com.lanye.dolladdon.PlayerDollAddon.LOGGER.warn("[物品渲染] ⚠️ 动作不存在: {}", actionName);
            }
        }
        
        // 优先使用姿态名称（如果保存了）
        if (entityTag.contains("PoseName", net.minecraft.nbt.Tag.TAG_STRING)) {
            String poseName = entityTag.getString("PoseName");
            com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[物品渲染] 检测到姿态名称: {}", poseName);
            DollPose pose = PoseActionManager.getPose(poseName);
            if (pose != null) {
                com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[物品渲染] ✅ 使用姿态名称恢复姿态: {}", poseName);
                return pose;
            } else {
                com.lanye.dolladdon.PlayerDollAddon.LOGGER.warn("[物品渲染] ⚠️ 姿态不存在: {}", poseName);
            }
        }
        
        // 如果没有姿态名称，尝试使用姿态索引（向后兼容）
        if (entityTag.contains("PoseIndex", net.minecraft.nbt.Tag.TAG_INT)) {
            int poseIndex = entityTag.getInt("PoseIndex");
            com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[物品渲染] 检测到姿态索引: {}", poseIndex);
            if (poseIndex >= 0) {
                java.util.List<String> poseNames = new java.util.ArrayList<>();
                java.util.Map<String, DollPose> allPoses = PoseActionManager.getAllPoses();
                poseNames.addAll(allPoses.keySet());
                poseNames.sort(String::compareTo);
                
                if (poseIndex < poseNames.size()) {
                    String poseName = poseNames.get(poseIndex);
                    DollPose pose = PoseActionManager.getPose(poseName);
                    if (pose != null) {
                        com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[物品渲染] ✅ 通过索引恢复姿态: {} (索引: {})", poseName, poseIndex);
                        return pose;
                    } else {
                        com.lanye.dolladdon.PlayerDollAddon.LOGGER.warn("[物品渲染] ⚠️ 通过索引找到的姿态不存在: {} (索引: {})", poseName, poseIndex);
                    }
                } else {
                    com.lanye.dolladdon.PlayerDollAddon.LOGGER.warn("[物品渲染] ⚠️ 姿态索引超出范围: {} (总数: {})", poseIndex, poseNames.size());
                }
            } else {
                com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[物品渲染] 姿态索引无效: {}", poseIndex);
            }
        }
        
        // 默认返回standing姿态
        com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[物品渲染] 未找到有效的姿态信息，使用默认姿态");
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
}

