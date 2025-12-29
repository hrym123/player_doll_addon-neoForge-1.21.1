package com.lanye.dolladdon.impl.render;

import com.lanye.dolladdon.base.render.BaseDollItemRenderer;
import com.lanye.dolladdon.util.PlayerSkinUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

/**
 * 艾利克斯玩偶物品渲染器
 * 固定使用Alex模型（细手臂）和Alex默认皮肤
 */
public class AlexDollItemRenderer extends BaseDollItemRenderer {
    
    public AlexDollItemRenderer(MinecraftClient client, EntityModelLoader modelLoader) {
        super(client, new PlayerEntityModel<>(modelLoader.getModelPart(net.minecraft.client.render.entity.model.EntityModelLayers.PLAYER_SLIM), true));
    }
    
    @Override
    protected Identifier getSkinLocation() {
        return PlayerSkinUtil.getAlexSkin();
    }
}

