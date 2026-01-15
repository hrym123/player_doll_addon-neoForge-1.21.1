package com.lanye.dolladdon;

import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import com.lanye.dolladdon.impl.item.ActionDebugStick;
import com.lanye.dolladdon.impl.item.PoseDebugStick;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.nio.file.Path;

@Mod(PlayerDollAddon.MODID)
public class PlayerDollAddon {
    public static final String MODID = "player_doll";
    
    // 玩偶图片目录路径（相对于游戏目录）
    public static final String PNG_DIR = "player_doll/png";
    // 姿态文件目录路径（相对于游戏目录）
    public static final String POSES_DIR = "player_doll/poses";
    // 动作文件目录路径（相对于游戏目录）
    public static final String ACTIONS_DIR = "player_doll/actions";
    
    // 创建创造模式物品栏注册器
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public PlayerDollAddon(IEventBus modEventBus, ModContainer modContainer) {
        try {
            // Step 1/6: Initialize default files (copy from resource pack to file system)
            initializeDefaultFiles();
            
            // Step 2/6: Generate resource files (item models and language files)
            generateResourceFiles();
            
            // Step 3/6: Scan directory and register dynamic dolls (must be before registry registration)
            // 注意：不再注册 CustomTextureDoll，因为 DynamicDoll 已经处理了所有 PNG 文件
            // ModItems.registerCustomTextureDollItems();
            // ModEntities.registerCustomTextureDollEntities();
            registerDynamicDolls();
            
            // Step 4/6: Register items and entities (using DeferredRegister)
            ModItems.ITEMS.register(modEventBus);
            ModEntities.ENTITIES.register(modEventBus);
            
            // Step 5/6: Register creative mode tab
            CREATIVE_MODE_TABS.register(modEventBus);
            modEventBus.addListener(this::addCreative);
            
            // Step 6/6: Register entity interaction events (register to game event bus, not mod event bus)
            NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
            
            // Register player login and logout events to restore and save debug stick data
            NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
            NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);

            // Register tick events for doll entity updates
            NeoForge.EVENT_BUS.addListener(this::onServerTick);
            NeoForge.EVENT_BUS.addListener(this::onLevelTick);
            
            // Client initialization (if on client)
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                PlayerDollAddonClient.init();
            }
        } catch (Exception e) {
            throw new RuntimeException("Player Doll Mod initialization failed", e);
        }
    }
    
    /**
     * 处理实体交互事件
     * 使用 NeoForge 的 PlayerInteractEvent.EntityInteract 来处理玩偶实体的右键交互
     */
    private void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        try {
            // 只处理主手（右键）交互
            if (event.getHand() != InteractionHand.MAIN_HAND) {
                return;
            }
            
            // 只处理玩偶实体
            if (!(event.getTarget() instanceof BaseDollEntity dollEntity)) {
                return;
            }
            
            // 检查玩家是否手持调试棒
            ItemStack heldStack = event.getEntity().getItemInHand(event.getHand());
            if (!heldStack.isEmpty()) {
                if (heldStack.getItem() instanceof ActionDebugStick) {
                    InteractionResult result = ActionDebugStick.applyActionToEntity(heldStack, event.getEntity(), dollEntity, event.getLevel());
                    if (result != InteractionResult.PASS) {
                        event.setCancellationResult(result);
                        event.setCanceled(true);
                    }
                    return;
                } else if (heldStack.getItem() instanceof PoseDebugStick) {
                    InteractionResult result = PoseDebugStick.applyPoseToEntity(heldStack, event.getEntity(), dollEntity, event.getLevel());
                    if (result != InteractionResult.PASS) {
                        event.setCancellationResult(result);
                        event.setCanceled(true);
                    }
                    return;
                }
            }
            
            // Call entity's interact method
            InteractionResult result = dollEntity.interact(event.getEntity(), event.getHand());
            
            // 如果实体返回了结果，使用它
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        } catch (Exception e) {
            // Error logging handled by Mixin
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
            // Error logging handled by Mixin
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
            // Error logging handled by Mixin
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
                // Error logging handled by Mixin
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
                                // Error logging handled by Mixin
                            }
                        }
                        
                        // 注意：不再添加 CustomTextureDoll 物品，因为已经不再注册这些物品
                        // 所有玩偶都通过 DynamicDoll 方式注册
                        /*
                        // Add all custom texture doll items
                        for (var entry : ModItems.CUSTOM_TEXTURE_DOLL_ITEMS.entrySet()) {
                            try {
                                ItemStack stack = new ItemStack(entry.getValue().get());
                                output.accept(stack);
                            } catch (Exception e) {
                                // Error logging handled by Mixin
                            }
                        }
                        */
                        
                        // Add debug sticks
                        try {
                            output.accept(new ItemStack(ModItems.ACTION_DEBUG_STICK.get()));
                            output.accept(new ItemStack(ModItems.POSE_DEBUG_STICK.get()));
                        } catch (Exception e) {
                            // Error logging handled by Mixin
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
    
    /**
     * Handle player login event, restore debug stick data
     */
    private void onPlayerLoggedIn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        try {
            net.minecraft.world.entity.player.Player player = event.getEntity();
            if (player != null) {
                // Restore debug stick data from inventory
                ActionDebugStick.restoreFromInventory(player);
                PoseDebugStick.restoreFromInventory(player);
            }
        } catch (Exception e) {
            // Error logging handled by Mixin
        }
    }
    
    /**
     * Handle player logout event, save debug stick data to ItemStack NBT
     */
    private void onPlayerLoggedOut(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        try {
            net.minecraft.world.entity.player.Player player = event.getEntity();
            if (player != null && !player.level().isClientSide()) {
                // Save data to ItemStack NBT on server
                ActionDebugStick.saveToInventory(player);
                PoseDebugStick.saveToInventory(player);
            }
        } catch (Exception e) {
            // Error logging handled by Mixin
        }
    }

    /**
     * Handle server tick event to update doll entity actions
     */
    private void onServerTick(ServerTickEvent.Post event) {
        try {
            // Update actions for all doll entities in all loaded levels
            for (var level : net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
                for (var entity : level.getAllEntities()) {
                    if (entity instanceof BaseDollEntity dollEntity && dollEntity.getCurrentAction() != null) {
                        dollEntity.updateServerAction();
                    }
                }
            }
        } catch (Exception e) {
            // Error logging handled by Mixin
        }
    }

    /**
     * Handle level tick event to update doll entity client poses
     */
    private void onLevelTick(LevelTickEvent.Post event) {
        try {
            // Only process on client side
            if (event.getLevel().isClientSide) {
                // Update client poses for all doll entities in the level
                // Use Minecraft world boundary limits (approximately -30000000 to 30000000)
                // This ensures we cover all loaded entities
                net.minecraft.world.phys.AABB searchArea = new net.minecraft.world.phys.AABB(
                    -30000000, -320, -30000000,
                    30000000, 320, 30000000
                );
                
                // Get all entities and filter for BaseDollEntity
                // Note: getEntitiesOfClass returns an iterable, we need to iterate it
                java.util.List<net.minecraft.world.entity.Entity> entities = new java.util.ArrayList<>();
                event.getLevel().getEntitiesOfClass(
                    net.minecraft.world.entity.Entity.class,
                    searchArea,
                    entity -> entity instanceof BaseDollEntity
                ).forEach(entities::add);
                
                // Update poses for all found doll entities
                for (net.minecraft.world.entity.Entity entity : entities) {
                    if (entity instanceof BaseDollEntity dollEntity) {
                        dollEntity.updateClientPose();
                    }
                }
            }
        } catch (Exception e) {
            // Print error for debugging
            System.err.println("[PlayerDoll] Error in onLevelTick: " + e.getMessage());
            e.printStackTrace();
            // Error logging handled by Mixin
        }
    }

}

