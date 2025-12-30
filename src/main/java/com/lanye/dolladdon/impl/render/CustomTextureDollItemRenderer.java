package com.lanye.dolladdon.impl.render;

import com.lanye.dolladdon.base.render.BaseDollItemRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/**
 * 自定义纹理玩偶物品渲染器
 * 使用外部 PNG 文件作为纹理
 * 默认使用粗手臂模型（Steve模型）
 */
public class CustomTextureDollItemRenderer extends BaseDollItemRenderer {
    private final Identifier textureIdentifier;
    
    public CustomTextureDollItemRenderer(MinecraftClient client, Identifier textureIdentifier) {
        super(client, false); // false 表示使用粗手臂模型（Steve）
        this.textureIdentifier = textureIdentifier;
    }
    
    @Override
    protected Identifier getSkinLocation() {
        return textureIdentifier;
    }
}

