package com.lanye.dolladdon.impl.item;

import com.lanye.dolladdon.base.item.BaseDollItem;
import com.lanye.dolladdon.impl.render.AlexDollItemRenderer;
import com.lanye.dolladdon.impl.entity.AlexDollEntity;
import com.lanye.dolladdon.base.entity.BaseDollEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * 艾利克斯玩偶物品
 * 固定使用Alex模型（细手臂）和Alex默认皮肤
 */
public class AlexDollItem extends BaseDollItem {
    
    @Override
    protected BaseDollEntity createDollEntity(Level level, double x, double y, double z) {
        return new AlexDollEntity(level, x, y, z);
    }
    
    @Override
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private AlexDollItemRenderer renderer = null;
            
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                if (renderer == null) {
                    renderer = new AlexDollItemRenderer(
                            minecraft.getBlockEntityRenderDispatcher(),
                            minecraft.getEntityModels()
                    );
                }
                return renderer;
            }
        });
    }
}

