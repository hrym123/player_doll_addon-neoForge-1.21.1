package com.lanye.dolladdon.dynamic;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.init.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 动态玩偶实体
 * 用于从文件加载的玩偶
 */
public class DynamicDollEntity extends BaseDollEntity {
    private final String dollId;
    
    public DynamicDollEntity(EntityType<? extends DynamicDollEntity> entityType, Level level) {
        super(entityType, level);
        this.dollId = entityType.getDescriptionId();
    }
    
    public DynamicDollEntity(EntityType<? extends DynamicDollEntity> entityType, Level level, double x, double y, double z) {
        super(entityType, level, x, y, z);
        this.dollId = entityType.getDescriptionId();
    }
    
    public String getDollId() {
        return dollId;
    }
    
    @Override
    protected ItemStack getDollItemStack() {
        // 从实体类型获取注册名称
        ResourceLocation entityKey = BuiltInRegistries.ENTITY_TYPE.getKey(this.getType());
        if (entityKey != null) {
            String registryName = entityKey.getPath();
            // 从 ModItems.DYNAMIC_DOLLS 中获取对应的物品
            var itemHolder = ModItems.DYNAMIC_DOLLS.get(registryName);
            if (itemHolder != null) {
                return new ItemStack(itemHolder.get());
            }
        }
        // 如果找不到，返回空物品堆（不应该发生）
        return ItemStack.EMPTY;
    }
}

