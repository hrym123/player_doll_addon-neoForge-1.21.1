package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.util.neoForge.DynamicResourcePack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 为 DynamicResourcePack 添加日志的 Mixin
 * 追踪动态资源包的资源加载过程
 * 
 * 注意：非关键日志已清理，资源加载过程日志过于频繁，仅在需要调试时启用
 */
@Mixin(DynamicResourcePack.class)
public class DynamicResourcePackMixin {
    // 非关键日志已清理：资源加载过程日志过于频繁，仅在需要调试时启用
}
