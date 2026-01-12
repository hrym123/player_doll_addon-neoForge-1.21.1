package com.lanye.dolladdon.util.resource;

import com.lanye.dolladdon.PlayerDollAddon;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资源文件生成器
 * 用于动态生成物品模型和语言文件
 */
public class ResourceFileGenerator {
    // Logger removed - logging handled by Mixin
    
    /**
     * 获取游戏目录
     */
    private static Path getGameDir() {
        try {
            Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
            java.lang.reflect.Method gameDirMethod = fmlPathsClass.getMethod("getGamePath");
            return (Path) gameDirMethod.invoke(null);
        } catch (Exception e) {
            return Paths.get(".").toAbsolutePath().normalize();
        }
    }
    
    /**
     * 获取项目根目录
     * 通过查找包含 build.gradle 文件的目录来确定项目根目录
     * 
     * @return 项目根目录路径（绝对路径）
     */
    private static Path getProjectRoot() {
        Path gameDir = getGameDir();
        Path normalizedGameDir = gameDir.normalize().toAbsolutePath();
        
        // Debug logging handled by Mixin"[资源生成] 开始解析项目根目录，游戏目录: {}", normalizedGameDir);
        
        // 从游戏目录开始，向上查找包含 build.gradle 的目录
        Path currentDir = normalizedGameDir;
        int maxDepth = 10; // 最多向上查找 10 层，防止无限循环
        int depth = 0;
        
        while (depth < maxDepth && currentDir != null) {
            Path buildGradle = currentDir.resolve("build.gradle");
            Path settingsGradle = currentDir.resolve("settings.gradle");
            
            // Debug logging handled by Mixin"[资源生成] 检查目录 (深度 {}): {}, build.gradle存在: {}, settings.gradle存在: {}", 
                    depth, currentDir, Files.exists(buildGradle), Files.exists(settingsGradle));
            
            // 如果找到 build.gradle 或 settings.gradle，说明这是项目根目录
            if (Files.exists(buildGradle) || Files.exists(settingsGradle)) {
                // Debug logging handled by Mixin"[资源生成] ✓ 找到项目根目录: {}", currentDir.toAbsolutePath());
                return currentDir.toAbsolutePath();
            }
            
            // 向上移动一层
            Path parent = currentDir.getParent();
            if (parent == null || parent.equals(currentDir)) {
                // Debug logging handled by Mixin"[资源生成] 已到达根目录，停止查找");
                break; // 已经到达根目录
            }
            currentDir = parent;
            depth++;
        }
        
        // 如果找不到 build.gradle，回退到原来的逻辑
        // Debug logging handled by Mixin"[资源生成] 未找到 build.gradle，使用回退逻辑");
        String lastSegment = normalizedGameDir.getFileName().toString();
        if (lastSegment.equals("run")) {
            Path result = normalizedGameDir.getParent().toAbsolutePath();
            // Debug logging handled by Mixin"[资源生成] 游戏目录是 run，返回父目录: {}", result);
            return result;
        } else {
            Path possibleRunDir = normalizedGameDir.resolve("run");
            if (Files.exists(possibleRunDir) && Files.isDirectory(possibleRunDir)) {
                // Debug logging handled by Mixin"[资源生成] 游戏目录包含 run 子目录，返回游戏目录: {}", normalizedGameDir.toAbsolutePath());
                return normalizedGameDir.toAbsolutePath();
            } else {
                Path result = normalizedGameDir.getParent().toAbsolutePath();
                // Debug logging handled by Mixin"[资源生成] 返回游戏目录的父目录: {}", result);
                return result;
            }
        }
    }
    
    /**
     * 生成所有自定义纹理玩偶的物品模型文件
     * 生成到 Mod 的资源目录，这样会自动加载
     */
    public static void generateItemModels() {
        try {
            // 获取项目根目录（绝对路径）
            Path projectRoot = getProjectRoot();
            
            // 生成到 build/resources/main（开发环境会自动加载）
            Path buildDir = projectRoot.resolve("build");
            Path buildResourcesDir = buildDir.resolve("resources");
            Path buildResourcesMainDir = buildResourcesDir.resolve("main");
            Path buildModelsDir = buildResourcesMainDir.resolve("assets").resolve(PlayerDollAddon.MODID).resolve("models").resolve("item");
            
            // 转换为绝对路径
            Path buildModelsDirAbs = buildModelsDir.toAbsolutePath();
            
            // 严格验证路径，确保不会创建到 run/resources
            String buildModelsDirStr = buildModelsDirAbs.toString().toLowerCase();
            String projectRootStr = projectRoot.toString().toLowerCase();
            
            // 检查路径是否包含 run/resources
            boolean containsRunResources = buildModelsDirStr.contains("run" + java.io.File.separator + "resources") || 
                buildModelsDirStr.contains("run\\resources") ||
                buildModelsDirStr.contains("run/resources");
                
            if (containsRunResources) {
                // Error logging handled by Mixin"[资源生成] ✗ 错误: 检测到路径包含 run/resources，拒绝创建");
                throw new IOException("路径解析错误: 检测到 run/resources 路径");
            }
            
            // 验证路径是否在项目根目录下
            boolean startsWithProjectRoot = buildModelsDirAbs.startsWith(projectRoot);
            if (!startsWithProjectRoot) {
                // Error logging handled by Mixin"[资源生成] ✗ 错误: 目标目录不在项目根目录下");
                throw new IOException("路径解析错误: 目标目录不在项目根目录下");
            }
            
            // Info logging handled by Mixin"[资源生成] ✓ 路径验证通过，生成物品模型到: {}", buildModelsDirAbs);
            
            // 创建目录
            Files.createDirectories(buildModelsDirAbs);
            // Debug logging handled by Mixin"[资源生成] 目录已创建: {}", buildModelsDirAbs);
            
            List<PngTextureScanner.PngTextureInfo> pngFiles = PngTextureScanner.scanPngFiles();
            // Debug logging handled by Mixin"[资源生成] 扫描到 {} 个PNG文件", pngFiles.size());
            
            int successCount = 0;
            int failCount = 0;
            
            for (PngTextureScanner.PngTextureInfo pngInfo : pngFiles) {
                try {
                    String registryName = pngInfo.getRegistryName();
                    String itemId = "custom_doll_" + registryName;
                    
                    // Debug logging handled by Mixin"[资源生成] 生成物品模型: 注册名={}, 物品ID={}", registryName, itemId);
                    
                    // 生成物品模型 JSON
                    String modelJson = generateItemModelJson();
                    
                    // 写入构建目录
                    Path buildModelFile = buildModelsDir.resolve(itemId + ".json");
                    Files.writeString(buildModelFile, modelJson, StandardCharsets.UTF_8);
                    
                    // 验证文件是否生成成功
                    if (Files.exists(buildModelFile)) {
                        // Debug logging handled by Mixin"[资源生成] ✓ 模型文件生成成功: {}", buildModelFile);
                        successCount++;
                    } else {
                        // Error logging handled by Mixin"[资源生成] ✗ 模型文件生成失败: {}", buildModelFile);
                        failCount++;
                    }
                } catch (Exception e) {
                    // Error logging handled by Mixin"[资源生成] ✗ 生成模型文件时出错: {}", pngInfo.getRegistryName(), e);
                    failCount++;
                }
            }
            
            // Info logging handled by Mixin"[资源生成] 物品模型生成完成: 成功 {} 个, 失败 {} 个", successCount, failCount);
        } catch (IOException e) {
            // Error logging handled by Mixin"[资源生成] ✗ 生成物品模型文件时出错", e);
        }
    }
    
    /**
     * 生成物品模型 JSON 内容
     * 使用 builtin/entity 以支持自定义渲染器
     */
    private static String generateItemModelJson() {
        return "{\n" +
               "  \"parent\": \"builtin/entity\"\n" +
               "}";
    }
    
    /**
     * 更新语言文件，添加所有自定义纹理玩偶的翻译
     * 生成到 Mod 的资源目录，这样会自动加载
     */
    public static void updateLanguageFiles() {
        try {
            // 获取项目根目录（绝对路径）
            Path projectRoot = getProjectRoot();
            
            // 生成到 build/resources/main（开发环境会自动加载）
            Path buildDir = projectRoot.resolve("build");
            Path buildResourcesDir = buildDir.resolve("resources");
            Path buildResourcesMainDir = buildResourcesDir.resolve("main");
            Path buildLangDir = buildResourcesMainDir.resolve("assets").resolve(PlayerDollAddon.MODID).resolve("lang");
            
            // 转换为绝对路径
            Path buildLangDirAbs = buildLangDir.toAbsolutePath();
            
            // 严格验证路径，确保不会创建到 run/resources
            String buildLangDirStr = buildLangDirAbs.toString().toLowerCase();
            String projectRootStr = projectRoot.toString().toLowerCase();
            
            // 检查路径是否包含 run/resources
            boolean containsRunResources = buildLangDirStr.contains("run" + java.io.File.separator + "resources") || 
                buildLangDirStr.contains("run\\resources") ||
                buildLangDirStr.contains("run/resources");
                
            if (containsRunResources) {
                // Error logging handled by Mixin"[资源生成] ✗ 错误: 检测到路径包含 run/resources，拒绝创建");
                throw new IOException("路径解析错误: 检测到 run/resources 路径");
            }
            
            // 验证路径是否在项目根目录下
            boolean startsWithProjectRoot = buildLangDirAbs.startsWith(projectRoot);
            if (!startsWithProjectRoot) {
                // Error logging handled by Mixin"[资源生成] ✗ 错误: 目标目录不在项目根目录下");
                throw new IOException("路径解析错误: 目标目录不在项目根目录下");
            }
            
            // Info logging handled by Mixin"[资源生成] ✓ 路径验证通过，生成语言文件到: {}", buildLangDirAbs);
            
            // 创建目录
            Files.createDirectories(buildLangDirAbs);
            // Debug logging handled by Mixin"[资源生成] 目录已创建: {}", buildLangDirAbs);
            
            List<PngTextureScanner.PngTextureInfo> pngFiles = PngTextureScanner.scanPngFiles();
            // Debug logging handled by Mixin"[资源生成] 扫描到 {} 个PNG文件用于生成翻译", pngFiles.size());
            
            // 生成中文语言文件
            Map<String, String> zhCnEntries = new HashMap<>();
            Map<String, String> enUsEntries = new HashMap<>();
            
            // 添加固定的翻译
            String modId = PlayerDollAddon.MODID;
            // Debug logging handled by Mixin"[资源生成] 添加固定翻译，MODID: {}", modId);
            
            zhCnEntries.put("item." + modId + ".steve_doll", "史蒂夫玩偶");
            zhCnEntries.put("entity." + modId + ".steve_doll", "史蒂夫玩偶");
            zhCnEntries.put("item." + modId + ".alex_doll", "艾利克斯玩偶");
            zhCnEntries.put("entity." + modId + ".alex_doll", "艾利克斯玩偶");
            zhCnEntries.put("item." + modId + ".action_debug_stick", "动作调试棒");
            zhCnEntries.put("item." + modId + ".pose_debug_stick", "姿态调试棒");
            zhCnEntries.put("itemGroup." + modId + ".player_doll_tab", "玩家玩偶");
            
            enUsEntries.put("item." + modId + ".steve_doll", "Steve Doll");
            enUsEntries.put("entity." + modId + ".steve_doll", "Steve Doll");
            enUsEntries.put("item." + modId + ".alex_doll", "Alex Doll");
            enUsEntries.put("entity." + modId + ".alex_doll", "Alex Doll");
            enUsEntries.put("item." + modId + ".action_debug_stick", "Action Debug Stick");
            enUsEntries.put("item." + modId + ".pose_debug_stick", "Pose Debug Stick");
            enUsEntries.put("itemGroup." + modId + ".player_doll_tab", "Player Dolls");
            
            // 为每个 PNG 文件生成翻译
            int translationCount = 0;
            for (PngTextureScanner.PngTextureInfo pngInfo : pngFiles) {
                try {
                    String registryName = pngInfo.getRegistryName();
                    String fileName = pngInfo.getFileName();
                    String itemId = "custom_doll_" + registryName;
                    
                    // Debug logging handled by Mixin"[资源生成] 生成翻译: 注册名={}, 文件名={}, 物品ID={}", registryName, fileName, itemId);
                    
                    // 从文件名提取显示名称（移除扩展名，保留原始名称的部分）
                    String displayName = extractDisplayName(fileName);
                    // Debug logging handled by Mixin"[资源生成] 提取的显示名称: {}", displayName);
                    
                    // 生成翻译键
                    String itemKey = "item." + modId + "." + itemId;
                    String entityKey = "entity." + modId + "." + itemId;
                    
                    zhCnEntries.put(itemKey, displayName + "玩偶");
                    zhCnEntries.put(entityKey, displayName + "玩偶");
                    
                    enUsEntries.put(itemKey, displayName + " Doll");
                    enUsEntries.put(entityKey, displayName + " Doll");
                    
                    translationCount++;
                } catch (Exception e) {
                    // Error logging handled by Mixin"[资源生成] ✗ 生成翻译条目时出错: {}", pngInfo.getRegistryName(), e);
                }
            }
            
            // Debug logging handled by Mixin"[资源生成] 生成了 {} 个翻译条目", translationCount);
            // Debug logging handled by Mixin"[资源生成] 中文翻译总数: {}, 英文翻译总数: {}", zhCnEntries.size(), enUsEntries.size());
            
            // 生成语言文件 JSON
            String zhCnJson = generateLanguageJson(zhCnEntries);
            String enUsJson = generateLanguageJson(enUsEntries);
            
            // Debug logging handled by Mixin"[资源生成] 语言文件JSON生成完成，中文大小: {} 字符, 英文大小: {} 字符", 
                    zhCnJson.length(), enUsJson.length());
            
            // 写入构建目录
            Path buildZhCnFile = buildLangDir.resolve("zh_cn.json");
            Path buildEnUsFile = buildLangDir.resolve("en_us.json");
            
            Files.writeString(buildZhCnFile, zhCnJson, StandardCharsets.UTF_8);
            Files.writeString(buildEnUsFile, enUsJson, StandardCharsets.UTF_8);
            
            // Debug logging handled by Mixin"[资源生成] 语言文件已写入: {}, {}", buildZhCnFile, buildEnUsFile);
            
            // 验证文件是否生成成功
            boolean zhCnSuccess = Files.exists(buildZhCnFile);
            boolean enUsSuccess = Files.exists(buildEnUsFile);
            
            if (zhCnSuccess && enUsSuccess) {
                // Info logging handled by Mixin"[资源生成] ✓ 语言文件生成成功: 中文 {} 个条目, 英文 {} 个条目", 
                        zhCnEntries.size(), enUsEntries.size());
            } else {
                if (!zhCnSuccess) {
                    // Error logging handled by Mixin"[资源生成] ✗ 中文语言文件生成失败: {}", buildZhCnFile);
                }
                if (!enUsSuccess) {
                    // Error logging handled by Mixin"[资源生成] ✗ 英文语言文件生成失败: {}", buildEnUsFile);
                }
            }
        } catch (IOException e) {
            // Error logging handled by Mixin"[资源生成] ✗ 生成语言文件时出错", e);
        }
    }
    
    /**
     * 从文件名提取显示名称
     * 移除扩展名，清理特殊字符
     */
    private static String extractDisplayName(String fileName) {
        // 移除 .png 扩展名
        String name = fileName;
        if (name.toLowerCase().endsWith(".png")) {
            name = name.substring(0, name.length() - 4);
        }
        
        // 移除前缀（如果有 S 或 A 前缀）
        if (name.length() > 0 && (name.charAt(0) == 'S' || name.charAt(0) == 'A')) {
            name = name.substring(1);
        }
        
        // 替换下划线为空格
        name = name.replace('_', ' ');
        
        // 如果名称为空，使用默认名称
        if (name.trim().isEmpty()) {
            name = "自定义";
        }
        
        return name;
    }
    
    /**
     * 生成语言文件 JSON 内容
     */
    private static String generateLanguageJson(Map<String, String> entries) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        int index = 0;
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            json.append("  \"");
            json.append(escapeJson(entry.getKey()));
            json.append("\": \"");
            json.append(escapeJson(entry.getValue()));
            json.append("\"");
            
            if (index < entries.size() - 1) {
                json.append(",");
            }
            json.append("\n");
            index++;
        }
        
        json.append("}");
        return json.toString();
    }
    
    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private static String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
