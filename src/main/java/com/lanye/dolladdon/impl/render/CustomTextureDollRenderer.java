package com.lanye.dolladdon.impl.render;

import com.lanye.dolladdon.base.render.BaseDollRenderer;
import com.lanye.dolladdon.impl.entity.CustomTextureDollEntity;
import com.lanye.dolladdon.util.resource.ExternalTextureLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.nio.file.Path;

/**
 * 自定义纹理玩偶实体渲染器
 * 使用外部 PNG 文件作为纹理
 * 根据文件名自动检测模型类型（细手臂/粗手臂）
 */
public class CustomTextureDollRenderer extends BaseDollRenderer<CustomTextureDollEntity> {
    
    /**
     * 构造函数
     * @param context 渲染器上下文
     * @param isAlexModel true 表示使用细手臂模型（Alex），false 表示使用粗手臂模型（Steve）
     */
    public CustomTextureDollRenderer(EntityRendererFactory.Context context, boolean isAlexModel) {
        super(context, new PlayerEntityModel<>(
            context.getPart(isAlexModel ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER), 
            isAlexModel
        ));
    }
    
    /**
     * 检测皮肤是否为细手臂模型（Alex类型）
     * 根据文件名判断：
     * - 如果文件名以 "A" 开头，使用细手臂模型
     * - 如果文件名包含 "slim" 或 "alex"（不区分大小写），使用细手臂模型
     * - 否则使用粗手臂模型（Steve类型）
     * 
     * @param registryName 注册名称
     * @param textureId 纹理标识符
     * @return true 表示使用细手臂模型，false 表示使用粗手臂模型
     */
    public static boolean detectIsAlexModel(String registryName, Identifier textureId) {
        // 检查注册名称（registryName）是否以 "a" 开头
        if (registryName != null && registryName.length() > 0) {
            char firstChar = registryName.charAt(0);
            if (firstChar == 'a' || firstChar == 'A') {
                return true;
            }
        }
        
        // 尝试从纹理路径获取文件名
        String texturePath = textureId.getPath();
        
        // 检查路径中是否包含 "slim" 或 "alex"
        String lowerPath = texturePath.toLowerCase();
        if (lowerPath.contains("slim") || lowerPath.contains("alex")) {
            return true;
        }
        
        // 尝试从文件路径获取原始文件名
        Path filePath = ExternalTextureLoader.getTexturePath(textureId);
        if (filePath != null) {
            String fileName = filePath.getFileName().toString();
            // 移除扩展名
            String nameWithoutExt = fileName;
            if (nameWithoutExt.toLowerCase().endsWith(".png")) {
                nameWithoutExt = nameWithoutExt.substring(0, nameWithoutExt.length() - 4);
            }
            
            // 检查文件名是否以 "A" 开头（表示Alex/细手臂）
            if (nameWithoutExt.length() > 0 && nameWithoutExt.charAt(0) == 'A') {
                return true;
            }
            
            // 检查文件名是否包含 "slim" 或 "alex"
            String lowerName = nameWithoutExt.toLowerCase();
            if (lowerName.contains("slim") || lowerName.contains("alex")) {
                return true;
            }
        }
        
        // 默认使用粗手臂模型
        return false;
    }
    
    @Override
    protected Identifier getSkinLocation(CustomTextureDollEntity entity) {
        Identifier textureId = entity.getTextureIdentifier();
        
        // 确保纹理已加载
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.getTextureManager() != null) {
            // 如果纹理文件存在但未加载，尝试加载它
            if (ExternalTextureLoader.getTexturePath(textureId) != null) {
                ExternalTextureLoader.loadTexture(textureId, client.getTextureManager());
            }
        }
        
        return textureId;
    }
}

