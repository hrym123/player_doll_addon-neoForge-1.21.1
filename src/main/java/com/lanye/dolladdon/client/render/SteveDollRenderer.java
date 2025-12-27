package com.lanye.dolladdon.client.render;

import com.lanye.dolladdon.entity.SteveDollEntity;
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
 * 史蒂夫玩偶实体渲染器
 * 固定使用Steve模型（粗手臂）和Steve默认皮肤
 */
public class SteveDollRenderer extends EntityRenderer<SteveDollEntity> {
    private final PlayerModel<Player> playerModelDefault;  // 粗手臂（Steve）
    
    public SteveDollRenderer(EntityRendererProvider.Context context) {
        super(context);
        // 创建粗手臂模型
        this.playerModelDefault = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
    }
    
    @Override
    public void render(SteveDollEntity entity, float entityYaw, float partialTick, 
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
        
        // 固定使用Steve默认皮肤
        ResourceLocation skinLocation = PlayerSkinUtil.getSteveSkin();
        
        // 设置模型姿态（站立姿态）
        playerModelDefault.head.setRotation(0.0F, 0.0F, 0.0F);
        playerModelDefault.hat.setRotation(0.0F, 0.0F, 0.0F);
        playerModelDefault.body.setRotation(0.0F, 0.0F, 0.0F);
        playerModelDefault.rightArm.setRotation(-0.6981317F, 0.0F, 0.0F); // 约 -40 度
        playerModelDefault.leftArm.setRotation(0.6981317F, 0.0F, 0.0F);   // 约 40 度
        playerModelDefault.rightLeg.setRotation(0.0F, 0.0F, 0.0F);
        playerModelDefault.leftLeg.setRotation(0.0F, 0.0F, 0.0F);
        
        // 渲染玩家模型
        var vertexConsumer = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(skinLocation));
        playerModelDefault.renderToBuffer(poseStack, vertexConsumer, packedLight, 
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
    
    @Override
    public ResourceLocation getTextureLocation(SteveDollEntity entity) {
        return PlayerSkinUtil.getSteveSkin();
    }
}

