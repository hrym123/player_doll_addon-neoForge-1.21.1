package com.lanye.dolladdon.client;

import com.lanye.dolladdon.impl.item.ActionDebugStick;
import com.lanye.dolladdon.impl.item.PoseDebugStick;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import com.lanye.dolladdon.util.pose.PoseActionManager;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/**
 * 调试棒 Tooltip 处理器
 * 在物品详细栏中显示当前绑定的姿态或动作
 */
public class DebugStickTooltipHandler {
    private static final String LOG_MODULE = LogModuleConfig.MODULE_ENTITY_POSE;
    
    /**
     * 初始化 Tooltip 处理器
     */
    public static void initialize() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            if (stack.isEmpty()) {
                return;
            }
            
            // 处理姿态调试棒
            if (stack.getItem() instanceof PoseDebugStick) {
                String selectedPoseName = PoseDebugStick.getSelectedPose(stack);
                if (selectedPoseName != null && !selectedPoseName.isEmpty()) {
                    var pose = PoseActionManager.getPose(selectedPoseName);
                    if (pose != null) {
                        String displayName = pose.getDisplayName();
                        lines.add(Text.literal("当前姿态: " + displayName));
                    } else {
                        lines.add(Text.literal("当前姿态: " + selectedPoseName + " (不存在)"));
                    }
                } else {
                    lines.add(Text.literal("未选择姿态"));
                }
            }
            
            // 处理动作调试棒
            if (stack.getItem() instanceof ActionDebugStick) {
                String selectedActionName = ActionDebugStick.getSelectedAction(stack);
                if (selectedActionName != null && !selectedActionName.isEmpty()) {
                    var action = PoseActionManager.getAction(selectedActionName);
                    if (action != null) {
                        String displayName = action.getDisplayName();
                        lines.add(Text.literal("当前动作: " + displayName));
                    } else {
                        lines.add(Text.literal("当前动作: " + selectedActionName + " (不存在)"));
                    }
                } else {
                    lines.add(Text.literal("未选择动作"));
                }
            }
        });
        
        ModuleLogger.debug(LOG_MODULE, "调试棒 Tooltip 处理器已初始化");
    }
}
