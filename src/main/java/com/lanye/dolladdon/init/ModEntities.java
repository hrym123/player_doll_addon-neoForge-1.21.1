package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.util.factory.DollEntityFactory;
import com.lanye.dolladdon.dynamic.DynamicDollEntity;
import com.lanye.dolladdon.impl.entity.AlexDollEntity;
import com.lanye.dolladdon.impl.entity.CustomTextureDollEntity;
import com.lanye.dolladdon.impl.entity.SteveDollEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.HashMap;
import java.util.Map;

public class ModEntities {
    // Logger removed - logging handled by Mixin
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
    
    // 自定义纹理玩偶实体映射表（注册名称 -> 实体类型持有者）
    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<CustomTextureDollEntity>>> CUSTOM_TEXTURE_DOLL_ENTITIES = new HashMap<>();
    
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
    
    /**
     * 注册自定义纹理玩偶实体
     * @param registryName 注册名称
     * @param textureId 纹理标识符
     * @return 注册的实体类型持有者
     */
    public static DeferredHolder<EntityType<?>, EntityType<CustomTextureDollEntity>> registerCustomTextureDollEntity(String registryName, ResourceLocation textureId) {
        DeferredHolder<EntityType<?>, EntityType<CustomTextureDollEntity>> holder = ENTITIES.register(
                "custom_doll_" + registryName,
                () -> DollEntityFactory.createDollEntityType(
                        "custom_doll_" + registryName,
                        (entityType, world) -> new CustomTextureDollEntity(entityType, world, textureId, registryName)
                )
        );
        CUSTOM_TEXTURE_DOLL_ENTITIES.put(registryName, holder);
        return holder;
    }
    
    /**
     * 获取自定义纹理玩偶实体类型
     * @param registryName 注册名称
     * @return 实体类型，如果不存在则返回 null
     */
    public static EntityType<CustomTextureDollEntity> getCustomTextureDollEntityType(String registryName) {
        DeferredHolder<EntityType<?>, EntityType<CustomTextureDollEntity>> holder = CUSTOM_TEXTURE_DOLL_ENTITIES.get(registryName);
        return holder != null ? holder.get() : null;
    }
    
    /**
     * 获取所有自定义纹理玩偶实体类型
     * @return 实体类型映射表
     */
    public static Map<String, EntityType<CustomTextureDollEntity>> getAllCustomTextureDollEntityTypes() {
        Map<String, EntityType<CustomTextureDollEntity>> result = new HashMap<>();
        for (Map.Entry<String, DeferredHolder<EntityType<?>, EntityType<CustomTextureDollEntity>>> entry : CUSTOM_TEXTURE_DOLL_ENTITIES.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get());
        }
        return result;
    }
    
    /**
     * 注册所有自定义纹理玩偶实体（从PNG文件扫描）
     */
    public static void registerCustomTextureDollEntities() {
        java.util.List<com.lanye.dolladdon.util.resource.PngTextureScanner.PngTextureInfo> pngFiles = 
                com.lanye.dolladdon.util.resource.PngTextureScanner.scanPngFiles();
        
        for (com.lanye.dolladdon.util.resource.PngTextureScanner.PngTextureInfo pngInfo : pngFiles) {
            try {
                String registryName = pngInfo.getRegistryName();
                ResourceLocation textureId = pngInfo.getTextureIdentifier();
                
                // 注册实体
                DeferredHolder<EntityType<?>, EntityType<CustomTextureDollEntity>> holder = 
                        registerCustomTextureDollEntity(registryName, textureId);
                
                // Debug logging handled by Mixin
            } catch (Exception e) {
                // Error logging handled by Mixin
            }
        }
    }
}

