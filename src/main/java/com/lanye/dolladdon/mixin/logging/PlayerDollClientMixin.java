package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.PlayerDollClient;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为 PlayerDollClient 添加日志的 Mixin
 * 追踪资源包注册和纹理扫描过程
 */
@Mixin(value = PlayerDollClient.class, remap = false)
public class PlayerDollClientMixin {
    
    /**
     * 在 onAddPackFinders 方法开始时添加日志
     */
    @Inject(
        method = "onAddPackFinders",
        at = @At("HEAD"),
        remap = false
    )
    private static void onAddPackFindersStart(AddPackFindersEvent event, CallbackInfo ci) {
        System.out.println("[PlayerDoll] [DEBUG] onAddPackFinders 开始执行，PackType: " + event.getPackType());
    }
    
    /**
     * 在 onAddPackFinders 方法成功完成时添加日志
     */
    @Inject(
        method = "onAddPackFinders",
        at = @At("TAIL"),
        remap = false
    )
    private static void onAddPackFindersEnd(AddPackFindersEvent event, CallbackInfo ci) {
        System.out.println("[PlayerDoll] [DEBUG] onAddPackFinders 执行完成");
    }
    
    /**
     * 在 onAddPackFinders 方法中，扫描纹理前添加日志
     */
    @Inject(
        method = "onAddPackFinders",
        at = @At(
            value = "INVOKE",
            target = "Lcom/lanye/dolladdon/util/neoForge/DynamicTextureManager;scanAndRegisterTextures(Ljava/nio/file/Path;)V",
            remap = false,
            shift = org.spongepowered.asm.mixin.injection.At.Shift.BEFORE
        ),
        remap = false
    )
    private static void onAddPackFindersBeforeTextureScan(AddPackFindersEvent event, CallbackInfo ci) {
        System.out.println("[PlayerDoll] [DEBUG] onAddPackFinders 准备扫描纹理");
    }
    
    /**
     * 在 onAddPackFinders 方法中，扫描纹理后添加日志
     */
    @Inject(
        method = "onAddPackFinders",
        at = @At(
            value = "INVOKE",
            target = "Lcom/lanye/dolladdon/util/neoForge/DynamicTextureManager;scanAndRegisterTextures(Ljava/nio/file/Path;)V",
            remap = false,
            shift = org.spongepowered.asm.mixin.injection.At.Shift.AFTER
        ),
        remap = false
    )
    private static void onAddPackFindersAfterTextureScan(AddPackFindersEvent event, CallbackInfo ci) {
        System.out.println("[PlayerDoll] [DEBUG] onAddPackFinders 纹理扫描完成");
    }
    
    /**
     * 在异常捕获处添加日志
     */
    @Inject(
        method = "onAddPackFinders",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Exception;printStackTrace()V",
            remap = false
        ),
        remap = false
    )
    private static void onAddPackFindersError(AddPackFindersEvent event, CallbackInfo ci) {
        System.out.println("[PlayerDoll] [ERROR] onAddPackFinders 发生异常，查看堆栈跟踪");
    }
}
