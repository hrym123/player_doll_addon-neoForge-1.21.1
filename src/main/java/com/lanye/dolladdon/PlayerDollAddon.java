package com.lanye.dolladdon;

import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import com.lanye.dolladdon.util.neoForge.DynamicDollLoader;
import com.lanye.dolladdon.util.neoForge.DynamicModelGenerator;
import com.lanye.dolladdon.util.resource.ResourceFileGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import com.lanye.dolladdon.base.entity.BaseDollEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.nio.file.Path;

@Mod(PlayerDollAddon.MODID)
public class PlayerDollAddon {
    public static final String MODID = "player_doll";
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
        LOGGER.info("========== 玩偶模组开始初始化 ==========");
        
        try {
            // 步骤 1/6: 初始化默认文件（从资源包复制到文件系统）
            LOGGER.info("步骤 1/6: 初始化默认文件...");
            initializeDefaultFiles();
            LOGGER.info("步骤 1/6: 完成");
            
            // 步骤 2/6: 生成资源文件（物品模型和语言文件）
            LOGGER.info("步骤 2/6: 生成资源文件...");
            generateResourceFiles();
            LOGGER.info("步骤 2/6: 完成");
            
            // 步骤 3/6: 注册自定义纹理玩偶（必须在注册器注册之前）
            LOGGER.info("步骤 3/6: 注册自定义纹理玩偶...");
            ModItems.registerCustomTextureDollItems();
            ModEntities.registerCustomTextureDollEntities();
            LOGGER.info("步骤 3/6: 完成");
            
            // 先扫描目录并注册动态玩偶（必须在注册器注册之前）
            registerDynamicDolls();
            
            // 步骤 4/6: 注册物品和实体（使用DeferredRegister）
            LOGGER.info("步骤 4/6: 注册物品和实体...");
            ModItems.ITEMS.register(modEventBus);
            ModEntities.ENTITIES.register(modEventBus);
            LOGGER.info("步骤 4/6: 完成");
            
            // 步骤 5/6: 注册创造模式物品栏
            LOGGER.info("步骤 5/6: 注册创造模式物品栏...");
            CREATIVE_MODE_TABS.register(modEventBus);
            modEventBus.addListener(this::addCreative);
            LOGGER.info("步骤 5/6: 完成");
            
            // 步骤 6/6: 注册实体交互事件
            LOGGER.info("步骤 6/6: 注册实体交互事件...");
            modEventBus.addListener(this::onEntityInteract);
            LOGGER.info("步骤 6/6: 完成");
            
            LOGGER.info("========== 玩偶模组初始化完成 ==========");
        } catch (Exception e) {
            LOGGER.error("========== 玩偶模组初始化失败 ==========", e);
            throw new RuntimeException("玩偶模组初始化失败", e);
        }
    }
    
    /**
     * 处理实体交互事件
     * 使用 NeoForge 的 PlayerInteractEvent.EntityInteract 来处理玩偶实体的右键交互
     */
    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        try {
            // 记录所有实体交互尝试（用于调试）
            LOGGER.info("[实体交互] 玩家 {} 交互实体: 类型={}, ID={}, 手={}, 服务端={}, 实体类={}", 
                    event.getEntity().getName().getString(), 
                    event.getTarget() != null ? event.getTarget().getType().toString() : "null",
                    event.getTarget() != null ? event.getTarget().getId() : -1,
                    event.getHand(), !event.getLevel().isClientSide(),
                    event.getTarget() != null ? event.getTarget().getClass().getName() : "null");
            
            // 只处理主手（右键）交互
            if (event.getHand() != InteractionHand.MAIN_HAND) {
                return;
            }
            
            // 只处理玩偶实体
            if (!(event.getTarget() instanceof BaseDollEntity dollEntity)) {
                return;
            }
            
            // 检查玩家是否手持调试棒（如果调试棒类存在）
            // 注意：NeoForge版本中调试棒类不存在，跳过此功能
            
            // 记录交互尝试
            double distance = event.getEntity().position().distanceTo(event.getTarget().position());
            LOGGER.info("[实体交互] 玩家 {} 尝试交互玩偶实体: 实体ID={}, 位置=({}, {}, {}), 玩家位置=({}, {}, {}), 距离={}, 手={}, 服务端={}", 
                    event.getEntity().getName().getString(), event.getTarget().getId(),
                    String.format("%.2f", event.getTarget().getX()), 
                    String.format("%.2f", event.getTarget().getY()), 
                    String.format("%.2f", event.getTarget().getZ()),
                    String.format("%.2f", event.getEntity().getX()), 
                    String.format("%.2f", event.getEntity().getY()), 
                    String.format("%.2f", event.getEntity().getZ()),
                    String.format("%.2f", distance), event.getHand(), !event.getLevel().isClientSide());
            
            // 调用实体的 interact 方法
            InteractionResult result = dollEntity.interact(event.getEntity(), event.getHand());
            
            // 记录结果
            LOGGER.info("[实体交互] 实体返回结果: {}, 服务端={}", 
                    result, !event.getLevel().isClientSide());
            
            // 如果实体返回了结果，使用它
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        } catch (Exception e) {
            LOGGER.error("处理实体交互事件时出错", e);
        }
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
            
            com.lanye.dolladdon.init.DefaultFileInitializer.initializeDefaultFiles(gameDir);
        } catch (Exception e) {
            LOGGER.error("初始化默认文件失败", e);
        }
    }
    
    /**
     * 生成资源文件（物品模型和语言文件）
     */
    private void generateResourceFiles() {
        try {
            ResourceFileGenerator.generateItemModels();
            ResourceFileGenerator.updateLanguageFiles();
        } catch (Exception e) {
            LOGGER.error("生成资源文件时出错", e);
        }
    }
    
    /**
     * 注册动态玩偶（从文件加载）
     * 在构造函数中调用，确保在注册器注册之前完成
     */
    private void registerDynamicDolls() {
        // 先清理旧的动态模型文件（保留 alex_doll.json 和 steve_doll.json）
        DynamicModelGenerator.cleanupOldModelFiles();
        
        // 扫描目录
        var dollInfos = DynamicDollLoader.scanDirectory(PNG_DIR);
        
        // 批量生成所有动态玩偶的模型文件（所有动态玩偶都使用相同的模型内容）
        java.util.List<String> registryNames = new java.util.ArrayList<>();
        for (var dollInfo : dollInfos) {
            registryNames.add(dollInfo.getFileName());
        }
        DynamicModelGenerator.generateAllItemModels(registryNames);
        
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
                    .title(Component.translatable("itemGroup.player_doll.player_doll_tab"))
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
                        
                        // 添加所有自定义纹理玩偶物品
                        for (var entry : ModItems.CUSTOM_TEXTURE_DOLL_ITEMS.entrySet()) {
                            try {
                                ItemStack stack = new ItemStack(entry.getValue().get());
                                output.accept(stack);
                            } catch (Exception e) {
                                LOGGER.error("添加自定义纹理玩偶到物品栏失败: {}", entry.getKey(), e);
                            }
                        }
                        
                        // 添加调试棒（如果存在）
                        // 注意：NeoForge版本中调试棒类不存在，跳过此功能
                        // 如果将来添加了调试棒，可以在这里添加：
                        // try {
                        //     if (ModItems.ACTION_DEBUG_STICK != null) {
                        //         output.accept(new ItemStack(ModItems.ACTION_DEBUG_STICK.get()));
                        //     }
                        //     if (ModItems.POSE_DEBUG_STICK != null) {
                        //         output.accept(new ItemStack(ModItems.POSE_DEBUG_STICK.get()));
                        //     }
                        // } catch (Exception e) {
                        //     LOGGER.debug("调试棒不存在，跳过添加到创造模式物品栏", e);
                        // }
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

