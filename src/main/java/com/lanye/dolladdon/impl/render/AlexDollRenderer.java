package com.lanye.dolladdon.impl.render;

import com.lanye.dolladdon.base.render.BaseDollRenderer;
import com.lanye.dolladdon.impl.entity.AlexDollEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 艾利克斯玩偶实体渲染器
 * 固定使用Alex模型（细手臂）和Alex默认皮肤
 */
public class AlexDollRenderer extends BaseDollRenderer<AlexDollEntity> {
    
    public AlexDollRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER_SLIM), true));
    }
    
    @Override
    protected Identifier getSkinLocation(AlexDollEntity entity) {
        return PlayerSkinUtil.getAlexSkin();
    }
}

