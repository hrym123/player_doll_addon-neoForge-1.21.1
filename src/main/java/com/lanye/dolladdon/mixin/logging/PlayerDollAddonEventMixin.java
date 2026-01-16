package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.PlayerDoll;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 为 PlayerDoll 的事件处理方法添加日志的 Mixin
 * 
 * 注意：非关键日志已清理，事件处理过程日志过于频繁，仅在需要调试时启用
 */
@Mixin(PlayerDoll.class)
public class PlayerDollAddonEventMixin {
    // 非关键日志已清理：事件处理过程日志过于频繁，仅在需要调试时启用
}
