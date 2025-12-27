package com.lanye.dolladdon;

import com.lanye.dolladdon.command.PlayerDollCommand;
import com.lanye.dolladdon.config.ModConfig;
import com.lanye.dolladdon.event.PlayerDollEvents;
import com.lanye.dolladdon.init.ModDataComponents;
import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
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
        
        // 注册配置
        // 注意：配置值只能在配置加载后才能访问，不能在构造函数中立即访问
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, ModConfig.SPEC);
        
        // 注册配置加载事件，在配置加载后检查测试模式状态
        modEventBus.addListener(this::onConfigLoad);
        
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
    
    /**
     * 配置加载事件处理
     * 在配置加载后检查测试模式状态
     */
    private void onConfigLoad(net.neoforged.fml.event.config.ModConfigEvent.Loading event) {
        if (event.getConfig().getModId().equals(MODID)) {
            // 配置已加载，现在可以安全地访问配置值
            if (ModConfig.isTestMode()) {
                LOGGER.info("测试模式已启用 - 将输出详细的调试日志");
                LOGGER.info("Test Mode enabled - Detailed debug logs will be output");
            }
        }
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
    
    /**
     * 注册命令
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        PlayerDollCommand.register(event.getDispatcher());
    }
}

