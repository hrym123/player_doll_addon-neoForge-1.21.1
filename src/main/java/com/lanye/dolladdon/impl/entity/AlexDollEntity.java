package com.lanye.dolladdon.impl.entity;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 艾利克斯玩偶实体
 * 固定使用Alex模型（细手臂）和Alex默认皮肤
 */
public class AlexDollEntity extends BaseDollEntity {
    
    public AlexDollEntity(EntityType<? extends AlexDollEntity> entityType, World world) {
        super(entityType, world);
    }
    
    public AlexDollEntity(World world, double x, double y, double z) {
        super(ModEntities.ALEX_DOLL, world, x, y, z);
    }
    
    @Override
    protected ItemStack getDollItemStack() {
        return new ItemStack(ModItems.ALEX_DOLL);
    }
}

