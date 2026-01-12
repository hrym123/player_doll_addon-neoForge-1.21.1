package com.lanye.dolladdon.client.data;

import net.minecraft.world.entity.player.Player;

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
    public static String getSelectedAction(Player player) {
        if (player == null) {
            return null;
        }
        return SELECTED_ACTIONS.get(player.getUUID());
    }
    
    /**
     * 设置玩家选中的动作名称
     */
    public static void setSelectedAction(Player player, String actionName) {
        if (player == null) {
            return;
        }
        if (actionName != null && !actionName.isEmpty()) {
            SELECTED_ACTIONS.put(player.getUUID(), actionName);
        } else {
            SELECTED_ACTIONS.remove(player.getUUID());
        }
    }
    
    /**
     * 清除玩家选中的动作
     */
    public static void clearSelectedAction(Player player) {
        if (player != null) {
            SELECTED_ACTIONS.remove(player.getUUID());
        }
    }
}
