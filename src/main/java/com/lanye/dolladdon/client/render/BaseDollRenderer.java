package com.lanye.dolladdon.client.render;

import com.lanye.dolladdon.entity.BaseDollEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 玩偶实体渲染器基类
 * 提供所有玩偶实体渲染器的共同功能
 */
public abstract class BaseDollRenderer<T extends BaseDollEntity> extends EntityRenderer<T> {
    protected final PlayerModel<Player> playerModel;
    
    protected BaseDollRenderer(EntityRendererProvider.Context context, PlayerModel<Player> playerModel) {
        super(context);
        this.playerModel = playerModel;
    }
    
    /**
     * 获取皮肤资源位置
     * @param entity 实体
     * @return 皮肤资源位置
     */
    protected abstract ResourceLocation getSkinLocation(T entity);
    
    @Override
    public void render(T entity, float entityYaw, float partialTick, 
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
        
        // 获取皮肤位置（由子类实现）
        ResourceLocation skinLocation = getSkinLocation(entity);
        
        // 设置模型姿态（站立姿态）
        playerModel.head.setRotation(0.0F, 0.0F, 0.0F);
        playerModel.hat.setRotation(0.0F, 0.0F, 0.0F);
        playerModel.body.setRotation(0.0F, 0.0F, 0.0F);
        playerModel.rightArm.setRotation(-0.6981317F, 0.0F, 0.0F); // 约 -40 度
        playerModel.leftArm.setRotation(0.6981317F, 0.0F, 0.0F);   // 约 40 度
        playerModel.rightLeg.setRotation(0.0F, 0.0F, 0.0F);
        playerModel.leftLeg.setRotation(0.0F, 0.0F, 0.0F);
        
        // 渲染玩家模型
        var vertexConsumer = bufferSource.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(skinLocation));
        playerModel.renderToBuffer(poseStack, vertexConsumer, packedLight, 
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
    
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return getSkinLocation(entity);
    }
}

