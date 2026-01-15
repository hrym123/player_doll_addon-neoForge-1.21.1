package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import com.lanye.dolladdon.util.neoForge.DynamicResourcePack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 为 DynamicResourcePack 添加日志的 Mixin
 * 追踪动态资源包的资源加载过程
 */
@Mixin(DynamicResourcePack.class)
public class DynamicResourcePackMixin {
    
    /**
     * 在 getResource 方法开始时注入日志
     */
    @Inject(
        method = "getResource",
        at = @At("HEAD"),
        remap = false
    )
    private void onGetResourceStart(
        PackType type,
        ResourceLocation location,
        CallbackInfoReturnable<net.minecraft.server.packs.resources.IoSupplier<java.io.InputStream>> cir
    ) {
        ModuleLogger.debug(
            LogModuleConfig.MODULE_RESOURCE,
            "DynamicResourcePack.getResource开始: type={}, location={}",
            type, location != null ? location.toString() : "null"
        );
    }
    
    /**
     * 在 getResource 方法返回时注入日志
     */
    @Inject(
        method = "getResource",
        at = @At("RETURN"),
        remap = false
    )
    private void onGetResourceReturn(
        PackType type,
        ResourceLocation location,
        CallbackInfoReturnable<net.minecraft.server.packs.resources.IoSupplier<java.io.InputStream>> cir
    ) {
        net.minecraft.server.packs.resources.IoSupplier<java.io.InputStream> result = cir.getReturnValue();
        boolean found = result != null;
        
        // 如果是png/路径，尝试获取文件路径信息
        String fileInfo = "未知";
        if (location != null && location.getPath().startsWith("png/")) {
            try {
                String fileName = location.getPath().substring(4); // 移除 "png/" 前缀
                // 尝试获取游戏目录（通过反射）
                java.nio.file.Path gameDir = null;
                try {
                    Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
                    java.lang.reflect.Method gameDirMethod = fmlPathsClass.getMethod("getGamePath");
                    gameDir = (java.nio.file.Path) gameDirMethod.invoke(null);
                } catch (Exception e) {
                    gameDir = java.nio.file.Paths.get(".").toAbsolutePath().normalize();
                }
                
                if (gameDir != null) {
                    java.nio.file.Path pngDir = gameDir.resolve("player_doll/png");
                    java.nio.file.Path texturePath = pngDir.resolve(fileName);
                    boolean exists = Files.exists(texturePath);
                    boolean isFile = Files.isRegularFile(texturePath);
                    fileInfo = String.format("路径=%s, 存在=%s, 是文件=%s", texturePath, exists, isFile);
                }
            } catch (Exception e) {
                fileInfo = "获取文件信息失败: " + e.getMessage();
            }
        }
        
        ModuleLogger.debug(
            LogModuleConfig.MODULE_RESOURCE,
            "DynamicResourcePack.getResource返回: type={}, location={}, 找到资源={}, 文件信息={}",
            type,
            location != null ? location.toString() : "null",
            found,
            fileInfo
        );
    }
}
