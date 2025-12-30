package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.impl.item.AlexDollItem;
import com.lanye.dolladdon.impl.item.CustomTextureDollItem;
import com.lanye.dolladdon.impl.item.SteveDollItem;
import com.lanye.dolladdon.util.PngTextureScanner;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModItems {
    // 史蒂夫玩偶物品（固定模型：粗手臂 + Steve默认皮肤）
    public static Item STEVE_DOLL;
    
    // 艾利克斯玩偶物品（固定模型：细手臂 + Alex默认皮肤）
    public static Item ALEX_DOLL;
    
    // 自定义纹理玩偶物品映射表（注册名称 -> 物品）
    private static final Map<String, Item> CUSTOM_TEXTURE_DOLL_ITEMS = new HashMap<>();
    
    /**
     * 注册所有物品
     */
    public static void register() {
        // 注册固定的玩偶物品
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
        
        // 扫描并注册所有 PNG 文件对应的物品
        registerCustomTextureDollItems();
    }
    
    /**
     * 注册所有自定义纹理玩偶物品
     */
    private static void registerCustomTextureDollItems() {
        List<PngTextureScanner.PngTextureInfo> pngFiles = PngTextureScanner.scanPngFiles();
        
        for (PngTextureScanner.PngTextureInfo pngInfo : pngFiles) {
            String registryName = pngInfo.getRegistryName();
            Identifier textureId = pngInfo.getTextureIdentifier();
            
            // 创建物品
            CustomTextureDollItem item = new CustomTextureDollItem(textureId, registryName);
            
            // 注册物品
            Item registeredItem = Registry.register(
                    Registries.ITEM,
                    new Identifier(PlayerDollAddon.MODID, "custom_doll_" + registryName),
                    item
            );
            
            // 存储到映射表
            CUSTOM_TEXTURE_DOLL_ITEMS.put(registryName, registeredItem);
            
            PlayerDollAddon.LOGGER.info("注册自定义纹理玩偶物品: {} -> {}", registryName, textureId);
        }
        
        PlayerDollAddon.LOGGER.info("共注册了 {} 个自定义纹理玩偶物品", CUSTOM_TEXTURE_DOLL_ITEMS.size());
    }
    
    /**
     * 获取自定义纹理玩偶物品
     * @param registryName 注册名称
     * @return 物品，如果不存在则返回 null
     */
    public static Item getCustomTextureDollItem(String registryName) {
        return CUSTOM_TEXTURE_DOLL_ITEMS.get(registryName);
    }
    
    /**
     * 获取所有自定义纹理玩偶物品
     * @return 物品映射表
     */
    public static Map<String, Item> getAllCustomTextureDollItems() {
        return new HashMap<>(CUSTOM_TEXTURE_DOLL_ITEMS);
    }
}

