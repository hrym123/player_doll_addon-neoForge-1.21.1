package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

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
    private static final Logger LOGGER = PlayerDollAddon.LOGGER;
    
    /**
     * 生成所有自定义纹理玩偶的物品模型文件
     * 生成到 Mod 的资源目录，这样会自动加载
     */
    public static void generateItemModels() {
        try {
            // 获取项目根目录（getGameDir() 返回 run 目录，其父目录就是项目根目录）
            Path gameDir = FabricLoader.getInstance().getGameDir();
            Path projectRoot = gameDir.getParent();
            
            // 生成到 build/resources/main（开发环境会自动加载）
            Path buildResourcesDir = projectRoot.resolve("build/resources/main");
            Path buildModelsDir = buildResourcesDir.resolve("assets/" + PlayerDollAddon.MODID + "/models/item");
            
            // 同时生成到 run/resources（运行时目录）
            Path runResourcesDir = gameDir.resolve("resources");
            Path runModelsDir = runResourcesDir.resolve("assets/" + PlayerDollAddon.MODID + "/models/item");
            
            // 创建两个目录
            Files.createDirectories(buildModelsDir);
            Files.createDirectories(runModelsDir);
            
            List<PngTextureScanner.PngTextureInfo> pngFiles = PngTextureScanner.scanPngFiles();
            
            for (PngTextureScanner.PngTextureInfo pngInfo : pngFiles) {
                try {
                    String registryName = pngInfo.getRegistryName();
                    String itemId = "custom_doll_" + registryName;
                    
                    // 生成物品模型 JSON
                    String modelJson = generateItemModelJson();
                    
                    // 同时写入两个位置
                    Path buildModelFile = buildModelsDir.resolve(itemId + ".json");
                    Path runModelFile = runModelsDir.resolve(itemId + ".json");
                    
                    Files.writeString(buildModelFile, modelJson, StandardCharsets.UTF_8);
                    Files.writeString(runModelFile, modelJson, StandardCharsets.UTF_8);
                    
                    // 验证文件是否生成成功
                    if (!Files.exists(buildModelFile) || !Files.exists(runModelFile)) {
                        LOGGER.error("[资源生成] ✗ 模型文件生成失败: build={}, run={}", buildModelFile, runModelFile);
                    }
                } catch (Exception e) {
                    LOGGER.error("[资源生成] ✗ 生成模型文件时出错: {}", pngInfo.getRegistryName(), e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("[资源生成] ✗ 生成物品模型文件时出错", e);
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
            // 获取项目根目录（getGameDir() 返回 run 目录，其父目录就是项目根目录）
            Path gameDir = FabricLoader.getInstance().getGameDir();
            Path projectRoot = gameDir.getParent();
            
            // 生成到 build/resources/main（开发环境会自动加载）
            Path buildResourcesDir = projectRoot.resolve("build/resources/main");
            Path buildLangDir = buildResourcesDir.resolve("assets/" + PlayerDollAddon.MODID + "/lang");
            
            // 同时生成到 run/resources（运行时目录）
            Path runResourcesDir = gameDir.resolve("resources");
            Path runLangDir = runResourcesDir.resolve("assets/" + PlayerDollAddon.MODID + "/lang");
            
            // 创建两个目录
            Files.createDirectories(buildLangDir);
            Files.createDirectories(runLangDir);
            
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
                    LOGGER.error("[资源生成] ✗ 生成翻译条目时出错: {}", pngInfo.getRegistryName(), e);
                }
            }
            
            // 生成语言文件 JSON
            String zhCnJson = generateLanguageJson(zhCnEntries);
            String enUsJson = generateLanguageJson(enUsEntries);
            
            // 同时写入到两个目录
            Path buildZhCnFile = buildLangDir.resolve("zh_cn.json");
            Path buildEnUsFile = buildLangDir.resolve("en_us.json");
            Path runZhCnFile = runLangDir.resolve("zh_cn.json");
            Path runEnUsFile = runLangDir.resolve("en_us.json");
            
            // 写入构建目录
            Files.writeString(buildZhCnFile, zhCnJson, StandardCharsets.UTF_8);
            Files.writeString(buildEnUsFile, enUsJson, StandardCharsets.UTF_8);
            
            // 写入运行时目录
            Files.writeString(runZhCnFile, zhCnJson, StandardCharsets.UTF_8);
            Files.writeString(runEnUsFile, enUsJson, StandardCharsets.UTF_8);
            
            // 验证文件是否生成成功
            if (!Files.exists(buildZhCnFile) || !Files.exists(runZhCnFile)) {
                LOGGER.error("[资源生成] ✗ 中文语言文件生成失败: build={}, run={}", buildZhCnFile, runZhCnFile);
            }
            
            if (!Files.exists(buildEnUsFile) || !Files.exists(runEnUsFile)) {
                LOGGER.error("[资源生成] ✗ 英文语言文件生成失败: build={}, run={}", buildEnUsFile, runEnUsFile);
            }
        } catch (IOException e) {
            LOGGER.error("[资源生成] ✗ 生成语言文件时出错", e);
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

