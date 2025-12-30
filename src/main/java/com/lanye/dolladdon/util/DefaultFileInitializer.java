package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
        try {
            // 创建目录
            Path playerDollDir = gameDir.resolve("player_doll");
            Path posesDir = gameDir.resolve(PlayerDollAddon.POSES_DIR);
            Path actionsDir = gameDir.resolve(PlayerDollAddon.ACTIONS_DIR);
            
            Files.createDirectories(posesDir);
            Files.createDirectories(actionsDir);
            
            // 生成 README.md 文档
            generateReadme(playerDollDir);
            
            // 从资源包复制姿态文件
            copyIfNotExists(posesDir.resolve("standing.json"), "assets/player_doll_addon/defaults/poses/standing.json");
            copyIfNotExists(posesDir.resolve("wave_up.json"), "assets/player_doll_addon/defaults/poses/wave_up.json");
            
            // 从资源包复制动作文件
            copyIfNotExists(actionsDir.resolve("dance.json"), "assets/player_doll_addon/defaults/actions/dance.json");
            copyIfNotExists(actionsDir.resolve("sit.json"), "assets/player_doll_addon/defaults/actions/sit.json");
            copyIfNotExists(actionsDir.resolve("wave.json"), "assets/player_doll_addon/defaults/actions/wave.json");
        } catch (Exception e) {
            LOGGER.error("初始化默认文件失败", e);
        }
    }
    
    /**
     * 生成 README.md 文档
     * @param playerDollDir player_doll 目录路径
     */
    private static void generateReadme(Path playerDollDir) {
        try {
            Path readmePath = playerDollDir.resolve("README.md");
            
            // 如果 README.md 已存在，跳过
            if (Files.exists(readmePath)) {
                return;
            }
            
            String readmeContent = generateReadmeContent();
            Files.writeString(readmePath, readmeContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("生成 README.md 失败", e);
        }
    }
    
    /**
     * 生成 README.md 内容
     * @return README.md 文件内容
     */
    private static String generateReadmeContent() {
        return "# Player Doll Addon 配置指南\n\n" +
               "欢迎使用 Player Doll Addon！本目录用于存放玩偶相关的配置文件。\n\n" +
               "## 目录结构\n\n" +
               "```\n" +
               "player_doll/\n" +
               "├── poses/            # 姿态配置文件目录\n" +
               "├── actions/          # 动作配置文件目录\n" +
               "└── README.md         # 本说明文档\n" +
               "```\n\n" +
               "## 配置姿态和动作\n\n" +
               "请参考模组文档了解如何配置姿态和动作文件。\n\n" +
               "*本文档由 Player Doll Addon 模组自动生成*\n";
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
                LOGGER.warn("找不到资源文件: {} (这是正常的，如果资源包中没有该文件)", resourcePath);
                return;
            }
            
            // 复制到文件系统
            Files.copy(resourceStream, targetPath);
            resourceStream.close();
        } catch (IOException e) {
            LOGGER.error("复制文件失败: {} -> {}", resourcePath, targetPath, e);
        }
    }
}

