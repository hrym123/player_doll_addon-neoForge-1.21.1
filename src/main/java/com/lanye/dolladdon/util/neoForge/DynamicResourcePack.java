package com.lanye.dolladdon.util.neoForge;

import com.lanye.dolladdon.PlayerDoll;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;

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
    // Logger removed - logging handled by Mixin
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
        String namespace = location.getNamespace();
        String path = location.getPath();
        
        // 只处理我们mod的资源
        if (!PlayerDoll.MODID.equals(namespace)) {
            return null;
        }
        
        // 如果是纹理，从 player_doll/png 目录加载
        // 支持两种路径格式：
        // 1. textures/entity/... (动态注册的玩偶，使用哈希值)
        // 2. png/... (通过指令获取的玩家皮肤，使用文件名)
        if (path.startsWith("textures/entity/")) {
            Path texturePath = DynamicTextureManager.getTexturePath(location);
            if (texturePath != null && Files.exists(texturePath) && Files.isRegularFile(texturePath)) {
                return () -> Files.newInputStream(texturePath);
            }
        } else if (path.startsWith("png/")) {
            // 优先从 DynamicTextureManager 获取文件路径（指令注册的纹理）
            // 这样可以处理包含特殊字符的文件名（使用哈希文件名）
            Path registeredTexturePath = DynamicTextureManager.getTexturePath(location);
            if (registeredTexturePath != null && Files.exists(registeredTexturePath) && Files.isRegularFile(registeredTexturePath)) {
                return () -> Files.newInputStream(registeredTexturePath);
            }
            
            // 如果 DynamicTextureManager 中没有，从 player_doll/png 目录加载纹理（向后兼容）
            String fileName = path.substring(4); // 移除 "png/" 前缀
            Path pngDir = gameDir.resolve("player_doll/png");
            Path fileTexturePath = pngDir.resolve(fileName);
            if (Files.exists(fileTexturePath) && Files.isRegularFile(fileTexturePath)) {
                return () -> Files.newInputStream(fileTexturePath);
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
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        // 只处理我们mod的资源
        if (!PlayerDoll.MODID.equals(namespace)) {
            return;
        }
        
        // 列出纹理资源
        if (path.equals("textures/entity")) {
            // 列出动态注册的纹理（使用哈希值）
            for (var entry : DynamicTextureManager.TEXTURE_PATHS.entrySet()) {
                ResourceLocation location = entry.getKey();
                if (location.getNamespace().equals(namespace) && location.getPath().startsWith("textures/entity/")) {
                    Path filePath = entry.getValue();
                    if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                        try {
                            output.accept(location, () -> Files.newInputStream(filePath));
                        } catch (Exception e) {
                            // Error logging handled by Mixin
                        }
                    }
                }
            }
        } else if (path.equals("png")) {
            // 首先列出已注册的纹理（通过DynamicTextureManager注册的，包括路径映射）
            for (var entry : DynamicTextureManager.TEXTURE_PATHS.entrySet()) {
                ResourceLocation location = entry.getKey();
                if (location.getNamespace().equals(namespace) && location.getPath().startsWith("png/")) {
                    Path filePath = entry.getValue();
                    if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                        try {
                            output.accept(location, () -> Files.newInputStream(filePath));
                        } catch (Exception e) {
                            // Error logging handled by Mixin
                        }
                    }
                }
            }
            
            // 然后列出 player_doll/png 目录下的所有PNG文件（向后兼容，处理未注册的文件）
            Path pngDir = gameDir.resolve("player_doll/png");
            if (Files.exists(pngDir) && Files.isDirectory(pngDir)) {
                try (var stream = Files.list(pngDir)) {
                    stream.filter(Files::isRegularFile)
                          .filter(p -> p.toString().toLowerCase().endsWith(".png"))
                          .forEach(pngFile -> {
                              try {
                                  String fileName = pngFile.getFileName().toString();
                                  
                                  // 检查是否有路径映射（如果文件名包含特殊字符，可能已使用哈希文件名）
                                  ResourceLocation mappedLocation = DynamicTextureManager.getMappedResourceLocation(fileName);
                                  if (mappedLocation != null) {
                                      // 如果已有映射，使用映射的ResourceLocation（已在上面列出，跳过）
                                      return;
                                  }
                                  
                                  // 如果没有映射，尝试直接使用文件名创建ResourceLocation
                                  // 如果文件名包含特殊字符，创建ResourceLocation会失败，但我们已经尝试了
                                  try {
                                      ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                                          namespace, "png/" + fileName
                                      );
                                      // 检查是否已经列出（避免重复）
                                      // 由于无法直接检查output是否已接受，我们依赖上面的已注册纹理列表
                                      // 如果纹理已注册，上面已经列出；如果未注册，这里列出
                                      output.accept(location, () -> Files.newInputStream(pngFile));
                                  } catch (IllegalArgumentException e) {
                                      // 文件名包含特殊字符，无法创建ResourceLocation，跳过
                                      // 这种情况应该通过DynamicTextureManager注册并使用路径映射
                                  }
                              } catch (Exception e) {
                                  // Error logging handled by Mixin
                              }
                          });
                } catch (Exception e) {
                    // Error logging handled by Mixin
                }
            }
        }
        
        // 不通过动态资源包列出模型资源
        // 模型文件应该已经在编译时生成到 src/main/resources 并打包到 JAR 中
        // Minecraft 会从 JAR 包中自动发现这些模型文件
        if (path.equals("models/item") || path.equals("models")) {
            return;
        }
    }
    
    @Override
    public Set<String> getNamespaces(PackType type) {
        Set<String> namespaces = new HashSet<>();
        namespaces.add(PlayerDoll.MODID);
        return namespaces;
    }
    
    @Override
    public void close() {
        // 不需要关闭
    }
    
    @Override
    public String packId() {
        return PlayerDoll.MODID + "_dynamic";
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
            PlayerDoll.MODID + "_dynamic",
            Component.literal("Dynamic Doll Resources"),
            PackSource.BUILT_IN,
            Optional.empty()
        );
    }
}

