package com.lanye.dolladdon.impl.item;

import com.lanye.dolladdon.base.item.BaseDollItem;
import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.impl.entity.CustomTextureDollEntity;
import com.lanye.dolladdon.impl.render.CustomTextureDollItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * 自定义纹理玩偶物品
 * 使用外部 PNG 文件作为纹理
 */
public class CustomTextureDollItem extends BaseDollItem {
    private final ResourceLocation textureIdentifier;
    private final String registryName;
    
    public CustomTextureDollItem(ResourceLocation textureIdentifier, String registryName) {
        super();
        this.textureIdentifier = textureIdentifier;
        this.registryName = registryName;
    }
    
    @Override
    protected BaseDollEntity createDollEntity(Level world, double x, double y, double z) {
        return new CustomTextureDollEntity(world, x, y, z, textureIdentifier, registryName);
    }
    
    /**
     * 获取纹理标识符
     */
    public ResourceLocation getTextureIdentifier() {
        return textureIdentifier;
    }
    
    /**
     * 获取注册名称
     */
    public String getRegistryName() {
        return registryName;
    }
    
    @Override
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private CustomTextureDollItemRenderer renderer = null;
            
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                if (renderer == null) {
                    renderer = new CustomTextureDollItemRenderer(
                            minecraft.getBlockEntityRenderDispatcher(),
                            minecraft.getEntityModels(),
                            textureIdentifier
                    );
                }
                return renderer;
            }
        });
    }
}
