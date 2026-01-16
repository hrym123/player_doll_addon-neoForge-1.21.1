package com.lanye.dolladdon.impl.entity;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 自定义纹理玩偶实体
 * 使用外部 PNG 文件作为纹理
 * 重构后：从NBT读取皮肤路径，不再在构造函数中持有纹理信息
 */
public class CustomTextureDollEntity extends BaseDollEntity {
    
    public CustomTextureDollEntity(EntityType<? extends CustomTextureDollEntity> entityType, Level world) {
        super(entityType, world);
    }
    
    public CustomTextureDollEntity(EntityType<? extends CustomTextureDollEntity> entityType, Level world, double x, double y, double z) {
        super(entityType, world, x, y, z);
    }
    
    @Override
    protected ItemStack getDollItemStack() {
        // 返回统一的物品，NBT会在 addAdditionalSaveData 中保存
        ItemStack stack = new ItemStack(ModItems.CUSTOM_TEXTURE_DOLL.get());
        
        // 将实体的NBT数据复制到物品
        net.minecraft.nbt.CompoundTag entityTag = new net.minecraft.nbt.CompoundTag();
        this.addAdditionalSaveData(entityTag);
        
        // 只有当entityTag不为空时才保存custom_data
        if (!entityTag.isEmpty()) {
            net.minecraft.nbt.CompoundTag customDataTag = new net.minecraft.nbt.CompoundTag();
            customDataTag.put("EntityData", entityTag);
            
            // 使用与 DollSkinCommand 相同的方法创建 CustomData
            Object customData = com.lanye.dolladdon.util.command.DollSkinCommand.createCustomData(customDataTag);
            if (customData != null) {
                try {
                    net.minecraft.core.component.DataComponentPatch.Builder builder = 
                        net.minecraft.core.component.DataComponentPatch.builder();
                    java.lang.reflect.Method setMethod = builder.getClass().getDeclaredMethod("set", 
                        net.minecraft.core.component.DataComponentType.class, Object.class);
                    setMethod.setAccessible(true);
                    setMethod.invoke(builder, 
                        net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                        customData);
                    
                    // 注意：不设置 CUSTOM_NAME，名称由 CustomTextureDollItem.getName() 从NBT读取
                    // DisplayName 已保存在 NBT 中，会随 CUSTOM_DATA 一起保存到物品
                    
                    stack.applyComponents(builder.build());
                } catch (Exception e) {
                    // 如果失败，尝试备用方法
                    try {
                        java.lang.reflect.Method setMethod = ItemStack.class.getMethod("set", 
                            net.minecraft.core.component.DataComponentType.class, Object.class);
                        setMethod.invoke(stack, net.minecraft.core.component.DataComponents.CUSTOM_DATA, customData);
                        
                        // 注意：不设置 CUSTOM_NAME，名称由 CustomTextureDollItem.getName() 从NBT读取
                        // DisplayName 已保存在 NBT 中，会随 CUSTOM_DATA 一起保存到物品
                    } catch (Exception e2) {
                        // 如果都失败，返回没有NBT的物品
                    }
                }
            }
        }
        
        return stack;
    }
}
