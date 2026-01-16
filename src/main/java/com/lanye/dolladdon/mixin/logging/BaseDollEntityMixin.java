package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 为 BaseDollEntity 添加日志的 Mixin
 * 在不修改原始类的情况下注入日志代码
 * 
 * 注意：非关键日志已清理，实体交互、NBT恢复和皮肤设置过程日志过于频繁，仅在需要调试时启用
 */
@Mixin(BaseDollEntity.class)
public class BaseDollEntityMixin {
    // 非关键日志已清理：实体交互过程日志过于频繁，仅在需要调试时启用
    
    // setAction 相关调试日志已关闭 - 问题010已修复
    
    // 非关键日志已清理：NBT恢复和皮肤设置过程日志过于频繁，仅在需要调试时启用
}
