package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.impl.item.ActionDebugStick;
import com.lanye.dolladdon.impl.item.AlexDollItem;
import com.lanye.dolladdon.impl.item.CustomTextureDollItem;
import com.lanye.dolladdon.impl.item.PoseDebugStick;
import com.lanye.dolladdon.impl.item.SteveDollItem;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import com.lanye.dolladdon.util.resource.PngTextureScanner;
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
    
    // 动作调试棒
    public static Item ACTION_DEBUG_STICK;
    
    // 姿态调试棒
    public static Item POSE_DEBUG_STICK;
    
    /**
     * 注册所有物品
     */
    public static void register() {
        // 注册固定的玩偶物品
        STEVE_DOLL = registerStandardDollItem("steve_doll", new SteveDollItem());
        ALEX_DOLL = registerStandardDollItem("alex_doll", new AlexDollItem());
        
        // 注册动作调试棒
        ACTION_DEBUG_STICK = registerStandardDollItem("action_debug_stick", new ActionDebugStick());
        
        // 注册姿态调试棒
        POSE_DEBUG_STICK = registerStandardDollItem("pose_debug_stick", new PoseDebugStick());
        
        // 扫描并注册所有 PNG 文件对应的物品
        registerCustomTextureDollItems();
    }
    
    /**
     * 注册标准玩偶物品（辅助方法）
     * @param name 物品名称
     * @param item 物品实例
     * @return 注册的物品
     */
    private static Item registerStandardDollItem(String name, Item item) {
        return Registry.register(
                Registries.ITEM,
                new Identifier(PlayerDollAddon.MODID, name),
                item
        );
    }
    
    /**
     * 注册所有自定义纹理玩偶物品
     */
    private static void registerCustomTextureDollItems() {
        List<PngTextureScanner.PngTextureInfo> pngFiles = PngTextureScanner.scanPngFiles();
        
        for (PngTextureScanner.PngTextureInfo pngInfo : pngFiles) {
            try {
                String registryName = pngInfo.getRegistryName();
                Identifier textureId = pngInfo.getTextureIdentifier();
                Identifier itemId = new Identifier(PlayerDollAddon.MODID, "custom_doll_" + registryName);
                
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
                } else {
                    ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE, "[物品注册] ✗ 注册验证失败: {} (注册的物品与验证的物品不匹配)", registryName);
                }
            } catch (Exception e) {
                ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE, "[物品注册] ✗ 注册失败: {}", pngInfo.getRegistryName(), e);
            }
        }
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

