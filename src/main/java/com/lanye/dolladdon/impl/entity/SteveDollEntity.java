package com.lanye.dolladdon.entity;

import com.lanye.dolladdon.init.ModEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 史蒂夫玩偶实体
 * 固定使用Steve模型（粗手臂）和Steve默认皮肤
 */
public class SteveDollEntity extends BaseDollEntity {
    
    public SteveDollEntity(EntityType<? extends SteveDollEntity> entityType, Level level) {
        super(entityType, level);
    }
    
    public SteveDollEntity(Level level, double x, double y, double z) {
        super(ModEntities.STEVE_DOLL.get(), level, x, y, z);
    }
}

