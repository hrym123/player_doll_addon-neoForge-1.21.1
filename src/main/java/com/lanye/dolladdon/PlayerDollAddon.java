package com.lanye.dolladdon;

import com.lanye.dolladdon.command.PlayerDollCommand;
import com.lanye.dolladdon.event.PlayerDollEvents;
import com.lanye.dolladdon.init.ModDataComponents;
import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
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
        LOGGER.info("玩家玩偶附属模组已加载！");
        LOGGER.info("Player Doll Addon loaded!");
        
        // 注册数据组件
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        // 注册物品
        ModItems.ITEMS.register(modEventBus);
        // 注册实体
        ModEntities.ENTITIES.register(modEventBus);
        // 注册创造模式物品栏
        CREATIVE_MODE_TABS.register(modEventBus);
        
        // 注册物品到创造模式物品栏的事件
        // 注意：BuildCreativeModeTabContentsEvent 是 mod 事件，必须通过 modEventBus 注册
        modEventBus.addListener(this::addCreative);
        
        // 注册命令事件（需要在通用事件总线上注册）
        NeoForge.EVENT_BUS.register(this);
        // 注册玩家玩偶事件处理器
        NeoForge.EVENT_BUS.register(PlayerDollEvents.class);
    }
    
    // 将物品添加到创造模式物品栏
    // 注意：不使用 @SubscribeEvent 注解，因为已经通过 modEventBus.addListener() 注册
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // 注意：玩家玩偶物品不添加到创造模式物品栏
        // 只能通过指令 /playerdoll get 获取
        // 如果需要，可以取消下面的注释来添加到创造模式物品栏
        // if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
        //     event.accept(ModItems.PLAYER_DOLL);
        // }
    }
    
    /**
     * 注册命令
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        PlayerDollCommand.register(event.getDispatcher());
    }
}

