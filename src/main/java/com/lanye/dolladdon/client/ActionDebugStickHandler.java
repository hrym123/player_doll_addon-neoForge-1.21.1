package com.lanye.dolladdon.client;

import com.lanye.dolladdon.impl.item.ActionDebugStick;
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
 * 动作调试棒客户端处理器
 * 处理滚轮切换动作
 */
public class ActionDebugStickHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 所有可用动作的列表（按加载顺序）
    private static final List<String> actionList = new ArrayList<>();
    
    /**
     * 初始化
     */
    public static void initialize() {
        LOGGER.debug("动作调试棒处理器已初始化");
    }
    
    /**
     * 切换到下一个动作
     */
    public static void switchToNextAction(Minecraft client, ItemStack stack, boolean forward) {
        updateActionList();
        
        if (actionList.isEmpty()) {
            if (client.player != null) {
                client.player.displayClientMessage(Component.literal("没有可用的动作"), true);
            }
            return;
        }
        
        String currentActionName = ActionDebugStick.getSelectedAction(client.player, stack);
        int currentIndex = -1;
        if (currentActionName != null) {
            currentIndex = actionList.indexOf(currentActionName);
        }
        
        int nextIndex;
        if (forward) {
            nextIndex = (currentIndex + 1) % actionList.size();
        } else {
            nextIndex = (currentIndex - 1 + actionList.size()) % actionList.size();
        }
        
        String nextActionName = actionList.get(nextIndex);
        ActionDebugStick.setSelectedAction(client.player, stack, nextActionName);
        
        var action = PoseActionManager.getAction(nextActionName);
        if (action != null && client.player != null) {
            String displayName = action.getDisplayName();
            client.player.displayClientMessage(Component.literal("选中动作: " + displayName + " (" + (nextIndex + 1) + "/" + actionList.size() + ")"), true);
        }
    }
    
    /**
     * 更新动作列表
     */
    private static void updateActionList() {
        Map<String, com.lanye.dolladdon.api.action.DollAction> allActions = PoseActionManager.getAllActions();
        actionList.clear();
        actionList.addAll(allActions.keySet());
    }
}
