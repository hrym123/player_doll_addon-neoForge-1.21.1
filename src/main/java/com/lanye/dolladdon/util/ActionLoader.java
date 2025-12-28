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
                    // 引用其他姿态文件
                    String poseName = poseElement.getAsString();
                    pose = PoseLoader.loadPose(resourceManager, poseName);
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
     * 加载所有动作
     */
    public static Map<String, DollAction> loadAllActions(ResourceManager resourceManager) {
        LOGGER.info("[WENTI004] loadAllActions 开始，ResourceManager: {}", resourceManager);
        Map<String, DollAction> actions = new HashMap<>();
        
        try {
            LOGGER.info("[WENTI004] 开始扫描资源包中的动作文件...");
            var resources = resourceManager.listResources("actions", path -> path.getPath().endsWith(".json"));
            LOGGER.info("[WENTI004] 找到 {} 个动作资源文件", resources.size());
            
            for (var entry : resources.entrySet()) {
                ResourceLocation location = entry.getKey();
                String name = location.getPath().substring("actions/".length(), location.getPath().length() - ".json".length());
                LOGGER.info("[WENTI004] 处理动作资源: {} (位置: {})", name, location);
                
                try {
                    DollAction action = loadAction(resourceManager, name);
                    if (action != null) {
                        actions.put(name, action);
                        LOGGER.info("[WENTI004] 已加载动作: {} -> {}", name, action.getName());
                    } else {
                        LOGGER.warn("[WENTI004] 动作加载返回null: {}", name);
                    }
                } catch (Exception e) {
                    LOGGER.error("[WENTI004] 加载动作文件失败: {}", location, e);
                }
            }
            LOGGER.info("[WENTI004] 从资源包加载了 {} 个动作", actions.size());
        } catch (Exception e) {
            LOGGER.error("[WENTI004] 扫描动作资源失败", e);
        }
        
        LOGGER.info("[WENTI004] loadAllActions 完成，返回 {} 个动作", actions.size());
        return actions;
    }
}

