package com.lanye.dolladdon.client;

import com.lanye.dolladdon.impl.item.PoseDebugStick;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import com.lanye.dolladdon.util.pose.PoseActionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 姿态调试棒客户端处理器
 * 处理滚轮切换姿态
 */
public class PoseDebugStickHandler {
    private static final String LOG_MODULE = LogModuleConfig.MODULE_ENTITY_POSE;
    
    // 所有可用姿态的列表（按加载顺序）
    private static final List<String> poseList = new ArrayList<>();
    
    /**
     * 初始化
     * 注意：滚轮事件处理通过Mixin实现，这里不需要额外初始化
     */
    public static void initialize() {
        // 滚轮事件通过MouseMixin处理
        ModuleLogger.debug(LOG_MODULE, "姿态调试棒处理器已初始化");
    }
    
    /**
     * 切换到下一个姿态
     */
    public static void switchToNextPose(MinecraftClient client, ItemStack stack, boolean forward) {
        updatePoseList();
        
        if (poseList.isEmpty()) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("没有可用的姿态"), false);
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
            client.player.sendMessage(Text.literal("选中姿态: " + displayName + " (" + (nextIndex + 1) + "/" + poseList.size() + ")"), true);
        }
        
        ModuleLogger.debug(LOG_MODULE, "姿态调试棒: 切换到姿态 {} (索引: {}), ItemStack NBT={}", 
            nextPoseName, nextIndex, stack.getNbt());
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
