package com.lanye.dolladdon.dynamic;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.dynamic.render.DynamicDollItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.function.Consumer;

/**
 * 动态玩偶物品
 * 用于从文件加载的玩偶
 */
public class DynamicDollItem extends com.lanye.dolladdon.base.item.BaseDollItem {
    private final EntityType<DynamicDollEntity> entityType;
    private final ResourceLocation textureLocation;
    private final boolean isAlexModel;
    
    public DynamicDollItem(EntityType<DynamicDollEntity> entityType, ResourceLocation textureLocation, boolean isAlexModel) {
        super();
        this.entityType = entityType;
        this.textureLocation = textureLocation;
        this.isAlexModel = isAlexModel;
    }
    
    @Override
    protected BaseDollEntity createDollEntity(Level level, double x, double y, double z) {
        return new DynamicDollEntity(entityType, level, x, y, z);
    }
    
    @Override
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private DynamicDollItemRenderer renderer = null;
            
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                if (renderer == null) {
                    renderer = new DynamicDollItemRenderer(
                            minecraft.getBlockEntityRenderDispatcher(),
                            minecraft.getEntityModels(),
                            textureLocation,
                            isAlexModel
                    );
                }
                return renderer;
            }
        });
    }
    
    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }
    
    public boolean isAlexModel() {
        return isAlexModel;
    }
}

