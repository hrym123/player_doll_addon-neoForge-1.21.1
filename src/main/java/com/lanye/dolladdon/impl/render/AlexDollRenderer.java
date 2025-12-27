package com.lanye.dolladdon.client.render;

import com.lanye.dolladdon.entity.AlexDollEntity;
import com.lanye.dolladdon.util.PlayerSkinUtil;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 艾利克斯玩偶实体渲染器
 * 固定使用Alex模型（细手臂）和Alex默认皮肤
 */
public class AlexDollRenderer extends BaseDollRenderer<AlexDollEntity> {
    
    public AlexDollRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true));
    }
    
    @Override
    protected ResourceLocation getSkinLocation(AlexDollEntity entity) {
        return PlayerSkinUtil.getAlexSkin();
    }
}

