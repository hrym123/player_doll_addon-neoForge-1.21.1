package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.impl.item.ActionDebugStick;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 为 ActionDebugStick 添加日志的 Mixin
 * 在不修改原始类的情况下注入日志代码
 * 
 * 注意：问题010已修复，调试日志已关闭
 */
@Mixin(ActionDebugStick.class)
public class ActionDebugStickMixin {
    // 调试日志已关闭 - 问题010已修复
}
