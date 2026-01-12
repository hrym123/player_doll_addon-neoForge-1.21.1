package com.lanye.dolladdon.compat.skinlayers3d;

import com.lanye.dolladdon.util.resource.ExternalTextureLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩偶3D皮肤工具类
 * 通过反射调用3D皮肤层mod的API来创建3D网格
 */
public class Doll3DSkinUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String LOG_MODULE = "3d_skin_layers";

    // 缓存已创建的3D皮肤数据
    private static final ConcurrentHashMap<CacheKey, Doll3DSkinData> CACHE = new ConcurrentHashMap<>();

    // 反射相关的类和对象（延迟初始化）
    private static Class<?> meshHelperClass;
    private static Class<?> meshClass;
    private static Class<?> offsetProviderClass;
    private static Object meshHelper;
    private static Method create3DMeshMethod;
    private static boolean initialized = false;
    private static boolean available = false;
    
    // 预加载标志位，确保preloadExternalTexturesFor3DSkinLayers只执行一次
    private static boolean texturesPreloaded = false;
    
    // 日志控制标志位，避免频繁输出日志
    private static boolean hasLoggedIsAvailable = false;
    private static boolean hasLoggedSetup3dLayers = false;

    /**
     * 缓存键
     */
    private record CacheKey(ResourceLocation skinLocation, boolean thinArms) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return thinArms == cacheKey.thinArms &&
                    skinLocation.equals(cacheKey.skinLocation);
        }
    }
    
    /**
     * 初始化反射相关对象
     */
    private static boolean initialize() {
        if (initialized) {
            LOGGER.debug("[{}] 已初始化，可用状态: {}", LOG_MODULE, available);
            return available;
        }
        
        initialized = true;
        
        if (!SkinLayersDetector.IS_3D_SKIN_LAYERS_LOADED) {
            LOGGER.warn("[{}] mod未加载，无法初始化", LOG_MODULE);
            return false;
        }
        
        LOGGER.info("[{}] 开始初始化API反射...", LOG_MODULE);
        
        try {
            // 获取SkinLayersAPI类
            LOGGER.debug("[{}] 正在加载SkinLayersAPI类...", LOG_MODULE);
            Class<?> apiClass = Class.forName("dev.tr7zw.skinlayers.api.SkinLayersAPI");
            LOGGER.debug("[{}] ✓ SkinLayersAPI类加载成功", LOG_MODULE);
            
            // 获取getMeshHelper方法
            LOGGER.debug("[{}] 正在获取getMeshHelper方法...", LOG_MODULE);
            Method getMeshHelperMethod = apiClass.getMethod("getMeshHelper");
            getMeshHelperMethod.setAccessible(true); // 绕过访问控制
            meshHelper = getMeshHelperMethod.invoke(null);
            
            if (meshHelper == null) {
                LOGGER.error("[{}] ✗ 无法获取MeshHelper实例（返回null）", LOG_MODULE);
                return false;
            }
            LOGGER.debug("[{}] ✓ MeshHelper实例获取成功: {}", LOG_MODULE, meshHelper.getClass().getName());
            
            // 获取MeshHelper类
            meshHelperClass = meshHelper.getClass();
            
            // 获取create3DMesh方法
            // 尝试8参数版本（兼容旧版本）
            LOGGER.debug("[{}] 正在获取create3DMesh方法...", LOG_MODULE);
            try {
                create3DMeshMethod = meshHelperClass.getMethod("create3DMesh",
                        NativeImage.class, int.class, int.class, int.class,
                        int.class, int.class, boolean.class, float.class);
                // 绕过访问控制，允许调用私有内部类的方法
                create3DMeshMethod.setAccessible(true);
                LOGGER.debug("[{}] ✓ 获取到8参数版本的create3DMesh方法", LOG_MODULE);
            } catch (NoSuchMethodException e) {
                // 尝试9参数版本（新版本）
                LOGGER.debug("[{}] 8参数版本不存在，尝试9参数版本...", LOG_MODULE);
                create3DMeshMethod = meshHelperClass.getMethod("create3DMesh",
                        NativeImage.class, int.class, int.class, int.class,
                        int.class, int.class, boolean.class, float.class, boolean.class);
                // 绕过访问控制，允许调用私有内部类的方法
                create3DMeshMethod.setAccessible(true);
                LOGGER.debug("[{}] ✓ 获取到9参数版本的create3DMesh方法", LOG_MODULE);
            }
            
            // 获取Mesh接口类
            LOGGER.debug("[{}] 正在加载Mesh接口类...", LOG_MODULE);
            meshClass = Class.forName("dev.tr7zw.skinlayers.api.Mesh");
            LOGGER.debug("[{}] ✓ Mesh接口类加载成功", LOG_MODULE);

            // 推迟API可用性测试到第一次实际使用时进行
            // 避免在mod初始化阶段测试，此时3D皮肤层mod的配置可能还未完全初始化
            // 这解决了 ModBase.config 为 null 的问题
            LOGGER.debug("[{}] 跳过初始化阶段的API测试，将在首次使用时进行完整测试", LOG_MODULE);
            LOGGER.info("[{}] ✓ 基础API反射初始化成功，将启用3D皮肤层功能", LOG_MODULE);
            available = true;
            
            // 获取OffsetProvider类（注意：这是一个接口，不是具体的类）
            LOGGER.debug("[{}] 正在加载OffsetProvider接口...", LOG_MODULE);
            try {
                offsetProviderClass = Class.forName("dev.tr7zw.skinlayers.api.OffsetProvider");
                LOGGER.debug("[{}] ✓ OffsetProvider接口加载成功", LOG_MODULE);
            } catch (ClassNotFoundException e) {
                LOGGER.warn("[{}] ⚠ OffsetProvider接口不存在，某些高级功能将被禁用", LOG_MODULE);
                LOGGER.warn("[{}]   这不会影响基本的3D网格创建，但可能影响位置偏移功能", LOG_MODULE);
                offsetProviderClass = null; // 标记为不可用
            }

            available = true;
            LOGGER.info("[{}] ✓ 成功初始化3D皮肤层API反射（OffsetProvider: {}）",
                LOG_MODULE, offsetProviderClass != null ? "可用" : "不可用");

            // 检查是否可以为3D Skin Layers提供外部文件支持
            try {
                boolean externalSupportEnabled = checkExternalFileSupport();
                if (externalSupportEnabled) {
                    LOGGER.info("[{}] ✓ 外部PNG文件支持已启用，3D皮肤层可以访问外部纹理", LOG_MODULE);
                } else {
                    LOGGER.warn("[{}] ⚠ 外部PNG文件支持未完全启用，某些功能可能受限", LOG_MODULE);
                }
            } catch (Exception e) {
                LOGGER.warn("[{}] ⚠ 检查外部文件支持时出错: {}", LOG_MODULE, e.getMessage());
            }

            return true;
            
        } catch (ClassNotFoundException e) {
            LOGGER.error("[{}] ✗ 类未找到: {} - 请检查3D皮肤层mod版本是否正确", LOG_MODULE, e.getMessage());
            LOGGER.error("[{}]   期望的API包结构: dev.tr7zw.skinlayers.api.*", LOG_MODULE);
            LOGGER.error("[{}]   可能的原因: 1) mod版本不匹配 2) API已更改 3) mod未正确加载", LOG_MODULE);
            LOGGER.error("[{}]   建议的兼容版本: skinlayers3d-fabric-1.6.x (for MC 1.20.1)", LOG_MODULE);
            LOGGER.error("[{}]   当前支持的方法签名: 8参数和9参数版本", LOG_MODULE);
            LOGGER.error("[{}]   如果问题持续，请检查mod版本或报告给开发者", LOG_MODULE);
            available = false;
            return false;
        } catch (NoSuchMethodException e) {
            LOGGER.error("[{}] ✗ 方法未找到: {}", LOG_MODULE, e.getMessage());
            available = false;
            return false;
        } catch (Exception e) {
            LOGGER.error("[{}] ✗ 初始化3D皮肤层API反射失败", LOG_MODULE, e);
            available = false;
            return false;
        }
    }
    
    /**
     * 检查3D皮肤层功能是否可用
     */
    public static boolean isAvailable() {
        if (!SkinLayersDetector.IS_3D_SKIN_LAYERS_LOADED) {
            // 只在第一次调用时记录日志，避免每帧都输出导致卡顿
            if (!hasLoggedIsAvailable) {
                LOGGER.debug("[{}] 3D皮肤层mod未加载", LOG_MODULE);
                hasLoggedIsAvailable = true;
            }
            return false;
        }

        if (!initialize()) {
            // 只在第一次调用时记录日志
            if (!hasLoggedIsAvailable) {
                LOGGER.debug("[{}] 3D皮肤层API初始化失败", LOG_MODULE);
                hasLoggedIsAvailable = true;
            }
            return false;
        }

        // 只在第一次调用时记录日志
        if (!hasLoggedIsAvailable) {
            LOGGER.debug("[{}] 已初始化，可用状态: {}", LOG_MODULE, available);
            LOGGER.debug("[{}] 3D皮肤层API状态: {}", LOG_MODULE, available ? "可用" : "不可用");
            hasLoggedIsAvailable = true;
        }
        return available;
    }
    
    /**
     * 从皮肤纹理创建3D网格
     *
     * @param skin 皮肤纹理图像（必须是64x64）
     * @param width 宽度
     * @param height 高度
     * @param depth 深度
     * @param textureU 纹理U坐标
     * @param textureV 纹理V坐标
     * @param topPivot 是否顶部枢轴
     * @param rotationOffset 旋转偏移
     * @return Mesh对象，如果失败返回null
     */
    private static Object create3DMesh(NativeImage skin, int width, int height, int depth,
                                       int textureU, int textureV, boolean topPivot, float rotationOffset) {
        if (!isAvailable()) {
            LOGGER.debug("[{}] API不可用，无法创建3D网格", LOG_MODULE);
            return null;
        }

        try {
            LOGGER.debug("[{}] 正在创建3D网格: {}x{}x{}, UV({},{}), topPivot={}, rotationOffset={}",
                    LOG_MODULE, width, height, depth, textureU, textureV, topPivot, rotationOffset);

            Object mesh;
            if (create3DMeshMethod.getParameterCount() == 8) {
                // 8参数版本
                mesh = create3DMeshMethod.invoke(meshHelper, skin, width, height, depth,
                        textureU, textureV, topPivot, rotationOffset);
            } else {
                // 9参数版本，加上mirror参数（默认为false）
                mesh = create3DMeshMethod.invoke(meshHelper, skin, width, height, depth,
                        textureU, textureV, topPivot, rotationOffset, false);
            }

            if (mesh == null) {
                LOGGER.warn("[{}] create3DMesh返回null", LOG_MODULE);
            } else {
                LOGGER.debug("[{}] ✓ 3D网格创建成功: {}", LOG_MODULE, mesh.getClass().getName());
            }
            return mesh;
        } catch (Exception e) {
            LOGGER.error("[{}] ✗ 创建3D网格失败", LOG_MODULE, e);

            // 检查是否是配置相关的问题，如果是则标记API不可用
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("ModBase.config") && errorMessage.contains("null")) {
                LOGGER.error("[{}] 检测到3D皮肤层配置问题，自动禁用API可用性", LOG_MODULE);
                available = false; // 标记API不可用，避免后续尝试
            }

            return null;
        }
    }
    
    /**
     * 从资源位置加载皮肤纹理
     * 优先从外部PNG文件加载，如果失败则尝试从资源包加载
     */
    private static NativeImage loadSkinTexture(ResourceLocation skinLocation) {
        LOGGER.debug("[{}] 正在加载皮肤纹理: {}", LOG_MODULE, skinLocation);

        // 首先尝试从外部PNG文件加载（玩偶系统使用的纹理）
        Path externalFilePath = ExternalTextureLoader.getTexturePath(skinLocation);
        if (externalFilePath != null && Files.exists(externalFilePath)) {
            LOGGER.debug("[{}] ✓ 找到外部PNG文件，正在从文件系统加载: {}", LOG_MODULE, externalFilePath);
            try {
                NativeImage skin = NativeImage.read(Files.newInputStream(externalFilePath));

                int width = skin.getWidth();
                int height = skin.getHeight();
                LOGGER.debug("[{}] 外部皮肤尺寸: {}x{}", LOG_MODULE, width, height);

                // 检查是否为64x64皮肤（3D皮肤层只支持64x64）
                if (width == 64 && height == 64) {
                    LOGGER.debug("[{}] ✓ 外部皮肤尺寸符合要求（64x64）", LOG_MODULE);
                    return skin;
                } else {
                    LOGGER.warn("[{}] ✗ 外部皮肤 {} 不是64x64（实际: {}x{}），无法使用3D渲染",
                            LOG_MODULE, skinLocation, width, height);
                    skin.close();
                    return null;
                }
            } catch (Exception e) {
                LOGGER.error("[{}] ✗ 从外部文件加载皮肤纹理失败: {} -> {}", LOG_MODULE, skinLocation, externalFilePath, e);
            }
        } else {
            LOGGER.debug("[{}] 未找到外部PNG文件，将尝试从资源包加载", LOG_MODULE);
        }

        // 如果外部文件加载失败，尝试从资源包加载（兼容标准Minecraft皮肤）
        try {
            Optional<Resource> resource = Minecraft.getInstance()
                    .getResourceManager().getResource(skinLocation);

            if (resource.isPresent()) {
                LOGGER.debug("[{}] ✓ 资源包中找到资源，正在读取...", LOG_MODULE);
                NativeImage skin = NativeImage.read(resource.get().open());

                int width = skin.getWidth();
                int height = skin.getHeight();
                LOGGER.debug("[{}] 资源包皮肤尺寸: {}x{}", LOG_MODULE, width, height);

                // 检查是否为64x64皮肤（3D皮肤层只支持64x64）
                if (width == 64 && height == 64) {
                    LOGGER.debug("[{}] ✓ 资源包皮肤尺寸符合要求（64x64）", LOG_MODULE);
                    return skin;
                } else {
                    LOGGER.warn("[{}] ✗ 资源包皮肤 {} 不是64x64（实际: {}x{}），无法使用3D渲染",
                            LOG_MODULE, skinLocation, width, height);
                    skin.close();
                    return null;
                }
            } else {
                LOGGER.warn("[{}] ✗ 资源包中也未找到资源: {}", LOG_MODULE, skinLocation);
            }
        } catch (Exception e) {
            LOGGER.error("[{}] ✗ 从资源包加载皮肤纹理失败: {}", LOG_MODULE, skinLocation, e);
        }

        return null;
    }

    /**
     * 检查外部文件支持状态
     * 验证我们的纹理系统是否能为3D Skin Layers提供外部文件支持
     */
    private static boolean checkExternalFileSupport() {
        LOGGER.debug("[{}] 正在检查外部文件支持状态...", LOG_MODULE);

        try {
            // 检查是否有已加载的外部纹理
            Map<ResourceLocation, Path> loadedTextures = ExternalTextureLoader.getAllLoadedTextures();
            if (loadedTextures.isEmpty()) {
                LOGGER.debug("[{}] 未检测到已加载的外部纹理文件", LOG_MODULE);
                return false;
            }

            LOGGER.debug("[{}] ✓ 检测到 {} 个已加载的外部纹理文件", LOG_MODULE, loadedTextures.size());

            // 检查纹理管理器是否可用
            var textureManager = Minecraft.getInstance().getTextureManager();
            if (textureManager == null) {
                LOGGER.warn("[{}] 纹理管理器不可用", LOG_MODULE);
                return false;
            }

            // 验证我们是否能正确加载外部纹理
            for (Map.Entry<ResourceLocation, Path> entry : loadedTextures.entrySet()) {
                ResourceLocation textureId = entry.getKey();
                Path filePath = entry.getValue();

                if (Files.exists(filePath)) {
                    LOGGER.debug("[{}] ✓ 验证外部纹理可用: {} -> {}", LOG_MODULE, textureId, filePath.getFileName());
                } else {
                    LOGGER.warn("[{}] ✗ 外部纹理文件不存在: {} -> {}", LOG_MODULE, textureId, filePath);
                }
            }

            LOGGER.debug("[{}] ✓ 外部文件支持检查完成", LOG_MODULE);
            return true;

        } catch (Exception e) {
            LOGGER.error("[{}] ✗ 检查外部文件支持时出错", LOG_MODULE, e);
            return false;
        }
    }

    /**
     * 为3D Skin Layers预注册外部纹理
     * 通过将外部PNG文件注册到纹理管理器，使3D Skin Layers能够访问它们
     * 注意：此方法只会执行一次，后续调用会被跳过（避免每帧都执行导致卡顿）
     */
    public static void preloadExternalTexturesFor3DSkinLayers() {
        // 如果已经预加载过，直接返回（避免每帧都执行导致卡顿）
        if (texturesPreloaded) {
            return;
        }

        LOGGER.debug("[{}] 正在为3D皮肤层预加载外部纹理...", LOG_MODULE);

        try {
            var textureManager = Minecraft.getInstance().getTextureManager();
            Map<ResourceLocation, Path> loadedTextures = ExternalTextureLoader.getAllLoadedTextures();

            int successCount = 0;
            for (Map.Entry<ResourceLocation, Path> entry : loadedTextures.entrySet()) {
                ResourceLocation textureId = entry.getKey();
                Path filePath = entry.getValue();

                if (Files.exists(filePath)) {
                    // 确保纹理已注册到纹理管理器
                    boolean registered = ExternalTextureLoader.loadTexture(textureId, textureManager);
                    if (registered) {
                        LOGGER.debug("[{}] ✓ 外部纹理已预注册到3D皮肤层: {}", LOG_MODULE, textureId);
                        successCount++;
                    } else {
                        LOGGER.warn("[{}] ✗ 外部纹理预注册失败: {}", LOG_MODULE, textureId);
                    }
                } else {
                    LOGGER.warn("[{}] ✗ 外部纹理文件不存在，跳过预注册: {} -> {}", LOG_MODULE, textureId, filePath);
                }
            }

            LOGGER.info("[{}] ✓ 外部纹理预加载完成，已为3D皮肤层注册 {} 个纹理", LOG_MODULE, successCount);
            
            // 标记为已预加载
            texturesPreloaded = true;

        } catch (Exception e) {
            LOGGER.error("[{}] ✗ 预加载外部纹理时出错", LOG_MODULE, e);
        }
    }

    /**
     * 为玩偶设置3D皮肤层
     * 
     * @param skinLocation 皮肤资源位置
     * @param thinArms 是否为细手臂模型
     * @return Doll3DSkinData对象，如果失败返回null
     */
    public static Doll3DSkinData setup3dLayers(ResourceLocation skinLocation, boolean thinArms) {
        // 只在第一次调用时记录日志，避免每帧都输出导致卡顿
        if (!hasLoggedSetup3dLayers) {
            LOGGER.info("[{}] 开始设置3D皮肤层: {}, thinArms={}", LOG_MODULE, skinLocation, thinArms);
            hasLoggedSetup3dLayers = true;
        }
        
        if (!isAvailable()) {
            // 只在第一次调用时记录日志
            if (!hasLoggedSetup3dLayers) {
                LOGGER.warn("[{}] ✗ API不可用，无法设置3D皮肤层", LOG_MODULE);
            }
            return null;
        }

        // 为3D Skin Layers预加载外部纹理，确保其能访问外部PNG文件
        preloadExternalTexturesFor3DSkinLayers();

        // 检查缓存
        CacheKey cacheKey = new CacheKey(skinLocation, thinArms);
        Doll3DSkinData cached = CACHE.get(cacheKey);
        if (cached != null && cached.getCurrentSkin() != null && 
            cached.getCurrentSkin().equals(skinLocation) && 
            cached.isThinArms() == thinArms) {
            // 只在第一次调用时记录日志
            if (!hasLoggedSetup3dLayers) {
                LOGGER.debug("[{}] ✓ 使用缓存的3D皮肤数据", LOG_MODULE);
            }
            return cached;
        }
        
        // 加载皮肤纹理
        LOGGER.debug("[{}] 加载皮肤纹理...", LOG_MODULE);
        NativeImage skin = loadSkinTexture(skinLocation);
        if (skin == null) {
            LOGGER.warn("[{}] ✗ 无法加载皮肤纹理，设置失败", LOG_MODULE);
            return null;
        }
        
        try {
            LOGGER.debug("[{}] 开始创建3D网格...", LOG_MODULE);
            Doll3DSkinData data = new Doll3DSkinData();
            
            // 创建各个部位的3D网格（参考SkinUtil.setup3dLayers的实现）
            LOGGER.debug("[{}] 创建左腿网格...", LOG_MODULE);
            Object leftLegMesh = create3DMesh(skin, 4, 12, 4, 0, 48, true, 0f);
            data.setLeftLegMesh(leftLegMesh);
            
            LOGGER.debug("[{}] 创建右腿网格...", LOG_MODULE);
            Object rightLegMesh = create3DMesh(skin, 4, 12, 4, 0, 32, true, 0f);
            data.setRightLegMesh(rightLegMesh);
            
            // 手臂（根据thinArms选择不同的宽度）
            if (thinArms) {
                LOGGER.debug("[{}] 创建细手臂网格...", LOG_MODULE);
                // 细手臂：宽度3
                Object leftArmMesh = create3DMesh(skin, 3, 12, 4, 48, 48, true, -2f);
                Object rightArmMesh = create3DMesh(skin, 3, 12, 4, 40, 32, true, -2f);
                data.setLeftArmMesh(leftArmMesh);
                data.setRightArmMesh(rightArmMesh);
            } else {
                LOGGER.debug("[{}] 创建粗手臂网格...", LOG_MODULE);
                // 粗手臂：宽度4
                Object leftArmMesh = create3DMesh(skin, 4, 12, 4, 48, 48, true, -2f);
                Object rightArmMesh = create3DMesh(skin, 4, 12, 4, 40, 32, true, -2f);
                data.setLeftArmMesh(leftArmMesh);
                data.setRightArmMesh(rightArmMesh);
            }
            
            LOGGER.debug("[{}] 创建身体网格...", LOG_MODULE);
            Object torsoMesh = create3DMesh(skin, 8, 12, 4, 16, 32, true, 0);
            data.setTorsoMesh(torsoMesh);
            
            LOGGER.debug("[{}] 创建头部网格...", LOG_MODULE);
            Object headMesh = create3DMesh(skin, 8, 8, 8, 32, 0, false, 0.6f);
            data.setHeadMesh(headMesh);
            
            data.setCurrentSkin(skinLocation);
            data.setThinArms(thinArms);
            
            // 检查数据有效性
            if (data.hasValidData()) {
                LOGGER.info("[{}] ✓ 3D皮肤层设置成功，有效网格数: {}", 
                        LOG_MODULE, countValidMeshes(data));
            } else {
                LOGGER.warn("[{}] ✗ 3D皮肤层设置完成但无有效网格", LOG_MODULE);
            }
            
            // 缓存结果
            CACHE.put(cacheKey, data);
            LOGGER.debug("[{}] 数据已缓存", LOG_MODULE);
            
            return data;
            
        } catch (Exception e) {
            LOGGER.error("[{}] ✗ 设置3D皮肤层失败: {}", LOG_MODULE, skinLocation, e);
            return null;
        } finally {
            // 关闭NativeImage（如果不再需要）
            // 注意：3D网格可能持有对纹理的引用，所以这里不关闭
            // skin.close();
        }
    }
    
    /**
     * 统计有效网格数量（用于调试）
     */
    private static int countValidMeshes(Doll3DSkinData data) {
        int count = 0;
        if (data.getHeadMesh() != null) count++;
        if (data.getTorsoMesh() != null) count++;
        if (data.getLeftArmMesh() != null) count++;
        if (data.getRightArmMesh() != null) count++;
        if (data.getLeftLegMesh() != null) count++;
        if (data.getRightLegMesh() != null) count++;
        return count;
    }
    
    /**
     * 获取OffsetProvider常量
     *
     * @param name 常量名称（如 "HEAD", "BODY", "LEFT_ARM" 等）
     * @return OffsetProvider对象，如果失败返回null
     */
    public static Object getOffsetProvider(String name) {
        if (!isAvailable() || offsetProviderClass == null) {
            LOGGER.debug("[{}] OffsetProvider不可用，跳过: {}", LOG_MODULE, name);
            return null;
        }

        try {
            java.lang.reflect.Field field = offsetProviderClass.getField(name);
            field.setAccessible(true); // 绕过访问控制
            Object result = field.get(null);
            LOGGER.debug("[{}] 获取OffsetProvider成功: {} = {}", LOG_MODULE, name, result);
            return result;
        } catch (Exception e) {
            LOGGER.warn("[{}] 获取OffsetProvider失败: {} - {}", LOG_MODULE, name, e.getMessage());
            return null;
        }
    }
    
    /**
     * 清除缓存
     */
    public static void clearCache() {
        CACHE.clear();
    }
}
