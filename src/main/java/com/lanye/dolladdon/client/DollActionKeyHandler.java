package com.lanye.dolladdon.client;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.api.action.DollAction;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import com.lanye.dolladdon.util.pose.PoseActionManager;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 玩偶动作按键处理器
 * 处理小键盘1键切换/停止动作的功能
 */
public class DollActionKeyHandler {
    private static final String LOG_MODULE = LogModuleConfig.MODULE_ENTITY_ACTION;
    
    private static KeyBinding playActionKey;
    
    // 所有可用动作的列表（按加载顺序）
    private static final List<String> actionList = new ArrayList<>();
    
    /**
     * 初始化按键绑定
     */
    public static void initialize() {
        // 注册播放/切换动作按键（小键盘1键）
        playActionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.player_doll.play_action", // 翻译键
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_KP_1, // 小键盘1键
            "category.player_doll" // 分类
        ));
        
        // 注册停止动作按键（潜行+小键盘1键）
        // 注意：这个按键绑定不会自动检测潜行，我们需要在tick事件中手动检测
        // 实际上我们只需要一个按键，然后在处理时检测潜行状态
        
        ModuleLogger.info(LOG_MODULE, "✓ 按键绑定已注册: 小键盘1键 = 播放/切换动作, 潜行+小键盘1键 = 停止动作");
    }
    
    /**
     * 注册客户端tick事件
     */
    public static void registerTickEvent() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (playActionKey == null) {
                return;
            }
            
            // 检查按键是否被按下
            if (playActionKey.wasPressed()) {
                ModuleLogger.info(LOG_MODULE, "✓ 检测到小键盘1键被按下");
                handleKeyPress(client);
            }
        });
    }
    
    /**
     * 处理按键按下事件
     */
    private static void handleKeyPress(MinecraftClient client) {
        ModuleLogger.info(LOG_MODULE, "========================================");
        ModuleLogger.info(LOG_MODULE, "按键处理：开始处理按键事件");
        ModuleLogger.info(LOG_MODULE, "========================================");
        
        if (client.player == null) {
            ModuleLogger.warn(LOG_MODULE, "按键处理：✗ 客户端玩家为空，取消处理");
            return;
        }
        if (client.world == null) {
            ModuleLogger.warn(LOG_MODULE, "按键处理：✗ 客户端世界为空，取消处理");
            return;
        }
        
        ModuleLogger.info(LOG_MODULE, "按键处理：客户端玩家 = {}, 世界 = {}, 是否为客户端 = {}",
            client.player.getName().getString(), client.world.getRegistryKey().getValue(), client.world.isClient);
        
        // 检查玩家是否在潜行
        boolean isSneaking = client.player.isSneaking();
        ModuleLogger.info(LOG_MODULE, "按键处理：玩家潜行状态 = {}", isSneaking);
        
        // 获取玩家看向的实体（使用10格距离，比默认交互距离稍大）
        Entity targetEntity = getEntityLookedAt(client, 10.0);
        ModuleLogger.info(LOG_MODULE, "按键处理：检测到的实体 = {}", targetEntity != null ? targetEntity.getClass().getSimpleName() : "null");
        
        if (targetEntity == null || !(targetEntity instanceof BaseDollEntity)) {
            // 没有看向玩偶实体，不执行任何操作
            ModuleLogger.warn(LOG_MODULE, "按键处理：✗ 未检测到玩偶实体（实体类型：{}）", 
                targetEntity != null ? targetEntity.getClass().getName() : "null");
            if (client.player != null) {
                client.player.sendMessage(Text.literal("未检测到玩偶实体"), false);
            }
            ModuleLogger.info(LOG_MODULE, "========================================");
            return;
        }
        
        BaseDollEntity dollEntity = (BaseDollEntity) targetEntity;
        ModuleLogger.info(LOG_MODULE, "按键处理：✓ 找到玩偶实体，类型 = {}, ID = {}, UUID = {}",
            dollEntity.getClass().getSimpleName(), dollEntity.getId(), dollEntity.getUuid());
        
        if (isSneaking) {
            // 潜行+小键盘1键：停止动作
            ModuleLogger.info(LOG_MODULE, "按键处理：执行停止动作");
            stopAction(dollEntity, client);
        } else {
            // 小键盘1键：播放/切换动作
            ModuleLogger.info(LOG_MODULE, "按键处理：执行播放/切换动作");
            playOrSwitchAction(dollEntity, client);
        }
        
        ModuleLogger.info(LOG_MODULE, "========================================");
    }
    
    /**
     * 播放或切换动作
     */
    private static void playOrSwitchAction(BaseDollEntity dollEntity, MinecraftClient client) {
        // 更新动作列表（如果动作加载有变化）
        updateActionList();
        
        if (actionList.isEmpty()) {
            ModuleLogger.warn(LOG_MODULE, "没有可用的动作");
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("message.player_doll.no_actions"), false);
            }
            return;
        }
        
        DollAction currentAction = dollEntity.getCurrentAction();
        String currentActionName = currentAction != null ? currentAction.getName() : null;
        
        // 如果当前动作为null（动作已停止），使用最后播放的动作名称
        if (currentActionName == null) {
            currentActionName = dollEntity.getLastActionName();
        }
        
        // 找到当前动作在列表中的索引
        int currentIndex = -1;
        if (currentActionName != null) {
            currentIndex = actionList.indexOf(currentActionName);
            ModuleLogger.debug(LOG_MODULE, "动作切换: 当前动作名称 = {}, 在列表中的索引 = {}", currentActionName, currentIndex);
        }
        
        // 切换到下一个动作
        int nextIndex = (currentIndex + 1) % actionList.size();
        String nextActionName = actionList.get(nextIndex);
        ModuleLogger.debug(LOG_MODULE, "动作切换: 下一个动作索引 = {}, 动作名称 = {}", nextIndex, nextActionName);
        
        DollAction nextAction = PoseActionManager.getAction(nextActionName);
        if (nextAction != null) {
            // 直接调用setAction，在单机游戏中会自动同步
            dollEntity.setAction(nextAction);
            ModuleLogger.info(LOG_MODULE, "切换到动作: {}", nextActionName);
            if (client.player != null) {
                String nextDisplayName = nextAction.getDisplayName();
                client.player.sendMessage(
                    Text.literal("切换到动作: " + nextDisplayName + " (" + (nextIndex + 1) + "/" + actionList.size() + ")"),
                    true
                );
            }
        } else {
            ModuleLogger.warn(LOG_MODULE, "动作不存在: {}", nextActionName);
        }
    }
    
    /**
     * 停止动作
     */
    private static void stopAction(BaseDollEntity dollEntity, MinecraftClient client) {
        DollAction currentAction = dollEntity.getCurrentAction();
        if (currentAction == null) {
            // 已经在停止状态
            if (client.player != null) {
                client.player.sendMessage(Text.translatable("message.player_doll.no_action_to_stop"), false);
            }
            return;
        }
        
        // 直接调用stopAction，在单机游戏中会自动同步
        dollEntity.stopAction();
        ModuleLogger.info(LOG_MODULE, "停止动作");
        if (client.player != null) {
            client.player.sendMessage(Text.translatable("message.player_doll.action_stopped"), false);
        }
    }
    
    /**
     * 获取玩家正在看向的实体
     * 使用备用方法：先尝试raycast，如果失败则查找玩家附近的玩偶实体
     * @param client 客户端实例
     * @param maxDistance 最大距离
     * @return 看向的实体，如果没有则返回null
     */
    private static Entity getEntityLookedAt(MinecraftClient client, double maxDistance) {
        ModuleLogger.info(LOG_MODULE, "========== getEntityLookedAt: 开始检测实体 ==========");
        
        if (client.player == null) {
            ModuleLogger.warn(LOG_MODULE, "getEntityLookedAt: ✗ 客户端玩家为空");
            return null;
        }
        if (client.world == null) {
            ModuleLogger.warn(LOG_MODULE, "getEntityLookedAt: ✗ 客户端世界为空");
            return null;
        }
        if (client.cameraEntity == null) {
            ModuleLogger.warn(LOG_MODULE, "getEntityLookedAt: ✗ 相机实体为空");
            return null;
        }
        
        // 记录玩家信息
        Vec3d playerPos = client.player.getEyePos();
        Vec3d playerLookVec = client.player.getRotationVec(client.getTickDelta());
        ModuleLogger.info(LOG_MODULE, "getEntityLookedAt: 玩家位置 = ({}, {}, {}), 视角向量 = ({}, {}, {})",
            String.format("%.2f", playerPos.x), String.format("%.2f", playerPos.y), String.format("%.2f", playerPos.z),
            String.format("%.2f", playerLookVec.x), String.format("%.2f", playerLookVec.y), String.format("%.2f", playerLookVec.z));
        
        // 方法1：尝试使用raycast（优先方法）
        float tickDelta = client.getTickDelta();
        HitResult hitResult = client.cameraEntity.raycast(64.0, tickDelta, false);
        ModuleLogger.info(LOG_MODULE, "getEntityLookedAt: raycast结果类型 = {}, tickDelta = {}, 距离 = 64.0", 
            hitResult.getType(), tickDelta);
        
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity entity = entityHitResult.getEntity();
            ModuleLogger.info(LOG_MODULE, "getEntityLookedAt: raycast检测到实体，类型 = {}, ID = {}, 类名 = {}",
                entity.getClass().getSimpleName(), entity.getId(), entity.getClass().getName());
            
            if (entity instanceof BaseDollEntity) {
                ModuleLogger.info(LOG_MODULE, "getEntityLookedAt: ✓ raycast检测到玩偶实体，类型 = {}, ID = {}", 
                    entity.getClass().getSimpleName(), entity.getId());
                return entity;
            } else {
                ModuleLogger.warn(LOG_MODULE, "getEntityLookedAt: raycast检测到的实体不是玩偶实体，而是: {}", 
                    entity.getClass().getName());
            }
        } else if (hitResult.getType() == HitResult.Type.BLOCK) {
            net.minecraft.util.hit.BlockHitResult blockHit = (net.minecraft.util.hit.BlockHitResult) hitResult;
            ModuleLogger.info(LOG_MODULE, "getEntityLookedAt: raycast检测到方块，位置 = ({}, {}, {})",
                blockHit.getBlockPos().getX(), blockHit.getBlockPos().getY(), blockHit.getBlockPos().getZ());
        } else {
            ModuleLogger.info(LOG_MODULE, "getEntityLookedAt: raycast未检测到任何目标（类型: {}）", hitResult.getType());
        }
        
        // 方法2：如果raycast失败，查找玩家附近最近的玩偶实体（备用方法）
        ModuleLogger.info(LOG_MODULE, "getEntityLookedAt: raycast未检测到玩偶实体，尝试查找附近实体（最大距离: {}）", maxDistance);
        return findNearestDollEntity(client, maxDistance);
    }
    
    /**
     * 查找玩家附近最近的玩偶实体（备用方法）
     */
    private static Entity findNearestDollEntity(MinecraftClient client, double maxDistance) {
        ModuleLogger.info(LOG_MODULE, "========== findNearestDollEntity: 开始查找附近实体 ==========");
        
        if (client.player == null || client.world == null) {
            ModuleLogger.warn(LOG_MODULE, "findNearestDollEntity: ✗ 客户端玩家或世界为空");
            return null;
        }
        
        Vec3d playerPos = client.player.getEyePos();
        Box searchBox = new Box(
            playerPos.x - maxDistance, playerPos.y - maxDistance, playerPos.z - maxDistance,
            playerPos.x + maxDistance, playerPos.y + maxDistance, playerPos.z + maxDistance
        );
        ModuleLogger.info(LOG_MODULE, "findNearestDollEntity: 搜索范围 = ({}, {}, {}) 到 ({}, {}, {})",
            String.format("%.2f", searchBox.minX), String.format("%.2f", searchBox.minY), String.format("%.2f", searchBox.minZ),
            String.format("%.2f", searchBox.maxX), String.format("%.2f", searchBox.maxY), String.format("%.2f", searchBox.maxZ));
        
        BaseDollEntity nearestEntity = null;
        double nearestDistance = maxDistance * maxDistance;
        int totalEntities = 0;
        int dollEntities = 0;
        
        // 遍历所有实体，查找玩偶实体
        // 注意：在客户端，可能只有部分实体被同步
        ModuleLogger.info(LOG_MODULE, "findNearestDollEntity: 开始遍历世界中的所有实体...");
        for (Entity entity : client.world.getEntities()) {
            totalEntities++;
            
            if (entity.equals(client.player)) {
                continue; // 跳过玩家自己
            }
            
            if (entity instanceof BaseDollEntity) {
                dollEntities++;
                Vec3d entityPos = entity.getPos();
                double distSq = playerPos.squaredDistanceTo(entityPos);
                double distance = Math.sqrt(distSq);
                
                ModuleLogger.info(LOG_MODULE, "findNearestDollEntity: 找到玩偶实体，类型 = {}, ID = {}, 位置 = ({}, {}, {}), 距离 = {}",
                    entity.getClass().getSimpleName(), entity.getId(),
                    String.format("%.2f", entityPos.x), String.format("%.2f", entityPos.y), String.format("%.2f", entityPos.z),
                    String.format("%.2f", distance));
                
                if (distSq < nearestDistance && distSq <= maxDistance * maxDistance) {
                    nearestDistance = distSq;
                    nearestEntity = (BaseDollEntity) entity;
                    ModuleLogger.info(LOG_MODULE, "findNearestDollEntity: ✓ 更新最近实体，类型 = {}, ID = {}, 距离 = {}",
                        entity.getClass().getSimpleName(), entity.getId(), String.format("%.2f", distance));
                } else if (distSq > maxDistance * maxDistance) {
                    ModuleLogger.info(LOG_MODULE, "findNearestDollEntity: 实体距离超出范围（{} > {}），跳过",
                        String.format("%.2f", distance), String.format("%.2f", maxDistance));
                }
            } else {
                // 记录非玩偶实体（用于调试）
                if (totalEntities <= 10) { // 只记录前10个实体，避免日志过多
                    ModuleLogger.debug(LOG_MODULE, "findNearestDollEntity: 非玩偶实体，类型 = {}, ID = {}",
                        entity.getClass().getSimpleName(), entity.getId());
                }
            }
        }
        
        ModuleLogger.info(LOG_MODULE, "findNearestDollEntity: 遍历完成，总实体数 = {}, 玩偶实体数 = {}", totalEntities, dollEntities);
        
        if (nearestEntity != null) {
            ModuleLogger.info(LOG_MODULE, "findNearestDollEntity: ✓ 找到最近玩偶实体，类型 = {}, ID = {}, 距离 = {}",
                nearestEntity.getClass().getSimpleName(), nearestEntity.getId(), 
                String.format("%.2f", Math.sqrt(nearestDistance)));
            return nearestEntity;
        }
        
        ModuleLogger.warn(LOG_MODULE, "findNearestDollEntity: ✗ 未找到附近的玩偶实体（范围内玩偶实体数: {}）", dollEntities);
        return null;
    }
    
    /**
     * 更新动作列表
     */
    private static void updateActionList() {
        Map<String, DollAction> allActions = PoseActionManager.getAllActions();
        actionList.clear();
        actionList.addAll(allActions.keySet());
        ModuleLogger.debug(LOG_MODULE, "updateActionList: 动作列表已更新，共 {} 个动作: {}", 
            actionList.size(), actionList);
        // 可以在这里对动作列表进行排序
    }
}
