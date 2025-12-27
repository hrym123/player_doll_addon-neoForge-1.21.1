package com.lanye.dolladdon.util;

import com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity;
import com.lanye.dolladdon.init.ModDataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 玩家玩偶工具类
 * 提供获取玩家信息的工具方法
 */
public class PlayerDollUtil {
    private static final String TAG_PLAYER_UUID = "player_uuid";
    private static final String TAG_PLAYER_NAME = "player_name";
    
    /**
     * 从玩偶实体获取玩家UUID
     */
    @Nullable
    public static UUID getPlayerUUID(DollEntity dollEntity) {
        CompoundTag playerData = dollEntity.getPersistentData().getCompound("player_data");
        if (playerData != null && playerData.hasUUID(TAG_PLAYER_UUID)) {
            return playerData.getUUID(TAG_PLAYER_UUID);
        }
        return null;
    }
    
    /**
     * 从物品获取玩家UUID
     */
    @Nullable
    public static UUID getPlayerUUID(ItemStack itemStack) {
        var playerData = itemStack.get(ModDataComponents.PLAYER_DATA.get());
        if (playerData != null && playerData.hasUUID(TAG_PLAYER_UUID)) {
            return playerData.getUUID(TAG_PLAYER_UUID);
        }
        return null;
    }
    
    /**
     * 从玩偶实体获取玩家名称
     */
    @Nullable
    public static String getPlayerName(DollEntity dollEntity) {
        CompoundTag playerData = dollEntity.getPersistentData().getCompound("player_data");
        if (playerData != null && playerData.contains(TAG_PLAYER_NAME)) {
            return playerData.getString(TAG_PLAYER_NAME);
        }
        return null;
    }
    
    /**
     * 从物品获取玩家名称
     */
    @Nullable
    public static String getPlayerName(ItemStack itemStack) {
        var playerData = itemStack.get(ModDataComponents.PLAYER_DATA.get());
        if (playerData != null && playerData.contains(TAG_PLAYER_NAME)) {
            return playerData.getString(TAG_PLAYER_NAME);
        }
        return null;
    }
}

