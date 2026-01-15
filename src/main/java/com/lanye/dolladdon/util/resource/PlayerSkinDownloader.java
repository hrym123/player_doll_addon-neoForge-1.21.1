package com.lanye.dolladdon.util.resource;

import com.lanye.dolladdon.PlayerDollAddon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 玩家皮肤下载器
 * 从服务器获取玩家皮肤纹理并保存为PNG文件
 */
public class PlayerSkinDownloader {
    
    /**
     * 是否允许使用回退方案
     * 可以通过系统属性 "player_doll.allow_skin_fallback" 控制
     * - true（默认）：允许使用回退方案（开发模式友好）
     * - false：禁用回退方案，只使用Mojang API（用于测试真实流程）
     */
    private static final boolean ALLOW_FALLBACK = 
        !"false".equalsIgnoreCase(System.getProperty("player_doll.allow_skin_fallback", "true"));
    
    /**
     * 下载结果类
     */
    public static class DownloadResult {
        private final boolean success;
        private final boolean fallback; // 是否使用了回退方案
        private final String errorMessage;
        
        private DownloadResult(boolean success, boolean fallback, String errorMessage) {
            this.success = success;
            this.fallback = fallback;
            this.errorMessage = errorMessage;
        }
        
        public static DownloadResult success(boolean fallback) {
            return new DownloadResult(true, fallback, null);
        }
        
        public static DownloadResult failure(String errorMessage) {
            return new DownloadResult(false, false, errorMessage);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public boolean isFallback() {
            return fallback;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
    
    /**
     * 下载指定玩家的皮肤并保存为PNG文件
     * 
     * @param player 玩家对象
     * @param targetPath 目标文件路径
     * @param overwrite 是否覆盖已存在的文件
     * @return 下载结果
     */
    public static DownloadResult downloadPlayerSkin(ServerPlayer player, Path targetPath, boolean overwrite) {
        if (player == null) {
            return DownloadResult.failure("玩家对象为null");
        }
        
        UUID playerUUID = player.getUUID();
        String playerName = player.getName().getString();
        
        return downloadPlayerSkin(playerUUID, playerName, targetPath, overwrite);
    }
    
    /**
     * 下载指定玩家的皮肤并保存为PNG文件
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称
     * @param targetPath 目标文件路径
     * @param overwrite 是否覆盖已存在的文件
     * @return 下载结果
     */
    public static DownloadResult downloadPlayerSkin(UUID playerUUID, String playerName, Path targetPath, boolean overwrite) {
        try {
            // 检查文件是否已存在
            if (Files.exists(targetPath) && !overwrite) {
                return DownloadResult.failure("文件已存在");
            }
            
            // 判断模型类型
            boolean isAlexModel = PlayerSkinUtil.isAlexModel(playerUUID, playerName);
            
            boolean usedFallback = false;
            BufferedImage skinImage = null;
            
            // 方法1：尝试从Mojang皮肤服务器下载皮肤
            skinImage = downloadSkinFromMojang(playerUUID);
            
            // 如果允许回退方案，尝试使用回退方法
            if (skinImage == null && ALLOW_FALLBACK) {
                // 方法2：如果Mojang API失败（开发模式或离线模式），尝试从本地资源获取
                skinImage = downloadSkinFromLocal(playerUUID, playerName);
                if (skinImage != null) {
                    usedFallback = true;
                }
                
                // 方法3：如果还是失败，使用默认皮肤作为测试（开发模式）
                if (skinImage == null) {
                    skinImage = getDefaultSkinImage(isAlexModel);
                    if (skinImage != null) {
                        usedFallback = true;
                    }
                }
            }
            
            if (skinImage == null) {
                String errorMsg = "无法获取皮肤（Mojang API失败";
                if (!ALLOW_FALLBACK) {
                    errorMsg += "，回退方案已禁用）";
                } else {
                    errorMsg += "，且无法使用本地资源）";
                }
                return DownloadResult.failure(errorMsg);
            }
            
            // 确保目录存在
            Path parentDir = targetPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // 保存为PNG文件
            boolean saved = saveSkinToFile(skinImage, targetPath);
            if (!saved) {
                return DownloadResult.failure("保存文件失败");
            }
            
            return DownloadResult.success(usedFallback);
            
        } catch (Exception e) {
            // Error logging handled by Mixin
            return DownloadResult.failure("异常: " + e.getMessage());
        }
    }
    
    /**
     * 从Mojang皮肤服务器下载皮肤
     * 
     * @param playerUUID 玩家UUID
     * @return 皮肤图像，如果下载失败返回null
     */
    private static BufferedImage downloadSkinFromMojang(UUID playerUUID) {
        try {
            // 构建Mojang皮肤服务器URL
            // 格式：https://sessionserver.mojang.com/session/minecraft/profile/<UUID（无连字符）>?unsigned=false
            // 然后从profile中获取textures.skin.url
            
            // 从Mojang API获取皮肤URL
            // UUID需要去掉连字符
            String uuidString = playerUUID.toString().replace("-", "");
            String profileUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + 
                               uuidString + "?unsigned=false";
            
            // 读取profile JSON
            String profileJson = readUrlContent(profileUrl);
            if (profileJson == null || profileJson.isEmpty()) {
                return null;
            }
            
            // 解析JSON获取皮肤URL
            String skinUrl = extractSkinUrlFromProfile(profileJson);
            if (skinUrl == null || skinUrl.isEmpty()) {
                return null;
            }
            
            // 下载皮肤图像
            return downloadImageFromUrl(skinUrl);
            
        } catch (Exception e) {
            // Error logging handled by Mixin
            return null;
        }
    }
    
    /**
     * 从URL读取内容
     */
    private static String readUrlContent(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000); // 减少连接超时时间
            connection.setReadTimeout(10000); // 读取超时时间稍长一些
            connection.setRequestProperty("User-Agent", "Minecraft-PlayerDoll-Mod/1.0");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }
            
            try (InputStream inputStream = connection.getInputStream()) {
                byte[] buffer = new byte[8192];
                StringBuilder result = new StringBuilder();
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    result.append(new String(buffer, 0, bytesRead, java.nio.charset.StandardCharsets.UTF_8));
                }
                return result.toString();
            }
        } catch (Exception e) {
            // Error logging handled by Mixin
            return null;
        }
    }
    
    /**
     * 从profile JSON中提取皮肤URL
     */
    private static String extractSkinUrlFromProfile(String profileJson) {
        try {
            // 简单的JSON解析（查找 "value" 字段中的base64编码的JSON）
            // 更健壮的方法应该使用JSON库，但为了减少依赖，这里使用简单方法
            
            // 查找 "value" 字段
            int valueIndex = profileJson.indexOf("\"value\"");
            if (valueIndex == -1) {
                return null;
            }
            
            // 提取value的值（base64编码的JSON）
            int startIndex = profileJson.indexOf("\"", valueIndex + 7) + 1;
            int endIndex = profileJson.indexOf("\"", startIndex);
            if (startIndex <= 0 || endIndex <= startIndex) {
                return null;
            }
            
            String base64Value = profileJson.substring(startIndex, endIndex);
            
            // 解码base64
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Value);
            String decodedJson = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            
            // 从解码的JSON中提取皮肤URL
            // 查找 "url" 字段（在 "SKIN" textures中）
            int urlIndex = decodedJson.indexOf("\"url\"");
            if (urlIndex == -1) {
                return null;
            }
            
            // 提取URL值
            int urlStartIndex = decodedJson.indexOf("\"", urlIndex + 5) + 1;
            int urlEndIndex = decodedJson.indexOf("\"", urlStartIndex);
            if (urlStartIndex <= 0 || urlEndIndex <= urlStartIndex) {
                return null;
            }
            
            return decodedJson.substring(urlStartIndex, urlEndIndex);
            
        } catch (Exception e) {
            // Error logging handled by Mixin
            return null;
        }
    }
    
    /**
     * 从URL下载图像
     */
    private static BufferedImage downloadImageFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000); // 减少连接超时时间
            connection.setReadTimeout(15000); // 图像下载可能需要更长时间
            connection.setRequestProperty("User-Agent", "Minecraft-PlayerDoll-Mod/1.0");
            
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return null;
            }
            
            try (InputStream inputStream = connection.getInputStream()) {
                return ImageIO.read(inputStream);
            }
        } catch (Exception e) {
            // Error logging handled by Mixin
            return null;
        }
    }
    
    /**
     * 将皮肤图像保存为PNG文件
     * 
     * @param skinImage 皮肤图像
     * @param targetPath 目标文件路径
     * @return 是否成功保存
     */
    public static boolean saveSkinToFile(BufferedImage skinImage, Path targetPath) {
        if (skinImage == null || targetPath == null) {
            return false;
        }
        
        try {
            // 确保目录存在
            Path parentDir = targetPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // 保存为PNG文件
            return ImageIO.write(skinImage, "PNG", targetPath.toFile());
            
        } catch (IOException e) {
            // Error logging handled by Mixin
            return false;
        }
    }
    
    /**
     * 从本地游戏资源获取玩家皮肤（开发模式回退方案）
     * 当无法从Mojang API获取时，尝试从游戏内的纹理资源获取
     * 注意：此方法仅在客户端可用，服务器端会返回null
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称
     * @return 皮肤图像，如果获取失败返回null
     */
    private static BufferedImage downloadSkinFromLocal(UUID playerUUID, String playerName) {
        try {
            // 仅在客户端尝试从资源管理器获取
            if (net.neoforged.fml.loading.FMLEnvironment.dist != net.neoforged.api.distmarker.Dist.CLIENT) {
                // 服务器端无法访问资源管理器，返回null
                return null;
            }
            
            // 获取玩家皮肤纹理位置
            ResourceLocation skinLocation = PlayerSkinUtil.getSkinLocation(playerUUID, playerName);
            if (skinLocation == null) {
                return null;
            }
            
            // 尝试从资源管理器加载纹理
            try {
                var minecraft = net.minecraft.client.Minecraft.getInstance();
                if (minecraft != null && minecraft.getResourceManager() != null) {
                    var resourceManager = minecraft.getResourceManager();
                    var resource = resourceManager.getResource(skinLocation);
                    if (resource.isPresent()) {
                        try (InputStream inputStream = resource.get().open()) {
                            return ImageIO.read(inputStream);
                        }
                    }
                }
            } catch (Exception e) {
                // 资源获取失败，返回null（会使用默认皮肤）
            }
            
            return null;
            
        } catch (Exception e) {
            // Error logging handled by Mixin
            return null;
        }
    }
    
    /**
     * 获取默认皮肤图像（开发模式测试用）
     * 当无法从Mojang API或本地资源获取时，生成一个测试皮肤
     * 
     * @param isAlexModel 是否为Alex模型
     * @return 默认皮肤图像
     */
    private static BufferedImage getDefaultSkinImage(boolean isAlexModel) {
        try {
            // 首先尝试从资源管理器加载默认皮肤（仅在客户端）
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                try {
                    ResourceLocation skinLocation = isAlexModel ? 
                        PlayerSkinUtil.getAlexSkin() : 
                        PlayerSkinUtil.getSteveSkin();
                    
                    if (skinLocation != null) {
                        var minecraft = net.minecraft.client.Minecraft.getInstance();
                        if (minecraft != null && minecraft.getResourceManager() != null) {
                            var resourceManager = minecraft.getResourceManager();
                            var resource = resourceManager.getResource(skinLocation);
                            if (resource.isPresent()) {
                                try (InputStream inputStream = resource.get().open()) {
                                    BufferedImage image = ImageIO.read(inputStream);
                                    if (image != null) {
                                        return image;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // 资源获取失败，继续使用生成的测试皮肤
                }
            }
            
            // 如果无法从资源管理器获取，创建一个简单的测试皮肤图像
            // 64x64像素，RGBA格式（标准Minecraft皮肤尺寸）
            BufferedImage defaultSkin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = defaultSkin.createGraphics();
            
            // 启用抗锯齿
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, 
                              java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            
            // 填充基础颜色（浅灰色，表示测试皮肤）
            g.setColor(new java.awt.Color(200, 200, 200, 255));
            g.fillRect(0, 0, 64, 64);
            
            // 绘制简单的测试标记（表示这是测试皮肤）
            g.setColor(new java.awt.Color(100, 100, 100, 255));
            g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 16));
            java.awt.FontMetrics fm = g.getFontMetrics();
            String label = isAlexModel ? "A" : "S";
            int textWidth = fm.stringWidth(label);
            int textHeight = fm.getHeight();
            g.drawString(label, (64 - textWidth) / 2, (64 + textHeight) / 2 - fm.getDescent());
            
            g.dispose();
            return defaultSkin;
            
        } catch (Exception e) {
            // Error logging handled by Mixin
            return null;
        }
    }
    
    /**
     * 获取游戏目录下的PNG目录路径
     */
    public static Path getPngDirectory() {
        try {
            // 获取游戏目录
            Path gameDir;
            try {
                Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
                java.lang.reflect.Method gameDirMethod = fmlPathsClass.getMethod("getGamePath");
                gameDir = (Path) gameDirMethod.invoke(null);
            } catch (Exception e) {
                gameDir = Paths.get(".").toAbsolutePath().normalize();
            }
            
            return gameDir.resolve(PlayerDollAddon.PNG_DIR).normalize();
        } catch (Exception e) {
            return Paths.get(PlayerDollAddon.PNG_DIR);
        }
    }
}
