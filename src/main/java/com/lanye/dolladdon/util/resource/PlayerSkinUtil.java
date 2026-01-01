package com.lanye.dolladdon.util.resource;

import com.lanye.dolladdon.PlayerDollAddon;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 玩家皮肤工具类
 * 用于获取玩家的真实皮肤纹理
 */
public class PlayerSkinUtil {
    
    // Steve 和 Alex 的固定 UUID（用于获取默认皮肤）
    public static final UUID STEVE_UUID = new UUID(0L, 0L);
    public static final UUID ALEX_UUID = new UUID(0L, 1L);
    
    /**
     * 获取 Steve 的默认皮肤（粗手臂）
     */
    public static Identifier getSteveSkin() {
        // Minecraft 1.20.1 中，Steve的默认皮肤路径是 minecraft:textures/entity/player/wide/steve.png
        return new Identifier("minecraft", "textures/entity/player/wide/steve.png");
    }
    
    /**
     * 获取 Alex 的默认皮肤（细手臂）
     */
    public static Identifier getAlexSkin() {
        // Minecraft 1.20.1 中，Alex的默认皮肤路径是 minecraft:textures/entity/player/slim/alex.png
        return new Identifier("minecraft", "textures/entity/player/slim/alex.png");
    }
    
    /**
     * 获取玩家皮肤纹理位置
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称（可选）
     * @return 皮肤纹理位置
     */
    public static Identifier getSkinLocation(@Nullable UUID playerUUID, @Nullable String playerName) {
        if (playerUUID == null) {
            return getSteveSkin();
        }
        
        // 检查是否是固定的默认模型UUID
        if (playerUUID.equals(STEVE_UUID)) {
            return getSteveSkin();
        }
        if (playerUUID.equals(ALEX_UUID)) {
            return getAlexSkin();
        }
        
        // 对于其他UUID，根据模型类型返回对应的默认皮肤
        // 如果无法确定，使用Steve默认皮肤作为回退
        boolean isAlex = isAlexModel(playerUUID, playerName);
        return isAlex ? getAlexSkin() : getSteveSkin();
    }
    
    /**
     * 获取玩家皮肤纹理位置（仅使用UUID）
     * 
     * @param playerUUID 玩家UUID
     * @return 皮肤纹理位置
     */
    public static Identifier getSkinLocation(@Nullable UUID playerUUID) {
        return getSkinLocation(playerUUID, null);
    }
    
    /**
     * 检查玩家是否使用Alex模型（细手臂）
     * 在Minecraft中，模型类型由UUID的哈希值决定：
     * - 如果 hashCode() % 2 == 0，使用粗手臂模型（Steve）
     * - 如果 hashCode() % 2 == 1，使用细手臂模型（Alex）
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称（可选，未使用）
     * @return true表示使用Alex模型，false表示使用Steve模型
     */
    public static boolean isAlexModel(@Nullable UUID playerUUID, @Nullable String playerName) {
        if (playerUUID == null) {
            return false;
        }
        
        // 检查是否是固定的默认模型UUID
        if (playerUUID.equals(STEVE_UUID)) {
            return false;
        }
        if (playerUUID.equals(ALEX_UUID)) {
            return true;
        }
        
        // 根据UUID的哈希值判断模型类型
        // Minecraft使用 UUID.hashCode() % 2 来决定模型类型
        // 0 = Steve (粗手臂), 1 = Alex (细手臂)
        int hash = playerUUID.hashCode();
        return (hash % 2) != 0;
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

