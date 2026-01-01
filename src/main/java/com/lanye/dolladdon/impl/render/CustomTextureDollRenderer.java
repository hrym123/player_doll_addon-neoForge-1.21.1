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

/**
 * 自定义纹理玩偶实体渲染器
 * 使用外部 PNG 文件作为纹理
 */
public class CustomTextureDollRenderer extends BaseDollRenderer<CustomTextureDollEntity> {
    
    public CustomTextureDollRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false));
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

