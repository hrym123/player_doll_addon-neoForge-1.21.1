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
        // 注意：动态加载功能已删除，此方法不再使用
        // 如果找不到，返回空物品堆
        return ItemStack.EMPTY;
    }
}

