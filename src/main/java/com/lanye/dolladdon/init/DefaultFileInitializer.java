package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 默认文件初始化器
 * 在 Mod 首次加载时，从资源包复制默认 JSON 文件到文件系统
 */
public class DefaultFileInitializer {
    
    /**
     * 初始化默认文件（从资源包复制到文件系统）
     * @param gameDir 游戏目录路径
     */
    public static void initializeDefaultFiles(Path gameDir) {
        try {
            // 创建目录
            Path playerDollDir = gameDir.resolve("player_doll");
            Path posesDir = gameDir.resolve(PlayerDollAddon.POSES_DIR);
            Path actionsDir = gameDir.resolve(PlayerDollAddon.ACTIONS_DIR);
            
            Files.createDirectories(posesDir);
            Files.createDirectories(actionsDir);
            
            // 从资源包复制 README.md 文档
            copyIfNotExists(playerDollDir.resolve("README.md"), "assets/player_doll/defaults/README.md");
            
            // 从资源包复制姿态文件
            copyIfNotExists(posesDir.resolve("standing.json"), "assets/player_doll/defaults/poses/standing.json");
            copyIfNotExists(posesDir.resolve("wave_up.json"), "assets/player_doll/defaults/poses/wave_up.json");
            
            // 从资源包复制动作文件
            copyIfNotExists(actionsDir.resolve("dance.json"), "assets/player_doll/defaults/actions/dance.json");
            copyIfNotExists(actionsDir.resolve("sit.json"), "assets/player_doll/defaults/actions/sit.json");
            copyIfNotExists(actionsDir.resolve("wave.json"), "assets/player_doll/defaults/actions/wave.json");
        } catch (Exception e) {
            ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE, "初始化默认文件失败", e);
        }
    }
    
    /**
     * 从资源包复制文件到文件系统（如果目标文件不存在）
     * @param targetPath 目标文件路径
     * @param resourcePath 资源包中的文件路径
     */
    private static void copyIfNotExists(Path targetPath, String resourcePath) {
        try {
            // 如果目标文件已存在，跳过
            if (Files.exists(targetPath)) {
                return;
            }
            
            // 从资源包读取文件
            InputStream resourceStream = DefaultFileInitializer.class.getClassLoader().getResourceAsStream(resourcePath);
            if (resourceStream == null) {
                ModuleLogger.warn(LogModuleConfig.MODULE_RESOURCE, "找不到资源文件: {} (这是正常的，如果资源包中没有该文件)", resourcePath);
                return;
            }
            
            // 复制到文件系统
            Files.copy(resourceStream, targetPath);
            resourceStream.close();
        } catch (IOException e) {
            ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE, "复制文件失败: {} -> {}", resourcePath, targetPath, e);
        }
    }
}

