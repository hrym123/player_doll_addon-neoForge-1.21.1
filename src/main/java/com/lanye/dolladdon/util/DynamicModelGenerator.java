package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * 动态模型生成器
 * 为动态物品生成模型文件
 */
public class DynamicModelGenerator {
    private static final Logger LOGGER = PlayerDollAddon.LOGGER;
    
    /**
     * 动态模型文件的前缀标识符（用于区分动态生成的模型文件）
     * 使用 "zzz_" 前缀可以让文件在按名称排序时排在最后
     */
    public static final String DYNAMIC_MODEL_PREFIX = "zzz_";
    
    /**
     * 所有动态玩偶使用的模型文件内容（所有动态玩偶都使用相同的模型）
     */
    private static final String MODEL_CONTENT = "{\n" +
            "  \"parent\": \"builtin/entity\"\n" +
            "}\n";
    
    /**
     * 为动态物品生成模型文件
     * 如果文件已存在且内容相同，则跳过生成
     * 
     * 注意：
     * - 在开发环境中：会生成到 build/resources/main 和 src/main/resources
     * - 在生产环境中：模型文件应该已经在 JAR 包中（通过编译时包含）
     * 
     * @param registryName 注册名称
     * @return 是否成功生成或已存在
     */
    public static boolean generateItemModel(String registryName) {
        try {
            boolean success = false;
            
            // 生成的文件名（registryName 已经包含了前缀标识符）
            String modelFileName = registryName + ".json";
            
            // 1. 生成到 build/resources/main 目录（开发环境，当前运行中立即生效）
            // 在生产环境中，这个目录不存在，会跳过（这是正常的）
            Path buildResourcesDir = getBuildResourcesDirectory();
            if (buildResourcesDir != null) {
                Path buildResourcesModelFile = buildResourcesDir.resolve("item")
                        .resolve(modelFileName);
                if (writeModelFileIfNeeded(buildResourcesModelFile, "build/resources/main")) {
                    success = true;
                }
            }
            
            // 2. 生成到 src/main/resources 目录（开发环境，下次编译时会被包含到 JAR）
            // 在生产环境中，这个目录不存在，会跳过（这是正常的）
            // 但模型文件应该已经在编译时被打包到 JAR 中了
            Path modResourcesDir = getModResourcesDirectory();
            if (modResourcesDir != null) {
                Path modResourcesModelFile = modResourcesDir.resolve("item")
                        .resolve(modelFileName);
                if (writeModelFileIfNeeded(modResourcesModelFile, "src/main/resources")) {
                    success = true;
                }
            }
            
            // 只要任一目录写入成功，就返回 true
            // 在生产环境中，模型文件应该已经在 JAR 包中
            return success;
        } catch (Exception e) {
            LOGGER.error("生成模型文件失败: {}", registryName, e);
            return false;
        }
    }
    
    /**
     * 写入模型文件（如果文件不存在或内容不同）
     * @param modelFile 模型文件路径
     * @param locationName 位置名称（用于日志）
     * @return 是否写入成功或文件已存在
     */
    private static boolean writeModelFileIfNeeded(Path modelFile, String locationName) {
        try {
            // 如果文件已存在且内容相同，跳过
            if (Files.exists(modelFile) && Files.isRegularFile(modelFile)) {
                try {
                    String existingContent = Files.readString(modelFile);
                    if (existingContent.trim().equals(MODEL_CONTENT.trim())) {
                        return true;
                    }
                } catch (IOException e) {
                    // 读取失败，将重新生成
                }
            }
            
            // 确保目录存在
            Files.createDirectories(modelFile.getParent());
            
            // 写入文件
            Files.writeString(modelFile, MODEL_CONTENT);
            return true;
        } catch (IOException e) {
            LOGGER.warn("生成模型文件失败: {} ({})", modelFile, locationName, e);
            return false;
        }
    }
    
    /**
     * 清理旧的动态模型文件
     * 清理所有带有前缀标识符的模型文件
     */
    public static void cleanupOldModelFiles() {
        // 清理 src/main/resources 目录下带有前缀的模型文件
        Path modResourcesDir = getModResourcesDirectory();
        if (modResourcesDir != null) {
            Path itemModelsDir = modResourcesDir.resolve("item");
            cleanupPrefixedModelFiles(itemModelsDir, "src/main/resources");
        }
        
        // 清理 build/resources/main 目录下带有前缀的模型文件
        Path buildResourcesDir = getBuildResourcesDirectory();
        if (buildResourcesDir != null) {
            Path itemModelsDir = buildResourcesDir.resolve("item");
            cleanupPrefixedModelFiles(itemModelsDir, "build/resources/main");
        }
        
        // 同时清理旧的 dynamic 子目录（如果存在）
        cleanupOldDynamicSubdirectory();
    }
    
    /**
     * 清理带有前缀标识符的模型文件
     * @param itemModelsDir item 模型目录
     * @param locationName 位置名称（用于日志）
     */
    private static void cleanupPrefixedModelFiles(Path itemModelsDir, String locationName) {
        if (itemModelsDir == null || !Files.exists(itemModelsDir) || !Files.isDirectory(itemModelsDir)) {
            return;
        }
        
        try (var paths = Files.list(itemModelsDir)) {
            int deletedCount = 0;
            for (Path filePath : paths.collect(java.util.stream.Collectors.toList())) {
                if (Files.isRegularFile(filePath)) {
                    String fileName = filePath.getFileName().toString();
                    // 只删除带有前缀标识符的文件
                    if (fileName.startsWith(DYNAMIC_MODEL_PREFIX) && fileName.endsWith(".json")) {
                        try {
                            Files.delete(filePath);
                            deletedCount++;
                        } catch (IOException e) {
                            // 删除文件失败，忽略
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("清理动态模型文件目录失败: {} ({})", itemModelsDir, locationName, e);
        }
    }
    
    /**
     * 清理旧的 dynamic 子目录（如果存在，用于向后兼容）
     */
    private static void cleanupOldDynamicSubdirectory() {
        // 清理 src/main/resources 目录下的 dynamic 子目录
        Path modResourcesDir = getModResourcesDirectory();
        if (modResourcesDir != null) {
            Path dynamicModelsDir = modResourcesDir.resolve("item").resolve("dynamic");
            if (Files.exists(dynamicModelsDir) && Files.isDirectory(dynamicModelsDir)) {
                try (var paths = Files.list(dynamicModelsDir)) {
                    for (Path filePath : paths.collect(java.util.stream.Collectors.toList())) {
                        if (Files.isRegularFile(filePath)) {
                            try {
                                Files.delete(filePath);
                            } catch (IOException e) {
                                // 删除文件失败，忽略
                            }
                        }
                    }
                    // 尝试删除空的 dynamic 目录
                    try {
                        Files.delete(dynamicModelsDir);
                    } catch (IOException e) {
                        // 如果目录不为空或删除失败，忽略错误
                    }
                } catch (IOException e) {
                    // 清理失败，忽略错误
                }
            }
        }
        
        // 清理 build/resources/main 目录下的 dynamic 子目录
        Path buildResourcesDir = getBuildResourcesDirectory();
        if (buildResourcesDir != null) {
            Path dynamicModelsDir = buildResourcesDir.resolve("item").resolve("dynamic");
            if (Files.exists(dynamicModelsDir) && Files.isDirectory(dynamicModelsDir)) {
                try (var paths = Files.list(dynamicModelsDir)) {
                    for (Path filePath : paths.collect(java.util.stream.Collectors.toList())) {
                        if (Files.isRegularFile(filePath)) {
                            try {
                                Files.delete(filePath);
                            } catch (IOException e) {
                                // 删除文件失败，忽略
                            }
                        }
                    }
                    // 尝试删除空的 dynamic 目录
                    try {
                        Files.delete(dynamicModelsDir);
                    } catch (IOException e) {
                        // 如果目录不为空或删除失败，忽略错误
                    }
                } catch (IOException e) {
                    // 清理失败，忽略错误
                }
            }
        }
    }
    
    /**
     * 批量生成所有动态玩偶的模型文件
     * @param registryNames 注册名称列表
     * @return 成功生成的数量
     */
    public static int generateAllItemModels(java.util.List<String> registryNames) {
        int successCount = 0;
        
        for (String registryName : registryNames) {
            if (generateItemModel(registryName)) {
                successCount++;
            }
        }
        
        return successCount;
    }
    
    /**
     * 获取 build/resources/main 目录路径（用于开发环境，可以在当前运行中立即生效）
     * @return 模型目录路径，如果无法获取返回null
     */
    private static Path getBuildResourcesDirectory() {
        try {
            // 获取游戏目录
            Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
            
            // 在开发环境中，gameDir 通常是 run 目录，项目根目录是 run 的父目录
            Path projectRoot = gameDir;
            
            // 如果 gameDir 是 run 目录，向上查找项目根目录
            if (gameDir.getFileName() != null && gameDir.getFileName().toString().equals("run")) {
                projectRoot = gameDir.getParent();
            }
            
            // 尝试查找 build/resources/main 目录
            Path buildResourcesDir = projectRoot.resolve("build")
                    .resolve("resources")
                    .resolve("main")
                    .resolve("assets")
                    .resolve(PlayerDollAddon.MODID)
                    .resolve("models");
            
            // 检查 build/resources/main 目录是否存在或可以创建
            Path buildResourcesBase = projectRoot.resolve("build").resolve("resources").resolve("main");
            if (Files.exists(buildResourcesBase) || Files.exists(projectRoot.resolve("build"))) {
                return buildResourcesDir;
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取 mod 的 src/main/resources 目录路径（用于持久化，下次编译时会被包含）
     * @return 模型目录路径，如果无法获取返回null
     */
    private static Path getModResourcesDirectory() {
        try {
            // 获取游戏目录
            Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
            
            // 在开发环境中，gameDir 通常是 run 目录，项目根目录是 run 的父目录
            Path projectRoot = gameDir;
            
            // 如果 gameDir 是 run 目录，向上查找项目根目录
            if (gameDir.getFileName() != null && gameDir.getFileName().toString().equals("run")) {
                projectRoot = gameDir.getParent();
            }
            
            // 尝试查找 src/main/resources 目录
            Path resourcesDir = projectRoot.resolve("src")
                    .resolve("main")
                    .resolve("resources")
                    .resolve("assets")
                    .resolve(PlayerDollAddon.MODID)
                    .resolve("models");
            
            // 检查 src/main/resources 目录是否存在
            Path resourcesBase = projectRoot.resolve("src").resolve("main").resolve("resources");
            if (Files.exists(resourcesBase) && Files.isDirectory(resourcesBase)) {
                return resourcesDir;
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}


