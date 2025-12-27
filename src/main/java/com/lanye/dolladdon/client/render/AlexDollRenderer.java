package com.lanye.dolladdon.client.render;

import com.lanye.dolladdon.entity.AlexDollEntity;
import com.lanye.dolladdon.util.PlayerSkinUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 艾利克斯玩偶实体渲染器
 * 固定使用Alex模型（细手臂）和Alex默认皮肤
 */
public class AlexDollRenderer extends EntityRenderer<AlexDollEntity> {
    private final PlayerModel<Player> playerModelSlim;     // 细手臂（Alex）
    
    public AlexDollRenderer(EntityRendererProvider.Context context) {
        super(context);
        // 创建细手臂模型
        this.playerModelSlim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }
    
    @Override
    public void render(AlexDollEntity entity, float entityYaw, float partialTick, 
                      PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        
        // 应用旋转
        float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        
        // 调整位置，使玩家模型底部对齐实体位置
        poseStack.translate(0.0, 1.5, 0.0);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        
        // 固定使用Alex默认皮肤
        ResourceLocation skinLocation = PlayerSkinUtil.getAlexSkin();
        
        // 设置模型姿态（站立姿态）
        playerModelSlim.head.setRotation(0.0F, 0.0F, 0.0F);
        playerModelSlim.hat.setRotation(0.0F, 0.0F, 0.0F);
        playerModelSlim.body.setRotation(0.0F, 0.0F, 0.0F);
        playerModelSlim.rightArm.setRotation(-0.6981317F, 0.0F, 0.0F); // 约 -40 度
        playerModelSlim.leftArm.setRotation(0.6981317F, 0.0F, 0.0F);   // 约 40 度
        playerModelSlim.rightLeg.setRotation(0.0F, 0.0F, 0.0F);
        playerModelSlim.leftLeg.setRotation(0.0F, 0.0F, 0.0F);
        
        // 渲染玩家模型
        var vertexConsumer = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(skinLocation));
        playerModelSlim.renderToBuffer(poseStack, vertexConsumer, packedLight, 
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
    
    @Override
    public ResourceLocation getTextureLocation(AlexDollEntity entity) {
        return PlayerSkinUtil.getAlexSkin();
    }
}

