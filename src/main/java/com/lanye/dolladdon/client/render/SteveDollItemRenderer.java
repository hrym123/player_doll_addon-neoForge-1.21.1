package com.lanye.dolladdon.client.render;

import com.lanye.dolladdon.util.PlayerSkinUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 史蒂夫玩偶物品渲染器
 * 固定使用Steve模型（粗手臂）和Steve默认皮肤
 */
public class SteveDollItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final PlayerModel<net.minecraft.world.entity.player.Player> playerModelDefault;  // 粗手臂（Steve）
    
    public SteveDollItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
        // 创建粗手臂模型
        this.playerModelDefault = new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER), false);
    }
    
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        
        // 添加调试日志（使用 INFO 级别以确保能看到）
        com.lanye.dolladdon.PlayerDollAddon.LOGGER.info("[SteveDollItemRenderer] 渲染物品 - 显示上下文: {}", transformType);
        
        // 根据显示上下文调整缩放和位置
        // 注意：玩家模型的原点在脚部（Y=0），模型高度约1.8，中心在Y=0.9处
        // 参考实体渲染器：translate(0.0, 1.5, 0.0) 使模型底部对齐实体位置
        // 参考参考项目：先缩放，再移动到中心（但参考项目渲染的是方块物品，原点在中心）
        if (transformType == ItemDisplayContext.GUI) {
            // GUI 中：居中显示
            // 参考参考项目：先缩放，再移动到中心
            poseStack.scale(0.8F, 0.8F, 0.8F);  // 先缩放
            poseStack.translate(0.5, 0.5, 0.5);  // 移动到物品槽中心（参考参考项目）
            // 玩家模型原点在脚部（Y=0），向上移动1
            poseStack.translate(0.0, 1.0, 0.0);
            // 逆时针旋转135度（Y轴逆时针为负值）
            poseStack.mulPose(Axis.YP.rotationDegrees(-135.0F));
        } else if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || 
                   transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            // 第一人称：调整位置和大小
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            // 玩家模型原点在脚部，向上移动使模型居中
            // 缩放0.5后，模型高度0.9，中心在0.45，需要向上移动1.1使中心对齐（在缩放后的坐标系中）
            poseStack.translate(0.0, 1.1, 0.0);
            // 前后反转（Z轴反转）
            poseStack.scale(1.0F, 1.0F, -1.0F);
            // 相对镜头向前移动0.5（Z轴正方向）
            poseStack.translate(0.0, 0.0, 0.5);
            // 顺时针旋转15度（Y轴顺时针为正值）
            poseStack.mulPose(Axis.YP.rotationDegrees(15.0F));
            // 向右移动0.5（X轴正方向，在翻转模型前）
            poseStack.translate(0.5, 0.0, 0.0);
        } else if (transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || 
                   transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            // 第三人称：调整位置和大小
            poseStack.translate(0.5, 1.0, 0.5);
            poseStack.scale(0.375F, 0.375F, 0.375F);
            // 玩家模型原点在脚部，向上移动使模型居中
            // 缩放0.375后，模型高度0.675，中心在0.3375，需要向上移动0.675使中心对齐（在缩放后的坐标系中）
            poseStack.translate(0.0, 0.675, 0.0);
            // 向下移动0.5（Y轴负方向）
            poseStack.translate(0.0, -0.5, 0.0);
            // 再向下移动0.05（Y轴负方向）
            poseStack.translate(0.0, -0.05, 0.0);
            // 前后反转（Z轴反转）
            poseStack.scale(1.0F, 1.0F, -1.0F);
        } else {
            // 其他情况（地面、框架等）
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            // 玩家模型原点在脚部，向上移动使模型居中
            poseStack.translate(0.0, 0.9, 0.0);
        }
        
        // 翻转模型（玩家模型需要翻转才能正确显示）
        // 原因：Minecraft的玩家模型在渲染时，默认朝向与物品渲染的坐标系不匹配
        // 实体渲染器（SteveDollRenderer）也使用 scale(-1.0F, -1.0F, 1.0F) 来翻转模型
        // 这是Minecraft渲染系统的约定，翻转后模型才能正确显示（正面朝向玩家）
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        
        // 在GUI模式下，翻转后向右移动0.2（X轴正方向）
        if (transformType == ItemDisplayContext.GUI) {
            poseStack.translate(0.2, 0.0, 0.0);
        }
        
        // 固定使用Steve默认皮肤
        ResourceLocation skinLocation = PlayerSkinUtil.getSteveSkin();
        com.lanye.dolladdon.PlayerDollAddon.LOGGER.info("[SteveDollItemRenderer] 使用皮肤: {}", skinLocation);
        
        // 设置模型姿态（站立姿态）
        playerModelDefault.head.setRotation(0.0F, 0.0F, 0.0F);
        playerModelDefault.hat.setRotation(0.0F, 0.0F, 0.0F);
        playerModelDefault.body.setRotation(0.0F, 0.0F, 0.0F);
        playerModelDefault.rightArm.setRotation(-0.6981317F, 0.0F, 0.0F);
        playerModelDefault.leftArm.setRotation(0.6981317F, 0.0F, 0.0F);
        playerModelDefault.rightLeg.setRotation(0.0F, 0.0F, 0.0F);
        playerModelDefault.leftLeg.setRotation(0.0F, 0.0F, 0.0F);
        
        // 渲染玩家模型
        var vertexConsumer = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(skinLocation));
        playerModelDefault.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay);
        
        poseStack.popPose();
    }
}

