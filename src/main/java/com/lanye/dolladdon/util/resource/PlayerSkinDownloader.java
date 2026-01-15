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
     * 下载指定玩家的皮肤并保存为PNG文件
     * 
     * @param player 玩家对象
     * @param targetPath 目标文件路径
     * @param overwrite 是否覆盖已存在的文件
     * @return 是否成功下载和保存
     */
    public static boolean downloadPlayerSkin(ServerPlayer player, Path targetPath, boolean overwrite) {
        if (player == null) {
            return false;
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
     * @return 是否成功下载和保存
     */
    public static boolean downloadPlayerSkin(UUID playerUUID, String playerName, Path targetPath, boolean overwrite) {
        try {
            // 检查文件是否已存在
            if (Files.exists(targetPath) && !overwrite) {
                return false;
            }
            
            // 判断模型类型
            boolean isAlexModel = PlayerSkinUtil.isAlexModel(playerUUID, playerName);
            
            // 从Mojang皮肤服务器下载皮肤
            BufferedImage skinImage = downloadSkinFromMojang(playerUUID);
            if (skinImage == null) {
                return false;
            }
            
            // 确保目录存在
            Path parentDir = targetPath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // 保存为PNG文件
            return saveSkinToFile(skinImage, targetPath);
            
        } catch (Exception e) {
            // Error logging handled by Mixin
            return false;
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
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
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
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
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
