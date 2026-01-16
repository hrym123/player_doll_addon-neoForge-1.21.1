package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.base.render.BaseDollRenderer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 为 BaseDollRenderer 添加日志的 Mixin
 * 追踪皮肤纹理获取过程
 * 
 * 注意：非关键日志已清理，皮肤纹理获取过程日志过于频繁，仅在需要调试时启用
 */
@Mixin(BaseDollRenderer.class)
public class BaseDollRendererMixin {
    // 非关键日志已清理：皮肤纹理获取过程日志过于频繁，仅在需要调试时启用
}
