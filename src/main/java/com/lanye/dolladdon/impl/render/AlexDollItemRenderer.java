package com.lanye.dolladdon.impl.render;

import com.lanye.dolladdon.base.render.BaseDollItemRenderer;
import com.lanye.dolladdon.util.PlayerSkinUtil;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;

/**
 * 艾利克斯玩偶物品渲染器
 * 固定使用Alex模型（细手臂）和Alex默认皮肤
 */
public class AlexDollItemRenderer extends BaseDollItemRenderer {
    
    public AlexDollItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet, new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER_SLIM), true));
    }
    
    @Override
    protected ResourceLocation getSkinLocation() {
        return PlayerSkinUtil.getAlexSkin();
    }
    
    @Override
    protected String getLogTag() {
        return "AlexDollItemRenderer";
    }
}

