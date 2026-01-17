package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.PlayerDoll;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import com.lanye.dolladdon.util.neoForge.DynamicTextureManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

/**
 * 为 DynamicTextureManager 添加日志的 Mixin
 * 追踪纹理扫描和注册过程，分析 backup 目录过滤逻辑
 */
@Mixin(DynamicTextureManager.class)
public class DynamicTextureManagerMixin {
    
    /**
     * 在 scanAndRegisterTextures 方法开始前注入日志
     */
    @Inject(
        method = "scanAndRegisterTextures(Ljava/nio/file/Path;)V",
        at = @At("HEAD")
    )
    private static void onScanAndRegisterTexturesStart(Path gameDir, CallbackInfo ci) {
        try {
            Path pngDir = gameDir.resolve(PlayerDoll.PNG_DIR);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_PACK,
                "scanAndRegisterTextures: 开始扫描 PNG 目录: {}", pngDir);
        } catch (Exception e) {
            // 忽略错误
        }
    }
    
    /**
     * 在 registerTexture 方法中注入日志，记录每个注册的纹理
     */
    @Inject(
        method = "registerTexture(Lnet/minecraft/resources/ResourceLocation;Ljava/nio/file/Path;)V",
        at = @At("RETURN")
    )
    private static void onRegisterTexture(ResourceLocation resourceLocation, Path filePath, CallbackInfo ci) {
        Path parent = filePath.getParent();
        String parentName = parent != null ? parent.getFileName().toString() : "null";
        String fileName = filePath.getFileName().toString();
        
        // 检查是否在 backup 目录中
        boolean isInBackup = parent != null && parent.getFileName().toString().equalsIgnoreCase("backup");
        
        if (isInBackup) {
            ModuleLogger.warn(LogModuleConfig.MODULE_RESOURCE_PACK,
                "registerTexture: ⚠️ 检测到 backup 目录中的文件被注册: {} -> {} (父目录: {})", 
                resourceLocation, fileName, parentName);
        } else {
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_PACK,
                "registerTexture: 已注册纹理: {} -> {} (父目录: {})", 
                resourceLocation, fileName, parentName);
        }
    }
    
    /**
     * 在 scanAndRegisterTextures 方法返回后注入日志
     */
    @Inject(
        method = "scanAndRegisterTextures(Ljava/nio/file/Path;)V",
        at = @At("RETURN")
    )
    private static void onScanAndRegisterTexturesReturn(Path gameDir, CallbackInfo ci) {
        ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_PACK,
            "scanAndRegisterTextures: 扫描完成，已注册 {} 个纹理", 
            DynamicTextureManager.TEXTURE_PATHS.size());
    }
}
