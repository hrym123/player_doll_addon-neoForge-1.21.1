package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * 玩家皮肤工具类
 * 用于获取玩家的真实皮肤纹理
 * 如果 MCCustomSkinLoader mod 存在，优先使用它来获取皮肤
 */
public class PlayerSkinUtil {
    
    // MCCustomSkinLoader 相关类的缓存
    private static Class<?> customSkinLoaderClass = null;
    private static Method loadProfileMethod = null;
    private static Class<?> userProfileClass = null;
    private static java.lang.reflect.Field skinUrlField = null;
    private static java.lang.reflect.Field modelField = null;
    private static boolean customSkinLoaderChecked = false;
    
    
    /**
     * 检查 MCCustomSkinLoader 是否可用
     */
    private static boolean isCustomSkinLoaderAvailable() {
        if (!customSkinLoaderChecked) {
            try {
                customSkinLoaderClass = Class.forName("customskinloader.CustomSkinLoader");
                loadProfileMethod = customSkinLoaderClass.getMethod("loadProfile", GameProfile.class);
                userProfileClass = Class.forName("customskinloader.profile.UserProfile");
                skinUrlField = userProfileClass.getField("skinUrl");
                modelField = userProfileClass.getField("model");
                customSkinLoaderChecked = true;
            } catch (Exception e) {
                // MCCustomSkinLoader 不存在或不可用
                customSkinLoaderClass = null;
                customSkinLoaderChecked = true;
            }
        }
        return customSkinLoaderClass != null;
    }
    
    /**
     * 尝试通过 MCCustomSkinLoader 触发皮肤加载
     * MCCustomSkinLoader 通过 Mixin 自动处理皮肤加载，所以我们只需要触发加载即可
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
            // MCCustomSkinLoader 会通过 Mixin 自动处理皮肤加载
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
    
    // Steve 和 Alex 的固定 UUID（用于获取默认皮肤）
    // 注意：Steve和Alex是固定的模型，不需要通过UUID来判断模型类型
    // - Steve固定使用粗手臂模型（wide/default）
    // - Alex固定使用细手臂模型（slim）
    // 当UUID是STEVE_UUID时，直接使用粗手臂模型
    // 当UUID是ALEX_UUID时，直接使用细手臂模型
    // Steve: 全0 UUID (0, 0)
    public static final UUID STEVE_UUID = new UUID(0L, 0L);
    // Alex: UUID (0, 1)
    public static final UUID ALEX_UUID = new UUID(0L, 1L);
    
    /**
     * 默认模型信息类
     * 包含模型类型、UUID和皮肤纹理
     */
    public static class DefaultModelInfo {
        private final String modelName;
        private final UUID uuid;
        private final boolean isAlexModel;
        private final Identifier skinTexture;
        
        public DefaultModelInfo(String modelName, UUID uuid, boolean isAlexModel, Identifier skinTexture) {
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
        
        public Identifier getSkinTexture() {
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
            // 直接使用Steve的默认皮肤资源路径
            // Minecraft 1.19+ 中，Steve的默认皮肤路径是 minecraft:textures/entity/player/wide/steve.png
            Identifier steveTexture = new Identifier("minecraft", "textures/entity/player/wide/steve.png");
            
            steveModelInfo = new DefaultModelInfo(
                "Steve",
                STEVE_UUID,
                false, // 固定：粗手臂
                steveTexture
            );
        }
        
        if (alexModelInfo == null) {
            // Alex是固定的细手臂模型
            // 直接使用Alex的默认皮肤资源路径
            // Minecraft 1.19+ 中，Alex的默认皮肤路径是 minecraft:textures/entity/player/slim/alex.png
            Identifier alexTexture = new Identifier("minecraft", "textures/entity/player/slim/alex.png");
            
            alexModelInfo = new DefaultModelInfo(
                "Alex",
                ALEX_UUID,
                true, // 固定：细手臂
                alexTexture
            );
        }
    }
    
    /**
     * 查找一个会被识别为Alex模型的UUID
     * 在Minecraft中，模型类型由UUID的哈希值决定：
     * - 如果 hashCode() % 2 == 0，使用粗手臂模型（Steve）
     * - 如果 hashCode() % 2 == 1，使用细手臂模型（Alex）
     */
    private static UUID findAlexUUID() {
        try {
            // 首先尝试使用预定义的 ALEX_UUID (0, 1)
            // UUID(0, 1) 的哈希值是 1，应该对应细手臂模型
            String modelType = DefaultSkinHelper.getModel(ALEX_UUID).toString();
            
            if ("slim".equals(modelType)) {
                return ALEX_UUID;
            }
            
            // 如果预定义的UUID不正确，尝试其他已知的Alex UUID
            // 这些UUID的哈希值应该是奇数
            UUID[] alexUUIDs = {
                UUID.fromString("61699b2e-d327-4a01-9f1e-2960b8f5d530"), // Alex的官方UUID
                UUID.fromString("853c80ef-3c37-49fd-9769-2b76a23cf447"), // 另一个已知的Alex UUID
            };
            
            for (UUID uuid : alexUUIDs) {
                try {
                    String model = DefaultSkinHelper.getModel(uuid).toString();
                    if ("slim".equals(model)) {
                        return uuid;
                    }
                } catch (Exception e) {
                    // 忽略单个UUID的错误（包括401等HTTP错误），继续尝试下一个
                    continue;
                }
            }
            
            // 如果都找不到，尝试生成一个哈希值为奇数的UUID
            // 通过不断尝试，直到找到一个被识别为slim模型的UUID
            for (long i = 1; i < 100; i++) {
                try {
                    UUID testUUID = new UUID(0L, i);
                    if ("slim".equals(DefaultSkinHelper.getModel(testUUID).toString())) {
                        return testUUID;
                    }
                } catch (Exception e) {
                    // 忽略单个UUID的错误（包括401等HTTP错误），继续尝试下一个
                    continue;
                }
            }
        } catch (MinecraftClientHttpException e) {
            // 如果访问在线服务失败（如401错误），使用预定义的UUID
            PlayerDollAddon.LOGGER.warn("无法访问在线皮肤服务（HTTP错误），使用默认Alex UUID: {}", e.getMessage());
        } catch (Exception e) {
            // 捕获其他所有异常
            PlayerDollAddon.LOGGER.warn("无法访问在线皮肤服务，使用默认Alex UUID: {}", e.getMessage());
        }
        
        // 如果都找不到或发生错误，返回预定义的UUID（即使可能不是Alex类型）
        return ALEX_UUID;
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
    public static Identifier getSteveSkin() {
        return getSteveModel().getSkinTexture();
    }
    
    /**
     * 获取 Alex 的默认皮肤（细手臂）
     */
    public static Identifier getAlexSkin() {
        return getAlexModel().getSkinTexture();
    }
    
    /**
     * 获取玩家皮肤纹理位置
     * 优先尝试使用 MCCustomSkinLoader（如果存在）
     * 确保返回的纹理与模型类型匹配：
     * - 粗手臂（Steve模型）使用粗手臂对应的皮肤纹理
     * - 细手臂（Alex模型）使用细手臂对应的皮肤纹理
     * 
     * 注意：MCCustomSkinLoader 通过 Mixin 自动拦截皮肤加载过程，
     * 但我们需要确保纹理和模型类型匹配，避免纹理和模型不匹配导致的渲染问题
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称（可选，用于 MCCustomSkinLoader）
     * @return 皮肤纹理位置
     */
    public static Identifier getSkinLocation(@Nullable UUID playerUUID, @Nullable String playerName) {
        if (playerUUID == null) {
            // UUID为null是正常情况（某些实体可能没有设置玩家信息），使用Steve默认皮肤
            return getSteveSkin();
        }
        
        try {
            // 首先检查是否是固定的默认模型UUID
            // Steve和Alex是固定的模型，直接通过UUID判断，不需要通过DefaultSkinHelper
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
            
            // 对于其他UUID，判断玩家模型类型（无论是否有自定义皮肤，都需要知道模型类型）
            boolean isAlexModel = isAlexModel(playerUUID, playerName);
            
            // 如果 MCCustomSkinLoader 存在，触发皮肤加载
            // MCCustomSkinLoader 会通过 Mixin 自动处理皮肤加载
            boolean hasCustomSkin = false;
            if (isCustomSkinLoaderAvailable()) {
                hasCustomSkin = triggerCustomSkinLoaderLoad(playerUUID, playerName);
            }
            
            // 获取基于UUID的默认皮肤信息
            String skinModel;
            Identifier texture;
            try {
                skinModel = DefaultSkinHelper.getModel(playerUUID).toString();
                texture = DefaultSkinHelper.getTexture(playerUUID);
            } catch (MinecraftClientHttpException e) {
                // 如果访问在线服务失败（如401错误），使用本地默认值
                PlayerDollAddon.LOGGER.warn("无法访问在线皮肤服务获取玩家 {} 的皮肤（HTTP错误），使用默认皮肤: {}", playerUUID, e.getMessage());
                // 根据模型类型使用对应的默认皮肤
                if (isAlexModel) {
                    return getAlexSkin();
                } else {
                    return getSteveSkin();
                }
            } catch (Exception e) {
                // 捕获其他所有异常
                PlayerDollAddon.LOGGER.warn("无法访问在线皮肤服务获取玩家 {} 的皮肤，使用默认皮肤: {}", playerUUID, e.getMessage());
                // 根据模型类型使用对应的默认皮肤
                if (isAlexModel) {
                    return getAlexSkin();
                } else {
                    return getSteveSkin();
                }
            }
            
            // 检查皮肤模型类型是否与玩家模型类型匹配
            boolean skinIsAlex = "slim".equals(skinModel);
            boolean modelMatches = (isAlexModel && skinIsAlex) || (!isAlexModel && !skinIsAlex);
            
            // 如果 MCCustomSkinLoader 提供了皮肤，且模型类型匹配，使用自定义皮肤
            // 注意：MCCustomSkinLoader 的 Mixin 会拦截 DefaultSkinHelper 的调用并替换为自定义皮肤
            if (hasCustomSkin && modelMatches) {
                return texture;
            }
            
            // 如果模型类型不匹配，或者无法获取到自定义皮肤，根据模型类型使用对应的默认皮肤
            // 这确保了纹理和模型类型总是匹配的
            // 重要：当检测到是Alex模型时，必须使用细手臂对应的皮肤纹理
            if (isAlexModel) {
                // 细手臂模型（Alex）：使用艾利克斯的默认皮肤，确保使用细手臂纹理
                // 即使MCCustomSkinLoader提供了皮肤，如果模型类型不匹配，也要使用Alex对应的皮肤
                if (!skinIsAlex) {
                    // 如果默认皮肤不是Alex类型，强制使用Alex默认皮肤
                    return getAlexSkin();
                }
                // 如果默认皮肤已经是Alex类型，可以使用它（可能是MCCustomSkinLoader提供的）
                return texture;
            } else {
                // 粗手臂模型（Steve）：使用史蒂夫的默认皮肤
                // 重要：如果无法正确获取到皮肤，或者模型类型不匹配，始终使用Steve默认皮肤
                if (skinIsAlex || !hasCustomSkin) {
                    // 如果默认皮肤是Alex类型但玩家是Steve模型，强制使用Steve默认皮肤
                    // 或者如果没有自定义皮肤，也使用Steve默认皮肤（更安全）
                    Identifier steveTexture = getSteveSkin();
                    return steveTexture;
                }
                // 如果默认皮肤已经是Steve类型，且可能有自定义皮肤，可以使用它
                return texture;
            }
        } catch (Exception e) {
            // 如果获取皮肤过程中出现任何错误，使用Steve默认皮肤作为回退
            PlayerDollAddon.LOGGER.error("[PlayerSkinUtil] 获取玩家皮肤时出错，使用Steve默认皮肤作为回退", e);
            return getSteveSkin();
        }
    }
    
    /**
     * 获取玩家皮肤纹理位置（仅使用UUID）
     * 
     * @param playerUUID 玩家UUID
     * @return 皮肤纹理位置
     */
    public static Identifier getSkinLocation(@Nullable UUID playerUUID) {
        return getSkinLocation(playerUUID, null);
    }
    
    /**
     * 检查玩家是否使用Alex模型（细手臂）
     * Steve和Alex是固定的模型，不需要通过UUID来判断模型类型
     * - 如果UUID是STEVE_UUID，固定返回false（粗手臂）
     * - 如果UUID是ALEX_UUID，固定返回true（细手臂）
     * - 其他UUID通过MCCustomSkinLoader或DefaultSkinHelper来判断
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称（可选，用于 MCCustomSkinLoader）
     * @return true表示使用Alex模型，false表示使用Steve模型
     */
    public static boolean isAlexModel(@Nullable UUID playerUUID, @Nullable String playerName) {
        if (playerUUID == null) {
            // UUID为null时默认返回false（Steve模型），这是正常情况
            return false;
        }
        
        try {
            // 首先检查是否是固定的默认模型UUID
            // Steve和Alex是固定的模型，直接通过UUID判断，不需要通过DefaultSkinHelper
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
            
            // 尝试从 MCCustomSkinLoader 获取模型信息
            if (isCustomSkinLoaderAvailable()) {
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
                    // 忽略错误，使用默认方法
                }
            }
            
            // 使用默认方法
            try {
                String defaultModel = DefaultSkinHelper.getModel(playerUUID).toString();
                return "slim".equals(defaultModel);
            } catch (MinecraftClientHttpException e) {
                // 如果访问在线服务失败（如401错误），根据UUID的哈希值判断
                // UUID的hashCode() % 2 == 1 表示Alex模型
                PlayerDollAddon.LOGGER.warn("无法访问在线皮肤服务判断玩家 {} 的模型类型（HTTP错误），使用UUID哈希值判断: {}", playerUUID, e.getMessage());
                return playerUUID.hashCode() % 2 == 1;
            } catch (Exception e) {
                // 捕获其他所有异常
                PlayerDollAddon.LOGGER.warn("无法访问在线皮肤服务判断玩家 {} 的模型类型，使用UUID哈希值判断: {}", playerUUID, e.getMessage());
                return playerUUID.hashCode() % 2 == 1;
            }
        } catch (Exception e) {
            // 如果检测过程中出现任何错误，默认返回false（Steve模型）
            PlayerDollAddon.LOGGER.warn("判断玩家模型类型时发生错误: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查玩家是否使用Alex模型（仅使用UUID）
     * 
     * @param playerUUID 玩家UUID
     * @return true表示使用Alex模型，false表示使用Steve模型
     */
    public static boolean isAlexModel(@Nullable UUID playerUUID) {
        return isAlexModel(playerUUID, null);
    }
}

