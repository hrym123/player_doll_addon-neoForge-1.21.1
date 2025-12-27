package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.entity.PlayerDollEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, PlayerDollAddon.MODID);
    
    public static final DeferredHolder<EntityType<?>, EntityType<PlayerDollEntity>> PLAYER_DOLL = ENTITIES.register(
            "player_doll",
            () -> EntityType.Builder.<PlayerDollEntity>of(PlayerDollEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f) // 玩家大小：宽0.6，高1.8
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("player_doll")
    );
}

