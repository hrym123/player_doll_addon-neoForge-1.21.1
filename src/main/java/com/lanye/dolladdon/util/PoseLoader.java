package com.lanye.dolladdon.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.api.pose.DollPose;
import com.lanye.dolladdon.api.pose.SimpleDollPose;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

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
 * 姿态加载器
 * 从资源文件加载姿态定义
 */
public class PoseLoader {
    private static final Logger LOGGER = PlayerDollAddon.LOGGER;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    /**
     * 从资源文件加载姿态
     * 资源文件路径格式：player_doll_addon:poses/{name}.json
     * 
     * JSON格式示例：
     * {
     *   "name": "standing",
     *   "displayName": "站立",
     *   "head": [0, 0, 0],
     *   "hat": [0, 0, 0],
     *   "body": [0, 0, 0],
     *   "rightArm": [-40, 0, 0],
     *   "leftArm": [40, 0, 0],
     *   "rightLeg": [0, 0, 0],
     *   "leftLeg": [0, 0, 0],
     *   "position": [0.0, 0.0, 0.0],  // 可选，默认 [0.0, 0.0, 0.0]
     *   "scale": [1.0, 1.0, 1.0]       // 可选，默认 [1.0, 1.0, 1.0]
     * }
     * 
     * 注意：
     * - 旋转值使用角度（360度制），会在内部自动转换为弧度
     * - position 和 scale 字段是可选的，如果省略则使用默认值
     */
    public static DollPose loadPose(ResourceManager resourceManager, String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
            PlayerDollAddon.MODID, 
            "poses/" + name + ".json"
        );
        
        try {
            Optional<Resource> resourceOpt = resourceManager.getResource(location);
            if (resourceOpt.isEmpty()) {
                return null;
            }
            
            Resource resource = resourceOpt.get();
            try (InputStream inputStream = resource.open();
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                return parsePose(json);
            }
        } catch (Exception e) {
            LOGGER.error("加载姿态文件失败: {}", location, e);
            return null;
        }
    }
    
    /**
     * 从JSON对象解析姿态
     * @param json JSON对象
     * @return 解析后的姿态
     */
    public static DollPose parsePose(JsonObject json) {
        String name = json.has("name") ? json.get("name").getAsString() : "unnamed";
        String displayName = json.has("displayName") ? json.get("displayName").getAsString() : null;
        
        float[] head = parseRotation(json, "head");
        float[] hat = parseRotation(json, "hat");
        float[] body = parseRotation(json, "body");
        float[] rightArm = parseRotation(json, "rightArm");
        float[] leftArm = parseRotation(json, "leftArm");
        float[] rightLeg = parseRotation(json, "rightLeg");
        float[] leftLeg = parseRotation(json, "leftLeg");
        
        float[] position = parseFloatArray(json, "position", new float[]{0.0f, 0.0f, 0.0f});
        float[] scale = parseFloatArray(json, "scale", new float[]{1.0f, 1.0f, 1.0f});
        
        return new SimpleDollPose(name, displayName, head, hat, body, rightArm, leftArm, rightLeg, leftLeg, position, scale);
    }
    
    /**
     * 解析旋转数组 [x, y, z]
     * JSON中的值使用角度（360度制），会转换为弧度返回
     * 注意：角度值需要取反以适应旋转方向（+90度朝前）
     */
    private static float[] parseRotation(JsonObject json, String key) {
        if (!json.has(key)) {
            return new float[]{0, 0, 0};
        }
        
        JsonElement element = json.get(key);
        if (element.isJsonArray()) {
            var array = element.getAsJsonArray();
            if (array.size() >= 3) {
                // 将角度转换为弧度，并取反以适应旋转方向（+90度朝前）
                return new float[]{
                    (float) Math.toRadians(-array.get(0).getAsFloat()),
                    (float) Math.toRadians(-array.get(1).getAsFloat()),
                    (float) Math.toRadians(-array.get(2).getAsFloat())
                };
            }
        }
        
        return new float[]{0, 0, 0};
    }
    
    /**
     * 解析浮点数数组 [x, y, z]
     * 用于解析位置和大小等不需要角度转换的数组
     * 如果字段不存在，返回默认值
     */
    private static float[] parseFloatArray(JsonObject json, String key, float[] defaultValue) {
        if (!json.has(key)) {
            return defaultValue;
        }
        
        JsonElement element = json.get(key);
        if (element.isJsonArray()) {
            var array = element.getAsJsonArray();
            if (array.size() >= 3) {
                return new float[]{
                    array.get(0).getAsFloat(),
                    array.get(1).getAsFloat(),
                    array.get(2).getAsFloat()
                };
            }
        }
        
        return defaultValue;
    }
    
    /**
     * 从文件系统加载单个姿态文件
     * @param poseFile 姿态文件路径
     * @return 加载的姿态，如果失败返回null
     */
    public static DollPose loadPoseFromFileSystem(Path poseFile) {
        if (!Files.exists(poseFile) || !Files.isRegularFile(poseFile)) {
            return null;
        }
        
        try {
            try (InputStreamReader reader = new InputStreamReader(
                    Files.newInputStream(poseFile), StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                return parsePose(json);
            }
        } catch (Exception e) {
            LOGGER.error("从文件系统加载姿态文件失败: {}", poseFile, e);
            return null;
        }
    }
    
    /**
     * 从文件系统加载姿态文件
     * @param posesDir 姿态文件目录路径
     * @return 加载的姿态映射
     */
    public static Map<String, DollPose> loadPosesFromFileSystem(Path posesDir) {
        Map<String, DollPose> poses = new HashMap<>();
        
        if (!Files.exists(posesDir) || !Files.isDirectory(posesDir)) {
            return poses;
        }
        
        try {
            Files.list(posesDir)
                .filter(path -> path.toString().endsWith(".json"))
                .filter(Files::isRegularFile)
                .forEach(poseFile -> {
                    try {
                        String fileName = poseFile.getFileName().toString();
                        String name = fileName.substring(0, fileName.length() - ".json".length());
                        
                        try (InputStreamReader reader = new InputStreamReader(
                                Files.newInputStream(poseFile), StandardCharsets.UTF_8)) {
                            JsonObject json = GSON.fromJson(reader, JsonObject.class);
                            DollPose pose = parsePose(json);
                            if (pose != null) {
                                poses.put(name, pose);
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("从文件系统加载姿态文件失败: {}", poseFile, e);
                    }
                });
        } catch (Exception e) {
            LOGGER.error("扫描文件系统姿态目录失败: {}", posesDir, e);
        }
        
        return poses;
    }
    
    /**
     * 加载所有姿态（从 ResourceManager 和文件系统）
     */
    public static Map<String, DollPose> loadAllPoses(ResourceManager resourceManager) {
        Map<String, DollPose> poses = new HashMap<>();
        
        // 首先从 ResourceManager 加载（资源包中的姿态）
        try {
            var resources = resourceManager.listResources("poses", path -> path.getPath().endsWith(".json"));
            
            for (var entry : resources.entrySet()) {
                ResourceLocation location = entry.getKey();
                String name = location.getPath().substring("poses/".length(), location.getPath().length() - ".json".length());
                
                try {
                    Resource resource = entry.getValue();
                    try (InputStream inputStream = resource.open();
                         InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                        
                        JsonObject json = GSON.fromJson(reader, JsonObject.class);
                        DollPose pose = parsePose(json);
                        if (pose != null) {
                            poses.put(name, pose);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("加载姿态文件失败: {}", location, e);
                }
            }
        } catch (Exception e) {
            LOGGER.error("扫描姿态资源失败", e);
        }
        
        // 然后从文件系统加载（文件系统中的姿态会覆盖资源包中的同名姿态）
        try {
            Path gameDir;
            try {
                Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
                java.lang.reflect.Method gameDirMethod = fmlPathsClass.getMethod("getGamePath");
                gameDir = (Path) gameDirMethod.invoke(null);
            } catch (Exception e) {
                gameDir = Paths.get(".").toAbsolutePath().normalize();
            }
            
            Path posesDir = gameDir.resolve(PlayerDollAddon.POSES_DIR);
            Map<String, DollPose> fileSystemPoses = loadPosesFromFileSystem(posesDir);
            poses.putAll(fileSystemPoses); // 文件系统的姿态会覆盖资源包中的同名姿态
        } catch (Exception e) {
            LOGGER.error("从文件系统加载姿态失败", e);
        }
        
        return poses;
    }
}

