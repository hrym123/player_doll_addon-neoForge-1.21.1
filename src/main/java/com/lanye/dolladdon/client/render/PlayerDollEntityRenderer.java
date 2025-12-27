package com.lanye.dolladdon.client.render;

import com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity;
import com.lanye.dolladdon.util.PlayerDollUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * 自定义的玩偶实体渲染器
 * 支持渲染玩家模型或方块模型
 */
public class PlayerDollEntityRenderer extends EntityRenderer<DollEntity> {
    private static final ResourceLocation EMPTY = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/empty.png");
    
    private final BlockRenderDispatcher blockRenderer;
    private final PlayerModel<Player> playerModel;

    public PlayerDollEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
        this.playerModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false);
    }

    @Override
    public void render(DollEntity dollEntity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // 检查是否有玩家 UUID
        UUID playerUUID = PlayerDollUtil.getPlayerUUID(dollEntity);
        
        // 如果实体没有玩家信息，尝试从物品获取（当实体从物品恢复时）
        if (playerUUID == null && dollEntity.getPickResult() != null) {
            playerUUID = PlayerDollUtil.getPlayerUUID(dollEntity.getPickResult());
        }
        
        // 如果有玩家 UUID 且 BlockState 是空气，渲染玩家模型
        // 如果 BlockState 不是空气，说明是方块玩偶，渲染方块模型
        if (playerUUID != null && dollEntity.getDisplayBlockState().isAir()) {
            // 渲染玩家模型
            renderPlayerModel(dollEntity, playerUUID, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        } else {
            // 渲染方块模型（默认行为）
            renderBlockModel(dollEntity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        }
    }
    
    /**
     * 渲染玩家模型
     */
    private void renderPlayerModel(DollEntity dollEntity, UUID playerUUID, float entityYaw, float partialTick, 
                                   PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        
        // 应用位移变换
        Vector3f translation = dollEntity.getDisplayTranslation();
        poseStack.translate(translation.x, translation.y, translation.z);
        
        // 应用旋转
        Entity vehicle = dollEntity.getVehicle();
        if (vehicle != null) {
            float vehicleYaw = Mth.lerp(partialTick, vehicle.yRotO, vehicle.getYRot());
            poseStack.mulPose(Axis.YP.rotationDegrees(-vehicleYaw));
        } else {
            entityYaw = Mth.lerp(partialTick, dollEntity.yRotO, dollEntity.getYRot());
            poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        }
        float pitchRadians = Mth.lerp(partialTick, dollEntity.xRotO, dollEntity.getXRot());
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchRadians));
        
        // 应用缩放
        Vector3f scale = dollEntity.getDisplayScale();
        poseStack.scale(scale.x, scale.y, scale.z);
        
        // 弹跳效果
        long time = dollEntity.getBounceTime() - System.currentTimeMillis();
        if (time > 0) {
            float dampingFactor = 0.6f;
            float frequency = 6f;
            float bounceStrength = 0.6f;
            float bounceProgress = 1.0f - ((float) time / 500.0f);
            float dampedAmplitude = (float) Math.exp(-dampingFactor * bounceProgress * 8.0f);
            float compressionIntensity = dampedAmplitude * bounceStrength;
            float groundContact = (float) Math.max(0, -Math.sin(bounceProgress * frequency * Math.PI));
            float scaleY = 1.0f - groundContact * compressionIntensity;
            float scaleXZ = 1.0f + groundContact * compressionIntensity;
            poseStack.scale(scaleXZ, scaleY, scaleXZ);
        }
        
        // 获取玩家皮肤
        // 使用 DefaultPlayerSkin 获取基于 UUID 的默认皮肤
        ResourceLocation skinLocation = DefaultPlayerSkin.get(playerUUID).texture();
        
        // 注意：如果需要获取在线玩家的自定义皮肤，需要额外的网络请求
        // 这里先使用基于 UUID 的默认皮肤，它已经能够区分 Steve 和 Alex 模型
        
        // 渲染玩家模型
        poseStack.translate(0, 1.5, 0); // 调整位置，使玩家模型居中
        poseStack.scale(-1, -1, 1); // 翻转模型
        
        // 设置模型姿态（站立姿态）
        this.playerModel.setupAnim(null, 0, 0, 0, 0, 0);
        
        // 渲染玩家模型
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(skinLocation));
        this.playerModel.renderToBuffer(poseStack, vertexConsumer, packedLight, 
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        
        poseStack.popPose();
        super.render(dollEntity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
    
    /**
     * 渲染方块模型（默认行为）
     */
    private void renderBlockModel(DollEntity dollEntity, float entityYaw, float partialTick,
                                  PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        BlockState blockState = dollEntity.getDisplayBlockState();
        if (blockState == null || blockState.isAir()) {
            return;
        }

        poseStack.pushPose();

        // 应用位移变换
        Vector3f translation = dollEntity.getDisplayTranslation();
        poseStack.translate(translation.x, translation.y, translation.z);

        // 应用 Y 轴旋转
        Entity vehicle = dollEntity.getVehicle();
        if (vehicle != null) {
            float vehicleYaw = Mth.lerp(partialTick, vehicle.yRotO, vehicle.getYRot());
            poseStack.mulPose(Axis.YP.rotationDegrees(-vehicleYaw));
        } else {
            entityYaw = Mth.lerp(partialTick, dollEntity.yRotO, dollEntity.getYRot());
            poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
        }
        float pitchRadians = Mth.lerp(partialTick, dollEntity.xRotO, dollEntity.getXRot());
        poseStack.mulPose(Axis.XP.rotationDegrees(pitchRadians));

        // 应用缩放变换
        Vector3f scale = dollEntity.getDisplayScale();
        poseStack.scale(scale.x, scale.y, scale.z);

        // 弹跳效果
        long time = dollEntity.getBounceTime() - System.currentTimeMillis();
        if (time > 0) {
            float dampingFactor = 0.6f;
            float frequency = 6f;
            float bounceStrength = 0.6f;
            float bounceProgress = 1.0f - ((float) time / 500.0f);
            float dampedAmplitude = (float) Math.exp(-dampingFactor * bounceProgress * 8.0f);
            float compressionIntensity = dampedAmplitude * bounceStrength;
            float groundContact = (float) Math.max(0, -Math.sin(bounceProgress * frequency * Math.PI));
            float scaleY = 1.0f - groundContact * compressionIntensity;
            float scaleXZ = 1.0f + groundContact * compressionIntensity;
            poseStack.scale(scaleXZ, scaleY, scaleXZ);
        }

        // 将方块中心对齐到实体位置
        poseStack.translate(-0.5, 0, -0.5);

        // 渲染方块
        Level level = dollEntity.level();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());
        blockRenderer.renderBatched(blockState, dollEntity.blockPosition(), level, poseStack, buffer, 
                false, level.random, ModelData.EMPTY, RenderType.cutout());

        poseStack.popPose();
        super.render(dollEntity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DollEntity dollEntity) {
        UUID playerUUID = PlayerDollUtil.getPlayerUUID(dollEntity);
        if (playerUUID != null) {
            return DefaultPlayerSkin.get(playerUUID).texture();
        }
        return EMPTY;
    }
}

