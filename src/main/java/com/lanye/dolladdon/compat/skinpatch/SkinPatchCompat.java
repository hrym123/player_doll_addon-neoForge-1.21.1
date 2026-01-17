package com.lanye.dolladdon.compat.skinpatch;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 皮肤补丁兼容模块核心类
 * 
 * 整合了所有皮肤加载相关的功能，包括：
 * - CustomSkinLoader 兼容性检测和集成
 * - 默认模型信息管理（Steve/Alex）
 * - 皮肤纹理获取和验证
 * - 模型类型检测
 * - 皮肤路径修复
 * - 错误处理和回退机制
 * 
 * 此模块是皮肤加载的统一实现，不依赖 PlayerSkinUtil
 */
public class SkinPatchCompat {
    
    // ==================== 常量定义 ====================
    
    /**
     * Steve 的固定 UUID（用于获取默认皮肤）
     * Steve 固定使用粗手臂模型（wide/default）
     */
    public static final UUID STEVE_UUID = new UUID(0L, 0L);
    
    /**
     * Alex 的固定 UUID（用于获取默认皮肤）
     * Alex 固定使用细手臂模型（slim）
     */
    public static final UUID ALEX_UUID = new UUID(0L, 1L);
    
    // ==================== 默认模型信息 ====================
    
    /**
     * 默认模型信息类
     * 包含模型类型、UUID和皮肤纹理
     */
    public static class DefaultModelInfo {
        private final String modelName;
        private final UUID uuid;
        private final boolean isAlexModel;
        private final ResourceLocation skinTexture;
        
        public DefaultModelInfo(String modelName, UUID uuid, boolean isAlexModel, ResourceLocation skinTexture) {
            this.modelName = modelName;
            this.uuid = uuid;
            this.isAlexModel = isAlexModel;
            this.skinTexture = skinTexture;
        }
        
        public String getModelName() {
            return modelName;
        }
        
        public UUID getUuid() {
            return uuid;
        }
        
        public boolean isAlexModel() {
            return isAlexModel;
        }
        
        public ResourceLocation getSkinTexture() {
            return skinTexture;
        }
    }
    
    // 粗手臂史蒂夫模型信息
    private static DefaultModelInfo steveModelInfo = null;
    
    // 细手臂艾利克斯模型信息
    private static DefaultModelInfo alexModelInfo = null;
    
    /**
     * 初始化默认模型信息
     * 在首次使用时调用
     */
    private static void initializeDefaultModels() {
        if (steveModelInfo == null) {
            // Steve是固定的粗手臂模型
            // Minecraft 1.19+ 中，Steve的默认皮肤路径是 minecraft:textures/entity/player/wide/steve.png
            ResourceLocation steveTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
            
            steveModelInfo = new DefaultModelInfo(
                "Steve",
                STEVE_UUID,
                false, // 固定：粗手臂
                steveTexture
            );
        }
        
        if (alexModelInfo == null) {
            // Alex是固定的细手臂模型
            // Minecraft 1.19+ 中，Alex的默认皮肤路径是 minecraft:textures/entity/player/slim/alex.png
            ResourceLocation alexTexture = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/slim/alex.png");
            
            alexModelInfo = new DefaultModelInfo(
                "Alex",
                ALEX_UUID,
                true, // 固定：细手臂
                alexTexture
            );
        }
    }
    
    /**
     * 获取粗手臂史蒂夫模型信息
     * @return 史蒂夫模型信息
     */
    public static DefaultModelInfo getSteveModel() {
        initializeDefaultModels();
        return steveModelInfo;
    }
    
    /**
     * 获取细手臂艾利克斯模型信息
     * @return 艾利克斯模型信息
     */
    public static DefaultModelInfo getAlexModel() {
        initializeDefaultModels();
        return alexModelInfo;
    }
    
    /**
     * 获取 Steve 的默认皮肤（粗手臂）
     */
    public static ResourceLocation getSteveSkin() {
        return getSteveModel().getSkinTexture();
    }
    
    /**
     * 获取 Alex 的默认皮肤（细手臂）
     */
    public static ResourceLocation getAlexSkin() {
        return getAlexModel().getSkinTexture();
    }
    
    // ==================== CustomSkinLoader 兼容 ====================
    
    // CustomSkinLoader 相关类的缓存
    private static Class<?> customSkinLoaderClass = null;
    private static Method loadProfileMethod = null;
    private static Class<?> userProfileClass = null;
    private static Field skinUrlField = null;
    private static Field modelField = null;
    private static Field skinTextureField = null;
    private static boolean customSkinLoaderChecked = false;
    private static boolean customSkinLoaderAvailable = false;
    
    /**
     * 检查 CustomSkinLoader 是否可用
     * @return true 如果 CustomSkinLoader 可用，false 否则
     */
    public static boolean isCustomSkinLoaderAvailable() {
        if (!customSkinLoaderChecked) {
            checkCustomSkinLoaderAvailability();
        }
        return customSkinLoaderAvailable;
    }
    
    /**
     * 检查 CustomSkinLoader 可用性（内部方法）
     */
    private static void checkCustomSkinLoaderAvailability() {
        if (customSkinLoaderChecked) {
            return;
        }
        
        try {
            // 尝试加载 CustomSkinLoader 的主要类
            customSkinLoaderClass = Class.forName("customskinloader.CustomSkinLoader");
            loadProfileMethod = customSkinLoaderClass.getMethod("loadProfile", GameProfile.class);
            userProfileClass = Class.forName("customskinloader.profile.UserProfile");
            skinUrlField = userProfileClass.getField("skinUrl");
            modelField = userProfileClass.getField("model");
            
            // 尝试获取皮肤纹理字段（如果存在）
            try {
                skinTextureField = userProfileClass.getField("skinTexture");
            } catch (NoSuchFieldException e) {
                // 如果不存在，使用其他方法获取纹理
                skinTextureField = null;
            }
            
            customSkinLoaderAvailable = true;
            customSkinLoaderChecked = true;
        } catch (Exception e) {
            // CustomSkinLoader 不存在或不可用
            customSkinLoaderClass = null;
            customSkinLoaderAvailable = false;
            customSkinLoaderChecked = true;
        }
    }
    
    /**
     * 尝试通过 CustomSkinLoader 触发皮肤加载
     * CustomSkinLoader 通过 Mixin 自动处理皮肤加载，所以我们只需要触发加载即可
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称
     * @return 如果成功触发加载返回 true，否则返回 false
     */
    private static boolean triggerCustomSkinLoaderLoad(UUID playerUUID, String playerName) {
        if (!isCustomSkinLoaderAvailable()) {
            return false;
        }
        
        try {
            // 创建 GameProfile
            GameProfile gameProfile = new GameProfile(playerUUID, playerName != null ? playerName : "Player");
            
            // 调用 CustomSkinLoader.loadProfile() 来触发皮肤加载
            // CustomSkinLoader 会通过 Mixin 自动处理皮肤加载
            Object userProfile = loadProfileMethod.invoke(null, gameProfile);
            
            if (userProfile != null) {
                // 检查是否有皮肤 URL
                Object skinUrlObj = skinUrlField.get(userProfile);
                if (skinUrlObj != null) {
                    String skinUrl = skinUrlObj.toString();
                    if (skinUrl != null && !skinUrl.isEmpty()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
        
        return false;
    }
    
    /**
     * 从 CustomSkinLoader 获取皮肤纹理位置
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称
     * @return 皮肤纹理位置，如果无法获取则返回 null
     */
    @Nullable
    private static ResourceLocation getSkinTextureFromCustomSkinLoader(@Nullable UUID playerUUID, @Nullable String playerName) {
        if (!isCustomSkinLoaderAvailable() || playerUUID == null) {
            return null;
        }
        
        try {
            // 创建 GameProfile
            GameProfile gameProfile = new GameProfile(playerUUID, playerName != null ? playerName : "Player");
            
            // 调用 CustomSkinLoader.loadProfile() 获取用户配置
            Object userProfile = loadProfileMethod.invoke(null, gameProfile);
            if (userProfile == null) {
                return null;
            }
            
            // 尝试从 UserProfile 获取皮肤纹理 ResourceLocation
            if (skinTextureField != null) {
                Object skinTextureObj = skinTextureField.get(userProfile);
                if (skinTextureObj instanceof ResourceLocation) {
                    return (ResourceLocation) skinTextureObj;
                }
            }
            
            // 如果没有直接的纹理字段，CustomSkinLoader 会通过 Mixin 处理
            // 返回 null 表示应该使用 DefaultPlayerSkin.get()（会被 Mixin 拦截）
            return null;
        } catch (Exception e) {
            // 忽略错误，返回 null
            return null;
        }
    }
    
    /**
     * 从 CustomSkinLoader 获取玩家模型类型
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称
     * @return true 表示 Alex 模型（细手臂），false 表示 Steve 模型（粗手臂）
     */
    private static boolean getModelTypeFromCustomSkinLoader(@Nullable UUID playerUUID, @Nullable String playerName) {
        if (!isCustomSkinLoaderAvailable() || playerUUID == null) {
            return false; // 默认 Steve 模型
        }
        
        try {
            GameProfile gameProfile = new GameProfile(playerUUID, playerName != null ? playerName : "Player");
            Object userProfile = loadProfileMethod.invoke(null, gameProfile);
            if (userProfile != null) {
                Object modelObj = modelField.get(userProfile);
                if (modelObj != null) {
                    String model = modelObj.toString();
                    return "slim".equals(model);
                }
            }
        } catch (Exception e) {
            // 忽略错误，返回默认值
        }
        
        return false; // 默认 Steve 模型
    }
    
    // ==================== 皮肤获取核心方法 ====================
    
    /**
     * 获取玩家皮肤纹理位置（增强版，带补丁修复）
     * 
     * 此方法提供以下功能：
     * 1. 优先使用 CustomSkinLoader（如果可用）
     * 2. 确保皮肤纹理与模型类型匹配
     * 3. 提供回退机制，确保始终返回有效的纹理
     * 4. 修复常见的皮肤加载问题
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称
     * @return 皮肤纹理位置（永远不会返回 null）
     */
    @Nullable
    public static ResourceLocation getPatchedSkinLocation(@Nullable UUID playerUUID, @Nullable String playerName) {
        if (playerUUID == null) {
            // UUID为null时，使用Steve默认皮肤
            return getSteveSkin();
        }
        
        try {
            // 首先检查是否是固定的默认模型UUID
            boolean isSteve = playerUUID.equals(STEVE_UUID);
            boolean isAlex = playerUUID.equals(ALEX_UUID);
            
            if (isSteve) {
                initializeDefaultModels();
                return steveModelInfo.getSkinTexture();
            }
            if (isAlex) {
                initializeDefaultModels();
                return alexModelInfo.getSkinTexture();
            }
            
            // 初始化默认模型信息（用于检查其他可能的Steve/Alex UUID）
            initializeDefaultModels();
            if (playerUUID.equals(steveModelInfo.getUuid())) {
                return steveModelInfo.getSkinTexture();
            }
            if (playerUUID.equals(alexModelInfo.getUuid())) {
                return alexModelInfo.getSkinTexture();
            }
            
            // 对于其他UUID，判断玩家模型类型
            boolean isAlexModel = getPatchedModelType(playerUUID, playerName);
            
            // 如果 CustomSkinLoader 存在，触发皮肤加载
            // CustomSkinLoader 会通过 Mixin 自动处理皮肤加载
            boolean hasCustomSkin = false;
            if (isCustomSkinLoaderAvailable()) {
                hasCustomSkin = triggerCustomSkinLoaderLoad(playerUUID, playerName);
            }
            
            // 获取基于UUID的默认皮肤信息
            var defaultSkin = DefaultPlayerSkin.get(playerUUID);
            String skinModel = defaultSkin.model().toString();
            ResourceLocation texture = defaultSkin.texture();
            
            // 检查皮肤模型类型是否与玩家模型类型匹配
            boolean skinIsAlex = "slim".equals(skinModel);
            boolean modelMatches = (isAlexModel && skinIsAlex) || (!isAlexModel && !skinIsAlex);
            
            // 如果 CustomSkinLoader 提供了皮肤，且模型类型匹配，使用自定义皮肤
            // 注意：CustomSkinLoader 的 Mixin 会拦截 DefaultPlayerSkin 的调用并替换为自定义皮肤
            if (hasCustomSkin && modelMatches) {
                return texture;
            }
            
            // 如果模型类型不匹配，或者无法获取到自定义皮肤，根据模型类型使用对应的默认皮肤
            // 这确保了纹理和模型类型总是匹配的
            if (isAlexModel) {
                // 细手臂模型（Alex）：使用艾利克斯的默认皮肤
                if (!skinIsAlex) {
                    // 如果默认皮肤不是Alex类型，强制使用Alex默认皮肤
                    return getAlexSkin();
                }
                // 如果默认皮肤已经是Alex类型，可以使用它（可能是CustomSkinLoader提供的）
                return texture;
            } else {
                // 粗手臂模型（Steve）：使用史蒂夫的默认皮肤
                if (skinIsAlex || !hasCustomSkin) {
                    // 如果默认皮肤是Alex类型但玩家是Steve模型，强制使用Steve默认皮肤
                    // 或者如果没有自定义皮肤，也使用Steve默认皮肤（更安全）
                    return getSteveSkin();
                }
                // 如果默认皮肤已经是Steve类型，且可能有自定义皮肤，可以使用它
                return texture;
            }
        } catch (Exception e) {
            // 如果获取皮肤过程中出现任何错误，使用Steve默认皮肤作为回退
            return getSteveSkin();
        }
    }
    
    /**
     * 获取玩家模型类型（增强版，带补丁修复）
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称
     * @return true 表示 Alex 模型（细手臂），false 表示 Steve 模型（粗手臂）
     */
    public static boolean getPatchedModelType(@Nullable UUID playerUUID, @Nullable String playerName) {
        if (playerUUID == null) {
            // UUID为null时默认返回false（Steve模型），这是正常情况
            return false;
        }
        
        try {
            // 首先检查是否是固定的默认模型UUID
            boolean isSteve = playerUUID.equals(STEVE_UUID);
            boolean isAlex = playerUUID.equals(ALEX_UUID);
            
            if (isSteve) {
                return false;
            }
            if (isAlex) {
                return true;
            }
            
            // 初始化默认模型信息（用于检查其他可能的Steve/Alex UUID）
            initializeDefaultModels();
            if (playerUUID.equals(steveModelInfo.getUuid())) {
                return false;
            }
            if (playerUUID.equals(alexModelInfo.getUuid())) {
                return true;
            }
            
            // 尝试从 CustomSkinLoader 获取模型信息
            if (isCustomSkinLoaderAvailable()) {
                boolean cslModelType = getModelTypeFromCustomSkinLoader(playerUUID, playerName);
                // 如果 CustomSkinLoader 返回了有效结果，使用它
                // 注意：如果 CustomSkinLoader 无法确定，会返回 false（默认 Steve）
                // 这里我们仍然可以回退到默认方法
                if (cslModelType || hasValidCustomSkinLoaderModel(playerUUID, playerName)) {
                    return cslModelType;
                }
            }
            
            // 使用默认方法
            String defaultModel = DefaultPlayerSkin.get(playerUUID).model().toString();
            return "slim".equals(defaultModel);
        } catch (Exception e) {
            // 如果检测过程中出现任何错误，默认返回false（Steve模型）
            return false;
        }
    }
    
    /**
     * 检查 CustomSkinLoader 是否有有效的模型信息
     */
    private static boolean hasValidCustomSkinLoaderModel(UUID playerUUID, String playerName) {
        try {
            GameProfile gameProfile = new GameProfile(playerUUID, playerName != null ? playerName : "Player");
            Object userProfile = loadProfileMethod.invoke(null, gameProfile);
            return userProfile != null && modelField.get(userProfile) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    // ==================== 纹理验证和修复 ====================
    
    /**
     * 确保纹理与模型类型匹配
     * 
     * @param texture 纹理位置
     * @param isAlexModel 是否为Alex模型
     * @return 匹配的纹理位置
     */
    private static ResourceLocation ensureModelTextureMatch(ResourceLocation texture, boolean isAlexModel) {
        // 检查纹理路径是否包含模型类型信息
        String path = texture.getPath();
        
        // 如果路径包含 "slim" 或 "alex"，表示是细手臂纹理
        boolean textureIsAlex = path.contains("slim") || path.contains("alex");
        
        // 如果路径包含 "wide" 或 "steve"，表示是粗手臂纹理
        boolean textureIsSteve = path.contains("wide") || path.contains("steve");
        
        // 如果模型类型与纹理类型不匹配，使用对应的默认皮肤
        if (isAlexModel && textureIsSteve) {
            // 模型是Alex但纹理是Steve，使用Alex默认皮肤
            return getAlexSkin();
        } else if (!isAlexModel && textureIsAlex) {
            // 模型是Steve但纹理是Alex，使用Steve默认皮肤
            return getSteveSkin();
        }
        
        // 如果匹配或无法确定，返回原纹理
        return texture;
    }
    
    /**
     * 验证皮肤纹理是否有效
     * 
     * @param texture 纹理位置
     * @return true 如果纹理有效，false 否则
     */
    public static boolean isValidSkinTexture(@Nullable ResourceLocation texture) {
        if (texture == null) {
            return false;
        }
        
        // 检查纹理路径是否有效
        String path = texture.getPath();
        if (path == null || path.isEmpty()) {
            return false;
        }
        
        // 检查是否是有效的皮肤纹理路径
        // 皮肤纹理路径通常包含 "textures/entity/player" 或 "textures/entity/player/"
        return path.contains("textures/entity/player") || 
               path.contains("textures/entity/player/") ||
               path.startsWith("png/") || // 动态加载的PNG纹理
               path.startsWith("textures/entity/"); // 其他实体纹理
    }
    
    /**
     * 修复皮肤纹理路径
     * 确保路径格式正确，修复常见的路径问题
     * 
     * @param texture 原始纹理位置
     * @return 修复后的纹理位置
     */
    public static ResourceLocation fixSkinTexturePath(ResourceLocation texture) {
        if (texture == null) {
            return getSteveSkin();
        }
        
        String namespace = texture.getNamespace();
        String path = texture.getPath();
        
        // 修复常见的路径问题
        // 1. 确保路径以正确的格式开始
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        // 2. 如果路径不包含 "textures/" 前缀，但看起来像皮肤路径，添加前缀
        if (!path.contains("textures/") && (path.contains("player") || path.contains("skin"))) {
            if (!path.startsWith("textures/")) {
                path = "textures/entity/player/" + path;
            }
        }
        
        // 返回修复后的纹理位置
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
