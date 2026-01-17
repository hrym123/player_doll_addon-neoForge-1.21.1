package com.lanye.dolladdon.util.neoForge;

import com.lanye.dolladdon.PlayerDoll;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 动态纹理管理器
 * 管理从文件系统加载的纹理文件
 */
public class DynamicTextureManager {
    // Logger removed - logging handled by Mixin
    public static final Map<ResourceLocation, Path> TEXTURE_PATHS = new HashMap<>();
    
    /**
     * 路径映射表：原始文件名 -> ResourceLocation路径
     * 用于处理包含特殊字符的文件名，避免复制文件
     */
    private static final Map<String, ResourceLocation> PATH_MAPPING = new HashMap<>();
    
    /**
     * 注册纹理文件路径
     * @param resourceLocation 资源位置
     * @param filePath 文件路径
     */
    public static void registerTexture(ResourceLocation resourceLocation, Path filePath) {
        TEXTURE_PATHS.put(resourceLocation, filePath);
    }
    
    /**
     * 注册路径映射（原始文件名 -> ResourceLocation路径）
     * 用于处理包含特殊字符的文件名，避免复制文件
     * 
     * @param originalFileName 原始文件名（可能包含特殊字符）
     * @param resourceLocation ResourceLocation路径（符合规范）
     */
    public static void registerPathMapping(String originalFileName, ResourceLocation resourceLocation) {
        PATH_MAPPING.put(originalFileName, resourceLocation);
    }
    
    /**
     * 获取路径映射的ResourceLocation
     * 如果原始文件名有映射，返回映射的ResourceLocation；否则返回null
     * 
     * @param originalFileName 原始文件名
     * @return 映射的ResourceLocation，如果不存在返回null
     */
    public static ResourceLocation getMappedResourceLocation(String originalFileName) {
        return PATH_MAPPING.get(originalFileName);
    }
    
    /**
     * 检查原始文件名是否有路径映射
     * 
     * @param originalFileName 原始文件名
     * @return 是否有映射
     */
    public static boolean hasPathMapping(String originalFileName) {
        return PATH_MAPPING.containsKey(originalFileName);
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
        PATH_MAPPING.clear();
    }
    
    /**
     * 扫描并注册 player_doll/png 目录下的所有 PNG 文件
     * 在游戏启动或资源重载时调用，确保所有纹理都被注册
     * 
     * 对于包含特殊字符的文件名，使用哈希文件名注册（与指令逻辑保持一致）
     * 
     * @param gameDir 游戏目录
     */
    public static void scanAndRegisterTextures(java.nio.file.Path gameDir) {
        try {
            java.nio.file.Path pngDir = gameDir.resolve(PlayerDoll.PNG_DIR);
            if (!java.nio.file.Files.exists(pngDir) || !java.nio.file.Files.isDirectory(pngDir)) {
                return;
            }
            
            try (var stream = java.nio.file.Files.list(pngDir)) {
                stream.filter(java.nio.file.Files::isRegularFile)
                      .filter(path -> path.toString().toLowerCase().endsWith(".png"))
                      // 排除 backup 目录下的文件
                      // 同时确保文件在 pngDir 的直接子目录中，不在子目录的子目录中
                      .filter(path -> {
                          // 获取文件的父目录
                          java.nio.file.Path parent = path.getParent();
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
                              java.nio.file.Path relativePath = pngDir.relativize(path);
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
                              
                              // 检查文件名是否包含特殊字符（与指令逻辑保持一致）
                              boolean containsNonAscii = fileName.chars().anyMatch(ch -> ch > 127 || (ch < 32 && ch != 9 && ch != 10 && ch != 13));
                              boolean containsUpperCase = fileName.chars().anyMatch(ch -> ch >= 'A' && ch <= 'Z');
                              boolean containsInvalidChars = fileName.chars().anyMatch(ch -> {
                                  return !((ch >= 'a' && ch <= 'z') || 
                                          (ch >= '0' && ch <= '9') || 
                                          ch == '.' || ch == '_' || ch == '-');
                              });
                              
                              ResourceLocation textureLocation;
                              String safeFileName = null;
                              
                              if (containsNonAscii || containsUpperCase || containsInvalidChars) {
                                  // 如果包含特殊字符，使用哈希文件名（与指令逻辑保持一致）
                                  try {
                                      java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                                      byte[] hashBytes = md.digest(fileName.getBytes("UTF-8"));
                                      StringBuilder hashString = new StringBuilder();
                                      for (byte b : hashBytes) {
                                          hashString.append(String.format("%02x", b));
                                      }
                                      safeFileName = "skin_" + hashString.substring(0, 16) + ".png";
                                  } catch (Exception e) {
                                      int fileNameHash = fileName.hashCode();
                                      safeFileName = "skin_" + Integer.toHexString(Math.abs(fileNameHash)) + ".png";
                                  }
                                  
                                  textureLocation = ResourceLocation.fromNamespaceAndPath(
                                      PlayerDoll.MODID, 
                                      "png/" + safeFileName
                                  );
                                  
                                  // 注册纹理路径（使用哈希文件名）
                                  registerTexture(textureLocation, pngFile);
                                  
                                  // 注册路径映射（原始文件名 -> 哈希文件名）
                                  registerPathMapping(fileName, textureLocation);
                              } else {
                                  // 如果文件名不包含特殊字符，直接使用文件名
                                  textureLocation = ResourceLocation.fromNamespaceAndPath(
                                      PlayerDoll.MODID, 
                                      "png/" + fileName
                                  );
                                  
                                  // 注册纹理路径
                                  registerTexture(textureLocation, pngFile);
                              }
                          } catch (Exception e) {
                              // Error logging handled by Mixin
                          }
                      });
            }
        } catch (Exception e) {
            // Error logging handled by Mixin
        }
    }
}

