package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.PlayerDoll;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为 PlayerDoll 添加日志的 Mixin
 * 在不修改原始类的情况下注入日志代码
 */
@Mixin(PlayerDoll.class)
public class PlayerDollAddonMixin {
    
    /**
     * 在构造函数开始处注入日志
     * 注意：在 super() 调用之前的注入必须是静态方法
     */
    @Inject(
        method = "<init>",
        at = @At("HEAD"),
        remap = false
    )
    private static void onInitHead(IEventBus modEventBus, ModContainer modContainer, CallbackInfo ci) {
        ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "========== Player Doll Mod Initialization Started ==========");
    }
    
    /**
     * 在构造函数返回前注入日志
     */
    @Inject(
        method = "<init>",
        at = @At("RETURN"),
        remap = false
    )
    private void onInitReturn(IEventBus modEventBus, ModContainer modContainer, CallbackInfo ci) {
        ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "========== Player Doll Mod Initialization Complete ==========");
    }
}
