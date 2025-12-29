package com.lanye.dolladdon.impl.render;

import com.lanye.dolladdon.base.render.BaseDollItemRenderer;
import com.lanye.dolladdon.util.PlayerSkinUtil;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;

/**
 * 史蒂夫玩偶物品渲染器
 * 固定使用Steve模型（粗手臂）和Steve默认皮肤
 */
public class SteveDollItemRenderer extends BaseDollItemRenderer {
    
    public SteveDollItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet, new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER), false));
    }
    
    @Override
    protected ResourceLocation getSkinLocation() {
        return PlayerSkinUtil.getSteveSkin();
    }
}

