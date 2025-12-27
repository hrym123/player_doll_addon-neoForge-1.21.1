package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态纹理管理器
 * 管理从文件系统加载的纹理文件
 */
public class DynamicTextureManager {
    private static final Logger LOGGER = PlayerDollAddon.LOGGER;
    public static final Map<ResourceLocation, Path> TEXTURE_PATHS = new HashMap<>();
    
    /**
     * 注册纹理文件路径
     * @param resourceLocation 资源位置
     * @param filePath 文件路径
     */
    public static void registerTexture(ResourceLocation resourceLocation, Path filePath) {
        TEXTURE_PATHS.put(resourceLocation, filePath);
        LOGGER.debug("注册动态纹理: {} -> {}", resourceLocation, filePath);
    }
    
    /**
     * 获取纹理文件路径
     * @param resourceLocation 资源位置
     * @return 文件路径，如果不存在返回null
     */
    public static Path getTexturePath(ResourceLocation resourceLocation) {
        return TEXTURE_PATHS.get(resourceLocation);
    }
    
    /**
     * 检查纹理是否已注册
     * @param resourceLocation 资源位置
     * @return 是否已注册
     */
    public static boolean isTextureRegistered(ResourceLocation resourceLocation) {
        return TEXTURE_PATHS.containsKey(resourceLocation);
    }
    
    /**
     * 清除所有注册的纹理
     */
    public static void clear() {
        TEXTURE_PATHS.clear();
    }
}

