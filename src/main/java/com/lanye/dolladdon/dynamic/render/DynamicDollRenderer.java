package com.lanye.dolladdon.dynamic.render;

import com.lanye.dolladdon.base.render.BaseDollRenderer;
import com.lanye.dolladdon.dynamic.DynamicDollEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 动态玩偶实体渲染器
 * 用于从文件加载的玩偶
 */
public class DynamicDollRenderer extends BaseDollRenderer<DynamicDollEntity> {
    private final ResourceLocation skinLocation;
    private final boolean isAlexModel;
    
    public DynamicDollRenderer(EntityRendererProvider.Context context, ResourceLocation skinLocation, boolean isAlexModel) {
        super(context, new PlayerModel<>(
            context.bakeLayer(isAlexModel ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), 
            isAlexModel
        ));
        this.skinLocation = skinLocation;
        this.isAlexModel = isAlexModel;
    }
    
    @Override
    protected ResourceLocation getSkinLocation(DynamicDollEntity entity) {
        return skinLocation;
    }
    
    @Override
    protected boolean isThinArms() {
        return isAlexModel;
    }
}

