package com.lanye.dolladdon.impl.render;

import com.lanye.dolladdon.base.render.BaseDollRenderer;
import com.lanye.dolladdon.impl.entity.SteveDollEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 史蒂夫玩偶实体渲染器
 * 固定使用Steve模型（粗手臂）和Steve默认皮肤
 */
public class SteveDollRenderer extends BaseDollRenderer<SteveDollEntity> {
    
    public SteveDollRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false));
    }
    
    @Override
    protected Identifier getSkinLocation(SteveDollEntity entity) {
        return PlayerSkinUtil.getSteveSkin();
    }
}

