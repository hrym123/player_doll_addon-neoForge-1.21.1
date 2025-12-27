package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.entity.AlexDollEntity;
import com.lanye.dolladdon.entity.PlayerDollEntity;
import com.lanye.dolladdon.entity.SteveDollEntity;
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
    
    // 史蒂夫玩偶实体（固定模型：粗手臂 + Steve默认皮肤）
    public static final DeferredHolder<EntityType<?>, EntityType<SteveDollEntity>> STEVE_DOLL = ENTITIES.register(
            "steve_doll",
            () -> EntityType.Builder.<SteveDollEntity>of(SteveDollEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f) // 玩家大小：宽0.6，高1.8
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("steve_doll")
    );
    
    // 艾利克斯玩偶实体（固定模型：细手臂 + Alex默认皮肤）
    public static final DeferredHolder<EntityType<?>, EntityType<AlexDollEntity>> ALEX_DOLL = ENTITIES.register(
            "alex_doll",
            () -> EntityType.Builder.<AlexDollEntity>of(AlexDollEntity::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f) // 玩家大小：宽0.6，高1.8
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("alex_doll")
    );
}

