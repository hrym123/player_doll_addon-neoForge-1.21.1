package com.lanye.dolladdon.client.render;

import com.lanye.dolladdon.entity.PlayerDollEntity;
import com.lanye.dolladdon.init.ModDataComponents;
import com.lanye.dolladdon.util.PlayerSkinUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 玩家玩偶物品渲染器
 * 在物品栏中渲染玩家模型
 * 根据玩家模型类型（粗手臂/细手臂）动态选择模型
 */
public class PlayerDollItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final PlayerModel<net.minecraft.world.entity.player.Player> playerModelDefault;  // 粗手臂（Steve）
    private final PlayerModel<net.minecraft.world.entity.player.Player> playerModelSlim;     // 细手臂（Alex）
    
    public PlayerDollItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
        // 创建两个模型：粗手臂和细手臂
        // 粗手臂模型使用 ModelLayers.PLAYER
        this.playerModelDefault = new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER), false);
        // 细手臂模型使用 ModelLayers.PLAYER_SLIM
        this.playerModelSlim = new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_SLIM), true);
    }
    
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        
        // 根据显示上下文调整缩放和位置
        if (transformType == ItemDisplayContext.GUI) {
            // GUI 中：缩小并居中
            poseStack.translate(0.5, 0.5, 0.0);
            poseStack.scale(0.625F, 0.625F, 0.625F);
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F)); // 旋转以便更好地查看
        } else if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || 
                   transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            // 第一人称：调整位置和大小
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(0.5F, 0.5F, 0.5F);
        } else if (transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || 
                   transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            // 第三人称：调整位置和大小
            poseStack.translate(0.5, 1.0, 0.5);
            poseStack.scale(0.375F, 0.375F, 0.375F);
        } else {
            // 其他情况（地面、框架等）
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
        
        // 翻转模型
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        
        // 从物品获取玩家信息
        UUID playerUUID = getPlayerUUID(stack);
        String playerName = getPlayerName(stack);
        
        // 如果UUID为null，尝试使用当前玩家的信息作为回退
        // 这有助于在开发环境中正确显示皮肤，或者当物品没有设置玩家信息时
        if (playerUUID == null && Minecraft.getInstance().player != null) {
            playerUUID = Minecraft.getInstance().player.getUUID();
            if (playerName == null) {
                playerName = Minecraft.getInstance().player.getName().getString();
            }
        }
        
        // 获取皮肤位置（如果UUID仍为null，会返回Steve默认皮肤）
        ResourceLocation skinLocation = getSkinLocation(playerUUID, playerName);
        
        // 根据玩家模型类型选择使用粗手臂还是细手臂模型
        boolean isAlexModel = PlayerSkinUtil.isAlexModel(playerUUID, playerName);
        PlayerModel<net.minecraft.world.entity.player.Player> playerModel = isAlexModel ? this.playerModelSlim : this.playerModelDefault;
        
        // 记录使用的模型类型（用于调试）
        com.lanye.dolladdon.PlayerDollAddon.LOGGER.info("[PlayerDollItemRenderer] 渲染物品 - UUID: {}, 名称: {}, 使用模型: {}", 
                playerUUID, playerName, isAlexModel ? "Alex(细手臂)" : "Steve(粗手臂)");
        
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
        playerModel.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay);
        
        poseStack.popPose();
    }
    
    /**
     * 从物品获取玩家UUID
     */
    @Nullable
    private UUID getPlayerUUID(ItemStack stack) {
        var playerData = stack.get(ModDataComponents.PLAYER_DATA.get());
        if (playerData != null && playerData.contains("player_uuid")) {
            return playerData.getUUID("player_uuid");
        }
        return null;
    }
    
    /**
     * 从物品获取玩家名称
     */
    @Nullable
    private String getPlayerName(ItemStack stack) {
        var playerData = stack.get(ModDataComponents.PLAYER_DATA.get());
        if (playerData != null && playerData.contains("player_name")) {
            return playerData.getString("player_name");
        }
        return null;
    }
    
    /**
     * 获取玩家皮肤纹理
     */
    private ResourceLocation getSkinLocation(@Nullable UUID playerUUID, @Nullable String playerName) {
        return PlayerSkinUtil.getSkinLocation(playerUUID, playerName);
    }
}

