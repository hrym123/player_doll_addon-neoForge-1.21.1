package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.impl.item.AlexDollItem;
import com.lanye.dolladdon.impl.item.SteveDollItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PlayerDollAddon.MODID);
    
    // 史蒂夫玩偶物品（固定模型：粗手臂 + Steve默认皮肤）
    public static final DeferredItem<SteveDollItem> STEVE_DOLL = ITEMS.register("steve_doll", SteveDollItem::new);
    
    // 艾利克斯玩偶物品（固定模型：细手臂 + Alex默认皮肤）
    public static final DeferredItem<AlexDollItem> ALEX_DOLL = ITEMS.register("alex_doll", AlexDollItem::new);
}

