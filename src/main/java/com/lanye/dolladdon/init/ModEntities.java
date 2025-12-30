package com.lanye.dolladdon.init;

import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.base.DollEntityFactory;
import com.lanye.dolladdon.impl.entity.AlexDollEntity;
import com.lanye.dolladdon.impl.entity.CustomTextureDollEntity;
import com.lanye.dolladdon.impl.entity.SteveDollEntity;
import com.lanye.dolladdon.util.PngTextureScanner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModEntities {
    // 史蒂夫玩偶实体（固定模型：粗手臂 + Steve默认皮肤）
    public static EntityType<SteveDollEntity> STEVE_DOLL;
    
    // 艾利克斯玩偶实体（固定模型：细手臂 + Alex默认皮肤）
    public static EntityType<AlexDollEntity> ALEX_DOLL;
    
    // 自定义纹理玩偶实体映射表（注册名称 -> 实体类型）
    private static final Map<String, EntityType<CustomTextureDollEntity>> CUSTOM_TEXTURE_DOLL_ENTITIES = new HashMap<>();
    
    /**
     * 注册所有实体
     */
    public static void register() {
        // 注册固定的玩偶实体
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
        
        // 扫描并注册所有 PNG 文件对应的实体
        registerCustomTextureDollEntities();
    }
    
    /**
     * 注册所有自定义纹理玩偶实体
     */
    private static void registerCustomTextureDollEntities() {
        List<PngTextureScanner.PngTextureInfo> pngFiles = PngTextureScanner.scanPngFiles();
        
        for (PngTextureScanner.PngTextureInfo pngInfo : pngFiles) {
            String registryName = pngInfo.getRegistryName();
            Identifier textureId = pngInfo.getTextureIdentifier();
            
            // 创建实体类型
            EntityType<CustomTextureDollEntity> entityType = Registry.register(
                    Registries.ENTITY_TYPE,
                    new Identifier(PlayerDollAddon.MODID, "custom_doll_" + registryName),
                    DollEntityFactory.createDollEntityType(
                            "custom_doll_" + registryName,
                            (entityType1, world) -> new CustomTextureDollEntity(entityType1, world, textureId, registryName)
                    )
            );
            
            // 存储到映射表
            CUSTOM_TEXTURE_DOLL_ENTITIES.put(registryName, entityType);
            
            PlayerDollAddon.LOGGER.info("注册自定义纹理玩偶实体: {} -> {}", registryName, textureId);
        }
        
        PlayerDollAddon.LOGGER.info("共注册了 {} 个自定义纹理玩偶实体", CUSTOM_TEXTURE_DOLL_ENTITIES.size());
    }
    
    /**
     * 获取自定义纹理玩偶实体类型
     * @param registryName 注册名称
     * @return 实体类型，如果不存在则返回 null
     */
    public static EntityType<CustomTextureDollEntity> getCustomTextureDollEntityType(String registryName) {
        return CUSTOM_TEXTURE_DOLL_ENTITIES.get(registryName);
    }
    
    /**
     * 获取所有自定义纹理玩偶实体类型
     * @return 实体类型映射表
     */
    public static Map<String, EntityType<CustomTextureDollEntity>> getAllCustomTextureDollEntityTypes() {
        return new HashMap<>(CUSTOM_TEXTURE_DOLL_ENTITIES);
    }
}

