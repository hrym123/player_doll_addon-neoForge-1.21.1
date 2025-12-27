package com.lanye.dolladdon.client.render;

import com.lanye.dolladdon.entity.SteveDollEntity;
import com.lanye.dolladdon.util.PlayerSkinUtil;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 史蒂夫玩偶实体渲染器
 * 固定使用Steve模型（粗手臂）和Steve默认皮肤
 */
public class SteveDollRenderer extends BaseDollRenderer<SteveDollEntity> {
    
    public SteveDollRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false));
    }
    
    @Override
    protected ResourceLocation getSkinLocation(SteveDollEntity entity) {
        return PlayerSkinUtil.getSteveSkin();
    }
}

