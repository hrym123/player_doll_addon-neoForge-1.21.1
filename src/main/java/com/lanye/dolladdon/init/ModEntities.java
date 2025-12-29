package com.lanye.dolladdon.init;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.base.DollEntityFactory;
import com.lanye.dolladdon.dynamic.DynamicDollEntity;
import com.lanye.dolladdon.impl.entity.AlexDollEntity;
import com.lanye.dolladdon.impl.entity.SteveDollEntity;

public class ModEntities {
    // 史蒂夫玩偶实体（固定模型：粗手臂 + Steve默认皮肤）
    public static EntityType<SteveDollEntity> STEVE_DOLL;
    
    // 艾利克斯玩偶实体（固定模型：细手臂 + Alex默认皮肤）
    public static EntityType<AlexDollEntity> ALEX_DOLL;
    
    // 动态注册的玩偶实体（从文件加载）
    public static final Map<String, EntityType<DynamicDollEntity>> DYNAMIC_DOLLS = new HashMap<>();
    
    /**
     * 注册所有实体
     */
    public static void register() {
        STEVE_DOLL = Registry.register(
                Registries.ENTITY_TYPE,
                new Identifier(PlayerDollAddon.MODID, "steve_doll"),
                DollEntityFactory.createDollEntityType("steve_doll", SteveDollEntity::new)
        );
        
        ALEX_DOLL = Registry.register(
                Registries.ENTITY_TYPE,
                new Identifier(PlayerDollAddon.MODID, "alex_doll"),
                DollEntityFactory.createDollEntityType("alex_doll", AlexDollEntity::new)
        );
    }
    
    /**
     * 动态注册玩偶实体
     * @param registryName 注册名称
     * @return 注册的实体类型
     */
    public static EntityType<DynamicDollEntity> registerDynamicDoll(String registryName) {
        EntityType<DynamicDollEntity> entityType = Registry.register(
                Registries.ENTITY_TYPE,
                new Identifier(PlayerDollAddon.MODID, registryName),
                DollEntityFactory.createDollEntityType(registryName, DynamicDollEntity::new)
        );
        DYNAMIC_DOLLS.put(registryName, entityType);
        return entityType;
    }
}

