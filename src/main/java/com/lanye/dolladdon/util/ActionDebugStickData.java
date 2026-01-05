package com.lanye.dolladdon.util;

import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 动作调试棒数据存储
 * 用于在客户端和服务端之间共享选中的动作名称
 * 在单机游戏中，客户端和服务端是同一个进程，所以这个 Map 可以在两者之间共享
 */
public class ActionDebugStickData {
    private static final Map<UUID, String> SELECTED_ACTIONS = new HashMap<>();
    
    /**
     * 获取玩家选中的动作名称
     */
    public static String getSelectedAction(PlayerEntity player) {
        if (player == null) {
            return null;
        }
        return SELECTED_ACTIONS.get(player.getUuid());
    }
    
    /**
     * 设置玩家选中的动作名称
     */
    public static void setSelectedAction(PlayerEntity player, String actionName) {
        if (player == null) {
            return;
        }
        if (actionName != null && !actionName.isEmpty()) {
            SELECTED_ACTIONS.put(player.getUuid(), actionName);
        } else {
            SELECTED_ACTIONS.remove(player.getUuid());
        }
    }
    
    /**
     * 清除玩家选中的动作
     */
    public static void clearSelectedAction(PlayerEntity player) {
        if (player != null) {
            SELECTED_ACTIONS.remove(player.getUuid());
        }
    }
}
