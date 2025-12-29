package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.util.DynamicTextureManager;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 动态玩偶加载器
 * 扫描指定目录下的PNG文件并解析为玩偶信息
 */
public class DynamicDollLoader {
    private static final Logger LOGGER = PlayerDollAddon.LOGGER;
    
    /**
     * 玩偶信息类
     */
    public static class DollInfo {
        private final String fileName;           // 原始文件名（不含扩展名）
        private final String displayName;        // 显示名称（处理后的）
        private final boolean isAlexModel;       // 是否为Alex模型（细手臂）
        private final ResourceLocation textureLocation; // 纹理资源位置
        private final Path filePath;             // 文件路径
        
        public DollInfo(String fileName, String displayName, boolean isAlexModel, 
                       ResourceLocation textureLocation, Path filePath) {
            this.fileName = fileName;
            this.displayName = displayName;
            this.isAlexModel = isAlexModel;
            this.textureLocation = textureLocation;
            this.filePath = filePath;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public boolean isAlexModel() {
            return isAlexModel;
        }
        
        public ResourceLocation getTextureLocation() {
            return textureLocation;
        }
        
        public Path getFilePath() {
            return filePath;
        }
    }
    
    /**
     * 扫描目录并加载所有PNG文件
     * @param directoryPath 目录路径（相对于游戏目录）
     * @return 玩偶信息列表
     */
    public static List<DollInfo> scanDirectory(String directoryPath) {
        List<DollInfo> dollInfos = new ArrayList<>();
        
        try {
            // 获取游戏目录
            Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
            
            Path targetDir = gameDir.resolve(directoryPath).normalize();
            
            // 检查目录是否存在
            if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
                // 尝试创建目录
                try {
                    Files.createDirectories(targetDir);
                } catch (IOException e) {
                    LOGGER.error("无法创建玩偶材质目录: {}", targetDir, e);
                    return dollInfos;
                }
            }
            
            // 扫描PNG文件
            try (Stream<Path> paths = Files.walk(targetDir)) {
                paths.filter(Files::isRegularFile)
                     .filter(path -> path.toString().toLowerCase().endsWith(".png"))
                     .forEach(path -> {
                         try {
                             DollInfo info = parseDollFile(path, targetDir);
                             if (info != null) {
                                 dollInfos.add(info);
                             }
                         } catch (Exception e) {
                             LOGGER.error("解析玩偶文件失败: {}", path, e);
                         }
                     });
            }
            
        } catch (Exception e) {
            LOGGER.error("扫描玩偶材质目录失败: {}", directoryPath, e);
        }
        
        return dollInfos;
    }
    
    /**
     * 解析玩偶文件
     * @param filePath 文件路径
     * @param baseDir 基础目录
     * @return 玩偶信息，如果解析失败返回null
     */
    private static DollInfo parseDollFile(Path filePath, Path baseDir) {
        String fileName = filePath.getFileName().toString();
        
        // 移除扩展名
        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));
        
        // 检查文件名长度
        if (nameWithoutExt.length() < 2) {
            return null;
        }
        
        // 获取第一个字符作为模型类型标识
        char modelType = nameWithoutExt.charAt(0);
        boolean isAlexModel;
        
        if (modelType == 'A' || modelType == 'a') {
            isAlexModel = true;
        } else if (modelType == 'S' || modelType == 's') {
            isAlexModel = false;
        } else {
            return null;
        }
        
        // 获取名称部分（去掉第一个字符）
        String namePart = nameWithoutExt.substring(1);
        
        // 处理名称：单个下划线替换为空格，双下划线替换为单个下划线
        String displayName = processDisplayName(namePart);
        
        // 计算文件哈希值（用于 ResourceLocation，因为 ResourceLocation 不支持中文字符）
        String fileHash = calculateFileHash(filePath);
        if (fileHash == null) {
            LOGGER.error("无法计算文件哈希值，跳过: {}", fileName);
            return null;
        }
        
        // 使用哈希值作为资源路径（确保符合 ResourceLocation 的要求：只包含 [a-z0-9/._-]）
        String resourcePath = "textures/entity/" + fileHash;
        ResourceLocation textureLocation;
        try {
            textureLocation = new ResourceLocation(
                PlayerDollAddon.MODID, 
                resourcePath
            );
        } catch (Exception e) {
            LOGGER.error("创建 ResourceLocation 失败: {} (资源路径: {})", fileName, resourcePath, e);
            return null;
        }
        
        // 注册纹理文件路径到管理器
        DynamicTextureManager.registerTexture(textureLocation, filePath);
        
        // 生成注册名称（使用哈希值，因为它已经符合规范）
        // 加上前缀标识符，确保模型文件名和注册名称一致，Minecraft 才能找到对应的模型文件
        String registryName = com.lanye.dolladdon.util.DynamicModelGenerator.DYNAMIC_MODEL_PREFIX + fileHash;
        
        return new DollInfo(registryName, displayName, isAlexModel, textureLocation, filePath);
    }
    
    /**
     * 处理显示名称
     * 单个下划线替换为空格，双下划线替换为单个下划线
     * @param name 原始名称
     * @return 处理后的名称
     */
    private static String processDisplayName(String name) {
        // 先处理双下划线
        String result = name.replace("__", "\u0000"); // 使用临时字符标记
        // 再处理单下划线
        result = result.replace("_", " ");
        // 恢复双下划线（替换临时标记为单个下划线）
        result = result.replace("\u0000", "_");
        return result;
    }
    
    /**
     * 计算文件的 MD5 哈希值
     * 用于生成符合 ResourceLocation 规范的标识符（只包含 [a-z0-9]）
     * @param filePath 文件路径
     * @return 文件的 MD5 哈希值（小写字符串），如果失败返回 null
     */
    private static String calculateFileHash(Path filePath) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = md.digest(fileBytes);
            
            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("MD5 算法不可用", e);
            return null;
        } catch (IOException e) {
            LOGGER.error("读取文件失败，无法计算哈希值: {}", filePath, e);
            return null;
        }
    }
}

