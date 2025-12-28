package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 默认文件初始化器
 * 在 Mod 首次加载时，从资源包复制默认 JSON 文件到文件系统
 */
public class DefaultFileInitializer {
    private static final Logger LOGGER = PlayerDollAddon.LOGGER;
    
    /**
     * 初始化默认文件（从资源包复制到文件系统）
     * @param gameDir 游戏目录路径
     */
    public static void initializeDefaultFiles(Path gameDir) {
        LOGGER.info("[WENTI004] 开始初始化默认文件...");
        
        try {
            // 创建目录
            Path posesDir = gameDir.resolve(PlayerDollAddon.POSES_DIR);
            Path actionsDir = gameDir.resolve(PlayerDollAddon.ACTIONS_DIR);
            
            Files.createDirectories(posesDir);
            Files.createDirectories(actionsDir);
            LOGGER.info("[WENTI004] 已创建目录: {} 和 {}", posesDir, actionsDir);
            
            // 从资源包复制姿态文件
            copyIfNotExists(posesDir.resolve("standing.json"), "assets/player_doll_addon/defaults/poses/standing.json");
            copyIfNotExists(posesDir.resolve("wave_up.json"), "assets/player_doll_addon/defaults/poses/wave_up.json");
            
            // 从资源包复制动作文件
            copyIfNotExists(actionsDir.resolve("dance.json"), "assets/player_doll_addon/defaults/actions/dance.json");
            copyIfNotExists(actionsDir.resolve("sit.json"), "assets/player_doll_addon/defaults/actions/sit.json");
            copyIfNotExists(actionsDir.resolve("wave.json"), "assets/player_doll_addon/defaults/actions/wave.json");
            
            LOGGER.info("[WENTI004] 默认文件初始化完成");
        } catch (Exception e) {
            LOGGER.error("[WENTI004] 初始化默认文件失败", e);
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
                LOGGER.debug("[WENTI004] 文件已存在，跳过: {}", targetPath);
                return;
            }
            
            // 从资源包读取文件
            InputStream resourceStream = DefaultFileInitializer.class.getClassLoader().getResourceAsStream(resourcePath);
            if (resourceStream == null) {
                LOGGER.error("[WENTI004] 找不到资源文件: {}", resourcePath);
                return;
            }
            
            // 复制到文件系统
            Files.copy(resourceStream, targetPath);
            resourceStream.close();
            LOGGER.info("[WENTI004] 已从资源包复制默认文件: {} -> {}", resourcePath, targetPath);
        } catch (IOException e) {
            LOGGER.error("[WENTI004] 复制文件失败: {} -> {}", resourcePath, targetPath, e);
        }
    }
}

