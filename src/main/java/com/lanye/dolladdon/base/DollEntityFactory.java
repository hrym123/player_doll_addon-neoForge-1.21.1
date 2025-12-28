package com.lanye.dolladdon.base;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * 玩偶实体工厂类
 * 提供创建玩偶实体类型的通用方法
 */
public class DollEntityFactory {
    
    /**
     * 创建玩偶实体类型的通用构建器
     *
     * @param name          实体注册名称
     * @param entityFactory 实体构造函数
     * @return EntityType
     */
    public static <T extends BaseDollEntity> EntityType<T> createDollEntityType(
            String name, EntityType.EntityFactory<T> entityFactory) {
        return EntityType.Builder.<T>of(entityFactory, MobCategory.MISC)
                .sized(0.6f, 1f) // 玩偶碰撞箱基础大小：宽0.6，高1.0（实际碰撞箱会根据姿态的scale动态调整）
                .clientTrackingRange(10)
                .updateInterval(20)
                .build(name);
    }
}

