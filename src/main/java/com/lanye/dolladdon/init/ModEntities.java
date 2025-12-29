package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.base.DollEntityFactory;
import com.lanye.dolladdon.dynamic.DynamicDollEntity;
import com.lanye.dolladdon.impl.entity.AlexDollEntity;
import com.lanye.dolladdon.impl.entity.SteveDollEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

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
    
    // 动态注册的玩偶实体（从文件加载）
    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<DynamicDollEntity>>> DYNAMIC_DOLLS = new HashMap<>();
    
    /**
     * 动态注册玩偶实体
     * @param registryName 注册名称
     * @return 注册的实体类型持有者
     */
    public static DeferredHolder<EntityType<?>, EntityType<DynamicDollEntity>> registerDynamicDoll(String registryName) {
        DeferredHolder<EntityType<?>, EntityType<DynamicDollEntity>> holder = ENTITIES.register(
                registryName,
                () -> DollEntityFactory.createDollEntityType(registryName, DynamicDollEntity::new)
        );
        DYNAMIC_DOLLS.put(registryName, holder);
        return holder;
    }
}

