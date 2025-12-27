package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.config.ModConfig;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
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
     * 检查是否启用了测试模式
     * 测试模式下会输出详细的调试日志
     */
    private static boolean isTestMode() {
        return ModConfig.isTestMode();
    }
    
    /**
     * 在测试模式下输出调试日志
     */
    private static void debugLog(String message, Object... args) {
        if (isTestMode()) {
            PlayerDollAddon.LOGGER.debug(message, args);
        }
    }
    
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
                PlayerDollAddon.LOGGER.info("MCCustomSkinLoader 已检测到，将优先使用它来获取玩家皮肤");
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
                        debugLog("MCCustomSkinLoader 已为玩家 {} 加载皮肤: {}", 
                                playerName != null ? playerName : playerUUID, skinUrl);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            PlayerDollAddon.LOGGER.warn("使用 MCCustomSkinLoader 加载皮肤时出错: {}", e.getMessage());
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
            // Steve是固定的粗手臂模型，直接使用STEVE_UUID获取纹理
            // 注意：不依赖DefaultPlayerSkin返回的模型类型，因为Steve固定是粗手臂
            var steveSkin = DefaultPlayerSkin.get(STEVE_UUID);
            steveModelInfo = new DefaultModelInfo(
                "Steve",
                STEVE_UUID,
                false, // 固定：粗手臂
                steveSkin.texture()
            );
            PlayerDollAddon.LOGGER.info("[PlayerSkinUtil] 初始化Steve模型（固定粗手臂）- UUID: {}, 纹理: {}", 
                    STEVE_UUID, steveSkin.texture());
            debugLog("[PlayerSkinUtil] 初始化Steve模型（固定粗手臂）- UUID: {}, 纹理: {}", 
                    STEVE_UUID, steveSkin.texture());
        }
        
        if (alexModelInfo == null) {
            // Alex是固定的细手臂模型，直接使用ALEX_UUID获取纹理
            // 注意：不依赖DefaultPlayerSkin返回的模型类型，因为Alex固定是细手臂
            var alexSkin = DefaultPlayerSkin.get(ALEX_UUID);
            alexModelInfo = new DefaultModelInfo(
                "Alex",
                ALEX_UUID,
                true, // 固定：细手臂
                alexSkin.texture()
            );
            PlayerDollAddon.LOGGER.info("[PlayerSkinUtil] 初始化Alex模型（固定细手臂）- UUID: {}, 纹理: {}", 
                    ALEX_UUID, alexSkin.texture());
            debugLog("[PlayerSkinUtil] 初始化Alex模型（固定细手臂）- UUID: {}, 纹理: {}", 
                    ALEX_UUID, alexSkin.texture());
            
            // 验证纹理是否与Steve相同（这是正常的）
            if (steveModelInfo != null && !steveModelInfo.getSkinTexture().equals(alexSkin.texture())) {
                debugLog("[PlayerSkinUtil] 注意：Alex和Steve的默认皮肤纹理不同 - Steve: {}, Alex: {}", 
                        steveModelInfo.getSkinTexture(), alexSkin.texture());
            } else if (steveModelInfo != null) {
                debugLog("[PlayerSkinUtil] 注意：Alex和Steve的默认皮肤纹理相同（这是正常的）: {}", alexSkin.texture());
            }
        }
    }
    
    /**
     * 查找一个会被识别为Alex模型的UUID
     * 在Minecraft中，模型类型由UUID的哈希值决定：
     * - 如果 hashCode() % 2 == 0，使用粗手臂模型（Steve）
     * - 如果 hashCode() % 2 == 1，使用细手臂模型（Alex）
     */
    private static UUID findAlexUUID() {
        // 首先尝试使用预定义的 ALEX_UUID (0, 1)
        // UUID(0, 1) 的哈希值是 1，应该对应细手臂模型
        var alexSkin = DefaultPlayerSkin.get(ALEX_UUID);
        String modelType = alexSkin.model().toString();
        debugLog("[PlayerSkinUtil] findAlexUUID - 检查ALEX_UUID: {}, 模型类型: {}", ALEX_UUID, modelType);
        
        if ("slim".equals(modelType)) {
            debugLog("[PlayerSkinUtil] findAlexUUID - ALEX_UUID正确，返回: {}", ALEX_UUID);
            return ALEX_UUID;
        }
        
        // 如果预定义的UUID不正确，尝试其他已知的Alex UUID
        // 这些UUID的哈希值应该是奇数
        UUID[] alexUUIDs = {
            UUID.fromString("61699b2e-d327-4a01-9f1e-2960b8f5d530"), // Alex的官方UUID
            UUID.fromString("853c80ef-3c37-49fd-9769-2b76a23cf447"), // 另一个已知的Alex UUID
        };
        
        for (UUID uuid : alexUUIDs) {
            var skin = DefaultPlayerSkin.get(uuid);
            String model = skin.model().toString();
            debugLog("[PlayerSkinUtil] findAlexUUID - 尝试UUID: {}, 模型类型: {}", uuid, model);
            if ("slim".equals(model)) {
                debugLog("[PlayerSkinUtil] findAlexUUID - 找到正确的Alex UUID: {}", uuid);
                return uuid;
            }
        }
        
        // 如果都找不到，尝试生成一个哈希值为奇数的UUID
        // 通过不断尝试，直到找到一个被识别为slim模型的UUID
        for (long i = 1; i < 100; i++) {
            UUID testUUID = new UUID(0L, i);
            var skin = DefaultPlayerSkin.get(testUUID);
            if ("slim".equals(skin.model().toString())) {
                debugLog("[PlayerSkinUtil] findAlexUUID - 通过测试找到Alex UUID: {}", testUUID);
                return testUUID;
            }
        }
        
        // 如果都找不到，返回预定义的UUID（即使可能不是Alex类型）
        debugLog("[PlayerSkinUtil] findAlexUUID - 警告：无法找到正确的Alex UUID，返回预定义的UUID: {}", ALEX_UUID);
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
    public static ResourceLocation getSteveSkin() {
        return getSteveModel().getSkinTexture();
    }
    
    /**
     * 获取 Alex 的默认皮肤（细手臂）
     */
    public static ResourceLocation getAlexSkin() {
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
    public static ResourceLocation getSkinLocation(@Nullable UUID playerUUID, @Nullable String playerName) {
        if (playerUUID == null) {
            // UUID为null是正常情况（某些实体可能没有设置玩家信息），使用debug级别而不是warn
            debugLog("[PlayerSkinUtil] UUID为null，使用Steve默认皮肤");
            return getSteveSkin();
        }
        
        debugLog("[PlayerSkinUtil] 开始获取皮肤 - UUID: {}, 名称: {}", playerUUID, playerName);
        PlayerDollAddon.LOGGER.info("[PlayerSkinUtil] getSkinLocation - 开始获取 - UUID: {}, STEVE_UUID: {}, ALEX_UUID: {}", 
                playerUUID, STEVE_UUID, ALEX_UUID);
        
        try {
            // 首先检查是否是固定的默认模型UUID
            // Steve和Alex是固定的模型，直接通过UUID判断，不需要通过DefaultPlayerSkin
            boolean isSteve = playerUUID.equals(STEVE_UUID);
            boolean isAlex = playerUUID.equals(ALEX_UUID);
            PlayerDollAddon.LOGGER.info("[PlayerSkinUtil] getSkinLocation - UUID比较结果 - 是Steve: {}, 是Alex: {}", isSteve, isAlex);
            
            if (isSteve) {
                initializeDefaultModels();
                PlayerDollAddon.LOGGER.info("[PlayerSkinUtil] 使用固定Steve模型（粗手臂）- UUID: {}, 纹理: {}", 
                        STEVE_UUID, steveModelInfo.getSkinTexture());
                return steveModelInfo.getSkinTexture();
            }
            if (isAlex) {
                initializeDefaultModels();
                PlayerDollAddon.LOGGER.info("[PlayerSkinUtil] 使用固定Alex模型（细手臂）- UUID: {}, 纹理: {}", 
                        ALEX_UUID, alexModelInfo.getSkinTexture());
                return alexModelInfo.getSkinTexture();
            }
            
            // 初始化默认模型信息（用于检查其他可能的Steve/Alex UUID）
            initializeDefaultModels();
            if (playerUUID.equals(steveModelInfo.getUuid())) {
                debugLog("[PlayerSkinUtil] 检测到Steve默认模型UUID，直接使用Steve默认皮肤: {}", steveModelInfo.getSkinTexture());
                return steveModelInfo.getSkinTexture();
            }
            if (playerUUID.equals(alexModelInfo.getUuid())) {
                debugLog("[PlayerSkinUtil] 检测到Alex默认模型UUID，直接使用Alex默认皮肤: {}", alexModelInfo.getSkinTexture());
                return alexModelInfo.getSkinTexture();
            }
            
            // 对于其他UUID，判断玩家模型类型（无论是否有自定义皮肤，都需要知道模型类型）
            boolean isAlexModel = isAlexModel(playerUUID, playerName);
            debugLog("[PlayerSkinUtil] 模型类型检测结果 - 是否为Alex模型(细手臂): {}", isAlexModel);
            
            // 如果 MCCustomSkinLoader 存在，触发皮肤加载
            // MCCustomSkinLoader 会通过 Mixin 自动处理皮肤加载
            boolean hasCustomSkin = false;
            if (isCustomSkinLoaderAvailable()) {
                debugLog("[PlayerSkinUtil] MCCustomSkinLoader可用，尝试加载自定义皮肤");
                hasCustomSkin = triggerCustomSkinLoaderLoad(playerUUID, playerName);
                debugLog("[PlayerSkinUtil] MCCustomSkinLoader加载结果: {}", hasCustomSkin);
            } else {
                debugLog("[PlayerSkinUtil] MCCustomSkinLoader不可用，使用默认皮肤");
            }
            
            // 获取基于UUID的默认皮肤信息
            var defaultSkin = DefaultPlayerSkin.get(playerUUID);
            String skinModel = defaultSkin.model().toString();
            ResourceLocation texture = defaultSkin.texture();
            
            debugLog("[PlayerSkinUtil] 默认皮肤信息 - 模型类型: {}, 纹理: {}", skinModel, texture);
            
            // 检查皮肤模型类型是否与玩家模型类型匹配
            boolean skinIsAlex = "slim".equals(skinModel);
            boolean modelMatches = (isAlexModel && skinIsAlex) || (!isAlexModel && !skinIsAlex);
            
            debugLog("[PlayerSkinUtil] 模型匹配检查 - 皮肤是否为Alex: {}, 模型是否匹配: {}", skinIsAlex, modelMatches);
            
            // 如果 MCCustomSkinLoader 提供了皮肤，且模型类型匹配，使用自定义皮肤
            // 注意：MCCustomSkinLoader 的 Mixin 会拦截 DefaultPlayerSkin 的调用并替换为自定义皮肤
            if (hasCustomSkin && modelMatches) {
                debugLog("[PlayerSkinUtil] 使用MCCustomSkinLoader提供的自定义皮肤: {}", texture);
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
                    ResourceLocation alexTexture = getAlexSkin();
                    debugLog("[PlayerSkinUtil] 检测到Alex模型但默认皮肤不匹配，强制使用Alex默认皮肤: {}", alexTexture);
                    return alexTexture;
                }
                // 如果默认皮肤已经是Alex类型，可以使用它（可能是MCCustomSkinLoader提供的）
                debugLog("[PlayerSkinUtil] 使用Alex类型皮肤: {}", texture);
                return texture;
            } else {
                // 粗手臂模型（Steve）：使用史蒂夫的默认皮肤
                // 重要：如果无法正确获取到皮肤，或者模型类型不匹配，始终使用Steve默认皮肤
                if (skinIsAlex || !hasCustomSkin) {
                    // 如果默认皮肤是Alex类型但玩家是Steve模型，强制使用Steve默认皮肤
                    // 或者如果没有自定义皮肤，也使用Steve默认皮肤（更安全）
                    ResourceLocation steveTexture = getSteveSkin();
                    if (skinIsAlex) {
                        debugLog("[PlayerSkinUtil] 检测到Steve模型但默认皮肤不匹配（Alex类型），强制使用Steve默认皮肤: {}", steveTexture);
                        PlayerDollAddon.LOGGER.warn("[PlayerSkinUtil] 警告：玩家是Steve模型但获取到Alex皮肤，已强制使用Steve默认皮肤");
                    } else if (!hasCustomSkin) {
                        debugLog("[PlayerSkinUtil] 无法获取自定义皮肤，使用Steve默认皮肤: {}", steveTexture);
                    }
                    return steveTexture;
                }
                // 如果默认皮肤已经是Steve类型，且可能有自定义皮肤，可以使用它
                debugLog("[PlayerSkinUtil] 使用Steve类型皮肤: {}", texture);
                return texture;
            }
        } catch (Exception e) {
            // 如果获取皮肤过程中出现任何错误，使用Steve默认皮肤作为回退
            PlayerDollAddon.LOGGER.error("[PlayerSkinUtil] 获取玩家皮肤时出错，使用Steve默认皮肤作为回退", e);
            debugLog("[PlayerSkinUtil] 发生异常，使用Steve默认皮肤作为回退: {}", e.getMessage());
            return getSteveSkin();
        }
    }
    
    /**
     * 获取玩家皮肤纹理位置（仅使用UUID）
     * 
     * @param playerUUID 玩家UUID
     * @return 皮肤纹理位置
     */
    public static ResourceLocation getSkinLocation(@Nullable UUID playerUUID) {
        return getSkinLocation(playerUUID, null);
    }
    
    /**
     * 检查玩家是否使用Alex模型（细手臂）
     * Steve和Alex是固定的模型，不需要通过UUID来判断模型类型
     * - 如果UUID是STEVE_UUID，固定返回false（粗手臂）
     * - 如果UUID是ALEX_UUID，固定返回true（细手臂）
     * - 其他UUID通过MCCustomSkinLoader或DefaultPlayerSkin来判断
     * 
     * @param playerUUID 玩家UUID
     * @param playerName 玩家名称（可选，用于 MCCustomSkinLoader）
     * @return true表示使用Alex模型，false表示使用Steve模型
     */
    public static boolean isAlexModel(@Nullable UUID playerUUID, @Nullable String playerName) {
        if (playerUUID == null) {
            // UUID为null时默认返回false（Steve模型），这是正常情况
            debugLog("[PlayerSkinUtil] isAlexModel - UUID为null，返回false (Steve模型)");
            return false;
        }
        
        debugLog("[PlayerSkinUtil] isAlexModel - 开始检测模型类型 - UUID: {}, 名称: {}", playerUUID, playerName);
        PlayerDollAddon.LOGGER.info("[PlayerSkinUtil] isAlexModel - 开始检测 - UUID: {}, STEVE_UUID: {}, ALEX_UUID: {}", 
                playerUUID, STEVE_UUID, ALEX_UUID);
        
        try {
            // 首先检查是否是固定的默认模型UUID
            // Steve和Alex是固定的模型，直接通过UUID判断，不需要通过DefaultPlayerSkin
            boolean isSteve = playerUUID.equals(STEVE_UUID);
            boolean isAlex = playerUUID.equals(ALEX_UUID);
            PlayerDollAddon.LOGGER.info("[PlayerSkinUtil] isAlexModel - UUID比较结果 - 是Steve: {}, 是Alex: {}", isSteve, isAlex);
            
            if (isSteve) {
                PlayerDollAddon.LOGGER.info("[PlayerSkinUtil] isAlexModel - 检测到Steve固定UUID，固定返回false (粗手臂)");
                return false;
            }
            if (isAlex) {
                PlayerDollAddon.LOGGER.info("[PlayerSkinUtil] isAlexModel - 检测到Alex固定UUID，固定返回true (细手臂)");
                return true;
            }
            
            // 初始化默认模型信息（用于检查其他可能的Steve/Alex UUID）
            initializeDefaultModels();
            if (playerUUID.equals(steveModelInfo.getUuid())) {
                debugLog("[PlayerSkinUtil] isAlexModel - 检测到Steve默认模型UUID，返回false (粗手臂)");
                return false;
            }
            if (playerUUID.equals(alexModelInfo.getUuid())) {
                debugLog("[PlayerSkinUtil] isAlexModel - 检测到Alex默认模型UUID，返回true (细手臂)");
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
                            debugLog("[PlayerSkinUtil] isAlexModel - MCCustomSkinLoader返回模型类型: {}", model);
                            if ("slim".equals(model)) {
                                debugLog("[PlayerSkinUtil] isAlexModel - 检测结果: Alex模型(细手臂)");
                                return true;
                            } else {
                                debugLog("[PlayerSkinUtil] isAlexModel - 检测结果: Steve模型(粗手臂)");
                                return false;
                            }
                        } else {
                            debugLog("[PlayerSkinUtil] isAlexModel - MCCustomSkinLoader返回的模型对象为null，使用默认方法");
                        }
                    } else {
                        debugLog("[PlayerSkinUtil] isAlexModel - MCCustomSkinLoader返回的UserProfile为null，使用默认方法");
                    }
                } catch (Exception e) {
                    debugLog("[PlayerSkinUtil] isAlexModel - MCCustomSkinLoader检测出错: {}, 使用默认方法", e.getMessage());
                    // 忽略错误，使用默认方法
                }
            } else {
                debugLog("[PlayerSkinUtil] isAlexModel - MCCustomSkinLoader不可用，使用默认方法");
            }
            
            // 使用默认方法
            String defaultModel = DefaultPlayerSkin.get(playerUUID).model().toString();
            boolean isAlexModel = "slim".equals(defaultModel);
            debugLog("[PlayerSkinUtil] isAlexModel - 默认方法检测 - 模型类型: {}, 是否为Alex: {}", defaultModel, isAlexModel);
            
            // 如果检测结果不确定，默认返回false（Steve模型），这样更安全
            if (!isAlexModel) {
                debugLog("[PlayerSkinUtil] isAlexModel - 最终结果: Steve模型(粗手臂)");
            } else {
                debugLog("[PlayerSkinUtil] isAlexModel - 最终结果: Alex模型(细手臂)");
            }
            
            return isAlexModel;
        } catch (Exception e) {
            // 如果检测过程中出现任何错误，默认返回false（Steve模型）
            PlayerDollAddon.LOGGER.warn("[PlayerSkinUtil] isAlexModel - 检测模型类型时出错，默认返回Steve模型: {}", e.getMessage());
            debugLog("[PlayerSkinUtil] isAlexModel - 发生异常，默认返回false (Steve模型): {}", e.getMessage());
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

