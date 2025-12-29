package com.lanye.dolladdon.dynamic.render;

import com.lanye.dolladdon.base.render.BaseDollRenderer;
import com.lanye.dolladdon.dynamic.DynamicDollEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 动态玩偶实体渲染器
 * 用于从文件加载的玩偶
 */
public class DynamicDollRenderer extends BaseDollRenderer<DynamicDollEntity> {
    private final Identifier skinLocation;
    
    public DynamicDollRenderer(EntityRendererFactory.Context context, Identifier skinLocation, boolean isAlexModel) {
        super(context, new PlayerEntityModel<>(
            context.getPart(isAlexModel ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER), 
            isAlexModel
        ));
        this.skinLocation = skinLocation;
    }
    
    @Override
    protected Identifier getSkinLocation(DynamicDollEntity entity) {
        return skinLocation;
    }
}

