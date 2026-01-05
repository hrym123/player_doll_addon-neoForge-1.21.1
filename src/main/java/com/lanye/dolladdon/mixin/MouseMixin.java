package com.lanye.dolladdon.mixin;

import com.lanye.dolladdon.client.ActionDebugStickHandler;
import com.lanye.dolladdon.client.PoseDebugStickHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mouse Mixin - 处理滚轮事件用于动作调试棒和姿态调试棒
 */
@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.world == null) {
            return;
        }
        
        // 如果当前有打开的屏幕（GUI），不处理（让默认行为执行）
        if (client.currentScreen != null) {
            return;
        }
        
        // 检查玩家是否在潜行
        if (!client.player.isSneaking()) {
            return;
        }
        
        // 检查玩家是否手持动作调试棒或姿态调试棒
        ItemStack mainHandStack = client.player.getMainHandStack();
        ItemStack offHandStack = client.player.getOffHandStack();
        
        ItemStack heldStack = null;
        boolean isActionDebugStick = false;
        boolean isPoseDebugStick = false;
        
        if (mainHandStack.getItem() instanceof com.lanye.dolladdon.impl.item.ActionDebugStick) {
            heldStack = mainHandStack;
            isActionDebugStick = true;
        } else if (offHandStack.getItem() instanceof com.lanye.dolladdon.impl.item.ActionDebugStick) {
            heldStack = offHandStack;
            isActionDebugStick = true;
        } else if (mainHandStack.getItem() instanceof com.lanye.dolladdon.impl.item.PoseDebugStick) {
            heldStack = mainHandStack;
            isPoseDebugStick = true;
        } else if (offHandStack.getItem() instanceof com.lanye.dolladdon.impl.item.PoseDebugStick) {
            heldStack = offHandStack;
            isPoseDebugStick = true;
        }
        
        if (heldStack == null) {
            return;
        }
        
        // 处理滚轮事件
        if (vertical != 0) {
            boolean forward = vertical > 0;
            if (isActionDebugStick) {
                ActionDebugStickHandler.switchToNextAction(client, heldStack, forward);
            } else if (isPoseDebugStick) {
                PoseDebugStickHandler.switchToNextPose(client, heldStack, forward);
            }
            // 取消默认的滚轮行为（防止切换物品）
            ci.cancel();
        }
    }
}
