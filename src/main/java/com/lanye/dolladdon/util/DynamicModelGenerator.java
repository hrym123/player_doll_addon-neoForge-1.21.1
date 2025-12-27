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
     * 动态模型文件存放的子目录名称
     */
    private static final String DYNAMIC_MODELS_SUBDIR = "dynamic";
    
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
            
            // 1. 生成到 build/resources/main 目录（开发环境，当前运行中立即生效）
            // 在生产环境中，这个目录不存在，会跳过（这是正常的）
            Path buildResourcesDir = getBuildResourcesDirectory();
            if (buildResourcesDir != null) {
                Path buildResourcesModelFile = buildResourcesDir.resolve("item")
                        .resolve(DYNAMIC_MODELS_SUBDIR)
                        .resolve(registryName + ".json");
                if (writeModelFileIfNeeded(buildResourcesModelFile, "build/resources/main")) {
                    success = true;
                    LOGGER.debug("模型文件已写入 build/resources/main（开发环境）");
                }
            } else {
                LOGGER.debug("无法获取 build/resources/main 目录（可能是生产环境，这是正常的）");
            }
            
            // 2. 生成到 src/main/resources 目录（开发环境，下次编译时会被包含到 JAR）
            // 在生产环境中，这个目录不存在，会跳过（这是正常的）
            // 但模型文件应该已经在编译时被打包到 JAR 中了
            Path modResourcesDir = getModResourcesDirectory();
            if (modResourcesDir != null) {
                Path modResourcesModelFile = modResourcesDir.resolve("item")
                        .resolve(DYNAMIC_MODELS_SUBDIR)
                        .resolve(registryName + ".json");
                if (writeModelFileIfNeeded(modResourcesModelFile, "src/main/resources")) {
                    success = true;
                    LOGGER.debug("模型文件已写入 src/main/resources（开发环境）");
                }
            } else {
                LOGGER.debug("无法获取 src/main/resources 目录（可能是生产环境，这是正常的）");
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
                        LOGGER.debug("模型文件已存在且内容相同，跳过: {} ({})", modelFile, locationName);
                        return true;
                    }
                } catch (IOException e) {
                    LOGGER.debug("读取现有模型文件失败，将重新生成: {}", modelFile, e);
                }
            }
            
            // 确保目录存在
            Files.createDirectories(modelFile.getParent());
            
            // 写入文件
            Files.writeString(modelFile, MODEL_CONTENT);
            LOGGER.info("已生成模型文件: {} ({})", modelFile, locationName);
            return true;
        } catch (IOException e) {
            LOGGER.warn("生成模型文件失败: {} ({})", modelFile, locationName, e);
            return false;
        }
    }
    
    /**
     * 清理旧的动态模型文件
     * 直接清空 dynamic 子目录，简化清理逻辑
     */
    public static void cleanupOldModelFiles() {
        // 清理 src/main/resources 目录下的 dynamic 子目录
        Path modResourcesDir = getModResourcesDirectory();
        if (modResourcesDir != null) {
            Path dynamicModelsDir = modResourcesDir.resolve("item").resolve(DYNAMIC_MODELS_SUBDIR);
            cleanupDynamicDirectory(dynamicModelsDir, "src/main/resources");
        }
        
        // 清理 build/resources/main 目录下的 dynamic 子目录
        Path buildResourcesDir = getBuildResourcesDirectory();
        if (buildResourcesDir != null) {
            Path dynamicModelsDir = buildResourcesDir.resolve("item").resolve(DYNAMIC_MODELS_SUBDIR);
            cleanupDynamicDirectory(dynamicModelsDir, "build/resources/main");
        }
    }
    
    /**
     * 清理 dynamic 子目录中的所有文件
     * @param directory 要清理的目录（应该是 item/dynamic 目录）
     * @param locationName 位置名称（用于日志）
     */
    private static void cleanupDynamicDirectory(Path directory, String locationName) {
        if (directory == null || !Files.exists(directory) || !Files.isDirectory(directory)) {
            return;
        }
        
        try (var paths = Files.list(directory)) {
            int deletedCount = 0;
            for (Path filePath : paths.collect(java.util.stream.Collectors.toList())) {
                if (Files.isRegularFile(filePath)) {
                    try {
                        Files.delete(filePath);
                        deletedCount++;
                        LOGGER.debug("已删除动态模型文件: {} ({})", filePath, locationName);
                    } catch (IOException e) {
                        LOGGER.warn("删除动态模型文件失败: {} ({})", filePath, locationName, e);
                    }
                }
            }
            if (deletedCount > 0) {
                LOGGER.info("已清理 {} 个动态模型文件 ({})", deletedCount, locationName);
            }
        } catch (IOException e) {
            LOGGER.error("清理动态模型文件目录失败: {} ({})", directory, locationName, e);
        }
    }
    
    /**
     * 批量生成所有动态玩偶的模型文件
     * @param registryNames 注册名称列表
     * @return 成功生成的数量
     */
    public static int generateAllItemModels(java.util.List<String> registryNames) {
        int successCount = 0;
        LOGGER.info("开始批量生成 {} 个动态玩偶的模型文件", registryNames.size());
        
        for (String registryName : registryNames) {
            if (generateItemModel(registryName)) {
                successCount++;
            }
        }
        
        LOGGER.info("批量生成模型文件完成，成功生成 {}/{} 个", successCount, registryNames.size());
        return successCount;
    }
    
    /**
     * 获取 build/resources/main 目录路径（用于开发环境，可以在当前运行中立即生效）
     * @return 模型目录路径，如果无法获取返回null
     */
    private static Path getBuildResourcesDirectory() {
        try {
            // 获取游戏目录
            Path gameDir;
            try {
                Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
                java.lang.reflect.Method gameDirMethod = fmlPathsClass.getMethod("getGamePath");
                gameDir = (Path) gameDirMethod.invoke(null);
            } catch (Exception e) {
                gameDir = Paths.get(".").toAbsolutePath().normalize();
            }
            
            // 在开发环境中，gameDir 通常是 run 目录，项目根目录是 run 的父目录
            Path projectRoot = gameDir;
            
            // 如果 gameDir 是 run 目录，向上查找项目根目录
            if (gameDir.getFileName() != null && gameDir.getFileName().toString().equals("run")) {
                projectRoot = gameDir.getParent();
                LOGGER.debug("检测到 run 目录，项目根目录: {}", projectRoot);
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
                LOGGER.info("找到 build/resources/main 目录: {}", buildResourcesDir);
                return buildResourcesDir;
            }
            
            LOGGER.debug("无法找到 build/resources/main 目录（可能是生产环境，这是正常的）。尝试的路径: {}", buildResourcesDir);
            return null;
        } catch (Exception e) {
            LOGGER.debug("获取 build/resources/main 目录失败（可能是生产环境，这是正常的）", e);
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
            Path gameDir;
            try {
                Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
                java.lang.reflect.Method gameDirMethod = fmlPathsClass.getMethod("getGamePath");
                gameDir = (Path) gameDirMethod.invoke(null);
            } catch (Exception e) {
                gameDir = Paths.get(".").toAbsolutePath().normalize();
            }
            
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
                LOGGER.debug("找到 src/main/resources 目录: {}", resourcesDir);
                return resourcesDir;
            }
            
            LOGGER.debug("无法找到 src/main/resources 目录（可能是生产环境，这是正常的）");
            return null;
        } catch (Exception e) {
            LOGGER.debug("获取 src/main/resources 目录失败（可能是生产环境，这是正常的）", e);
            return null;
        }
    }
}

