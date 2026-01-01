package com.lanye.dolladdon.util.pose;

import com.lanye.dolladdon.api.action.DollAction;
import com.lanye.dolladdon.api.pose.DollPose;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import net.minecraft.resource.ResourceManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 姿态和动作管理器
 * 管理所有加载的姿态和动作资源
 */
public class PoseActionManager {
    
    // 模块化日志：从 LogModuleConfig 读取模块名称
    private static final String LOG_MODULE_POSE_LOADER = LogModuleConfig.MODULE_POSE_LOADER;
    private static final String LOG_MODULE_ACTION_LOADER = LogModuleConfig.MODULE_ACTION_LOADER;
    
    private static final Map<String, DollPose> poses = new HashMap<>();
    private static final Map<String, DollAction> actions = new HashMap<>();
    
    /**
     * 加载所有姿态和动作资源
     * 应该在游戏启动时或资源重载时调用（如执行 /reload 命令）
     */
    public static void loadResources(ResourceManager resourceManager) {
        ModuleLogger.info(LogModuleConfig.MODULE_RESOURCE, "开始加载姿态和动作资源...");
        
        // 加载所有姿态（从资源包和文件系统）
        Map<String, DollPose> loadedPoses = PoseLoader.loadAllPoses(resourceManager);
        
        int oldPoseCount = poses.size();
        poses.clear();
        poses.putAll(loadedPoses);
        
        ModuleLogger.info(LOG_MODULE_POSE_LOADER, "姿态加载完成: 共加载 {} 个姿态 (之前: {}, 新增: {})", 
            loadedPoses.size(), oldPoseCount, loadedPoses.size());
        
        // 记录每个加载的姿态
        for (Map.Entry<String, DollPose> entry : loadedPoses.entrySet()) {
            String poseName = entry.getKey();
            DollPose pose = entry.getValue();
            String displayName = pose.getDisplayName() != null ? pose.getDisplayName() : poseName;
            ModuleLogger.debug(LOG_MODULE_POSE_LOADER, "注册姿态: {} (显示名称: {})", poseName, displayName);
        }
        
        // 加载所有动作
        Map<String, DollAction> loadedActions = ActionLoader.loadAllActions(resourceManager);
        
        int oldActionCount = actions.size();
        actions.clear();
        actions.putAll(loadedActions);
        
        ModuleLogger.info(LOG_MODULE_ACTION_LOADER, "动作加载完成: 共加载 {} 个动作 (之前: {}, 新增: {})", 
            loadedActions.size(), oldActionCount, loadedActions.size());
        
        // 记录每个加载的动作
        for (Map.Entry<String, DollAction> entry : loadedActions.entrySet()) {
            String actionName = entry.getKey();
            DollAction action = entry.getValue();
            ModuleLogger.debug(LOG_MODULE_ACTION_LOADER, "注册动作: {} (循环: {}, 时长: {} ticks)", 
                actionName, action.isLooping(), action.getDuration());
        }
        
        ModuleLogger.info(LogModuleConfig.MODULE_RESOURCE, "姿态和动作资源加载完成: {} 个姿态, {} 个动作", loadedPoses.size(), loadedActions.size());
    }
    
    /**
     * 获取姿态
     * @param name 姿态名称
     * @return 姿态，如果不存在返回null
     */
    public static DollPose getPose(String name) {
        return poses.get(name);
    }
    
    /**
     * 获取动作
     * @param name 动作名称
     * @return 动作，如果不存在返回null
     */
    public static DollAction getAction(String name) {
        return actions.get(name);
    }
    
    /**
     * 获取所有姿态
     * @return 姿态映射
     */
    public static Map<String, DollPose> getAllPoses() {
        return new HashMap<>(poses);
    }
    
    /**
     * 获取所有动作
     * @return 动作映射
     */
    public static Map<String, DollAction> getAllActions() {
        return new HashMap<>(actions);
    }
    
    /**
     * 注册自定义姿态（供开发者使用）
     * @param name 姿态名称
     * @param pose 姿态对象
     */
    public static void registerPose(String name, DollPose pose) {
        boolean isNew = !poses.containsKey(name);
        poses.put(name, pose);
        String displayName = pose.getDisplayName() != null ? pose.getDisplayName() : name;
        if (isNew) {
            ModuleLogger.info(LOG_MODULE_POSE_LOADER, "注册新姿态: {} (显示名称: {})", name, displayName);
        } else {
            ModuleLogger.debug(LOG_MODULE_POSE_LOADER, "覆盖已存在姿态: {} (显示名称: {})", name, displayName);
        }
    }
    
    /**
     * 注册自定义动作（供开发者使用）
     * @param name 动作名称
     * @param action 动作对象
     */
    public static void registerAction(String name, DollAction action) {
        boolean isNew = !actions.containsKey(name);
        actions.put(name, action);
        if (isNew) {
            ModuleLogger.info(LOG_MODULE_ACTION_LOADER, "注册新动作: {} (循环: {}, 时长: {} ticks)", 
                name, action.isLooping(), action.getDuration());
        } else {
            ModuleLogger.debug(LOG_MODULE_ACTION_LOADER, "覆盖已存在动作: {} (循环: {}, 时长: {} ticks)", 
                name, action.isLooping(), action.getDuration());
        }
    }
    
    /**
     * 从文件系统重新加载姿态文件（动态读取）
     * 可以在游戏运行时调用此方法来重新加载 poses 目录中的姿态文件
     */
    public static void reloadPosesFromFileSystem() {
        try {
            Path gameDir;
            try {
                // 使用 FabricLoader 获取游戏目录
                gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
            } catch (Exception e) {
                gameDir = Paths.get(".").toAbsolutePath().normalize();
            }
            
            Path posesDir = gameDir.resolve(PlayerDollAddon.POSES_DIR);
            Map<String, DollPose> fileSystemPoses = PoseLoader.loadPosesFromFileSystem(posesDir);
            
            // 更新姿态映射（保留资源包中的姿态，但用文件系统中的姿态覆盖同名姿态）
            for (Map.Entry<String, DollPose> entry : fileSystemPoses.entrySet()) {
                String poseName = entry.getKey();
                boolean isNew = !poses.containsKey(poseName);
                poses.put(poseName, entry.getValue());
                if (isNew) {
                    ModuleLogger.debug(LOG_MODULE_POSE_LOADER, "从文件系统动态加载新姿态: {}", poseName);
                } else {
                    ModuleLogger.debug(LOG_MODULE_POSE_LOADER, "从文件系统动态覆盖姿态: {}", poseName);
                }
            }
        } catch (Exception e) {
            ModuleLogger.error(LogModuleConfig.MODULE_POSE_LOADER, "从文件系统重新加载姿态失败", e);
        }
    }
}

