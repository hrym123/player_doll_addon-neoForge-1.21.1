package com.lanye.dolladdon;

import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(PlayerDollAddon.MODID)
public class PlayerDollAddon {
    public static final String MODID = "player_doll_addon";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    // 创建创造模式物品栏注册器
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public PlayerDollAddon(IEventBus modEventBus, ModContainer modContainer) {
        // 注册物品
        ModItems.ITEMS.register(modEventBus);
        // 注册实体
        ModEntities.ENTITIES.register(modEventBus);
        // 注册创造模式物品栏
        CREATIVE_MODE_TABS.register(modEventBus);
        
        // 注册物品到创造模式物品栏的事件
        // 注意：BuildCreativeModeTabContentsEvent 是 mod 事件，必须通过 modEventBus 注册
        modEventBus.addListener(this::addCreative);
    }
    
    // 创建玩家玩偶物品栏
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PLAYER_DOLL_TAB = CREATIVE_MODE_TABS.register(
            "player_doll_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.player_doll_addon.player_doll_tab"))
                    .icon(() -> {
                        // 使用史蒂夫玩偶物品作为图标
                        return new ItemStack(ModItems.STEVE_DOLL.get());
                    })
                    .displayItems((parameters, output) -> {
                        // 添加史蒂夫玩偶物品（固定模型：粗手臂 + Steve默认皮肤）
                        output.accept(new ItemStack(ModItems.STEVE_DOLL.get()));
                        
                        // 添加艾利克斯玩偶物品（固定模型：细手臂 + Alex默认皮肤）
                        output.accept(new ItemStack(ModItems.ALEX_DOLL.get()));
                    })
                    .build()
    );
    
    // 将物品添加到创造模式物品栏
    // 注意：不使用 @SubscribeEvent 注解，因为已经通过 modEventBus.addListener() 注册
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // 注意：玩家玩偶物品不添加到原版创造模式物品栏
        // 它们有自己的物品栏（PLAYER_DOLL_TAB）
    }
    
}

