package com.lanye.dolladdon.client.render;

import com.lanye.dolladdon.entity.PlayerDollEntity;
import com.lanye.dolladdon.util.PlayerSkinUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 玩家玩偶实体渲染器
 * 使用 PlayerModel 渲染玩家模型
 * 根据玩家模型类型（粗手臂/细手臂）动态选择模型
 */
public class PlayerDollRenderer extends EntityRenderer<PlayerDollEntity> {
    private final PlayerModel<Player> playerModelDefault;  // 粗手臂（Steve）
    private final PlayerModel<Player> playerModelSlim;     // 细手臂（Alex）
    
    public PlayerDollRenderer(EntityRendererProvider.Context context) {
        super(context);
        // 创建两个模型：粗手臂和细手臂
        // 粗手臂模型使用 ModelLayers.PLAYER
        this.playerModelDefault = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
        // 细手臂模型使用 ModelLayers.PLAYER_SLIM
        this.playerModelSlim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }
    
    @Override
    public void render(PlayerDollEntity entity, float entityYaw, float partialTick, 
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
        
        // 获取玩家UUID
        UUID playerUUID = entity.getPlayerUUID();
        
        // 判断是否是固定模型（Steve或Alex）
        // Steve和Alex是固定模型，直接通过UUID判断，使用固定的皮肤和模型
        boolean isAlexModel = false;
        ResourceLocation skinLocation;
        
        if (playerUUID != null) {
            if (playerUUID.equals(PlayerSkinUtil.STEVE_UUID)) {
                // Steve固定模型：粗手臂 + Steve默认皮肤
                isAlexModel = false;
                skinLocation = PlayerSkinUtil.getSteveSkin();
                com.lanye.dolladdon.PlayerDollAddon.LOGGER.info("[PlayerDollRenderer] 使用固定Steve模型（粗手臂）");
            } else if (playerUUID.equals(PlayerSkinUtil.ALEX_UUID)) {
                // Alex固定模型：细手臂 + Alex默认皮肤
                isAlexModel = true;
                skinLocation = PlayerSkinUtil.getAlexSkin();
                com.lanye.dolladdon.PlayerDollAddon.LOGGER.info("[PlayerDollRenderer] 使用固定Alex模型（细手臂）");
            } else {
                // 其他UUID：使用动态判断
                String playerName = entity.getPlayerName();
                skinLocation = PlayerSkinUtil.getSkinLocation(playerUUID, playerName);
                isAlexModel = PlayerSkinUtil.isAlexModel(playerUUID, playerName);
            }
        } else {
            // UUID为null：使用Steve默认模型
            skinLocation = PlayerSkinUtil.getSteveSkin();
            isAlexModel = false;
        }
        
        // 选择模型
        PlayerModel<Player> playerModel = isAlexModel ? this.playerModelSlim : this.playerModelDefault;
        
        // 设置模型姿态（站立姿态）
        // 手动设置各个部分，避免调用 setupAnim(null, ...) 导致的 NullPointerException
        // 使用默认的站立姿态
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
    public ResourceLocation getTextureLocation(PlayerDollEntity entity) {
        UUID playerUUID = entity.getPlayerUUID();
        if (playerUUID != null) {
            if (playerUUID.equals(PlayerSkinUtil.STEVE_UUID)) {
                return PlayerSkinUtil.getSteveSkin();
            } else if (playerUUID.equals(PlayerSkinUtil.ALEX_UUID)) {
                return PlayerSkinUtil.getAlexSkin();
            }
        }
        // 其他情况使用默认方法
        return PlayerSkinUtil.getSkinLocation(playerUUID, entity.getPlayerName());
    }
}

