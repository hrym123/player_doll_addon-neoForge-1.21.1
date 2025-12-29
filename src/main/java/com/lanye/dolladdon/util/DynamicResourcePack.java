package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.util.Set;
import java.util.function.Predicate;
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
public class DynamicResourcePack implements ResourcePack {
    private static final Logger LOGGER = PlayerDollAddon.LOGGER;
    private final Path gameDir;
    
    public DynamicResourcePack(Path gameDir) {
        this.gameDir = gameDir;
    }
    
    @Override
    @Nullable
    public InputStream openRoot(String... paths) {
        return null;
    }
    
    @Override
    @Nullable
    public InputStream open(ResourceType type, Identifier location) {
        String namespace = location.getNamespace();
        String path = location.getPath();
        
        // 只处理我们mod的资源
        if (!PlayerDollAddon.MODID.equals(namespace)) {
            return null;
        }
        
        // 如果是纹理，从 player_doll/png 目录加载
        if (path.startsWith("textures/entity/")) {
            Path texturePath = DynamicTextureManager.getTexturePath(location);
            if (texturePath != null && Files.exists(texturePath) && Files.isRegularFile(texturePath)) {
                try {
                    return Files.newInputStream(texturePath);
                } catch (IOException e) {
                    LOGGER.error("打开纹理文件失败: {}", location, e);
                }
            }
        }
        
        // 如果是模型，不需要通过动态资源包提供
        // 模型文件应该已经在编译时生成到 src/main/resources 并打包到 JAR 中
        // Minecraft 会从 JAR 包中加载这些模型文件
        if (path.startsWith("models/")) {
            return null;
        }
        
        return null;
    }
    
    @Override
    public void findResources(ResourceType type, String namespace, String prefix, Predicate<Identifier> pathFilter, net.minecraft.resource.ResourcePack.ResultConsumer consumer) {
        // 只处理我们mod的资源
        if (!PlayerDollAddon.MODID.equals(namespace)) {
            return;
        }
        
        // 列出纹理资源
        if (prefix.equals("textures/entity")) {
            for (var entry : DynamicTextureManager.TEXTURE_PATHS.entrySet()) {
                Identifier location = entry.getKey();
                if (location.getNamespace().equals(namespace) && location.getPath().startsWith("textures/entity/")) {
                    if (pathFilter.test(location)) {
                        Path filePath = entry.getValue();
                        if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                            try {
                                consumer.accept(location, Files.newInputStream(filePath));
                            } catch (Exception e) {
                                LOGGER.error("列出纹理资源失败: {}", location, e);
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public Set<String> getNamespaces(ResourceType type) {
        Set<String> namespaces = new HashSet<>();
        namespaces.add(PlayerDollAddon.MODID);
        return namespaces;
    }
    
    @Override
    public void close() {
        // 不需要关闭
    }
    
    @Override
    public String getName() {
        return Text.literal("Dynamic Doll Resources").getString();
    }
}

