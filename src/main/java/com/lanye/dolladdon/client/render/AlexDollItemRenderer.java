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
        
        // 根据显示上下文调整缩放和位置
        if (transformType == ItemDisplayContext.GUI) {
            poseStack.translate(0.5, 0.5, 0.0);
            poseStack.scale(0.625F, 0.625F, 0.625F);
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        } else if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || 
                   transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(0.5F, 0.5F, 0.5F);
        } else if (transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || 
                   transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            poseStack.translate(0.5, 1.0, 0.5);
            poseStack.scale(0.375F, 0.375F, 0.375F);
        } else {
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
        
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        
        // 固定使用Alex默认皮肤
        ResourceLocation skinLocation = PlayerSkinUtil.getAlexSkin();
        
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

