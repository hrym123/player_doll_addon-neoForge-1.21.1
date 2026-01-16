package com.lanye.dolladdon.impl.item;

import com.lanye.dolladdon.base.item.BaseDollItem;
import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.impl.entity.CustomTextureDollEntity;
import com.lanye.dolladdon.impl.render.CustomTextureDollItemRenderer;
import com.lanye.dolladdon.init.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
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
        // 优先读取 CUSTOM_NAME（如果玩家通过铁砧重命名了物品）
        var customName = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (customName != null) {
            return customName;
        }
        
        // 如果没有 CUSTOM_NAME，从NBT读取 PlayerName 作为显示名称
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var dataTag = customData.copyTag();
            if (dataTag != null && dataTag.contains("EntityData")) {
                var entityTag = dataTag.getCompound("EntityData");
                if (entityTag.contains("PlayerName", net.minecraft.nbt.Tag.TAG_STRING)) {
                    return Component.literal(entityTag.getString("PlayerName"));
                }
            }
        }
        return super.getName(stack);
    }
    
    // 注意：在 Minecraft 1.21.1 NeoForge 中，Item 类没有 canCombine 方法可以重写
    // 物品叠加的判断由 ItemStack.areItemsAndComponentsEqual() 方法处理
    // 由于我们已经通过 DollItemFactory 统一创建物品，确保相同材质的物品NBT结构完全一致
    // 因此相同材质的物品会自动叠加，无需额外处理
    
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
