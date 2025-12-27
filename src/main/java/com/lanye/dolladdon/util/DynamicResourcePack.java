package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 动态资源包
 * 用于加载外部文件（纹理、模型等）
 */
public class DynamicResourcePack implements PackResources {
    private static final Logger LOGGER = PlayerDollAddon.LOGGER;
    private final Path gameDir;
    
    public DynamicResourcePack(Path gameDir) {
        this.gameDir = gameDir;
    }
    
    @Override
    public IoSupplier<InputStream> getRootResource(String... paths) {
        return null;
    }
    
    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        // 对所有资源请求都记录日志，包括模型
        // 使用 WARN 级别以便更容易看到模型加载请求
        String namespace = location.getNamespace();
        String path = location.getPath();
        
        // 对于模型资源的请求，使用 ERROR 级别以便更容易看到
        if (path.startsWith("models/")) {
            LOGGER.error("【关键模型加载】DynamicResourcePack.getResource 被调用来加载模型: type={}, location={}, path={}", type, location, path);
        } else {
            LOGGER.warn("【关键】DynamicResourcePack.getResource 被调用: type={}, location={}", type, location);
        }
        
        // 只处理我们mod的资源
        if (!PlayerDollAddon.MODID.equals(namespace)) {
            LOGGER.debug("资源不属于我们的mod，跳过: {}", location);
            return null;
        }
        
        // 如果是纹理，从 doll_textures 目录加载
        if (path.startsWith("textures/entity/")) {
            Path texturePath = DynamicTextureManager.getTexturePath(location);
            if (texturePath != null && Files.exists(texturePath) && Files.isRegularFile(texturePath)) {
                LOGGER.info("动态资源包提供纹理: {} -> {}", location, texturePath);
                return () -> Files.newInputStream(texturePath);
            } else {
                LOGGER.warn("动态资源包无法找到纹理: {} (路径: {})", location, texturePath);
            }
        }
        
        // 如果是模型，不需要通过动态资源包提供
        // 模型文件应该已经在编译时生成到 src/main/resources 并打包到 JAR 中
        // Minecraft 会从 JAR 包中加载这些模型文件
        if (path.startsWith("models/")) {
            LOGGER.debug("模型文件应该从 JAR 包中加载，不通过动态资源包提供: {}", location);
            return null;
        }
        
        return null;
    }
    
    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        // 只处理我们mod的资源
        if (!PlayerDollAddon.MODID.equals(namespace)) {
            return;
        }
        
        LOGGER.debug("listResources 被调用: type={}, namespace={}, path={}", type, namespace, path);
        
        // 列出纹理资源
        if (path.equals("textures/entity")) {
            LOGGER.debug("列出纹理资源，当前注册的纹理数量: {}", DynamicTextureManager.TEXTURE_PATHS.size());
            for (var entry : DynamicTextureManager.TEXTURE_PATHS.entrySet()) {
                ResourceLocation location = entry.getKey();
                if (location.getNamespace().equals(namespace) && location.getPath().startsWith("textures/entity/")) {
                    Path filePath = entry.getValue();
                    if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                        try {
                            output.accept(location, () -> Files.newInputStream(filePath));
                            LOGGER.debug("列出纹理资源: {} -> {}", location, filePath);
                        } catch (Exception e) {
                            LOGGER.error("列出纹理资源失败: {}", location, e);
                        }
                    } else {
                        LOGGER.warn("纹理文件不存在: {} -> {}", location, filePath);
                    }
                }
            }
        }
        
        // 不通过动态资源包列出模型资源
        // 模型文件应该已经在编译时生成到 src/main/resources 并打包到 JAR 中
        // Minecraft 会从 JAR 包中自动发现这些模型文件
        if (path.equals("models/item") || path.equals("models")) {
            LOGGER.debug("模型资源应该从 JAR 包中加载，不通过动态资源包列出: {}", path);
            return;
        }
    }
    
    @Override
    public Set<String> getNamespaces(PackType type) {
        Set<String> namespaces = new HashSet<>();
        namespaces.add(PlayerDollAddon.MODID);
        return namespaces;
    }
    
    @Override
    public void close() {
        // 不需要关闭
    }
    
    @Override
    public String packId() {
        return PlayerDollAddon.MODID + "_dynamic";
    }
    
    public boolean isBuiltin() {
        return true;
    }
    
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        return null;
    }
    
    @Override
    public PackLocationInfo location() {
        return new PackLocationInfo(
            PlayerDollAddon.MODID + "_dynamic",
            Component.literal("Dynamic Doll Resources"),
            PackSource.BUILT_IN,
            Optional.empty()
        );
    }
}

