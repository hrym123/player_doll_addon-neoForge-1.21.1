package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.dynamic.DynamicDollItem;
import com.lanye.dolladdon.impl.item.AlexDollItem;
import com.lanye.dolladdon.impl.item.SteveDollItem;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.HashMap;
import java.util.Map;

public class ModItems {
    // 史蒂夫玩偶物品（固定模型：粗手臂 + Steve默认皮肤）
    public static Item STEVE_DOLL;
    
    // 艾利克斯玩偶物品（固定模型：细手臂 + Alex默认皮肤）
    public static Item ALEX_DOLL;
    
    // 动态注册的玩偶物品（从文件加载）
    public static final Map<String, Item> DYNAMIC_DOLLS = new HashMap<>();
    
    /**
     * 注册所有物品
     */
    public static void register() {
        STEVE_DOLL = Registry.register(
                Registries.ITEM,
                new Identifier(PlayerDollAddon.MODID, "steve_doll"),
                new SteveDollItem()
        );
        
        ALEX_DOLL = Registry.register(
                Registries.ITEM,
                new Identifier(PlayerDollAddon.MODID, "alex_doll"),
                new AlexDollItem()
        );
    }
    
    /**
     * 动态注册玩偶物品
     * @param registryName 注册名称
     * @param entityType 实体类型
     * @param textureLocation 纹理位置
     * @param isAlexModel 是否为Alex模型
     * @param displayName 显示名称
     * @return 注册的物品
     */
    public static Item registerDynamicDoll(String registryName, 
                                            EntityType<com.lanye.dolladdon.dynamic.DynamicDollEntity> entityType,
                                            Identifier textureLocation, 
                                                                     boolean isAlexModel,
                                                                     String displayName) {
        Item item = Registry.register(
                Registries.ITEM,
                new Identifier(PlayerDollAddon.MODID, registryName),
                new DynamicDollItem(entityType, textureLocation, isAlexModel, displayName)
        );
        DYNAMIC_DOLLS.put(registryName, item);
        return item;
    }
}

