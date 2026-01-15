package com.lanye.dolladdon.impl.render;

import com.lanye.dolladdon.base.render.BaseDollItemRenderer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 自定义纹理玩偶物品渲染器
 * 使用外部 PNG 文件作为纹理
 * 重构后：从物品NBT读取皮肤路径，不再在构造函数中持有纹理信息
 * 默认使用粗手臂模型（Steve模型），实际模型类型从NBT读取
 */
public class CustomTextureDollItemRenderer extends BaseDollItemRenderer {
    
    public CustomTextureDollItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        // 使用粗手臂模型作为默认，实际模型类型从NBT读取
        super(dispatcher, modelSet, new PlayerModel<>(modelSet.bakeLayer(ModelLayers.PLAYER), false));
    }
    
    @Override
    protected ResourceLocation getDefaultTexture(ItemStack stack) {
        // 注意：不要在这里调用 getSkinLocation()，因为 getSkinLocation() 会调用 getDefaultTexture()
        // 这会导致无限递归！
        // getSkinLocation() 已经在基类中处理了从NBT读取皮肤路径的逻辑
        
        // 如果 getSkinLocation() 返回 null（NBT中没有皮肤路径），这里返回默认纹理
        // 使用Steve默认皮肤作为回退
        return com.lanye.dolladdon.util.resource.PlayerSkinUtil.getSteveSkin();
    }
}
