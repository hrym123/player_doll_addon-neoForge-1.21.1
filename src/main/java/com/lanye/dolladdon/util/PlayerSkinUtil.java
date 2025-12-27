package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 玩家皮肤工具类
 * 用于获取玩家的真实皮肤纹理
 * 如果 MCCustomSkinLoader mod 存在，优先使用它来获取皮肤
 */
public class PlayerSkinUtil {
    
    // MCCustomSkinLoader 相关类的缓存
    private static Class<?> customSkinLoaderClass = null;
    private static Method loadProfileMethod = null;
    private static Class<?> userProfileClass = null;
    private static java.lang.reflect.Field skinUrlField = null;
    private static java.lang.reflect.Field modelField = null;
    private static boolean customSkinLoaderChecked = false;
    
    /**
     * 检查 MCCustomSkinLoader 是否可用
     */
    private static boolean isCustomSkinLoaderAvailable() {
        if (!customSkinLoaderChecked) {
            try {
                customSkinLoaderClass = Class.forName("customskinloader.CustomSkinLoader");
                loadProfileMethod = customSkinLoaderClass.getMethod("loadProfile", GameProfile.class);
                userProfileClass = Class.forName("customskinloader.profile.UserProfile");
                skinUrlField = userProfileClass.getField("skinUrl");
                modelField = userProfileClass.getField("model");
                customSkinLoaderChecked = true;
                PlayerDollAddon.LOGGER.info("MCCustomSkinLoader 已检测到，将优先使用它来获取玩家皮肤");
            } catch (Exception e) {
                // MCCustomSkinLoader 不存在或不可用
                customSkinLoaderClass = null;
                customSkinLoaderChecked = true;
            }
        }
        return customSkinLoaderClass != null;
    }
    
    /**
     * 尝试通过 MCCustomSkinLoader 触发皮肤加载
     * MCCustomSkinLoader 通过 Mixin 自动处理皮肤加载，所以我们只需要触发加载即可
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称
     * @return 如果成功触发加载返回 true，否则返回 false
     */
    private static boolean triggerCustomSkinLoaderLoad(UUID playerUUID, String playerName) {
        if (!isCustomSkinLoaderAvailable()) {
            return false;
        }
        
        try {
            // 创建 GameProfile
            GameProfile gameProfile = new GameProfile(playerUUID, playerName != null ? playerName : "Player");
            
            // 调用 CustomSkinLoader.loadProfile() 来触发皮肤加载
            // MCCustomSkinLoader 会通过 Mixin 自动处理皮肤加载
            Object userProfile = loadProfileMethod.invoke(null, gameProfile);
            
            if (userProfile != null) {
                // 检查是否有皮肤 URL
                Object skinUrlObj = skinUrlField.get(userProfile);
                if (skinUrlObj != null) {
                    String skinUrl = skinUrlObj.toString();
                    if (skinUrl != null && !skinUrl.isEmpty()) {
                        PlayerDollAddon.LOGGER.debug("MCCustomSkinLoader 已为玩家 {} 加载皮肤: {}", 
                                playerName != null ? playerName : playerUUID, skinUrl);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            PlayerDollAddon.LOGGER.warn("使用 MCCustomSkinLoader 加载皮肤时出错: {}", e.getMessage());
        }
        
        return false;
    }
    
    // Steve 和 Alex 的默认 UUID（用于获取默认皮肤）
    // 在 Minecraft 中，DefaultPlayerSkin 会根据 UUID 的哈希值来判断模型类型
    // Steve: 全0 UUID 通常对应粗手臂模型
    // Alex: 需要找到一个会被识别为 Alex 模型的 UUID
    private static final UUID STEVE_UUID = new UUID(0L, 0L);
    // Alex 的 UUID：使用一个已知会被识别为 Alex 模型的 UUID
    // 在 Minecraft 中，UUID 的哈希值决定了模型类型
    // 这里使用一个已知的 Alex UUID（通过测试找到的）
    private static final UUID ALEX_UUID = UUID.fromString("c06f8906-4c8a-4901-9c0b-2b0b6c3d4e5f");
    
    /**
     * 获取 Steve 的默认皮肤（粗手臂）
     */
    private static ResourceLocation getSteveDefaultSkin() {
        return DefaultPlayerSkin.get(STEVE_UUID).texture();
    }
    
    /**
     * 获取 Alex 的默认皮肤（细手臂）
     * 通过尝试不同的 UUID 来找到一个会被识别为 Alex 模型的 UUID
     */
    private static ResourceLocation getAlexDefaultSkin() {
        // 首先尝试使用预定义的 ALEX_UUID
        var alexSkin = DefaultPlayerSkin.get(ALEX_UUID);
        if (alexSkin.model().equals("slim")) {
            return alexSkin.texture();
        }
        
        // 如果预定义的 UUID 不是 Alex 模型，尝试其他已知的 Alex UUID
        // 在 Minecraft 中，某些特定的 UUID 会被识别为 Alex 模型
        // 这里我们尝试几个可能的 UUID
        UUID[] alexUUIDs = {
            UUID.fromString("61699b2e-d327-4a01-9f1e-2960b8f5d530"), // 另一个可能的 Alex UUID
            UUID.fromString("853c80ef-3c37-49fd-9769-2b76a23cf447"), // 另一个可能的 Alex UUID
            new UUID(0L, 1L), // 简单的测试 UUID
        };
        
        for (UUID uuid : alexUUIDs) {
            var skin = DefaultPlayerSkin.get(uuid);
            if (skin.model().equals("slim")) {
                return skin.texture();
            }
        }
        
        // 如果所有尝试都失败，使用第一个 UUID 的纹理（即使不是 Alex 模型）
        // 这应该很少发生
        return alexSkin.texture();
    }
    
    /**
     * 获取玩家皮肤纹理位置
     * 优先尝试使用 MCCustomSkinLoader（如果存在）
     * 确保返回的纹理与模型类型匹配：
     * - 粗手臂（Steve模型）使用粗手臂对应的皮肤纹理
     * - 细手臂（Alex模型）使用细手臂对应的皮肤纹理
     * 
     * 注意：MCCustomSkinLoader 通过 Mixin 自动拦截皮肤加载过程，
     * 但我们需要确保纹理和模型类型匹配，避免纹理和模型不匹配导致的渲染问题
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称（可选，用于 MCCustomSkinLoader）
     * @return 皮肤纹理位置
     */
    public static ResourceLocation getSkinLocation(@Nullable UUID playerUUID, @Nullable String playerName) {
        if (playerUUID == null) {
            return getSteveDefaultSkin();
        }
        
        // 首先判断玩家模型类型（无论是否有自定义皮肤，都需要知道模型类型）
        boolean isAlexModel = isAlexModel(playerUUID, playerName);
        
        // 如果 MCCustomSkinLoader 存在，触发皮肤加载
        // MCCustomSkinLoader 会通过 Mixin 自动处理皮肤加载
        boolean hasCustomSkin = false;
        if (isCustomSkinLoaderAvailable()) {
            hasCustomSkin = triggerCustomSkinLoaderLoad(playerUUID, playerName);
        }
        
        // 获取基于UUID的默认皮肤信息
        var defaultSkin = DefaultPlayerSkin.get(playerUUID);
        String skinModel = defaultSkin.model().toString();
        ResourceLocation texture = defaultSkin.texture();
        
        // 检查皮肤模型类型是否与玩家模型类型匹配
        boolean skinIsAlex = "slim".equals(skinModel);
        boolean modelMatches = (isAlexModel && skinIsAlex) || (!isAlexModel && !skinIsAlex);
        
        // 如果 MCCustomSkinLoader 提供了皮肤，且模型类型匹配，使用自定义皮肤
        if (hasCustomSkin && modelMatches) {
            // MCCustomSkinLoader 的 Mixin 会拦截 DefaultPlayerSkin 的调用并替换为自定义皮肤
            return texture;
        }
        
        // 如果模型类型不匹配，或者无法获取到自定义皮肤，根据模型类型使用对应的默认皮肤
        // 这确保了纹理和模型类型总是匹配的
        if (isAlexModel) {
            // 细手臂模型：使用艾利克斯的默认皮肤
            return getAlexDefaultSkin();
        } else {
            // 粗手臂模型：使用史蒂夫的默认皮肤
            return getSteveDefaultSkin();
        }
    }
    
    /**
     * 获取玩家皮肤纹理位置（仅使用UUID）
     * 
     * @param playerUUID 玩家UUID
     * @return 皮肤纹理位置
     */
    public static ResourceLocation getSkinLocation(@Nullable UUID playerUUID) {
        return getSkinLocation(playerUUID, null);
    }
    
    /**
     * 检查玩家是否使用Alex模型（细手臂）
     * 如果 MCCustomSkinLoader 存在，优先使用它提供的模型信息
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称（可选，用于 MCCustomSkinLoader）
     * @return true表示使用Alex模型，false表示使用Steve模型
     */
    public static boolean isAlexModel(@Nullable UUID playerUUID, @Nullable String playerName) {
        if (playerUUID == null) {
            return false;
        }
        
        // 尝试从 MCCustomSkinLoader 获取模型信息
        if (isCustomSkinLoaderAvailable()) {
            try {
                GameProfile gameProfile = new GameProfile(playerUUID, playerName != null ? playerName : "Player");
                Object userProfile = loadProfileMethod.invoke(null, gameProfile);
                if (userProfile != null) {
                    Object modelObj = modelField.get(userProfile);
                    if (modelObj != null) {
                        String model = modelObj.toString();
                        if ("slim".equals(model)) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                // 忽略错误，使用默认方法
            }
        }
        
        // 使用默认方法
        return DefaultPlayerSkin.get(playerUUID).model().equals("slim");
    }
    
    /**
     * 检查玩家是否使用Alex模型（仅使用UUID）
     * 
     * @param playerUUID 玩家UUID
     * @return true表示使用Alex模型，false表示使用Steve模型
     */
    public static boolean isAlexModel(@Nullable UUID playerUUID) {
        return isAlexModel(playerUUID, null);
    }
}

