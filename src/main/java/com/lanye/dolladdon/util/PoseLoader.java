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
     *   "leftLeg": [0, 0, 0]
     * }
     * 
     * 注意：旋转值使用角度（360度制），会在内部自动转换为弧度
     */
    public static DollPose loadPose(ResourceManager resourceManager, String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
            PlayerDollAddon.MODID, 
            "poses/" + name + ".json"
        );
        
        try {
            Optional<Resource> resourceOpt = resourceManager.getResource(location);
            if (resourceOpt.isEmpty()) {
                LOGGER.warn("找不到姿态资源文件: {}", location);
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
        
        return new SimpleDollPose(name, displayName, head, hat, body, rightArm, leftArm, rightLeg, leftLeg);
    }
    
    /**
     * 解析旋转数组 [x, y, z]
     * JSON中的值使用角度（360度制），会转换为弧度返回
     */
    private static float[] parseRotation(JsonObject json, String key) {
        if (!json.has(key)) {
            return new float[]{0, 0, 0};
        }
        
        JsonElement element = json.get(key);
        if (element.isJsonArray()) {
            var array = element.getAsJsonArray();
            if (array.size() >= 3) {
                // 将角度转换为弧度
                return new float[]{
                    (float) Math.toRadians(array.get(0).getAsFloat()),
                    (float) Math.toRadians(array.get(1).getAsFloat()),
                    (float) Math.toRadians(array.get(2).getAsFloat())
                };
            }
        }
        
        return new float[]{0, 0, 0};
    }
    
    /**
     * 从文件系统加载姿态文件
     * @param posesDir 姿态文件目录路径
     * @return 加载的姿态映射
     */
    public static Map<String, DollPose> loadPosesFromFileSystem(Path posesDir) {
        Map<String, DollPose> poses = new HashMap<>();
        
        if (!Files.exists(posesDir) || !Files.isDirectory(posesDir)) {
            LOGGER.debug("姿态目录不存在或不是目录: {}", posesDir);
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
                                LOGGER.info("从文件系统加载姿态: {} -> {}", name, pose.getName());
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
        LOGGER.info("[WENTI004] loadAllPoses 开始，ResourceManager: {}", resourceManager);
        Map<String, DollPose> poses = new HashMap<>();
        
        // 首先从 ResourceManager 加载（资源包中的姿态）
        try {
            LOGGER.info("[WENTI004] 开始扫描资源包中的姿态文件...");
            var resources = resourceManager.listResources("poses", path -> path.getPath().endsWith(".json"));
            LOGGER.info("[WENTI004] 找到 {} 个姿态资源文件", resources.size());
            
            for (var entry : resources.entrySet()) {
                ResourceLocation location = entry.getKey();
                String name = location.getPath().substring("poses/".length(), location.getPath().length() - ".json".length());
                LOGGER.info("[WENTI004] 处理姿态资源: {} (位置: {})", name, location);
                
                try {
                    Resource resource = entry.getValue();
                    LOGGER.info("[WENTI004] 打开姿态资源: {} -> {}", name, resource);
                    try (InputStream inputStream = resource.open();
                         InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                        
                        JsonObject json = GSON.fromJson(reader, JsonObject.class);
                        LOGGER.info("[WENTI004] 解析姿态JSON: {} -> {}", name, json);
                        DollPose pose = parsePose(json);
                        if (pose != null) {
                            poses.put(name, pose);
                            LOGGER.info("[WENTI004] 从资源包加载姿态成功: {} -> {}", name, pose.getName());
                        } else {
                            LOGGER.warn("[WENTI004] 姿态解析失败: {}", name);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("[WENTI004] 加载姿态文件失败: {}", location, e);
                }
            }
            LOGGER.info("[WENTI004] 从资源包加载了 {} 个姿态", poses.size());
        } catch (Exception e) {
            LOGGER.error("[WENTI004] 扫描姿态资源失败", e);
        }
        
        // 然后从文件系统加载（文件系统中的姿态会覆盖资源包中的同名姿态）
        try {
            LOGGER.info("[WENTI004] 开始从文件系统加载姿态...");
            Path gameDir;
            try {
                Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
                java.lang.reflect.Method gameDirMethod = fmlPathsClass.getMethod("getGamePath");
                gameDir = (Path) gameDirMethod.invoke(null);
            } catch (Exception e) {
                gameDir = Paths.get(".").toAbsolutePath().normalize();
            }
            
            Path posesDir = gameDir.resolve(PlayerDollAddon.POSES_DIR);
            LOGGER.info("[WENTI004] 文件系统姿态目录: {}", posesDir);
            Map<String, DollPose> fileSystemPoses = loadPosesFromFileSystem(posesDir);
            LOGGER.info("[WENTI004] 从文件系统加载了 {} 个姿态: {}", fileSystemPoses.size(), fileSystemPoses.keySet());
            poses.putAll(fileSystemPoses); // 文件系统的姿态会覆盖资源包中的同名姿态
            LOGGER.info("[WENTI004] 合并后总共 {} 个姿态", poses.size());
        } catch (Exception e) {
            LOGGER.error("[WENTI004] 从文件系统加载姿态失败", e);
        }
        
        LOGGER.info("[WENTI004] loadAllPoses 完成，返回 {} 个姿态", poses.size());
        return poses;
    }
}

