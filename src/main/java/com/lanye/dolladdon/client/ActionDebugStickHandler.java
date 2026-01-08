package com.lanye.dolladdon.client;

import com.lanye.dolladdon.impl.item.ActionDebugStick;
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
 * 动作调试棒客户端处理器
 * 处理滚轮切换动作
 */
public class ActionDebugStickHandler {
    private static final String LOG_MODULE = LogModuleConfig.MODULE_ENTITY_ACTION;
    
    // 所有可用动作的列表（按加载顺序）
    private static final List<String> actionList = new ArrayList<>();
    
    /**
     * 初始化
     * 注意：滚轮事件处理通过Mixin实现，这里不需要额外初始化
     */
    public static void initialize() {
        // 滚轮事件通过MouseMixin处理
        ModuleLogger.debug(LOG_MODULE, "动作调试棒处理器已初始化");
    }
    
    /**
     * 切换到下一个动作
     */
    public static void switchToNextAction(MinecraftClient client, ItemStack stack, boolean forward) {
        updateActionList();
        
        if (actionList.isEmpty()) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("没有可用的动作"), true);
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
            client.player.sendMessage(Text.literal("选中动作: " + displayName + " (" + (nextIndex + 1) + "/" + actionList.size() + ")"), true);
        }
        
        ModuleLogger.debug(LOG_MODULE, "动作调试棒: 切换到动作 {} (索引: {}), ItemStack NBT={}", 
            nextActionName, nextIndex, stack.getNbt());
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
