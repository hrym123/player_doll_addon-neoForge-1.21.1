package com.lanye.dolladdon.client.render;

import com.lanye.dolladdon.util.PlayerSkinUtil;
import com.mojang.blaze3d.vertex.PoseStack;
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
 * 艾利克斯玩偶物品渲染器
 * 固定使用Alex模型（细手臂）和Alex默认皮肤
 */
public class AlexDollItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final PlayerModel<net.minecraft.world.entity.player.Player> playerModelSlim;     // 细手臂（Alex）
    
    public AlexDollItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
        // 创建细手臂模型
        this.playerModelSlim = new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }
    
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        
        // 添加调试日志（使用 INFO 级别以确保能看到）
        com.lanye.dolladdon.PlayerDollAddon.LOGGER.info("[AlexDollItemRenderer] 渲染物品 - 显示上下文: {}", transformType);
        
        // 根据显示上下文调整模型的位置、缩放和旋转
        PlayerDollRenderHelper.applyPlayerModelTransform(poseStack, transformType);
        
        // 固定使用Alex默认皮肤
        ResourceLocation skinLocation = PlayerSkinUtil.getAlexSkin();
        com.lanye.dolladdon.PlayerDollAddon.LOGGER.info("[AlexDollItemRenderer] 使用皮肤: {}", skinLocation);
        
        // 设置模型姿态（站立姿态）
        playerModelSlim.head.setRotation(0.0F, 0.0F, 0.0F);
        playerModelSlim.hat.setRotation(0.0F, 0.0F, 0.0F);
        playerModelSlim.body.setRotation(0.0F, 0.0F, 0.0F);
        playerModelSlim.rightArm.setRotation(-0.6981317F, 0.0F, 0.0F);
        playerModelSlim.leftArm.setRotation(0.6981317F, 0.0F, 0.0F);
        playerModelSlim.rightLeg.setRotation(0.0F, 0.0F, 0.0F);
        playerModelSlim.leftLeg.setRotation(0.0F, 0.0F, 0.0F);
        
        // 渲染玩家模型
        var vertexConsumer = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(skinLocation));
        playerModelSlim.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay);
        
        poseStack.popPose();
    }
}

