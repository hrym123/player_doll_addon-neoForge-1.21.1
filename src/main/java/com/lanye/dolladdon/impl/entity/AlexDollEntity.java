package com.lanye.dolladdon.impl.entity;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.init.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 艾利克斯玩偶实体
 * 固定使用Alex模型（细手臂）和Alex默认皮肤
 */
public class AlexDollEntity extends BaseDollEntity {
    
    public AlexDollEntity(EntityType<? extends AlexDollEntity> entityType, Level level) {
        super(entityType, level);
    }
    
    public AlexDollEntity(Level level, double x, double y, double z) {
        super(ModEntities.ALEX_DOLL.get(), level, x, y, z);
    }
}

