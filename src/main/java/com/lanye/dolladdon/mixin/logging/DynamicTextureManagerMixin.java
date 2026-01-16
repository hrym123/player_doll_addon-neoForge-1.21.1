package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.util.neoForge.DynamicTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

/**
 * 为 DynamicTextureManager 添加日志的 Mixin
 * 追踪纹理扫描和注册过程
 */
@Mixin(value = DynamicTextureManager.class, remap = false)
public class DynamicTextureManagerMixin {
    
    /**
     * 在 scanAndRegisterTextures 方法开始时添加日志
     */
    @Inject(
        method = "scanAndRegisterTextures",
        at = @At("HEAD"),
        remap = false
    )
    private static void onScanAndRegisterTexturesStart(Path gameDir, CallbackInfo ci) {
        System.out.println("[PlayerDoll] [DEBUG] DynamicTextureManager.scanAndRegisterTextures 开始，游戏目录: " + gameDir);
    }
    
    /**
     * 在 scanAndRegisterTextures 方法完成时添加日志
     */
    @Inject(
        method = "scanAndRegisterTextures",
        at = @At("TAIL"),
        remap = false
    )
    private static void onScanAndRegisterTexturesEnd(Path gameDir, CallbackInfo ci) {
        int textureCount = DynamicTextureManager.TEXTURE_PATHS.size();
        System.out.println("[PlayerDoll] [DEBUG] DynamicTextureManager.scanAndRegisterTextures 完成，注册纹理数量: " + textureCount);
        if (textureCount > 0) {
            System.out.println("[PlayerDoll] [DEBUG] 前10个注册的纹理:");
            DynamicTextureManager.TEXTURE_PATHS.entrySet().stream()
                .limit(10)
                .forEach(entry -> {
                    System.out.println("[PlayerDoll] [DEBUG]   - " + entry.getKey() + " -> " + entry.getValue());
                });
            if (textureCount > 10) {
                System.out.println("[PlayerDoll] [DEBUG]   ... 还有 " + (textureCount - 10) + " 个纹理");
            }
        }
    }
    
}
