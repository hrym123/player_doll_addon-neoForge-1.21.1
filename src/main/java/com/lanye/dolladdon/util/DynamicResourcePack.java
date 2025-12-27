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
        
        // 如果是模型，从 generated_resources 目录加载
        // 注意：Minecraft 在查找模型时使用的路径是 "models/item/slan_ye.json"，
        // 但 ResourceLocation 中的 path 是 "models/item/slan_ye"（没有 .json 扩展名）
        if (path.startsWith("models/")) {
            LOGGER.error("【关键模型加载】尝试加载模型: location={}, path={}", location, path);
            
            // 首先尝试添加 .json 扩展名
            Path modelPath = gameDir.resolve("generated_resources")
                    .resolve("assets")
                    .resolve(namespace)
                    .resolve(path + ".json");
            
            // 尝试不添加 .json 扩展名的路径
            Path modelPathWithoutExt = gameDir.resolve("generated_resources")
                    .resolve("assets")
                    .resolve(namespace)
                    .resolve(path);
            
            LOGGER.warn("【关键】模型路径检查: modelPath={}, exists={}, modelPathWithoutExt={}, exists={}", 
                modelPath, Files.exists(modelPath), modelPathWithoutExt, Files.exists(modelPathWithoutExt));
            
            if (Files.exists(modelPath) && Files.isRegularFile(modelPath)) {
                LOGGER.warn("【关键】动态资源包提供模型: {} -> {}", location, modelPath);
                return () -> {
                    LOGGER.warn("【关键】正在打开模型文件流: {}", modelPath);
                    try {
                        return Files.newInputStream(modelPath);
                    } catch (IOException e) {
                        LOGGER.error("打开模型文件流失败: {}", modelPath, e);
                        throw e;
                    }
                };
            } else if (Files.exists(modelPathWithoutExt) && Files.isRegularFile(modelPathWithoutExt)) {
                LOGGER.warn("【关键】动态资源包提供模型（无扩展名）: {} -> {}", location, modelPathWithoutExt);
                return () -> {
                    LOGGER.warn("【关键】正在打开模型文件流（无扩展名）: {}", modelPathWithoutExt);
                    try {
                        return Files.newInputStream(modelPathWithoutExt);
                    } catch (IOException e) {
                        LOGGER.error("打开模型文件流失败: {}", modelPathWithoutExt, e);
                        throw e;
                    }
                };
            } else {
                // 如果都找不到，输出更详细的调试信息
                LOGGER.warn("【关键】模型文件完全无法找到！location={}, path={}, 尝试的路径: 1) {} (存在: {}), 2) {} (存在: {})", 
                    location, path, modelPath, Files.exists(modelPath), modelPathWithoutExt, Files.exists(modelPathWithoutExt));
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
        
        // 列出模型资源 - 支持多种路径格式
        // Minecraft可能使用 "models" 或 "models/item" 来查找模型
        if (path.equals("models/item") || path.equals("models")) {
            Path modelsDir = gameDir.resolve("generated_resources")
                    .resolve("assets")
                    .resolve(namespace)
                    .resolve("models")
                    .resolve("item");
            
            LOGGER.warn("【模型查找】listResources 检查模型目录: {} (存在: {}, 是目录: {})", 
                modelsDir, Files.exists(modelsDir), Files.exists(modelsDir) && Files.isDirectory(modelsDir));
            
            if (Files.exists(modelsDir) && Files.isDirectory(modelsDir)) {
                LOGGER.info("列出模型资源，目录: {}", modelsDir);
                try (Stream<Path> paths = Files.list(modelsDir)) {
                    long count = paths.filter(Files::isRegularFile)
                         .filter(p -> p.toString().endsWith(".json"))
                         .peek(filePath -> {
                             try {
                                 String fileName = filePath.getFileName().toString();
                                 String modelName = fileName.substring(0, fileName.length() - 5); // 移除 .json
                                 ResourceLocation location = ResourceLocation.fromNamespaceAndPath(namespace, "models/item/" + modelName);
                                 
                                 // 确保文件存在并可以读取
                                 if (Files.exists(filePath) && Files.isRegularFile(filePath) && Files.isReadable(filePath)) {
                                     // 获取文件大小（必须在 lambda 外部完成，使其成为 effectively final）
                                     long tempFileSize = 0;
                                     try {
                                         tempFileSize = Files.size(filePath);
                                     } catch (IOException e) {
                                         LOGGER.error("无法获取文件大小: {}", filePath, e);
                                     }
                                     final long fileSize = tempFileSize;
                                     
                                     // 创建一个包装的 IoSupplier，添加日志
                                     // 使用 WARN 级别以便更容易看到
                                     output.accept(location, () -> {
                                         LOGGER.warn("【重要】listResources 提供的 IoSupplier 被调用，打开文件: {} (大小: {} 字节)", filePath, fileSize);
                                         try {
                                             InputStream stream = Files.newInputStream(filePath);
                                             LOGGER.warn("【重要】成功打开模型文件流: {}", filePath);
                                             return stream;
                                         } catch (IOException e) {
                                             LOGGER.error("打开模型文件失败: {}", filePath, e);
                                             throw e;
                                         }
                                     });
                                     LOGGER.warn("【重要】列出模型资源到 output: {} -> {} (大小: {} 字节)", location, filePath, fileSize);
                                 } else {
                                     LOGGER.warn("模型文件不可访问: {} (存在: {}, 是文件: {}, 可读: {})", 
                                         filePath, 
                                         Files.exists(filePath),
                                         Files.exists(filePath) && Files.isRegularFile(filePath),
                                         Files.exists(filePath) && Files.isReadable(filePath));
                                 }
                             } catch (Exception e) {
                                 LOGGER.error("列出模型资源失败: {}", filePath, e);
                             }
                         })
                         .count();
                    LOGGER.warn("【模型查找】共列出 {} 个模型文件", count);
                } catch (IOException e) {
                    LOGGER.error("列出模型资源失败", e);
                }
            } else {
                LOGGER.warn("模型目录不存在或不是目录: {} (存在: {}, 是目录: {})", 
                    modelsDir, Files.exists(modelsDir), Files.exists(modelsDir) && Files.isDirectory(modelsDir));
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

