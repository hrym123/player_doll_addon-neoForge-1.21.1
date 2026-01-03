package com.lanye.dolladdon.util.pose;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.api.action.ActionKeyframe;
import com.lanye.dolladdon.api.action.ActionMode;
import com.lanye.dolladdon.api.action.DollAction;
import com.lanye.dolladdon.api.action.SimpleDollAction;
import com.lanye.dolladdon.api.pose.DollPose;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 动作加载器
 * 从资源文件加载动作定义
 */
public class ActionLoader {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * 从资源文件加载动作
     * 资源文件路径格式：player_doll:actions/{name}.json
     * 
     * JSON格式示例：
     * {
     *   "name": "wave",
     *   "looping": false,
     *   "keyframes": [
     *     {
     *       "tick": 0,
     *       "pose": {
     *         "name": "start",
     *         "rightArm": [-0.6981317, 0, 0]
     *       }
     *     },
     *     {
     *       "tick": 10,
     *       "pose": {
     *         "name": "wave_up",
     *         "rightArm": [-1.5708, 0, 0]
     *       }
     *     }
     *   ]
     * }
     */
    public static DollAction loadAction(ResourceManager resourceManager, String name) {
        Identifier location = new Identifier(
            PlayerDollAddon.MODID, 
            "actions/" + name + ".json"
        );
        
        try {
            Optional<Resource> resourceOpt = resourceManager.getResource(location);
            if (resourceOpt.isEmpty()) {
                return null;
            }
            
            Resource resource = resourceOpt.get();
            try (InputStream inputStream = resource.getInputStream();
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                return parseAction(resourceManager, json);
            }
        } catch (Exception e) {
            ModuleLogger.error(LogModuleConfig.MODULE_ACTION_LOADER,"加载动作文件失败: {}", location, e);
            return null;
        }
    }
    
    /**
     * 从JSON对象解析动作模式
     * 优先读取 "mode" 字段，如果没有则从 "looping" 字段推断
     */
    private static ActionMode parseActionMode(JsonObject json) {
        if (json.has("mode")) {
            String modeStr = json.get("mode").getAsString().toUpperCase();
            try {
                return ActionMode.valueOf(modeStr);
            } catch (IllegalArgumentException e) {
                ModuleLogger.warn(LogModuleConfig.MODULE_ACTION_LOADER, "未知的动作模式: {}，使用默认模式 ONCE", modeStr);
                return ActionMode.ONCE;
            }
        } else if (json.has("looping")) {
            // 向后兼容：从 looping 字段推断模式
            boolean looping = json.get("looping").getAsBoolean();
            return looping ? ActionMode.LOOP : ActionMode.ONCE;
        } else {
            // 默认模式
            return ActionMode.ONCE;
        }
    }
    
    /**
     * 从JSON对象解析动作
     */
    private static DollAction parseAction(ResourceManager resourceManager, JsonObject json) {
        String name = json.has("name") ? json.get("name").getAsString() : "unnamed";
        String displayName = json.has("displayName") ? json.get("displayName").getAsString() : null;
        ActionMode mode = parseActionMode(json);
        
        if (!json.has("keyframes") || !json.get("keyframes").isJsonArray()) {
            ModuleLogger.error(LogModuleConfig.MODULE_ACTION_LOADER,"动作缺少keyframes数组: {}", name);
            return null;
        }
        
        JsonArray keyframesArray = json.get("keyframes").getAsJsonArray();
        ActionKeyframe[] keyframes = new ActionKeyframe[keyframesArray.size()];
        
        for (int i = 0; i < keyframesArray.size(); i++) {
            JsonObject keyframeObj = keyframesArray.get(i).getAsJsonObject();
            
            int tick = keyframeObj.has("tick") ? keyframeObj.get("tick").getAsInt() : 0;
            
            DollPose pose = null;
            if (keyframeObj.has("pose")) {
                JsonElement poseElement = keyframeObj.get("pose");
                if (poseElement.isJsonObject()) {
                    // 内联姿态定义
                    pose = PoseLoader.parsePose(poseElement.getAsJsonObject());
                } else if (poseElement.isJsonPrimitive()) {
                    // 引用其他姿态文件，优先从文件系统加载
                    String poseName = poseElement.getAsString();
                    // 先尝试从文件系统加载
                    try {
                        Path gameDir;
                        try {
                            // 使用 FabricLoader 获取游戏目录
                            gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
                        } catch (Exception e) {
                            gameDir = Paths.get(".").toAbsolutePath().normalize();
                        }
                        Path posesDir = gameDir.resolve(PlayerDollAddon.POSES_DIR);
                        Path poseFile = posesDir.resolve(poseName + ".json");
                        pose = PoseLoader.loadPoseFromFileSystem(poseFile);
                    } catch (Exception e) {
                        // 如果文件系统加载失败，尝试从资源包加载（向后兼容）
                        pose = PoseLoader.loadPose(resourceManager, poseName);
                    }
                }
            }
            
            if (pose == null) {
                pose = com.lanye.dolladdon.api.pose.SimpleDollPose.createDefaultStandingPose();
            }
            
            keyframes[i] = new ActionKeyframe(tick, pose);
        }
        
        return new SimpleDollAction(name, displayName, mode, keyframes);
    }
    
    /**
     * 从文件系统加载动作文件
     * @param actionsDir 动作文件目录路径
     * @return 加载的动作映射
     */
    public static Map<String, DollAction> loadActionsFromFileSystem(Path actionsDir) {
        Map<String, DollAction> actions = new HashMap<>();
        
        if (!Files.exists(actionsDir) || !Files.isDirectory(actionsDir)) {
            ModuleLogger.debug(LogModuleConfig.MODULE_ACTION_LOADER, "动作目录不存在或不是目录: {}", actionsDir);
            return actions;
        }
        
        try (var stream = Files.list(actionsDir)) {
            stream.filter(path -> path.toString().endsWith(".json"))
                .filter(Files::isRegularFile)
                .forEach(actionFile -> {
                    try {
                        String fileName = actionFile.getFileName().toString();
                        String name = fileName.substring(0, fileName.length() - ".json".length());
                        
                        ModuleLogger.debug(LogModuleConfig.MODULE_ACTION_LOADER, "正在加载动作文件: {}", actionFile);
                        
                        try (InputStreamReader reader = new InputStreamReader(
                                Files.newInputStream(actionFile), StandardCharsets.UTF_8)) {
                            JsonObject json = GSON.fromJson(reader, JsonObject.class);
                            // 解析动作时，如果引用了姿态，需要从文件系统加载
                            DollAction action = parseActionFromFileSystem(json, actionsDir.getParent().resolve("poses"));
                            if (action != null) {
                                actions.put(name, action);
                                String displayName = action.getDisplayName() != null ? action.getDisplayName() : name;
                                ModuleLogger.debug(LogModuleConfig.MODULE_ACTION_LOADER, "成功加载动作    : {} (显示名称: {}, 模式: {}, 时长: {} ticks)", 
                                    name, displayName, action.getMode(), action.getDuration());
                            } else {
                                ModuleLogger.warn(LogModuleConfig.MODULE_ACTION_LOADER, "动作文件解析返回null: {}", actionFile);
                            }
                        }
                    } catch (Exception e) {
                        ModuleLogger.error(LogModuleConfig.MODULE_ACTION_LOADER,"从文件系统加载动作文件失败: {}", actionFile, e);
                    }
                });
        } catch (Exception e) {
            ModuleLogger.error(LogModuleConfig.MODULE_ACTION_LOADER,"扫描文件系统动作目录失败: {}", actionsDir, e);
        }
        
        ModuleLogger.debug(LogModuleConfig.MODULE_ACTION_LOADER, "从文件系统加载了 {} 个动作文件", actions.size());
        
        return actions;
    }
    
    /**
     * 从文件系统解析动作（用于处理姿态引用）
     */
    private static DollAction parseActionFromFileSystem(JsonObject json, Path posesDir) {
        try {
            String name = json.has("name") ? json.get("name").getAsString() : "unnamed";
            String displayName = json.has("displayName") ? json.get("displayName").getAsString() : null;
            ActionMode mode = parseActionMode(json);
            
            if (!json.has("keyframes") || !json.get("keyframes").isJsonArray()) {
                ModuleLogger.error(LogModuleConfig.MODULE_ACTION_LOADER,"动作缺少keyframes数组: {}", name);
                return null;
            }
            
            JsonArray keyframesArray = json.get("keyframes").getAsJsonArray();
            ActionKeyframe[] keyframes = new ActionKeyframe[keyframesArray.size()];
            
            for (int i = 0; i < keyframesArray.size(); i++) {
                try {
                    JsonObject keyframeObj = keyframesArray.get(i).getAsJsonObject();
                    
                    int tick = keyframeObj.has("tick") ? keyframeObj.get("tick").getAsInt() : 0;
                    
                    DollPose pose = null;
                    if (keyframeObj.has("pose")) {
                        JsonElement poseElement = keyframeObj.get("pose");
                        if (poseElement.isJsonObject()) {
                            // 内联姿态定义
                            pose = PoseLoader.parsePose(poseElement.getAsJsonObject());
                            if (pose == null) {
                                ModuleLogger.warn(LogModuleConfig.MODULE_ACTION_LOADER, "动作 {} 的第 {} 个关键帧的内联姿态解析失败，使用默认姿态", name, i);
                            }
                        } else if (poseElement.isJsonPrimitive()) {
                            // 引用其他姿态文件，从文件系统加载
                            String poseName = poseElement.getAsString();
                            Path poseFile = posesDir.resolve(poseName + ".json");
                            pose = PoseLoader.loadPoseFromFileSystem(poseFile);
                            if (pose == null) {
                                ModuleLogger.debug(LogModuleConfig.MODULE_ACTION_LOADER, "动作 {} 的第 {} 个关键帧引用的姿态文件不存在: {}，使用默认姿态", name, i, poseFile);
                            }
                        }
                    }
                    
                    if (pose == null) {
                        pose = com.lanye.dolladdon.api.pose.SimpleDollPose.createDefaultStandingPose();
                    }
                    
                    keyframes[i] = new ActionKeyframe(tick, pose);
                } catch (Exception e) {
                    ModuleLogger.error(LogModuleConfig.MODULE_ACTION_LOADER, "解析动作 {} 的第 {} 个关键帧失败", name, i, e);
                    // 使用默认姿态作为后备
                    keyframes[i] = new ActionKeyframe(0, com.lanye.dolladdon.api.pose.SimpleDollPose.createDefaultStandingPose());
                }
            }
            
            return new SimpleDollAction(name, displayName, mode, keyframes);
        } catch (Exception e) {
            ModuleLogger.error(LogModuleConfig.MODULE_ACTION_LOADER, "解析动作文件失败", e);
            return null;
        }
    }
    
    /**
     * 加载所有动作（从 ResourceManager 和文件系统）
     */
    public static Map<String, DollAction> loadAllActions(ResourceManager resourceManager) {
        Map<String, DollAction> actions = new HashMap<>();
        
        // 首先从 ResourceManager 加载（资源包中的动作）
        try {
            var resources = resourceManager.findResources("actions", path -> path.getPath().endsWith(".json"));
            
            for (var entry : resources.entrySet()) {
                Identifier location = entry.getKey();
                String name = location.getPath().substring("actions/".length(), location.getPath().length() - ".json".length());
                
                try {
                    DollAction action = loadAction(resourceManager, name);
                    if (action != null) {
                        actions.put(name, action);
                    }
                } catch (Exception e) {
                    ModuleLogger.error(LogModuleConfig.MODULE_ACTION_LOADER,"加载动作文件失败: {}", location, e);
                }
            }
        } catch (Exception e) {
            ModuleLogger.error(LogModuleConfig.MODULE_ACTION_LOADER,"扫描动作资源失败", e);
        }
        
        // 然后从文件系统加载（文件系统中的动作会覆盖资源包中的同名动作）
        try {
            Path gameDir;
            try {
                // 使用 FabricLoader 获取游戏目录
                gameDir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir();
            } catch (Exception e) {
                gameDir = Paths.get(".").toAbsolutePath().normalize();
            }
            
            Path actionsDir = gameDir.resolve(PlayerDollAddon.ACTIONS_DIR);
            Map<String, DollAction> fileSystemActions = loadActionsFromFileSystem(actionsDir);
            actions.putAll(fileSystemActions); // 文件系统的动作会覆盖资源包中的同名动作
        } catch (Exception e) {
            ModuleLogger.error(LogModuleConfig.MODULE_ACTION_LOADER,"从文件系统加载动作失败", e);
        }
        
        return actions;
    }
}

