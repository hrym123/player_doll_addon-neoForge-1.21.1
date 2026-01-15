package com.lanye.dolladdon.impl.render;

import com.lanye.dolladdon.base.render.BaseDollItemRenderer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;

/**
 * 自定义纹理玩偶物品渲染器
 * 使用外部 PNG 文件作为纹理
 * 默认使用粗手臂模型（Steve模型）
 */
public class CustomTextureDollItemRenderer extends BaseDollItemRenderer {
    private final ResourceLocation textureIdentifier;
    
    public CustomTextureDollItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet, ResourceLocation textureIdentifier) {
        super(dispatcher, modelSet, new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER), false));
        this.textureIdentifier = textureIdentifier;
    }
    
    @Override
    protected ResourceLocation getDefaultTexture(net.minecraft.world.item.ItemStack stack) {
        return textureIdentifier;
    }
}
