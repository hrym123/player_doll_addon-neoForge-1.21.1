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
import java.util.stream.Stream;

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
        LOGGER.debug("DynamicResourcePack.getResource 被调用: type={}, location={}", type, location);
        String namespace = location.getNamespace();
        String path = location.getPath();
        
        // 只处理我们mod的资源
        if (!PlayerDollAddon.MODID.equals(namespace)) {
            LOGGER.debug("资源不属于我们的mod，跳过: {}", location);
            return null;
        }
        
        // 如果是纹理，从 doll_textures 目录加载
        if (path.startsWith("textures/entity/")) {
            Path texturePath = DynamicTextureManager.getTexturePath(location);
            if (texturePath != null && Files.exists(texturePath) && Files.isRegularFile(texturePath)) {
                LOGGER.debug("动态资源包提供纹理: {} -> {}", location, texturePath);
                return () -> Files.newInputStream(texturePath);
            } else {
                LOGGER.warn("动态资源包无法找到纹理: {} (路径: {})", location, texturePath);
            }
        }
        
        // 如果是模型，从 generated_resources 目录加载
        if (path.startsWith("models/")) {
            Path modelPath = gameDir.resolve("generated_resources")
                    .resolve("assets")
                    .resolve(namespace)
                    .resolve(path);
            if (Files.exists(modelPath) && Files.isRegularFile(modelPath)) {
                LOGGER.debug("动态资源包提供模型: {} -> {}", location, modelPath);
                return () -> Files.newInputStream(modelPath);
            } else {
                LOGGER.debug("动态资源包无法找到模型: {} (路径: {})", location, modelPath);
            }
        }
        
        return null;
    }
    
    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        // 只处理我们mod的资源
        if (!PlayerDollAddon.MODID.equals(namespace)) {
            return;
        }
        
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
        
        // 列出模型资源
        if (path.equals("models/item")) {
            Path modelsDir = gameDir.resolve("generated_resources")
                    .resolve("assets")
                    .resolve(namespace)
                    .resolve("models")
                    .resolve("item");
            if (Files.exists(modelsDir) && Files.isDirectory(modelsDir)) {
                try (Stream<Path> paths = Files.list(modelsDir)) {
                    paths.filter(Files::isRegularFile)
                         .filter(p -> p.toString().endsWith(".json"))
                         .forEach(filePath -> {
                             try {
                                 String fileName = filePath.getFileName().toString();
                                 String modelName = fileName.substring(0, fileName.length() - 5); // 移除 .json
                                 ResourceLocation location = ResourceLocation.fromNamespaceAndPath(namespace, "models/item/" + modelName);
                                 output.accept(location, () -> Files.newInputStream(filePath));
                                 LOGGER.debug("列出模型资源: {}", location);
                             } catch (Exception e) {
                                 LOGGER.error("列出模型资源失败: {}", filePath, e);
                             }
                         });
                } catch (IOException e) {
                    LOGGER.error("列出模型资源失败", e);
                }
            }
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

