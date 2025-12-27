package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.item.PlayerDollItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PlayerDollAddon.MODID);
    
    // 玩家玩偶物品（原版方式）
    // 只能通过指令获取，不显示在创造模式物品栏
    public static final DeferredItem<PlayerDollItem> PLAYER_DOLL = ITEMS.register("player_doll", PlayerDollItem::new);
}

