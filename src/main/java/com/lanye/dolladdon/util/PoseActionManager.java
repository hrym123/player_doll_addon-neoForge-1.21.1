package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.api.action.DollAction;
import com.lanye.dolladdon.api.pose.DollPose;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 姿态和动作管理器
 * 管理所有加载的姿态和动作资源
 */
public class PoseActionManager {
    private static final Logger LOGGER = PlayerDollAddon.LOGGER;
    
    private static final Map<String, DollPose> poses = new HashMap<>();
    private static final Map<String, DollAction> actions = new HashMap<>();
    
    /**
     * 加载所有姿态和动作资源
     * 应该在游戏启动时或资源重载时调用（如执行 /reload 命令）
     */
    public static void loadResources(ResourceManager resourceManager) {
        // 加载所有姿态（从资源包和文件系统）
        Map<String, DollPose> loadedPoses = PoseLoader.loadAllPoses(resourceManager);
        
        poses.clear();
        poses.putAll(loadedPoses);
        
        // 加载所有动作
        Map<String, DollAction> loadedActions = ActionLoader.loadAllActions(resourceManager);
        
        actions.clear();
        actions.putAll(loadedActions);
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
        poses.put(name, pose);
    }
    
    /**
     * 注册自定义动作（供开发者使用）
     * @param name 动作名称
     * @param action 动作对象
     */
    public static void registerAction(String name, DollAction action) {
        actions.put(name, action);
    }
    
    /**
     * 从文件系统重新加载姿态文件（动态读取）
     * 可以在游戏运行时调用此方法来重新加载 poses 目录中的姿态文件
     */
    public static void reloadPosesFromFileSystem() {
        try {
            Path gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
            
            Path posesDir = gameDir.resolve(PlayerDollAddon.POSES_DIR);
            Map<String, DollPose> fileSystemPoses = PoseLoader.loadPosesFromFileSystem(posesDir);
            
            // 更新姿态映射（保留资源包中的姿态，但用文件系统中的姿态覆盖同名姿态）
            for (Map.Entry<String, DollPose> entry : fileSystemPoses.entrySet()) {
                poses.put(entry.getKey(), entry.getValue());
            }
        } catch (Exception e) {
            LOGGER.error("从文件系统重新加载姿态失败", e);
        }
    }
}

