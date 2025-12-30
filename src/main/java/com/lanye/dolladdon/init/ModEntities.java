package com.lanye.dolladdon.init;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.base.DollEntityFactory;
import com.lanye.dolladdon.impl.entity.AlexDollEntity;
import com.lanye.dolladdon.impl.entity.SteveDollEntity;

public class ModEntities {
    // 史蒂夫玩偶实体（固定模型：粗手臂 + Steve默认皮肤）
    public static EntityType<SteveDollEntity> STEVE_DOLL;
    
    // 艾利克斯玩偶实体（固定模型：细手臂 + Alex默认皮肤）
    public static EntityType<AlexDollEntity> ALEX_DOLL;
    
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
}

