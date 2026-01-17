package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import com.lanye.dolladdon.util.resource.PngTextureScanner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.List;

/**
 * 为 PngTextureScanner 添加日志的 Mixin
 * 追踪 PNG 文件扫描过程，分析 backup 目录过滤逻辑
 */
@Mixin(PngTextureScanner.class)
public class PngTextureScannerMixin {
    
    /**
     * 在 scanPngFiles 方法开始前注入日志
     */
    @Inject(
        method = "scanPngFiles()Ljava/util/List;",
        at = @At("HEAD")
    )
    private static void onScanPngFilesStart(CallbackInfoReturnable<List<PngTextureScanner.PngTextureInfo>> cir) {
        try {
            // 获取游戏目录和 PNG 目录
            java.lang.reflect.Method getGameDirMethod = PngTextureScanner.class.getDeclaredMethod("getGameDir");
            getGameDirMethod.setAccessible(true);
            Path gameDir = (Path) getGameDirMethod.invoke(null);
            Path pngDir = gameDir.resolve(PngTextureScanner.PNG_DIR);
            
            ModuleLogger.debug(LogModuleConfig.MODULE_TEXTURE_SCANNER,
                "scanPngFiles: 开始扫描 PNG 目录: {}", pngDir);
        } catch (Exception e) {
            // 忽略反射错误
        }
    }
    
    /**
     * 在 scanPngFiles 方法返回后注入日志
     */
    @Inject(
        method = "scanPngFiles()Ljava/util/List;",
        at = @At("RETURN")
    )
    private static void onScanPngFilesReturn(CallbackInfoReturnable<List<PngTextureScanner.PngTextureInfo>> cir) {
        List<PngTextureScanner.PngTextureInfo> result = cir.getReturnValue();
        if (result != null) {
            ModuleLogger.debug(LogModuleConfig.MODULE_TEXTURE_SCANNER,
                "scanPngFiles: 扫描完成，找到 {} 个 PNG 文件", result.size());
            
            // 记录每个文件的详细信息，特别检查 backup 目录
            for (PngTextureScanner.PngTextureInfo info : result) {
                Path filePath = info.getFilePath();
                Path parent = filePath.getParent();
                String parentName = parent != null ? parent.getFileName().toString() : "null";
                
                // 检查是否在 backup 目录中
                boolean isInBackup = parent != null && parent.getFileName().toString().equalsIgnoreCase("backup");
                
                if (isInBackup) {
                    ModuleLogger.warn(LogModuleConfig.MODULE_TEXTURE_SCANNER,
                        "scanPngFiles: ⚠️ 检测到 backup 目录中的文件被扫描: {} -> {} (路径: {}, 父目录: {})", 
                        info.getFileName(), info.getRegistryName(), filePath, parentName);
                } else {
                    ModuleLogger.debug(LogModuleConfig.MODULE_TEXTURE_SCANNER,
                        "scanPngFiles: 已添加 PNG 文件: {} -> {} (路径: {}, 父目录: {})", 
                        info.getFileName(), info.getRegistryName(), filePath, parentName);
                }
            }
        }
    }
}
