package com.lanye.dolladdon.dynamic;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.init.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.world.World;

/**
 * 动态玩偶实体
 * 用于从文件加载的玩偶
 */
public class DynamicDollEntity extends BaseDollEntity {
    private final String dollId;
    
    public DynamicDollEntity(EntityType<? extends DynamicDollEntity> entityType, World world) {
        super(entityType, world);
        this.dollId = entityType.getTranslationKey();
    }
    
    public DynamicDollEntity(EntityType<? extends DynamicDollEntity> entityType, World world, double x, double y, double z) {
        super(entityType, world, x, y, z);
        this.dollId = entityType.getTranslationKey();
    }
    
    public String getDollId() {
        return dollId;
    }
    
    @Override
    protected ItemStack getDollItemStack() {
        // 从实体类型获取注册名称
        Identifier entityKey = Registries.ENTITY_TYPE.getId(this.getType());
        if (entityKey != null) {
            String registryName = entityKey.getPath();
            // 从 ModItems.DYNAMIC_DOLLS 中获取对应的物品
            var item = ModItems.DYNAMIC_DOLLS.get(registryName);
            if (item != null) {
                return new ItemStack(item);
            }
        }
        // 如果找不到，返回空物品堆（不应该发生）
        return ItemStack.EMPTY;
    }
}

