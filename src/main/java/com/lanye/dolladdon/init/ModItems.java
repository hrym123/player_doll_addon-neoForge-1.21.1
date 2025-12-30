package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.impl.item.AlexDollItem;
import com.lanye.dolladdon.impl.item.SteveDollItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModItems {
    // 史蒂夫玩偶物品（固定模型：粗手臂 + Steve默认皮肤）
    public static Item STEVE_DOLL;
    
    // 艾利克斯玩偶物品（固定模型：细手臂 + Alex默认皮肤）
    public static Item ALEX_DOLL;
    
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
}

