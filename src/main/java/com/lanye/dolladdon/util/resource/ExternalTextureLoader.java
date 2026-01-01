package com.lanye.dolladdon.util.resource;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 外部纹理加载器
 * 用于从文件系统加载 PNG 纹理文件并注册到 Minecraft 纹理管理器
 */
public class ExternalTextureLoader {
    private static final Map<Identifier, Path> LOADED_TEXTURES = new HashMap<>();
    
    /**
     * 加载所有外部纹理
     * 应该在客户端资源重载时调用
     */
    public static void loadExternalTextures() {
        // 清除已加载的纹理
        LOADED_TEXTURES.clear();
        
        // 扫描 PNG 文件
        List<PngTextureScanner.PngTextureInfo> pngFiles = PngTextureScanner.scanPngFiles();
        
        for (PngTextureScanner.PngTextureInfo pngInfo : pngFiles) {
            Identifier textureId = pngInfo.getTextureIdentifier();
            Path filePath = pngInfo.getFilePath();
            
            // 存储纹理路径（实际加载将在需要时进行）
            LOADED_TEXTURES.put(textureId, filePath);
        }
    }
    
    /**
     * 获取纹理文件路径
     * @param textureId 纹理标识符
     * @return 文件路径，如果不存在则返回 null
     */
    public static Path getTexturePath(Identifier textureId) {
        return LOADED_TEXTURES.get(textureId);
    }
    
    /**
     * 加载单个纹理到 Minecraft 纹理管理器
     * @param textureId 纹理标识符
     * @param textureManager 纹理管理器
     * @return 是否成功加载
     */
    public static boolean loadTexture(Identifier textureId, net.minecraft.client.texture.TextureManager textureManager) {
        Path filePath = LOADED_TEXTURES.get(textureId);
        if (filePath == null || !Files.exists(filePath)) {
            ModuleLogger.warn(LogModuleConfig.MODULE_RESOURCE, "找不到纹理文件: {} -> {}", textureId, filePath);
            return false;
        }
        
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            // 读取 PNG 图像
            NativeImage image = NativeImage.read(inputStream);
            
            // 创建纹理
            NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
            
            // 注册到纹理管理器
            textureManager.registerTexture(textureId, texture);
            
            return true;
        } catch (IOException e) {
            ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE, "加载外部纹理失败: {} -> {}", textureId, filePath, e);
            return false;
        }
    }
    
    /**
     * 获取所有已加载的纹理标识符
     * @return 纹理标识符集合
     */
    public static Map<Identifier, Path> getAllLoadedTextures() {
        return new HashMap<>(LOADED_TEXTURES);
    }
}

