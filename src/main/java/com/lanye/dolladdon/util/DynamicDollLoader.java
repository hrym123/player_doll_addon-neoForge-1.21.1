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
            // 优先尝试使用 FMLPaths，如果不可用则使用当前工作目录
            Path gameDir;
            try {
                // 尝试使用 NeoForge 的 FMLPaths 获取游戏目录
                Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
                java.lang.reflect.Method gameDirMethod = fmlPathsClass.getMethod("getGamePath");
                gameDir = (Path) gameDirMethod.invoke(null);
                LOGGER.info("使用 FMLPaths 获取游戏目录: {}", gameDir);
            } catch (Exception e) {
                // 如果 FMLPaths 不可用，使用当前工作目录
                gameDir = Paths.get(".").toAbsolutePath().normalize();
                LOGGER.warn("无法使用 FMLPaths，使用当前工作目录: {}", gameDir);
            }
            
            Path targetDir = gameDir.resolve(directoryPath).normalize();
            LOGGER.info("扫描玩偶材质目录: {} (绝对路径: {})", directoryPath, targetDir);
            
            // 检查目录是否存在
            if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
                LOGGER.warn("玩偶材质目录不存在: {}", targetDir);
                // 尝试创建目录
                try {
                    Files.createDirectories(targetDir);
                    LOGGER.info("已创建玩偶材质目录: {}", targetDir);
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
                                 LOGGER.info("加载玩偶材质: {} -> {}", info.getFileName(), info.getDisplayName());
                             }
                         } catch (Exception e) {
                             LOGGER.error("解析玩偶文件失败: {}", path, e);
                         }
                     });
            }
            
            LOGGER.info("共加载 {} 个玩偶材质", dollInfos.size());
            
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
            LOGGER.warn("文件名太短，跳过: {}", fileName);
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
            LOGGER.warn("文件名首字符不是S或A，跳过: {} (首字符: {})", fileName, modelType);
            return null;
        }
        
        // 获取名称部分（去掉第一个字符）
        String namePart = nameWithoutExt.substring(1);
        
        // 处理名称：单个下划线替换为空格，双下划线替换为单个下划线
        String displayName = processDisplayName(namePart);
        
        // 生成资源位置
        // 使用文件名作为资源路径（简化处理）
        // ResourceLocation 只允许小写字母，所以需要将文件名转换为小写
        String lowerFileName = fileName.toLowerCase();
        String resourcePath = "textures/entity/" + lowerFileName;
        ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
            PlayerDollAddon.MODID, 
            resourcePath
        );
        
        // 注册纹理文件路径到管理器
        DynamicTextureManager.registerTexture(textureLocation, filePath);
        
        // 生成注册名称（使用文件名，但转换为小写并替换特殊字符）
        String registryName = nameWithoutExt.toLowerCase()
            .replace(' ', '_')
            .replace('-', '_')
            .replaceAll("[^a-z0-9_]", "");
        
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
}

