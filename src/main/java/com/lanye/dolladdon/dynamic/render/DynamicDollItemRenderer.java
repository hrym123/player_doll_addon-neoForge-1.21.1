package com.lanye.dolladdon.dynamic.render;

import com.lanye.dolladdon.base.render.BaseDollItemRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/**
 * 动态玩偶物品渲染器
 * 用于从文件加载的玩偶
 */
public class DynamicDollItemRenderer extends BaseDollItemRenderer {
    private final Identifier textureLocation;
    private final boolean isAlexModel;
    
    public DynamicDollItemRenderer(MinecraftClient client, @SuppressWarnings("unused") Object unused, 
                                  Identifier textureLocation, boolean isAlexModel) {
        super(client, isAlexModel);
        this.textureLocation = textureLocation;
        this.isAlexModel = isAlexModel;
    }
    
    @Override
    protected Identifier getSkinLocation() {
        return textureLocation;
    }
}

