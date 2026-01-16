package com.lanye.dolladdon;

import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import com.lanye.dolladdon.impl.item.ActionDebugStick;
import com.lanye.dolladdon.impl.item.PoseDebugStick;
import com.lanye.dolladdon.util.command.DollSkinCommand;
import com.lanye.dolladdon.util.neoForge.DynamicDollLoader;
import com.lanye.dolladdon.util.neoForge.DynamicModelGenerator;
import com.lanye.dolladdon.util.resource.ResourceFileGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.nio.file.Path;

@Mod(PlayerDoll.MODID)
public class PlayerDoll {
    public static final String MODID = "player_doll";
    
    // 玩偶图片目录路径（相对于游戏目录）
    public static final String PNG_DIR = "player_doll/png";
    // 姿态文件目录路径（相对于游戏目录）
    public static final String POSES_DIR = "player_doll/poses";
    // 动作文件目录路径（相对于游戏目录）
    public static final String ACTIONS_DIR = "player_doll/actions";
    
    // 创建创造模式物品栏注册器
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public PlayerDoll(IEventBus modEventBus, ModContainer modContainer) {
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
            
            // Register command registration event
            NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
            
            // Client initialization (if on client)
            if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                PlayerDollClient.init();
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
     * 重构后：不再注册新的物品和实体，而是使用统一的 CustomTextureDollItem
     * 皮肤路径通过NBT存储，在创造模式物品栏中创建带NBT的物品
     */
    private void registerDynamicDolls() {
        // 扫描目录（用于在创造模式物品栏中创建带NBT的物品）
        // 不再注册新的物品和实体，而是使用统一的 CustomTextureDollItem
        cachedDollInfos = DynamicDollLoader.scanDirectory(PNG_DIR);
        
        // 存储到静态变量，供创造模式物品栏使用
        // 注意：这里不直接创建物品，而是在 displayItems 回调中创建带NBT的物品
        // 这样可以避免在注册阶段创建 ItemStack（可能导致问题）
    }
    
    /**
     * 创建带NBT的玩偶物品（用于动态注册的玩偶）
     * 使用统一的 CustomTextureDollItem，通过NBT存储皮肤路径
     * 
     * @deprecated 请使用 {@link com.lanye.dolladdon.util.factory.DollItemFactory#createCustomTextureDoll(String, boolean, String)} 替代
     */
    @Deprecated(forRemoval = false)
    public static ItemStack createDynamicDollItemWithNBT(
            String displayName,
            ResourceLocation textureLocation,
            boolean isAlexModel) {
        // 使用工厂类创建，确保NBT结构标准化
        // 从textureLocation提取skinPath
        String skinPath = textureLocation.toString();
        
        // 从displayName或textureLocation提取playerName
        // 如果textureLocation是 player_doll:textures/entity/xxx 格式，需要从文件路径提取
        String playerName = displayName; // 默认使用displayName
        
        // 尝试从textureLocation的路径中提取文件名
        // 如果textureLocation是 player_doll:textures/entity/xxx，需要从文件路径获取原始文件名
        // 但这里我们无法直接获取，所以使用displayName作为playerName
        
        // 使用工厂类创建物品，确保包含PlayerName
        return com.lanye.dolladdon.util.factory.DollItemFactory.createCustomTextureDoll(
            skinPath,
            isAlexModel,
            playerName
        );
    }
    
    // 存储扫描到的玩偶信息，供创造模式物品栏使用
    private static java.util.List<DynamicDollLoader.DollInfo> cachedDollInfos = new java.util.ArrayList<>();
    
    
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
                        
                        // 添加动态注册的玩偶物品（从文件扫描，创建带NBT的物品）
                        // 使用统一的 CustomTextureDollItem，通过NBT存储皮肤路径
                        for (var dollInfo : cachedDollInfos) {
                            try {
                                // 从文件路径提取文件名（带扩展名）
                                String fileName = dollInfo.getFilePath().getFileName().toString();
                                
                                // 从文件名提取玩家名（使用工具类）
                                String playerName = com.lanye.dolladdon.util.resource.SkinFileNamingUtil.extractPlayerNameFromFileName(fileName);
                                if (playerName == null || playerName.isEmpty()) {
                                    // 如果无法提取，使用displayName作为fallback
                                    playerName = dollInfo.getDisplayName();
                                }
                                
                                // 构建正确的skinPath格式：player_doll:png/xxx.png
                                String skinPath = "player_doll:png/" + fileName;
                                
                                // 使用工厂类创建物品，确保包含PlayerName
                                ItemStack stack = com.lanye.dolladdon.util.factory.DollItemFactory.createCustomTextureDoll(
                                    skinPath,
                                    dollInfo.isAlexModel(),
                                    playerName
                                );
                                output.accept(stack);
                            } catch (Exception e) {
                                // Error logging handled by Mixin
                            }
                        }
                        
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
     * 注意：动作更新现在在实体自己的tick()中处理，这里不再需要
     * 保留此方法以防需要全局处理逻辑
     */
    private void onServerTick(ServerTickEvent.Post event) {
        // 动作更新现在在BaseDollEntity.tick()中处理，不需要外部搜索
        // 这样可以避免每tick搜索所有实体，提高性能
    }

    /**
     * Handle level tick event to update doll entity client poses
     * 注意：姿态更新现在在实体自己的tick()和onSyncedDataUpdated()中处理，这里不再需要
     * 保留此方法以防需要全局处理逻辑
     */
    private void onLevelTick(LevelTickEvent.Post event) {
        // 姿态更新现在在BaseDollEntity.tick()和onSyncedDataUpdated()中处理
        // 这样可以避免每tick搜索所有实体，提高性能
        // 数据同步时通过onSyncedDataUpdated()立即更新，tick时也更新以确保同步
    }
    
    /**
     * Handle command registration event
     * Register the /dollskin command
     */
    private void onRegisterCommands(RegisterCommandsEvent event) {
        try {
            DollSkinCommand.register(event.getDispatcher(), event.getBuildContext());
        } catch (Exception e) {
            // Error logging handled by Mixin
        }
    }

}

