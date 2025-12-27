package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 动态模型生成器
 * 为动态物品生成模型文件
 */
public class DynamicModelGenerator {
    private static final Logger LOGGER = PlayerDollAddon.LOGGER;
    
    /**
     * 为动态物品生成模型文件
     * @param registryName 注册名称
     * @return 是否成功生成
     */
    public static boolean generateItemModel(String registryName) {
        try {
            // 获取资源目录路径
            Path modelsDir = getModelsDirectory();
            if (modelsDir == null) {
                LOGGER.warn("无法获取模型目录，跳过生成模型文件: {}", registryName);
                return false;
            }
            
            // 创建模型文件路径
            Path modelFile = modelsDir.resolve("item").resolve(registryName + ".json");
            
            // 如果文件已存在，跳过
            if (Files.exists(modelFile)) {
                LOGGER.debug("模型文件已存在，跳过: {}", modelFile);
                return true;
            }
            
            // 确保目录存在
            Files.createDirectories(modelFile.getParent());
            
            // 生成模型文件内容（使用 builtin/entity 模型，用于自定义渲染）
            String modelContent = "{\n" +
                    "  \"parent\": \"builtin/entity\"\n" +
                    "}\n";
            
            // 写入文件
            Files.writeString(modelFile, modelContent);
            LOGGER.info("已生成模型文件: {}", modelFile);
            
            return true;
        } catch (IOException e) {
            LOGGER.error("生成模型文件失败: {}", registryName, e);
            return false;
        }
    }
    
    /**
     * 获取模型目录路径
     * @return 模型目录路径，如果无法获取返回null
     */
    private static Path getModelsDirectory() {
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
            
            // 在游戏目录下创建 resources/assets/player_doll_addon/models 目录
            // 注意：这不会影响已打包的资源，但可以在开发环境中使用
            Path modelsDir = gameDir.resolve("generated_resources")
                    .resolve("assets")
                    .resolve(PlayerDollAddon.MODID)
                    .resolve("models");
            
            return modelsDir;
        } catch (Exception e) {
            LOGGER.error("获取模型目录失败", e);
            return null;
        }
    }
}

