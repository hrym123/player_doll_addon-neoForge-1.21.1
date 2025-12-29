package com.lanye.dolladdon.dynamic.render;

import com.lanye.dolladdon.base.render.BaseDollItemRenderer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;

/**
 * 动态玩偶物品渲染器
 * 用于从文件加载的玩偶
 */
public class DynamicDollItemRenderer extends BaseDollItemRenderer {
    private final ResourceLocation textureLocation;
    private final boolean isAlexModel;
    
    public DynamicDollItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet, 
                                  ResourceLocation textureLocation, boolean isAlexModel) {
        super(dispatcher, modelSet, new PlayerModel<>(
            modelSet.bakeLayer(isAlexModel ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), 
            isAlexModel
        ));
        this.textureLocation = textureLocation;
        this.isAlexModel = isAlexModel;
    }
    
    @Override
    protected ResourceLocation getSkinLocation() {
        return textureLocation;
    }
}

