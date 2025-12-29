package com.lanye.dolladdon.impl.entity;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * 史蒂夫玩偶实体
 * 固定使用Steve模型（粗手臂）和Steve默认皮肤
 */
public class SteveDollEntity extends BaseDollEntity {
    
    public SteveDollEntity(EntityType<? extends SteveDollEntity> entityType, World world) {
        super(entityType, world);
    }
    
    public SteveDollEntity(World world, double x, double y, double z) {
        super(ModEntities.STEVE_DOLL, world, x, y, z);
    }
    
    @Override
    protected ItemStack getDollItemStack() {
        return new ItemStack(ModItems.STEVE_DOLL);
    }
}

