package com.lanye.dolladdon.impl.render;

import com.lanye.dolladdon.base.render.BaseDollItemRenderer;
import com.lanye.dolladdon.util.PlayerSkinUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

/**
 * 史蒂夫玩偶物品渲染器
 * 固定使用Steve模型（粗手臂）和Steve默认皮肤
 */
public class SteveDollItemRenderer extends BaseDollItemRenderer {
    
    public SteveDollItemRenderer(MinecraftClient client, EntityModelLoader modelLoader) {
        super(client, new PlayerEntityModel<>(modelLoader.getModelPart(net.minecraft.client.render.entity.model.EntityModelLayers.PLAYER), false));
    }
    
    @Override
    protected Identifier getSkinLocation() {
        return PlayerSkinUtil.getSteveSkin();
    }
}

