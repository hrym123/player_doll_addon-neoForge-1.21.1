package com.lanye.dolladdon.base;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

/**
 * 玩偶实体工厂类
 * 提供创建玩偶实体类型的通用方法
 */
public class DollEntityFactory {
    
    /**
     * 默认碰撞箱宽度（单位：格）
     * 默认值：0.6f
     */
    public static final float DEFAULT_WIDTH = 0.6f;
    
    /**
     * 默认碰撞箱高度（单位：格）
     * 默认值：1.0f
     * 注意：实际碰撞箱会根据姿态的scale动态调整
     */
    public static final float DEFAULT_HEIGHT = 1.0f;
    
    /**
     * 默认最大跟踪范围（单位：格）
     * 默认值：10
     */
    public static final int DEFAULT_MAX_TRACKING_RANGE = 10;
    
    /**
     * 默认跟踪tick间隔
     * 默认值：20（每秒更新一次）
     */
    public static final int DEFAULT_TRACKING_TICK_INTERVAL = 20;
    
    /**
     * 创建玩偶实体类型的通用构建器（使用默认配置）
     *
     * @param name          实体注册名称
     * @param entityFactory 实体构造函数
     * @return EntityType
     */
    public static <T extends BaseDollEntity> EntityType<T> createDollEntityType(
            String name, EntityType.EntityFactory<T> entityFactory) {
        return createDollEntityType(name, entityFactory, DEFAULT_WIDTH, DEFAULT_HEIGHT, 
                                    DEFAULT_MAX_TRACKING_RANGE, DEFAULT_TRACKING_TICK_INTERVAL);
    }
    
    /**
     * 创建玩偶实体类型的通用构建器（自定义配置）
     *
     * @param name                  实体注册名称
     * @param entityFactory         实体构造函数
     * @param width                 碰撞箱宽度（单位：格）
     * @param height                碰撞箱高度（单位：格）
     * @param maxTrackingRange      最大跟踪范围（单位：格）
     * @param trackingTickInterval  跟踪tick间隔
     * @return EntityType
     */
    public static <T extends BaseDollEntity> EntityType<T> createDollEntityType(
            String name, 
            EntityType.EntityFactory<T> entityFactory,
            float width,
            float height,
            int maxTrackingRange,
            int trackingTickInterval) {
        return EntityType.Builder.<T>create(entityFactory, SpawnGroup.MISC)
                .setDimensions(width, height) // 玩偶碰撞箱基础大小（实际碰撞箱会根据姿态的scale动态调整）
                .maxTrackingRange(maxTrackingRange)
                .trackingTickInterval(trackingTickInterval)
                .build(name);
    }
}

