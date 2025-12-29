package com.lanye.dolladdon;

import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import com.lanye.dolladdon.util.DynamicDollLoader;
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

import java.nio.file.Path;

@Mod(PlayerDollAddon.MODID)
public class PlayerDollAddon {
    public static final String MODID = "player_doll_addon";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    // 玩偶图片目录路径（相对于游戏目录）
    public static final String PNG_DIR = "player_doll/png";
    // 姿态文件目录路径（相对于游戏目录）
    public static final String POSES_DIR = "player_doll/poses";
    // 动作文件目录路径（相对于游戏目录）
    public static final String ACTIONS_DIR = "player_doll/actions";
    
    // 创建创造模式物品栏注册器
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public PlayerDollAddon(IEventBus modEventBus, ModContainer modContainer) {
        // 初始化默认文件（从资源包复制到文件系统）
        initializeDefaultFiles();
        // 先扫描目录并注册动态玩偶（必须在注册器注册之前）
        registerDynamicDolls();
        
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
    
    /**
     * 初始化默认文件（生成到文件系统）
     */
    private void initializeDefaultFiles() {
        try {
            Path gameDir;
            try {
                Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
                java.lang.reflect.Method gameDirMethod = fmlPathsClass.getMethod("getGamePath");
                gameDir = (Path) gameDirMethod.invoke(null);
            } catch (Exception e) {
                gameDir = java.nio.file.Paths.get(".").toAbsolutePath().normalize();
            }
            
            com.lanye.dolladdon.util.DefaultFileInitializer.initializeDefaultFiles(gameDir);
        } catch (Exception e) {
            LOGGER.error("初始化默认文件失败", e);
        }
    }
    
    /**
     * 注册动态玩偶（从文件加载）
     * 在构造函数中调用，确保在注册器注册之前完成
     */
    private void registerDynamicDolls() {
        // 先清理旧的动态模型文件（保留 alex_doll.json 和 steve_doll.json）
        com.lanye.dolladdon.util.DynamicModelGenerator.cleanupOldModelFiles();
        
        // 扫描目录
        var dollInfos = DynamicDollLoader.scanDirectory(PNG_DIR);
        
        // 批量生成所有动态玩偶的模型文件（所有动态玩偶都使用相同的模型内容）
        java.util.List<String> registryNames = new java.util.ArrayList<>();
        for (var dollInfo : dollInfos) {
            registryNames.add(dollInfo.getFileName());
        }
        com.lanye.dolladdon.util.DynamicModelGenerator.generateAllItemModels(registryNames);
        
        // 注册每个玩偶
        int successCount = 0;
        for (var dollInfo : dollInfos) {
            try {
                // 注册实体
                var entityHolder = ModEntities.registerDynamicDoll(dollInfo.getFileName());
                
                // 模型文件已在上面批量生成，这里不需要再生成
                
                // 注册物品（传递 DeferredHolder，延迟获取 EntityType）
                ModItems.registerDynamicDoll(
                    dollInfo.getFileName(),
                    entityHolder,
                    dollInfo.getTextureLocation(),
                    dollInfo.isAlexModel(),
                    dollInfo.getDisplayName()
                );
                
                successCount++;
            } catch (Exception e) {
                LOGGER.error("注册动态玩偶失败: {}", dollInfo.getFileName(), e);
                e.printStackTrace();
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
                        
                        // 添加动态注册的玩偶物品
                        int dynamicCount = 0;
                        for (var entry : ModItems.DYNAMIC_DOLLS.entrySet()) {
                            try {
                                ItemStack stack = new ItemStack(entry.getValue().get());
                                output.accept(stack);
                                dynamicCount++;
                            } catch (Exception e) {
                                LOGGER.error("添加动态玩偶到物品栏失败: {}", entry.getKey(), e);
                            }
                        }
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

