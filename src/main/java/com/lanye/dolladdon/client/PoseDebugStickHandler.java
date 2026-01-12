package com.lanye.dolladdon.client;

import com.lanye.dolladdon.impl.item.PoseDebugStick;
import com.lanye.dolladdon.util.pose.PoseActionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 姿态调试棒客户端处理器
 * 处理滚轮切换姿态
 */
public class PoseDebugStickHandler {
    // Logger removed - logging handled by Mixin
    
    // 所有可用姿态的列表（按加载顺序）
    private static final List<String> poseList = new ArrayList<>();
    
    /**
     * 初始化
     */
    public static void initialize() {
        // Debug logging handled by Mixin
    }
    
    /**
     * 切换到下一个姿态
     */
    public static void switchToNextPose(Minecraft client, ItemStack stack, boolean forward) {
        updatePoseList();
        
        if (poseList.isEmpty()) {
            if (client.player != null) {
                client.player.displayClientMessage(Component.literal("没有可用的姿态"), true);
            }
            return;
        }
        
        String currentPoseName = PoseDebugStick.getSelectedPose(client.player, stack);
        int currentIndex = -1;
        if (currentPoseName != null) {
            currentIndex = poseList.indexOf(currentPoseName);
        }
        
        int nextIndex;
        if (forward) {
            nextIndex = (currentIndex + 1) % poseList.size();
        } else {
            nextIndex = (currentIndex - 1 + poseList.size()) % poseList.size();
        }
        
        String nextPoseName = poseList.get(nextIndex);
        PoseDebugStick.setSelectedPose(client.player, stack, nextPoseName);
        
        var pose = PoseActionManager.getPose(nextPoseName);
        if (pose != null && client.player != null) {
            String displayName = pose.getDisplayName();
            client.player.displayClientMessage(Component.literal("选中姿态: " + displayName + " (" + (nextIndex + 1) + "/" + poseList.size() + ")"), true);
        }
    }
    
    /**
     * 更新姿态列表
     */
    private static void updatePoseList() {
        Map<String, com.lanye.dolladdon.api.pose.DollPose> allPoses = PoseActionManager.getAllPoses();
        poseList.clear();
        poseList.addAll(allPoses.keySet());
        
        // 确保列表有序（字母顺序）
        poseList.sort(String::compareTo);
        
        // 确保standing始终在第一个位置
        if (poseList.contains("standing")) {
            poseList.remove("standing");
            poseList.add(0, "standing");
        }
    }
}
