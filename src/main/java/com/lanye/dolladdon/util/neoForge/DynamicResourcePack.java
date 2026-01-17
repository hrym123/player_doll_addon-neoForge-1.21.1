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
            com.lanye.dolladdon.util.logging.ModuleLogger.debug(
                com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_RESOURCE_PACK,
                "getResource: 请求PNG纹理资源: {}",
                location
            );
            
            // 优先从 DynamicTextureManager 获取文件路径（指令注册的纹理）
            // 这样可以处理包含特殊字符的文件名（使用哈希文件名）
            Path registeredTexturePath = DynamicTextureManager.getTexturePath(location);
            if (registeredTexturePath != null && Files.exists(registeredTexturePath) && Files.isRegularFile(registeredTexturePath)) {
                com.lanye.dolladdon.util.logging.ModuleLogger.debug(
                    com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_RESOURCE_PACK,
                    "getResource: 通过DynamicTextureManager加载纹理: {} -> {}",
                    location, registeredTexturePath
                );
                return () -> Files.newInputStream(registeredTexturePath);
            } else {
                com.lanye.dolladdon.util.logging.ModuleLogger.debug(
                    com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_RESOURCE_PACK,
                    "getResource: DynamicTextureManager中未找到纹理: {} (路径: {})",
                    location, registeredTexturePath
                );
            }
            
            // 如果 DynamicTextureManager 中没有，尝试从 player_doll/png 目录加载纹理（向后兼容）
            // 注意：如果文件名包含特殊字符，这个路径可能不存在，因为实际文件名可能不同
            String fileName = path.substring(4); // 移除 "png/" 前缀
            Path pngDir = gameDir.resolve("player_doll/png");
            Path fileTexturePath = pngDir.resolve(fileName);
            if (Files.exists(fileTexturePath) && Files.isRegularFile(fileTexturePath)) {
                com.lanye.dolladdon.util.logging.ModuleLogger.debug(
                    com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_RESOURCE_PACK,
                    "getResource: 通过文件系统加载纹理: {} -> {}",
                    location, fileTexturePath
                );
                return () -> Files.newInputStream(fileTexturePath);
            } else {
                com.lanye.dolladdon.util.logging.ModuleLogger.debug(
                    com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_RESOURCE_PACK,
                    "getResource: 文件系统中未找到纹理: {} (路径: {})",
                    location, fileTexturePath
                );
            }
            
            // 如果直接路径不存在，尝试通过路径映射查找（处理包含特殊字符的文件名）
            // 遍历所有已注册的纹理，查找匹配的 ResourceLocation
            // 这主要用于处理启动时扫描注册的纹理
            for (var entry : DynamicTextureManager.TEXTURE_PATHS.entrySet()) {
                ResourceLocation registeredLocation = entry.getKey();
                if (registeredLocation.equals(location)) {
                    Path mappedPath = entry.getValue();
                    if (Files.exists(mappedPath) && Files.isRegularFile(mappedPath)) {
                        com.lanye.dolladdon.util.logging.ModuleLogger.debug(
                            com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_RESOURCE_PACK,
                            "getResource: 通过路径映射加载纹理: {} -> {}",
                            location, mappedPath
                        );
                        return () -> Files.newInputStream(mappedPath);
                    }
                }
            }
            
            com.lanye.dolladdon.util.logging.ModuleLogger.warn(
                com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_RESOURCE_PACK,
                "getResource: 无法加载PNG纹理资源: {}",
                location
            );
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
                    // 排除 backup 目录下的文件（检查父目录名称）
                    Path parent = filePath.getParent();
                    if (parent != null) {
                        String parentName = parent.getFileName().toString();
                        if (parentName.equalsIgnoreCase("backup")) {
                            continue; // 跳过 backup 目录中的文件
                        }
                    }
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
                          // 排除 backup 目录下的文件
                          // 同时确保文件在 pngDir 的直接子目录中，不在子目录的子目录中
                          .filter(p -> {
                              // 获取文件的父目录
                              Path parent = p.getParent();
                              if (parent == null) {
                                  return false; // 没有父目录，跳过
                              }
                              
                              // 检查父目录名称是否为 backup（不区分大小写）
                              String parentName = parent.getFileName().toString();
                              if (parentName.equalsIgnoreCase("backup")) {
                                  return false; // 在 backup 目录中，跳过
                              }
                              
                              // 确保文件在 pngDir 的直接子目录中（不是子目录的子目录）
                              // 通过比较父目录和 pngDir 来判断
                              try {
                                  Path relativePath = pngDir.relativize(p);
                                  // 如果相对路径包含路径分隔符，说明文件在子目录中
                                  if (relativePath.getNameCount() > 1) {
                                      return false; // 文件在子目录中，跳过（只处理 pngDir 直接子文件）
                                  }
                              } catch (IllegalArgumentException e) {
                                  // 如果无法计算相对路径，说明文件不在 pngDir 下，跳过
                                  return false;
                              }
                              
                              return true;
                          })
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

