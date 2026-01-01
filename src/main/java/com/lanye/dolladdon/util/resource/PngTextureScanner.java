package com.lanye.dolladdon.util.resource;

import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * PNG 纹理扫描器
 * 用于扫描外部文件夹中的 PNG 文件
 */
public class PngTextureScanner {
    
    // PNG 文件夹路径（相对于游戏目录）
    public static final String PNG_DIR = "player_doll/png";
    
    /**
     * 扫描 PNG 文件夹，获取所有 PNG 文件
     * @return PNG 文件信息列表（包含文件名和路径）
     */
    public static List<PngTextureInfo> scanPngFiles() {
        List<PngTextureInfo> pngFiles = new ArrayList<>();
        
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir();
            Path pngDir = gameDir.resolve(PNG_DIR);
            
            // 如果文件夹不存在，创建它
            if (!Files.exists(pngDir)) {
                Files.createDirectories(pngDir);
                ModuleLogger.warn(LogModuleConfig.MODULE_TEXTURE_SCANNER, "[PNG扫描] PNG 文件夹不存在，已创建: {}", pngDir);
                return pngFiles; // 返回空列表
            }
            
            if (!Files.isDirectory(pngDir)) {
                ModuleLogger.error(LogModuleConfig.MODULE_TEXTURE_SCANNER, "[PNG扫描] 路径不是文件夹: {}", pngDir);
                return pngFiles;
            }
            
            // 扫描文件夹中的所有 PNG 文件
            try (Stream<Path> paths = Files.walk(pngDir)) {
                paths.filter(Files::isRegularFile)
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
                                     ModuleLogger.warn(LogModuleConfig.MODULE_TEXTURE_SCANNER, "[PNG扫描] ✗ 跳过无效的 PNG 文件名: {} (无法生成有效的注册名称)", fileName);
                                 }
                             } catch (Exception e) {
                                 ModuleLogger.error(LogModuleConfig.MODULE_TEXTURE_SCANNER, "[PNG扫描] ✗ 处理 PNG 文件时出错: {}", path, e);
                             }
                         }
                     });
            }
            
            if (pngFiles.isEmpty()) {
                ModuleLogger.warn(LogModuleConfig.MODULE_TEXTURE_SCANNER, "[PNG扫描] 警告: 未找到任何 PNG 文件！请确保文件位于: {}", pngDir);
            }
        } catch (IOException e) {
            ModuleLogger.error(LogModuleConfig.MODULE_TEXTURE_SCANNER, "[PNG扫描] ✗ 扫描 PNG 文件夹时出错", e);
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
         * @return Identifier
         */
        public net.minecraft.util.Identifier getTextureIdentifier() {
            return new net.minecraft.util.Identifier(PlayerDollAddon.MODID, "textures/entity/custom_doll/" + registryName);
        }
    }
}

