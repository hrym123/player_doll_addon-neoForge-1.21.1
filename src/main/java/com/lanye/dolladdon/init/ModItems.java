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
        PlayerDollAddon.LOGGER.info("[物品注册] 开始注册物品...");
        
        // 注册固定的玩偶物品
        PlayerDollAddon.LOGGER.info("[物品注册] 注册固定物品: steve_doll");
        STEVE_DOLL = Registry.register(
                Registries.ITEM,
                new Identifier(PlayerDollAddon.MODID, "steve_doll"),
                new SteveDollItem()
        );
        PlayerDollAddon.LOGGER.info("[物品注册] ✓ steve_doll 注册成功");
        
        PlayerDollAddon.LOGGER.info("[物品注册] 注册固定物品: alex_doll");
        ALEX_DOLL = Registry.register(
                Registries.ITEM,
                new Identifier(PlayerDollAddon.MODID, "alex_doll"),
                new AlexDollItem()
        );
        PlayerDollAddon.LOGGER.info("[物品注册] ✓ alex_doll 注册成功");
        
        // 扫描并注册所有 PNG 文件对应的物品
        registerCustomTextureDollItems();
        
        PlayerDollAddon.LOGGER.info("[物品注册] 物品注册完成");
    }
    
    /**
     * 注册所有自定义纹理玩偶物品
     */
    private static void registerCustomTextureDollItems() {
        PlayerDollAddon.LOGGER.info("[物品注册] 开始注册自定义纹理玩偶物品...");
        
        List<PngTextureScanner.PngTextureInfo> pngFiles = PngTextureScanner.scanPngFiles();
        PlayerDollAddon.LOGGER.info("[物品注册] 扫描到 {} 个 PNG 文件，准备注册", pngFiles.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (PngTextureScanner.PngTextureInfo pngInfo : pngFiles) {
            try {
                String registryName = pngInfo.getRegistryName();
                Identifier textureId = pngInfo.getTextureIdentifier();
                Identifier itemId = new Identifier(PlayerDollAddon.MODID, "custom_doll_" + registryName);
                
                PlayerDollAddon.LOGGER.info("[物品注册] 正在注册: 注册名={}, 物品ID={}, 纹理ID={}", 
                        registryName, itemId, textureId);
                
                // 创建物品
                CustomTextureDollItem item = new CustomTextureDollItem(textureId, registryName);
                
                // 注册物品
                Item registeredItem = Registry.register(
                        Registries.ITEM,
                        itemId,
                        item
                );
                
                // 验证注册是否成功
                Item verifyItem = Registries.ITEM.get(itemId);
                if (verifyItem == registeredItem) {
                    // 存储到映射表
                    CUSTOM_TEXTURE_DOLL_ITEMS.put(registryName, registeredItem);
                    successCount++;
                    PlayerDollAddon.LOGGER.info("[物品注册] ✓ 成功注册: {} -> {}", registryName, itemId);
                } else {
                    failCount++;
                    PlayerDollAddon.LOGGER.error("[物品注册] ✗ 注册验证失败: {} (注册的物品与验证的物品不匹配)", registryName);
                }
            } catch (Exception e) {
                failCount++;
                PlayerDollAddon.LOGGER.error("[物品注册] ✗ 注册失败: {}", pngInfo.getRegistryName(), e);
            }
        }
        
        PlayerDollAddon.LOGGER.info("[物品注册] 自定义纹理玩偶物品注册完成: 成功={}, 失败={}, 总计={}", 
                successCount, failCount, pngFiles.size());
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

