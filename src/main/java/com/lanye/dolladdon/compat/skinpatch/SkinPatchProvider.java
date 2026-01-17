package com.lanye.dolladdon.compat.skinpatch;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 皮肤补丁提供器
 * 
 * 提供统一的皮肤获取接口，自动应用补丁修复
 * 这是推荐使用的皮肤获取方式，它会自动处理各种兼容性问题
 * 
 * 此提供器完全基于 SkinPatchCompat，不依赖 PlayerSkinUtil
 */
public class SkinPatchProvider {
    
    /**
     * 获取玩家皮肤纹理位置（带补丁修复）
     * 
     * 这是推荐的皮肤获取方法，它会：
     * 1. 自动检测并使用 CustomSkinLoader（如果可用）
     * 2. 确保皮肤纹理与模型类型匹配
     * 3. 提供回退机制，确保始终返回有效的纹理
     * 4. 修复常见的皮肤加载问题
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称
     * @return 皮肤纹理位置（永远不会返回 null）
     */
    @Nullable
    public static ResourceLocation getSkinLocation(@Nullable UUID playerUUID, @Nullable String playerName) {
        return SkinPatchCompat.getPatchedSkinLocation(playerUUID, playerName);
    }
    
    /**
     * 获取玩家皮肤纹理位置（仅使用UUID，带补丁修复）
     * 
     * @param playerUUID 玩家UUID
     * @return 皮肤纹理位置（永远不会返回 null）
     */
    @Nullable
    public static ResourceLocation getSkinLocation(@Nullable UUID playerUUID) {
        return getSkinLocation(playerUUID, null);
    }
    
    /**
     * 检查玩家是否使用 Alex 模型（带补丁修复）
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称
     * @return true 表示 Alex 模型（细手臂），false 表示 Steve 模型（粗手臂）
     */
    public static boolean isAlexModel(@Nullable UUID playerUUID, @Nullable String playerName) {
        return SkinPatchCompat.getPatchedModelType(playerUUID, playerName);
    }
    
    /**
     * 检查玩家是否使用 Alex 模型（仅使用UUID，带补丁修复）
     * 
     * @param playerUUID 玩家UUID
     * @return true 表示 Alex 模型，false 表示 Steve 模型
     */
    public static boolean isAlexModel(@Nullable UUID playerUUID) {
        return isAlexModel(playerUUID, null);
    }
    
    /**
     * 验证并修复皮肤纹理路径
     * 
     * @param texture 原始纹理位置
     * @return 修复后的纹理位置，如果无效则返回默认Steve皮肤
     */
    @Nullable
    public static ResourceLocation validateAndFixTexture(@Nullable ResourceLocation texture) {
        if (texture == null) {
            return SkinPatchCompat.getSteveSkin();
        }
        
        // 验证纹理是否有效
        if (!SkinPatchCompat.isValidSkinTexture(texture)) {
            return SkinPatchCompat.getSteveSkin();
        }
        
        // 修复纹理路径
        return SkinPatchCompat.fixSkinTexturePath(texture);
    }
    
    /**
     * 检查 CustomSkinLoader 是否可用
     * 
     * @return true 如果可用，false 否则
     */
    public static boolean isCustomSkinLoaderAvailable() {
        return SkinPatchCompat.isCustomSkinLoaderAvailable();
    }
    
    /**
     * 获取默认 Steve 皮肤
     * 
     * @return Steve 默认皮肤纹理位置
     */
    public static ResourceLocation getSteveSkin() {
        return SkinPatchCompat.getSteveSkin();
    }
    
    /**
     * 获取默认 Alex 皮肤
     * 
     * @return Alex 默认皮肤纹理位置
     */
    public static ResourceLocation getAlexSkin() {
        return SkinPatchCompat.getAlexSkin();
    }
    
    /**
     * 获取 Steve 模型信息
     * 
     * @return Steve 模型信息
     */
    public static SkinPatchCompat.DefaultModelInfo getSteveModel() {
        return SkinPatchCompat.getSteveModel();
    }
    
    /**
     * 获取 Alex 模型信息
     * 
     * @return Alex 模型信息
     */
    public static SkinPatchCompat.DefaultModelInfo getAlexModel() {
        return SkinPatchCompat.getAlexModel();
    }
    
    /**
     * 获取 Steve UUID
     * 
     * @return Steve 的固定 UUID
     */
    public static UUID getSteveUUID() {
        return SkinPatchCompat.STEVE_UUID;
    }
    
    /**
     * 获取 Alex UUID
     * 
     * @return Alex 的固定 UUID
     */
    public static UUID getAlexUUID() {
        return SkinPatchCompat.ALEX_UUID;
    }
}
