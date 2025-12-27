package com.lanye.dolladdon.base.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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
        
        // 设置模型姿态（站立姿态）
        playerModel.head.setRotation(0.0F, 0.0F, 0.0F);
        playerModel.hat.setRotation(0.0F, 0.0F, 0.0F);
        playerModel.body.setRotation(0.0F, 0.0F, 0.0F);
        playerModel.rightArm.setRotation(-0.6981317F, 0.0F, 0.0F);
        playerModel.leftArm.setRotation(0.6981317F, 0.0F, 0.0F);
        playerModel.rightLeg.setRotation(0.0F, 0.0F, 0.0F);
        playerModel.leftLeg.setRotation(0.0F, 0.0F, 0.0F);
        
        // 渲染玩家模型
        var vertexConsumer = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(skinLocation));
        playerModel.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay);
        
        poseStack.popPose();
    }
    
    /**
     * 根据显示上下文调整玩家模型的位置、缩放和旋转
     * 注意：玩家模型的原点在脚部（Y=0），模型高度约1.8，中心在Y=0.9处
     * 
     * @param poseStack 变换矩阵栈
     * @param transformType 显示上下文类型
     */
    protected void applyPlayerModelTransform(PoseStack poseStack, ItemDisplayContext transformType) {
        if (transformType == ItemDisplayContext.GUI) {
            // GUI 中：居中显示
            // 参考参考项目：先缩放，再移动到中心
            poseStack.scale(0.8F, 0.8F, 0.8F);  // 先缩放
            // 移动到物品槽中心并向上移动使模型居中
            // 合并translate(0.5, 1.5, 0.5)和后续的translate(0.2, 0.0, 0.0)
            // 考虑旋转-135度和scale(-1.0, -1.0, 1.0)的影响：
            // 1. scale(-1, -1, 1)后translate(0.2, 0, 0) → 在缩放前是translate(-0.2, 0, 0)
            // 2. 旋转-135度后，需要逆旋转135度：(-0.2, 0, 0) → (0.2*√2/2, 0, -0.2*√2/2) ≈ (0.141, 0, -0.141)
            // 合并后：translate(0.5 + 0.141, 1.5 + 0, 0.5 + (-0.141)) = translate(0.641, 1.5, 0.359)
            poseStack.translate(0.641, 1.5, 0.359);
            // 逆时针旋转135度（Y轴逆时针为负值）
            poseStack.mulPose(Axis.YP.rotationDegrees(-135.0F));
        } else if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || 
                   transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            // 第一人称：左手和右手使用相同的初始变换
            // 合并所有平移：translate(0.5, 1.6, 0.5) + translate(0.5/0.5, 0.0/0.5, 0.5/(-0.5)) = translate(1.5, 1.6, -0.5)
            // 向左移动1.5：X坐标从1.5改为0.0，向前移动1：Z坐标从-0.5改为0.5
            poseStack.translate(0.5, 1.25, 0.5);
            // 缩放并前后反转（合并两次scale：0.5 * 1.0 = 0.5，Z轴反转）
            poseStack.scale(0.5F, 0.5F, -0.5F);
            // 旋转

            if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                // 第一人称左手
                poseStack.mulPose(Axis.YP.rotationDegrees(-15.0F));
            } else{
                // 第一人称右手
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F));
            }
        } else if (transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || 
                   transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            // 第三人称：调整位置和大小
            // 合并所有Y轴平移：1.0 + 0.675 - 0.5 - 0.05 = 1.125，再向上移动0.5后向下移动0.05 = 1.075
            poseStack.translate(0.5, 1.075, 0.5);
            // 缩放并前后反转（合并两次scale：0.375 * 1.0 = 0.375，Z轴反转）
            poseStack.scale(0.375F, 0.375F, -0.375F);
        } else {
            // 其他情况（地面、框架等）
            // 移动到中心并向上移动使模型居中（合并两次translate）
            poseStack.translate(0.5, 1.4, 0.5);
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
        
        // 翻转模型（玩家模型需要翻转才能正确显示）
        // 原因：Minecraft的玩家模型在渲染时，默认朝向与物品渲染的坐标系不匹配
        // 实体渲染器（SteveDollRenderer）也使用 scale(-1.0F, -1.0F, 1.0F) 来翻转模型
        // 这是Minecraft渲染系统的约定，翻转后模型才能正确显示（正面朝向玩家）
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        
    }
}

