package com.lanye.dolladdon.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.api.action.ActionKeyframe;
import com.lanye.dolladdon.api.action.DollAction;
import com.lanye.dolladdon.api.action.SimpleDollAction;
import com.lanye.dolladdon.api.pose.DollPose;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 动作加载器
 * 从资源文件加载动作定义
 */
public class ActionLoader {
    private static final Logger LOGGER = PlayerDollAddon.LOGGER;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * 从资源文件加载动作
     * 资源文件路径格式：player_doll_addon:actions/{name}.json
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
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
            PlayerDollAddon.MODID, 
            "actions/" + name + ".json"
        );
        
        try {
            Optional<Resource> resourceOpt = resourceManager.getResource(location);
            if (resourceOpt.isEmpty()) {
                LOGGER.warn("找不到动作资源文件: {}", location);
                return null;
            }
            
            Resource resource = resourceOpt.get();
            try (InputStream inputStream = resource.open();
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                return parseAction(resourceManager, json);
            }
        } catch (Exception e) {
            LOGGER.error("加载动作文件失败: {}", location, e);
            return null;
        }
    }
    
    /**
     * 从JSON对象解析动作
     */
    private static DollAction parseAction(ResourceManager resourceManager, JsonObject json) {
        String name = json.has("name") ? json.get("name").getAsString() : "unnamed";
        boolean looping = json.has("looping") && json.get("looping").getAsBoolean();
        
        if (!json.has("keyframes") || !json.get("keyframes").isJsonArray()) {
            LOGGER.error("动作缺少keyframes数组: {}", name);
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
                            Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
                            java.lang.reflect.Method gameDirMethod = fmlPathsClass.getMethod("getGamePath");
                            gameDir = (Path) gameDirMethod.invoke(null);
                        } catch (Exception e) {
                            gameDir = java.nio.file.Paths.get(".").toAbsolutePath().normalize();
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
                LOGGER.warn("关键帧 {} 缺少有效的姿态定义，使用默认姿态", i);
                pose = com.lanye.dolladdon.api.pose.SimpleDollPose.createDefaultStandingPose();
            }
            
            keyframes[i] = new ActionKeyframe(tick, pose);
        }
        
        return new SimpleDollAction(name, looping, keyframes);
    }
    
    /**
     * 从文件系统加载动作文件
     * @param actionsDir 动作文件目录路径
     * @return 加载的动作映射
     */
    public static Map<String, DollAction> loadActionsFromFileSystem(Path actionsDir) {
        Map<String, DollAction> actions = new HashMap<>();
        
        if (!Files.exists(actionsDir) || !Files.isDirectory(actionsDir)) {
            return actions;
        }
        
        try {
            Files.list(actionsDir)
                .filter(path -> path.toString().endsWith(".json"))
                .filter(Files::isRegularFile)
                .forEach(actionFile -> {
                    try {
                        String fileName = actionFile.getFileName().toString();
                        String name = fileName.substring(0, fileName.length() - ".json".length());
                        
                        try (InputStreamReader reader = new InputStreamReader(
                                Files.newInputStream(actionFile), StandardCharsets.UTF_8)) {
                            JsonObject json = GSON.fromJson(reader, JsonObject.class);
                            // 解析动作时，如果引用了姿态，需要从文件系统加载
                            DollAction action = parseActionFromFileSystem(json, actionsDir.getParent().resolve("poses"));
                            if (action != null) {
                                actions.put(name, action);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("从文件系统加载动作文件失败: {}", actionFile, e);
                    }
                });
        } catch (Exception e) {
            LOGGER.error("扫描文件系统动作目录失败: {}", actionsDir, e);
        }
        
        return actions;
    }
    
    /**
     * 从文件系统解析动作（用于处理姿态引用）
     */
    private static DollAction parseActionFromFileSystem(JsonObject json, Path posesDir) {
        String name = json.has("name") ? json.get("name").getAsString() : "unnamed";
        boolean looping = json.has("looping") && json.get("looping").getAsBoolean();
        
        if (!json.has("keyframes") || !json.get("keyframes").isJsonArray()) {
            LOGGER.error("动作缺少keyframes数组: {}", name);
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
                    // 引用其他姿态文件，从文件系统加载
                    String poseName = poseElement.getAsString();
                    pose = PoseLoader.loadPoseFromFileSystem(posesDir.resolve(poseName + ".json"));
                }
            }
            
            if (pose == null) {
                LOGGER.warn("关键帧 {} 缺少有效的姿态定义，使用默认姿态", i);
                pose = com.lanye.dolladdon.api.pose.SimpleDollPose.createDefaultStandingPose();
            }
            
            keyframes[i] = new ActionKeyframe(tick, pose);
        }
        
        return new SimpleDollAction(name, looping, keyframes);
    }
    
    /**
     * 加载所有动作（从 ResourceManager 和文件系统）
     */
    public static Map<String, DollAction> loadAllActions(ResourceManager resourceManager) {
        Map<String, DollAction> actions = new HashMap<>();
        
        // 首先从 ResourceManager 加载（资源包中的动作）
        try {
            var resources = resourceManager.listResources("actions", path -> path.getPath().endsWith(".json"));
            
            for (var entry : resources.entrySet()) {
                ResourceLocation location = entry.getKey();
                String name = location.getPath().substring("actions/".length(), location.getPath().length() - ".json".length());
                
                try {
                    DollAction action = loadAction(resourceManager, name);
                    if (action != null) {
                        actions.put(name, action);
                    }
                } catch (Exception e) {
                    LOGGER.error("加载动作文件失败: {}", location, e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("扫描动作资源失败", e);
        }
        
        // 然后从文件系统加载（文件系统中的动作会覆盖资源包中的同名动作）
        try {
            Path gameDir;
            try {
                Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
                java.lang.reflect.Method gameDirMethod = fmlPathsClass.getMethod("getGamePath");
                gameDir = (Path) gameDirMethod.invoke(null);
            } catch (Exception e) {
                gameDir = java.nio.file.Paths.get(".").toAbsolutePath().normalize();
            }
            
            Path actionsDir = gameDir.resolve(PlayerDollAddon.ACTIONS_DIR);
            Map<String, DollAction> fileSystemActions = loadActionsFromFileSystem(actionsDir);
            actions.putAll(fileSystemActions); // 文件系统的动作会覆盖资源包中的同名动作
        } catch (Exception e) {
            LOGGER.error("从文件系统加载动作失败", e);
        }
        
        return actions;
    }
}

