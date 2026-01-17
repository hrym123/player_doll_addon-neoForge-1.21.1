package com.lanye.dolladdon.util.resource;

import com.lanye.dolladdon.PlayerDoll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * PNG 纹理扫描器
 * 用于扫描外部文件夹中的 PNG 文件
 */
public class PngTextureScanner {
    // Logger removed - logging handled by Mixin
    
    // PNG 文件夹路径（相对于游戏目录）
    public static final String PNG_DIR = "player_doll/png";
    
    /**
     * 获取游戏目录
     */
    private static Path getGameDir() {
        try {
            Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
            java.lang.reflect.Method gameDirMethod = fmlPathsClass.getMethod("getGamePath");
            return (Path) gameDirMethod.invoke(null);
        } catch (Exception e) {
            return Paths.get(".").toAbsolutePath().normalize();
        }
    }
    
    /**
     * 扫描 PNG 文件夹，获取所有 PNG 文件
     * @return PNG 文件信息列表（包含文件名和路径）
     */
    public static List<PngTextureInfo> scanPngFiles() {
        List<PngTextureInfo> pngFiles = new ArrayList<>();
        
        try {
            Path gameDir = getGameDir();
            Path pngDir = gameDir.resolve(PNG_DIR);
            
            // 如果文件夹不存在，创建它
            if (!Files.exists(pngDir)) {
                Files.createDirectories(pngDir);
                // Warning logging handled by Mixin
                return pngFiles; // 返回空列表
            }
            
            if (!Files.isDirectory(pngDir)) {
                // Error logging handled by Mixin
                return pngFiles;
            }
            
            // 扫描文件夹中的所有 PNG 文件
            // 注意：Files.walk() 会递归遍历所有子目录，所以需要排除 backup 目录
            try (Stream<Path> paths = Files.walk(pngDir)) {
                paths.filter(Files::isRegularFile)
                     .filter(path -> {
                         // 排除 backup 目录下的文件（检查父目录名称）
                         Path parent = path.getParent();
                         if (parent != null) {
                             String parentName = parent.getFileName().toString();
                             if (parentName.equalsIgnoreCase("backup")) {
                                 return false; // 在 backup 目录中，跳过
                             }
                         }
                         
                         // 确保文件在 pngDir 的直接子目录中（不是子目录的子目录）
                         // 通过计算相对路径来判断
                         try {
                             Path relativePath = pngDir.relativize(path);
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
                     .forEach(path -> {
                         String fileName = path.getFileName().toString();
                         String lowerFileName = fileName.toLowerCase();
                         
                         if (lowerFileName.endsWith(".png")) {
                             try {
                                 // 移除 .png 扩展名
                                 String nameWithoutExt = fileName.substring(0, fileName.length() - 4);
                                 
                                 // 生成有效的注册名称（只包含小写字母、数字、下划线）
                                 String registryName = sanitizeRegistryName(nameWithoutExt);
                                 
                                 if (!registryName.isEmpty()) {
                                     pngFiles.add(new PngTextureInfo(registryName, path, fileName));
                                 } else {
                                     // Warning logging handled by Mixin
                                 }
                             } catch (Exception e) {
                                 // Error logging handled by Mixin
                             }
                         }
                     });
            }
            
            if (pngFiles.isEmpty()) {
                // Warning logging handled by Mixin
            }
        } catch (IOException e) {
            // Error logging handled by Mixin
        }
        
        return pngFiles;
    }
    
    /**
     * 清理文件名，生成有效的注册名称
     * 只保留小写字母、数字、下划线
     * @param fileName 原始文件名
     * @return 清理后的注册名称
     */
    private static String sanitizeRegistryName(String fileName) {
        StringBuilder sb = new StringBuilder();
        for (char c : fileName.toCharArray()) {
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_') {
                sb.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                sb.append(Character.toLowerCase(c));
            } else {
                // 其他字符替换为下划线
                sb.append('_');
            }
        }
        
        // 移除连续的下划线
        String result = sb.toString().replaceAll("_{2,}", "_");
        
        // 移除开头和结尾的下划线
        if (result.startsWith("_")) {
            result = result.substring(1);
        }
        if (result.endsWith("_")) {
            result = result.substring(0, result.length() - 1);
        }
        
        return result;
    }
    
    /**
     * PNG 纹理信息
     */
    public static class PngTextureInfo {
        private final String registryName;  // 注册名称（用于物品和实体）
        private final Path filePath;         // 文件路径
        private final String fileName;       // 原始文件名
        
        public PngTextureInfo(String registryName, Path filePath, String fileName) {
            this.registryName = registryName;
            this.filePath = filePath;
            this.fileName = fileName;
        }
        
        public String getRegistryName() {
            return registryName;
        }
        
        public Path getFilePath() {
            return filePath;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        /**
         * 获取纹理标识符（用于资源包）
         * @return ResourceLocation
         */
        public net.minecraft.resources.ResourceLocation getTextureIdentifier() {
            return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                PlayerDoll.MODID, 
                "textures/entity/custom_doll/" + registryName
            );
        }
    }
}
