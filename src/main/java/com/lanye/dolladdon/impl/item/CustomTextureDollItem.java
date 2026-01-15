package com.lanye.dolladdon.impl.item;

import com.lanye.dolladdon.base.item.BaseDollItem;
import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.impl.entity.CustomTextureDollEntity;
import com.lanye.dolladdon.impl.render.CustomTextureDollItemRenderer;
import com.lanye.dolladdon.init.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * 自定义纹理玩偶物品
 * 使用外部 PNG 文件作为纹理
 * 重构后：从NBT读取皮肤路径，不再在构造函数中持有纹理信息
 */
public class CustomTextureDollItem extends BaseDollItem {
    
    public CustomTextureDollItem() {
        super();
    }
    
    @Override
    protected BaseDollEntity createDollEntity(Level world, double x, double y, double z) {
        // 使用统一的实体类型，皮肤路径从NBT读取
        return new CustomTextureDollEntity(ModEntities.CUSTOM_TEXTURE_DOLL.get(), world, x, y, z);
    }
    
    @Override
    public Component getName(ItemStack stack) {
        // 从NBT读取显示名称
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var dataTag = customData.copyTag();
            if (dataTag != null && dataTag.contains("EntityData")) {
                var entityTag = dataTag.getCompound("EntityData");
                if (entityTag.contains("DisplayName", net.minecraft.nbt.Tag.TAG_STRING)) {
                    return Component.literal(entityTag.getString("DisplayName"));
                }
                // 如果没有DisplayName，尝试使用PlayerName
                if (entityTag.contains("PlayerName", net.minecraft.nbt.Tag.TAG_STRING)) {
                    return Component.literal(entityTag.getString("PlayerName"));
                }
            }
        }
        return super.getName(stack);
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
                    // 渲染器不再需要纹理标识符，从NBT读取
                    renderer = new CustomTextureDollItemRenderer(
                            minecraft.getBlockEntityRenderDispatcher(),
                            minecraft.getEntityModels()
                    );
                }
                return renderer;
            }
        });
    }
}
