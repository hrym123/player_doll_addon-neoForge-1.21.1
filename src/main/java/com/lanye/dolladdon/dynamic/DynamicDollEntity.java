package com.lanye.dolladdon.dynamic;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import net.minecraft.world.entity.EntityType;
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
}

