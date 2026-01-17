package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDoll;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 默认文件初始化器
 * 在 Mod 首次加载时，从资源包复制默认 JSON 文件到文件系统
 */
public class DefaultFileInitializer {
    // Logger removed - logging handled by Mixin
    
    /**
     * 初始化默认文件（从资源包复制到文件系统）
     * @param gameDir 游戏目录路径
     */
    public static void initializeDefaultFiles(Path gameDir) {
        try {
            // 创建 player_doll 目录
            Path playerDollDir = gameDir.resolve("player_doll");
            Files.createDirectories(playerDollDir);
            
            // 从资源包复制整个 defaults 目录到 player_doll 目录
            copyDirectoryTreeFromResources(playerDollDir, "assets/player_doll/defaults");
        } catch (Exception e) {
            // Error logging handled by Mixin
        }
    }
    
    /**
     * 从资源包递归复制整个目录树到文件系统
     * 
     * 注意：由于 Java ClassLoader 的限制，无法直接列出资源目录
     * 所以这个方法会：
     * 1. 尝试从文件系统读取（开发环境）：使用 Files.walk() 递归遍历目录
     * 2. 如果资源在 JAR 包中（生产环境）：使用 Files.walk() 遍历文件系统路径（如果可用）
     * 
     * @param targetBaseDir 目标基础目录路径（如 player_doll 目录）
     * @param resourceBasePath 资源包中的基础路径（相对于类路径，如 "assets/player_doll/defaults"）
     */
    private static void copyDirectoryTreeFromResources(Path targetBaseDir, String resourceBasePath) {
        try {
            // 确保目标基础目录存在
            Files.createDirectories(targetBaseDir);
            
            // 尝试方法1：从文件系统读取（开发环境）
            URL resourceUrl = DefaultFileInitializer.class.getClassLoader().getResource(resourceBasePath);
            if (resourceUrl != null && "file".equals(resourceUrl.getProtocol())) {
                try {
                    // 资源在文件系统中（开发环境）
                    Path resourceSourceDir = Path.of(resourceUrl.toURI());
                    if (Files.exists(resourceSourceDir) && Files.isDirectory(resourceSourceDir)) {
                        // 递归遍历目录树并复制所有文件
                        try (var stream = Files.walk(resourceSourceDir)) {
                            stream.filter(Files::isRegularFile)
                                  .forEach(sourceFile -> {
                                      try {
                                          // 计算相对路径
                                          Path relativePath = resourceSourceDir.relativize(sourceFile);
                                          
                                          // 构建目标文件路径
                                          Path targetFile = targetBaseDir.resolve(relativePath);
                                          
                                          // 确保目标文件的父目录存在
                                          Files.createDirectories(targetFile.getParent());
                                          
                                          // 构建资源文件路径
                                          String resourceFilePath = resourceBasePath + "/" + relativePath.toString().replace("\\", "/");
                                          
                                          // 复制文件（如果不存在）
                                          copyIfNotExists(targetFile, resourceFilePath);
                                      } catch (Exception e) {
                                          // Error logging handled by Mixin
                                      }
                                  });
                        }
                        return; // 成功从文件系统复制，返回
                    }
                } catch (Exception e) {
                    // 无法从文件系统读取，继续尝试其他方法
                }
            }
            
            // 尝试方法2：从 JAR 包读取（生产环境）
            // 由于 ClassLoader 无法直接列出 JAR 中的资源目录，我们使用递归尝试方法
            // 尝试复制已知的文件和目录结构
            copyFromJarRecursive(targetBaseDir, resourceBasePath, "");
            
        } catch (Exception e) {
            // Error logging handled by Mixin
        }
    }
    
    /**
     * 从 JAR 包递归复制资源（通过尝试读取资源来确定文件是否存在）
     * 
     * @param targetBaseDir 目标基础目录
     * @param resourceBasePath 资源基础路径
     * @param relativePath 相对路径（用于递归）
     */
    private static void copyFromJarRecursive(Path targetBaseDir, String resourceBasePath, String relativePath) {
        try {
            // 构建当前路径
            String currentResourcePath = resourceBasePath + (relativePath.isEmpty() ? "" : "/" + relativePath);
            
            // 首先尝试作为文件复制
            Path targetFile = targetBaseDir.resolve(relativePath.isEmpty() ? "" : relativePath);
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }
            copyIfNotExists(targetFile, currentResourcePath);
            
            // 然后尝试作为目录处理（尝试常见的子文件和子目录）
            // 由于无法列出目录，我们使用已知的文件列表或尝试常见的文件名
            
            // 尝试复制已知的文件（基于 defaults 目录结构）
            String[] knownFiles = {
                "README.md",
                "actions/dance.json",
                "actions/run.json",
                "actions/sit.json",
                "actions/wave.json",
                "poses/crouching.json",
                "poses/lying.json",
                "poses/running.json",
                "poses/sitting.json",
                "poses/spread_arms.json",
                "poses/spread_legs.json",
                "poses/standing.json",
                "poses/wave_up.json"
            };
            
            for (String knownFile : knownFiles) {
                try {
                    String fileResourcePath = resourceBasePath + "/" + knownFile;
                    Path fileTargetPath = targetBaseDir.resolve(knownFile);
                    
                    // 确保父目录存在
                    if (fileTargetPath.getParent() != null) {
                        Files.createDirectories(fileTargetPath.getParent());
                    }
                    
                    // 尝试复制文件
                    copyIfNotExists(fileTargetPath, fileResourcePath);
                } catch (Exception e) {
                    // 忽略单个文件的错误，继续处理其他文件
                }
            }
        } catch (Exception e) {
            // Error logging handled by Mixin
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
                // Error logging handled by Mixin
                return;
            }
            
            // 复制到文件系统
            Files.copy(resourceStream, targetPath);
            resourceStream.close();
        } catch (IOException e) {
            // Error logging handled by Mixin
        }
    }
}
