package com.lanye.dolladdon.client.data;

import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 姿态调试棒数据存储
 * 用于在客户端和服务端之间共享选中的姿态名称
 * 在单机游戏中，客户端和服务端是同一个进程，所以这个 Map 可以在两者之间共享
 */
public class PoseDebugStickData {
    private static final Map<UUID, String> SELECTED_POSES = new HashMap<>();
    
    /**
     * 获取玩家选中的姿态名称
     */
    public static String getSelectedPose(Player player) {
        if (player == null) {
            return null;
        }
        return SELECTED_POSES.get(player.getUUID());
    }
    
    /**
     * 设置玩家选中的姿态名称
     */
    public static void setSelectedPose(Player player, String poseName) {
        if (player == null) {
            return;
        }
        if (poseName != null && !poseName.isEmpty()) {
            SELECTED_POSES.put(player.getUUID(), poseName);
        } else {
            SELECTED_POSES.remove(player.getUUID());
        }
    }
    
    /**
     * 清除玩家选中的姿态
     */
    public static void clearSelectedPose(Player player) {
        if (player != null) {
            SELECTED_POSES.remove(player.getUUID());
        }
    }
}
