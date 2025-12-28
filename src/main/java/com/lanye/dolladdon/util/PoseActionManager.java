package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.api.action.DollAction;
import com.lanye.dolladdon.api.pose.DollPose;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

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
     * 应该在游戏启动时调用
     */
    public static void loadResources(ResourceManager resourceManager) {
        LOGGER.info("开始加载姿态和动作资源...");
        
        // 加载所有姿态
        Map<String, DollPose> loadedPoses = PoseLoader.loadAllPoses(resourceManager);
        poses.clear();
        poses.putAll(loadedPoses);
        LOGGER.info("已加载 {} 个姿态", poses.size());
        
        // 加载所有动作
        Map<String, DollAction> loadedActions = ActionLoader.loadAllActions(resourceManager);
        actions.clear();
        actions.putAll(loadedActions);
        LOGGER.info("已加载 {} 个动作", actions.size());
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
        LOGGER.info("已注册自定义姿态: {}", name);
    }
    
    /**
     * 注册自定义动作（供开发者使用）
     * @param name 动作名称
     * @param action 动作对象
     */
    public static void registerAction(String name, DollAction action) {
        actions.put(name, action);
        LOGGER.info("已注册自定义动作: {}", name);
    }
}

