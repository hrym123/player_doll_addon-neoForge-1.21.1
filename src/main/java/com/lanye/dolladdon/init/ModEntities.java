package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.base.DollEntityFactory;
import com.lanye.dolladdon.impl.entity.AlexDollEntity;
import com.lanye.dolladdon.impl.entity.SteveDollEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, PlayerDollAddon.MODID);
    
    // 史蒂夫玩偶实体（固定模型：粗手臂 + Steve默认皮肤）
    public static final DeferredHolder<EntityType<?>, EntityType<SteveDollEntity>> STEVE_DOLL = ENTITIES.register(
            "steve_doll",
            () -> DollEntityFactory.createDollEntityType("steve_doll", SteveDollEntity::new)
    );
    
    // 艾利克斯玩偶实体（固定模型：细手臂 + Alex默认皮肤）
    public static final DeferredHolder<EntityType<?>, EntityType<AlexDollEntity>> ALEX_DOLL = ENTITIES.register(
            "alex_doll",
            () -> DollEntityFactory.createDollEntityType("alex_doll", AlexDollEntity::new)
    );
}

