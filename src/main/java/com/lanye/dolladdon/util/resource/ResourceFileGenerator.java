package com.lanye.dolladdon.util.resource;

import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 资源文件生成器
 * 用于动态生成物品模型和语言文件
 */
public class ResourceFileGenerator {
    
    /**
     * 获取项目根目录
     * 通过查找包含 build.gradle 文件的目录来确定项目根目录
     * 这是最可靠的方法，不依赖于 getGameDir() 的返回值
     * 
     * @return 项目根目录路径（绝对路径）
     */
    private static Path getProjectRoot() {
        // 获取游戏目录（getGameDir() 在开发环境返回 run 目录，在生产环境返回 .minecraft）
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path normalizedGameDir = gameDir.normalize().toAbsolutePath();
        
        ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ========== 开始解析项目根目录 ==========");
        ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] getGameDir() 原始路径: {}", gameDir);
        ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] getGameDir() 规范化后: {}", normalizedGameDir);
        ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 路径字符串表示: {}", normalizedGameDir.toString());
        
        // 从游戏目录开始，向上查找包含 build.gradle 的目录
        Path currentDir = normalizedGameDir;
        int maxDepth = 10; // 最多向上查找 10 层，防止无限循环
        int depth = 0;
        
        ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 开始向上查找 build.gradle，起始目录: {}", currentDir);
        
        while (depth < maxDepth && currentDir != null) {
            Path buildGradle = currentDir.resolve("build.gradle");
            Path settingsGradle = currentDir.resolve("settings.gradle");
            
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] [深度 {}] 检查目录: {}", depth, currentDir);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] [深度 {}] build.gradle 路径: {} (存在: {})", depth, buildGradle, Files.exists(buildGradle));
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] [深度 {}] settings.gradle 路径: {} (存在: {})", depth, settingsGradle, Files.exists(settingsGradle));
            
            // 如果找到 build.gradle 或 settings.gradle，说明这是项目根目录
            if (Files.exists(buildGradle) || Files.exists(settingsGradle)) {
                Path projectRoot = currentDir.toAbsolutePath();
                ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ✓ 找到项目根目录（包含 build.gradle/settings.gradle）: {}", projectRoot);
                ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 项目根目录绝对路径: {}", projectRoot.toAbsolutePath());
                ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 项目根目录字符串: {}", projectRoot.toString());
                
                // 验证项目根目录不包含 run/resources
                String projectRootStr = projectRoot.toString().toLowerCase();
                ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 项目根目录（小写）: {}", projectRootStr);
                
                boolean containsRunResources = projectRootStr.contains("run" + java.io.File.separator + "resources") || 
                    projectRootStr.contains("run\\resources") ||
                    projectRootStr.contains("run/resources");
                ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 是否包含 run/resources: {}", containsRunResources);
                
                if (containsRunResources) {
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ✗ 错误: 项目根目录路径异常，包含 run/resources: {}", projectRoot);
                    throw new IllegalStateException("项目根目录路径解析错误: " + projectRoot);
                }
                
                ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ========== 项目根目录解析完成 ==========");
                return projectRoot;
            }
            
            // 向上移动一层
            Path parent = currentDir.getParent();
            if (parent == null || parent.equals(currentDir)) {
                ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 已到达根目录，停止查找");
                break; // 已经到达根目录
            }
            currentDir = parent;
            depth++;
        }
        
        // 如果找不到 build.gradle，回退到原来的逻辑
        ModuleLogger.warn(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 未找到 build.gradle，使用备用方法");
        String lastSegment = normalizedGameDir.getFileName().toString();
        ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 备用方法: 最后路径段 = {}", lastSegment);
        
        Path projectRoot;
        if (lastSegment.equals("run")) {
            projectRoot = normalizedGameDir.getParent().toAbsolutePath();
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 备用方法: 检测到 run 目录，项目根目录 = {}", projectRoot);
        } else {
            Path possibleRunDir = normalizedGameDir.resolve("run");
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 备用方法: 检查 run 子目录: {} (存在: {})", possibleRunDir, Files.exists(possibleRunDir));
            if (Files.exists(possibleRunDir) && Files.isDirectory(possibleRunDir)) {
                projectRoot = normalizedGameDir.toAbsolutePath();
                ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 备用方法: 找到 run 子目录，项目根目录 = {}", projectRoot);
            } else {
                projectRoot = normalizedGameDir.getParent().toAbsolutePath();
                ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 备用方法: 未找到 run 子目录，项目根目录 = {}", projectRoot);
            }
        }
        
        ModuleLogger.warn(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 使用备用方法确定的项目根目录: {}", projectRoot);
        ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ========== 项目根目录解析完成（备用方法）==========");
        return projectRoot.toAbsolutePath();
    }
    
    /**
     * 生成所有自定义纹理玩偶的物品模型文件
     * 生成到 Mod 的资源目录，这样会自动加载
     */
    public static void generateItemModels() {
        try {
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ========== 开始生成物品模型 ==========");
            
            // 获取项目根目录（绝对路径）
            Path projectRoot = getProjectRoot();
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR,"[资源生成] 项目根目录: {}", projectRoot);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR,"[资源生成] 项目根目录绝对路径: {}", projectRoot.toAbsolutePath());
            
            // 生成到 build/resources/main（开发环境会自动加载）
            // 使用绝对路径确保路径解析正确
            Path buildDir = projectRoot.resolve("build");
            Path buildResourcesDir = buildDir.resolve("resources");
            Path buildResourcesMainDir = buildResourcesDir.resolve("main");
            Path buildModelsDir = buildResourcesMainDir.resolve("assets").resolve(PlayerDollAddon.MODID).resolve("models").resolve("item");
            
            // 转换为绝对路径
            Path buildResourcesDirAbs = buildResourcesDir.toAbsolutePath();
            Path buildModelsDirAbs = buildModelsDir.toAbsolutePath();
            
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 路径构建过程:");
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   项目根目录: {}", projectRoot);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   build 目录: {} (绝对: {})", buildDir, buildDir.toAbsolutePath());
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   build/resources 目录: {} (绝对: {})", buildResourcesDir, buildResourcesDirAbs);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   build/resources/main 目录: {} (绝对: {})", buildResourcesMainDir, buildResourcesMainDir.toAbsolutePath());
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   最终目标目录: {} (绝对: {})", buildModelsDir, buildModelsDirAbs);
            
            // 严格验证路径，确保不会创建到 run/resources
            String buildModelsDirStr = buildModelsDirAbs.toString().toLowerCase();
            String projectRootStr = projectRoot.toString().toLowerCase();
            
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 路径验证:");
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   目标目录字符串（小写）: {}", buildModelsDirStr);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   项目根目录字符串（小写）: {}", projectRootStr);
            
            // 检查路径是否包含 run/resources
            boolean containsRunResources = buildModelsDirStr.contains("run" + java.io.File.separator + "resources") || 
                buildModelsDirStr.contains("run\\resources") ||
                buildModelsDirStr.contains("run/resources");
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   是否包含 run/resources: {}", containsRunResources);
                
                if (containsRunResources) {
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ✗ 错误: 检测到路径包含 run/resources，拒绝创建");
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 项目根目录: {}", projectRoot);
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 目标目录: {}", buildModelsDirAbs);
                throw new IOException("路径解析错误: 检测到 run/resources 路径");
            }
            
            // 验证路径是否在项目根目录下
            boolean startsWithProjectRoot = buildModelsDirAbs.startsWith(projectRoot);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   目标目录是否在项目根目录下: {}", startsWithProjectRoot);
                
                if (!startsWithProjectRoot) {
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ✗ 错误: 目标目录不在项目根目录下");
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 项目根目录: {}", projectRoot);
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 目标目录: {}", buildModelsDirAbs);
                throw new IOException("路径解析错误: 目标目录不在项目根目录下");
            }
            
            ModuleLogger.info(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ✓ 路径验证通过，生成物品模型到: {}", buildModelsDirAbs);
            
            // 创建目录
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 开始创建目录: {}", buildModelsDirAbs);
            boolean dirExisted = Files.exists(buildModelsDirAbs);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR,"[资源生成] 目录是否已存在: {}", dirExisted);
            
            Files.createDirectories(buildModelsDirAbs);
            
            boolean dirExistsNow = Files.exists(buildModelsDirAbs);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 目录创建后是否存在: {}", dirExistsNow);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 目录创建完成: {}", buildModelsDirAbs);
            
            List<PngTextureScanner.PngTextureInfo> pngFiles = PngTextureScanner.scanPngFiles();
            
            for (PngTextureScanner.PngTextureInfo pngInfo : pngFiles) {
                try {
                    String registryName = pngInfo.getRegistryName();
                    String itemId = "custom_doll_" + registryName;
                    
                    // 生成物品模型 JSON
                    String modelJson = generateItemModelJson();
                    
                    // 写入构建目录
                    Path buildModelFile = buildModelsDir.resolve(itemId + ".json");
                    Files.writeString(buildModelFile, modelJson, StandardCharsets.UTF_8);
                    
                    // 验证文件是否生成成功
                    if (!Files.exists(buildModelFile)) {
                        ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ✗ 模型文件生成失败: {}", buildModelFile);
                    }
                } catch (Exception e) {
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ✗ 生成模型文件时出错: {}", pngInfo.getRegistryName(), e);
                }
            }
        } catch (IOException e) {
            ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ✗ 生成物品模型文件时出错", e);
        }
    }
    
    /**
     * 生成 pack.mcmeta 文件
     */
    private static void generatePackMcmeta(Path resourcePackDir) throws IOException {
        String mcmeta = "{\n" +
                       "  \"pack\": {\n" +
                       "    \"pack_format\": 15,\n" +
                       "    \"description\": \"Player Doll Dynamic Textures\"\n" +
                       "  }\n" +
                       "}";
        Path mcmetaFile = resourcePackDir.resolve("pack.mcmeta");
        if (!Files.exists(mcmetaFile)) {
            Files.writeString(mcmetaFile, mcmeta, StandardCharsets.UTF_8);
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
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ========== 开始生成语言文件 ==========");
            
            // 获取项目根目录（绝对路径）
            Path projectRoot = getProjectRoot();
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 项目根目录: {}", projectRoot);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 项目根目录绝对路径: {}", projectRoot.toAbsolutePath());
            
            // 生成到 build/resources/main（开发环境会自动加载）
            // 使用绝对路径确保路径解析正确
            Path buildDir = projectRoot.resolve("build");
            Path buildResourcesDir = buildDir.resolve("resources");
            Path buildResourcesMainDir = buildResourcesDir.resolve("main");
            Path buildLangDir = buildResourcesMainDir.resolve("assets").resolve(PlayerDollAddon.MODID).resolve("lang");
            
            // 转换为绝对路径
            Path buildResourcesDirAbs = buildResourcesDir.toAbsolutePath();
            Path buildLangDirAbs = buildLangDir.toAbsolutePath();
            
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 路径构建过程:");
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   项目根目录: {}", projectRoot);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   build 目录: {} (绝对: {})", buildDir, buildDir.toAbsolutePath());
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   build/resources 目录: {} (绝对: {})", buildResourcesDir, buildResourcesDirAbs);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   build/resources/main 目录: {} (绝对: {})", buildResourcesMainDir, buildResourcesMainDir.toAbsolutePath());
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   最终目标目录: {} (绝对: {})", buildLangDir, buildLangDirAbs);
            
            // 严格验证路径，确保不会创建到 run/resources
            String buildLangDirStr = buildLangDirAbs.toString().toLowerCase();
            String projectRootStr = projectRoot.toString().toLowerCase();
            
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 路径验证:");
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   目标目录字符串（小写）: {}", buildLangDirStr);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   项目根目录字符串（小写）: {}", projectRootStr);
            
            // 检查路径是否包含 run/resources
            boolean containsRunResources = buildLangDirStr.contains("run" + java.io.File.separator + "resources") || 
                buildLangDirStr.contains("run\\resources") ||
                buildLangDirStr.contains("run/resources");
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   是否包含 run/resources: {}", containsRunResources);
                
                if (containsRunResources) {
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ✗ 错误: 检测到路径包含 run/resources，拒绝创建");
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 项目根目录: {}", projectRoot);
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 目标目录: {}", buildLangDirAbs);
                throw new IOException("路径解析错误: 检测到 run/resources 路径");
            }
            
            // 验证路径是否在项目根目录下
            boolean startsWithProjectRoot = buildLangDirAbs.startsWith(projectRoot);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成]   目标目录是否在项目根目录下: {}", startsWithProjectRoot);
                
                if (!startsWithProjectRoot) {
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ✗ 错误: 目标目录不在项目根目录下");
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 项目根目录: {}", projectRoot);
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 目标目录: {}", buildLangDirAbs);
                throw new IOException("路径解析错误: 目标目录不在项目根目录下");
            }
            
            ModuleLogger.info(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ✓ 路径验证通过，生成语言文件到: {}", buildLangDirAbs);
            
            // 创建目录
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 开始创建目录: {}", buildLangDirAbs);
            boolean dirExisted = Files.exists(buildLangDirAbs);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR,"[资源生成] 目录是否已存在: {}", dirExisted);
            
            Files.createDirectories(buildLangDirAbs);
            
            boolean dirExistsNow = Files.exists(buildLangDirAbs);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 目录创建后是否存在: {}", dirExistsNow);
            ModuleLogger.debug(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] 目录创建完成: {}", buildLangDirAbs);
            
            List<PngTextureScanner.PngTextureInfo> pngFiles = PngTextureScanner.scanPngFiles();
            
            // 生成中文语言文件
            Map<String, String> zhCnEntries = new HashMap<>();
            Map<String, String> enUsEntries = new HashMap<>();
            
            // 添加固定的翻译
            String modId = PlayerDollAddon.MODID;
            zhCnEntries.put("item." + modId + ".steve_doll", "史蒂夫玩偶");
            zhCnEntries.put("entity." + modId + ".steve_doll", "史蒂夫玩偶");
            zhCnEntries.put("item." + modId + ".alex_doll", "艾利克斯玩偶");
            zhCnEntries.put("entity." + modId + ".alex_doll", "艾利克斯玩偶");
            zhCnEntries.put("itemGroup." + modId + ".player_doll_tab", "玩家玩偶");
            
            enUsEntries.put("item." + modId + ".steve_doll", "Steve Doll");
            enUsEntries.put("entity." + modId + ".steve_doll", "Steve Doll");
            enUsEntries.put("item." + modId + ".alex_doll", "Alex Doll");
            enUsEntries.put("entity." + modId + ".alex_doll", "Alex Doll");
            enUsEntries.put("itemGroup." + modId + ".player_doll_tab", "Player Dolls");
            
            // 为每个 PNG 文件生成翻译
            for (PngTextureScanner.PngTextureInfo pngInfo : pngFiles) {
                try {
                    String registryName = pngInfo.getRegistryName();
                    String fileName = pngInfo.getFileName();
                    String itemId = "custom_doll_" + registryName;
                    
                    // 从文件名提取显示名称（移除扩展名，保留原始名称的部分）
                    String displayName = extractDisplayName(fileName);
                    
                    // 生成翻译键（使用之前声明的modId变量）
                    String itemKey = "item." + modId + "." + itemId;
                    String entityKey = "entity." + modId + "." + itemId;
                    
                    zhCnEntries.put(itemKey, displayName + "玩偶");
                    zhCnEntries.put(entityKey, displayName + "玩偶");
                    
                    enUsEntries.put(itemKey, displayName + " Doll");
                    enUsEntries.put(entityKey, displayName + " Doll");
                } catch (Exception e) {
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR, "[资源生成] ✗ 生成翻译条目时出错: {}", pngInfo.getRegistryName(), e);
                }
            }
            
            // 生成语言文件 JSON
            String zhCnJson = generateLanguageJson(zhCnEntries);
            String enUsJson = generateLanguageJson(enUsEntries);
            
            // 写入构建目录
            Path buildZhCnFile = buildLangDir.resolve("zh_cn.json");
            Path buildEnUsFile = buildLangDir.resolve("en_us.json");
            
            Files.writeString(buildZhCnFile, zhCnJson, StandardCharsets.UTF_8);
            Files.writeString(buildEnUsFile, enUsJson, StandardCharsets.UTF_8);
            
            // 验证文件是否生成成功
            if (!Files.exists(buildZhCnFile)) {
                ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR,"[资源生成] ✗ 中文语言文件生成失败: {}", buildZhCnFile);
            }
            
            if (!Files.exists(buildEnUsFile)) {
                ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR,"[资源生成] ✗ 英文语言文件生成失败: {}", buildEnUsFile);
            }
        } catch (IOException e) {
            ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE_GENERATOR,"[资源生成] ✗ 生成语言文件时出错", e);
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

