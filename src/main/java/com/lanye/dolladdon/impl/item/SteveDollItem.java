package com.lanye.dolladdon.impl.item;

import com.lanye.dolladdon.base.item.BaseDollItem;
import com.lanye.dolladdon.impl.render.SteveDollItemRenderer;
import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.impl.entity.SteveDollEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * 史蒂夫玩偶物品
 * 固定使用Steve模型（粗手臂）和Steve默认皮肤
 */
public class SteveDollItem extends BaseDollItem {
    
    @Override
    protected BaseDollEntity createDollEntity(Level level, double x, double y, double z) {
        return new SteveDollEntity(level, x, y, z);
    }
    
    @Override
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private SteveDollItemRenderer renderer = null;
            
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                if (renderer == null) {
                    renderer = new SteveDollItemRenderer(
                            minecraft.getBlockEntityRenderDispatcher(),
                            minecraft.getEntityModels()
                    );
                }
                return renderer;
            }
        });
    }
}

