package com.lanye.dolladdon;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.impl.item.ActionDebugStick;
import com.lanye.dolladdon.impl.item.PoseDebugStick;
import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import com.lanye.dolladdon.init.DefaultFileInitializer;
import com.lanye.dolladdon.util.logging.LogConfigManager;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import com.lanye.dolladdon.util.resource.ResourceFileGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class PlayerDollAddon implements ModInitializer {
    public static final String MODID = "player_doll";
    
    // 姿态文件目录路径（相对于游戏目录）
    public static final String POSES_DIR = "player_doll/poses";
    // 动作文件目录路径（相对于游戏目录）
    public static final String ACTIONS_DIR = "player_doll/actions";
    
    // 创造模式物品栏
    public static final ItemGroup PLAYER_DOLL_TAB = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.STEVE_DOLL))
            .displayName(Text.translatable("itemGroup.player_doll.player_doll_tab"))
            .entries((displayContext, entries) -> {
                // 添加史蒂夫玩偶物品（固定模型：粗手臂 + Steve默认皮肤）
                entries.add(new ItemStack(ModItems.STEVE_DOLL));
                
                // 添加艾利克斯玩偶物品（固定模型：细手臂 + Alex默认皮肤）
                entries.add(new ItemStack(ModItems.ALEX_DOLL));
                
                // 添加所有自定义纹理玩偶物品
                java.util.Map<String, net.minecraft.item.Item> customItems = ModItems.getAllCustomTextureDollItems();
                for (net.minecraft.item.Item item : customItems.values()) {
                    entries.add(new ItemStack(item));
                }
                
                // 添加动作调试棒
                entries.add(new ItemStack(ModItems.ACTION_DEBUG_STICK));
                
                // 添加姿态调试棒
                entries.add(new ItemStack(ModItems.POSE_DEBUG_STICK));
            })
            .build();

    @Override
    public void onInitialize() {
        // 初始化日志模块配置（必须在其他初始化之前）
        LogConfigManager.initializeModuleLevels();

        // 初始化文件日志系统（必须在其他日志操作之前）
        LogConfigManager.initializeFileLogging();

        ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "========== 玩偶模组开始初始化 ==========");
        
        try {
            // 初始化默认文件（从资源包复制到文件系统）
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "步骤 1/6: 初始化默认文件...");
            initializeDefaultFiles();
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "步骤 1/6: 完成");
            
            // 生成资源文件（物品模型和语言文件）
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "步骤 2/6: 生成资源文件...");
            generateResourceFiles();
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "步骤 2/6: 完成");
            
            // 注册物品
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "步骤 3/6: 注册物品...");
            ModItems.register();
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "步骤 3/6: 完成");
            
            // 注册实体
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "步骤 4/6: 注册实体...");
            ModEntities.register();
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "步骤 4/6: 完成");
            
            // 注册创造模式物品栏
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "步骤 5/6: 注册创造模式物品栏...");
            Registry.register(Registries.ITEM_GROUP, 
                    new Identifier(MODID, "player_doll_tab"), 
                    PLAYER_DOLL_TAB);
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "步骤 5/6: 完成");
            
            // 注册实体交互事件
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "步骤 6/6: 注册实体交互事件...");
            registerEntityInteractionEvent();
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "步骤 6/6: 完成");
            
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "========== 玩偶模组初始化完成 ==========");
        } catch (Exception e) {
            ModuleLogger.error(LogModuleConfig.MODULE_MAIN, "========== 玩偶模组初始化失败 ==========", e);
            throw new RuntimeException("玩偶模组初始化失败", e);
        }
    }
    
    /**
     * 注册实体交互事件
     * 使用 Fabric 的 UseEntityCallback 来处理玩偶实体的右键交互
     * 注意：UseEntityCallback 在客户端和服务端都会触发，但主要逻辑应在服务端处理
     */
    private void registerEntityInteractionEvent() {
        try {
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "开始注册 UseEntityCallback 事件... (当前环境: {})", 
                FabricLoader.getInstance().getEnvironmentType());
            
            // 注册事件处理器（在客户端和服务端都会注册）
            // 先添加一个测试处理器，记录所有实体交互
            UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
                // 记录所有实体交互尝试（用于调试）- 使用 INFO 级别确保能看到
                ModuleLogger.info(LogModuleConfig.MODULE_ENTITY_INTERACT, 
                    "[UseEntityCallback测试] 玩家 {} 交互实体: 类型={}, ID={}, 手={}, 服务端={}, 实体类={}", 
                    player.getName().getString(), 
                    entity != null ? entity.getType().toString() : "null",
                    entity != null ? entity.getId() : -1,
                    hand, !world.isClient,
                    entity != null ? entity.getClass().getName() : "null");
                
                // 只处理主手（右键）交互
                if (hand != Hand.MAIN_HAND) {
                    return ActionResult.PASS;
                }
                
                // 只处理玩偶实体
                if (!(entity instanceof BaseDollEntity dollEntity)) {
                    return ActionResult.PASS;
                }
                
                // 检查玩家是否手持动作调试棒或姿态调试棒
                ItemStack stack = player.getStackInHand(hand);
                if (stack.getItem() instanceof ActionDebugStick) {
                    // 使用动作调试棒应用动作
                    return ActionDebugStick.applyActionToEntity(stack, player, dollEntity, world);
                } else if (stack.getItem() instanceof PoseDebugStick) {
                    // 使用姿态调试棒应用姿态
                    return PoseDebugStick.applyPoseToEntity(stack, player, dollEntity, world);
                }

                // 记录交互尝试
                double distance = player.getPos().distanceTo(entity.getPos());
                ModuleLogger.info(LogModuleConfig.MODULE_ENTITY_INTERACT, 
                    "[UseEntityCallback] 玩家 {} 尝试交互玩偶实体: 实体ID={}, 位置=({}, {}, {}), 玩家位置=({}, {}, {}), 距离={}, 手={}, 服务端={}", 
                    player.getName().getString(), entity.getId(),
                    String.format("%.2f", entity.getX()), String.format("%.2f", entity.getY()), String.format("%.2f", entity.getZ()),
                    String.format("%.2f", player.getX()), String.format("%.2f", player.getY()), String.format("%.2f", player.getZ()),
                    String.format("%.2f", distance), hand, !world.isClient);
                
                // 调用实体的 interact 方法
                ActionResult result = dollEntity.interact(player, hand);
                
                // 记录结果
                ModuleLogger.info(LogModuleConfig.MODULE_ENTITY_INTERACT, 
                    "[UseEntityCallback] 实体返回结果: {}, 服务端={}", 
                    result, !world.isClient);
                
                // 如果实体返回了结果，使用它
                if (result != ActionResult.PASS) {
                    return result;
                }
                
                // 否则继续默认交互
                return ActionResult.PASS;
            });
            
            ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "UseEntityCallback 事件已成功注册");
        } catch (Exception e) {
            ModuleLogger.error(LogModuleConfig.MODULE_MAIN, "注册 UseEntityCallback 事件时出错", e);
            throw new RuntimeException("注册实体交互事件失败", e);
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
            ModuleLogger.error(LogModuleConfig.MODULE_MAIN, "生成资源文件时出错", e);
        }
    }
    
    /**
     * 初始化默认文件（生成到文件系统）
     */
    private void initializeDefaultFiles() {
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir();
            DefaultFileInitializer.initializeDefaultFiles(gameDir);
        } catch (Exception e) {
            ModuleLogger.error(LogModuleConfig.MODULE_MAIN, "初始化默认文件失败", e);
        }
    }
    
}

