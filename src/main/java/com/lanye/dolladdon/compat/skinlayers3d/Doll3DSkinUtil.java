package com.lanye.dolladdon.compat.skinlayers3d;

import com.lanye.dolladdon.util.logging.ModuleLogger;
import com.lanye.dolladdon.util.resource.ExternalTextureLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

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
    // 日志模块名称
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
        private record CacheKey(Identifier skinLocation, boolean thinArms) {

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
            ModuleLogger.debug(LOG_MODULE, "已初始化，可用状态: {}", available);
            return available;
        }
        
        initialized = true;
        
        if (!SkinLayersDetector.IS_3D_SKIN_LAYERS_LOADED) {
            ModuleLogger.warn(LOG_MODULE, "mod未加载，无法初始化");
            return false;
        }
        
        ModuleLogger.info(LOG_MODULE, "开始初始化API反射...");
        
        try {
            // 获取SkinLayersAPI类
            ModuleLogger.debug(LOG_MODULE, "正在加载SkinLayersAPI类...");
            Class<?> apiClass = Class.forName("dev.tr7zw.skinlayers.api.SkinLayersAPI");
            ModuleLogger.debug(LOG_MODULE, "✓ SkinLayersAPI类加载成功");
            
            // 获取getMeshHelper方法
            ModuleLogger.debug(LOG_MODULE, "正在获取getMeshHelper方法...");
            Method getMeshHelperMethod = apiClass.getMethod("getMeshHelper");
            getMeshHelperMethod.setAccessible(true); // 绕过访问控制
            meshHelper = getMeshHelperMethod.invoke(null);
            
            if (meshHelper == null) {
                ModuleLogger.error(LOG_MODULE, "✗ 无法获取MeshHelper实例（返回null）");
                return false;
            }
            ModuleLogger.debug(LOG_MODULE, "✓ MeshHelper实例获取成功: {}", meshHelper.getClass().getName());
            
            // 获取MeshHelper类
            meshHelperClass = meshHelper.getClass();
            
            // 获取create3DMesh方法
            // 尝试8参数版本（兼容旧版本）
            ModuleLogger.debug(LOG_MODULE, "正在获取create3DMesh方法...");
            try {
                create3DMeshMethod = meshHelperClass.getMethod("create3DMesh",
                        NativeImage.class, int.class, int.class, int.class,
                        int.class, int.class, boolean.class, float.class);
                // 绕过访问控制，允许调用私有内部类的方法
                create3DMeshMethod.setAccessible(true);
                ModuleLogger.debug(LOG_MODULE, "✓ 获取到8参数版本的create3DMesh方法");
            } catch (NoSuchMethodException e) {
                // 尝试9参数版本（新版本）
                ModuleLogger.debug(LOG_MODULE, "8参数版本不存在，尝试9参数版本...");
                create3DMeshMethod = meshHelperClass.getMethod("create3DMesh",
                        NativeImage.class, int.class, int.class, int.class,
                        int.class, int.class, boolean.class, float.class, boolean.class);
                // 绕过访问控制，允许调用私有内部类的方法
                create3DMeshMethod.setAccessible(true);
                ModuleLogger.debug(LOG_MODULE, "✓ 获取到9参数版本的create3DMesh方法");
            }
            
            // 获取Mesh接口类
            ModuleLogger.debug(LOG_MODULE, "正在加载Mesh接口类...");
            meshClass = Class.forName("dev.tr7zw.skinlayers.api.Mesh");
            ModuleLogger.debug(LOG_MODULE, "✓ Mesh接口类加载成功");

            // 推迟API可用性测试到第一次实际使用时进行
            // 避免在mod初始化阶段测试，此时3D皮肤层mod的配置可能还未完全初始化
            // 这解决了 ModBase.config 为 null 的问题
            ModuleLogger.debug(LOG_MODULE, "跳过初始化阶段的API测试，将在首次使用时进行完整测试");
            ModuleLogger.info(LOG_MODULE, "✓ 基础API反射初始化成功，将启用3D皮肤层功能");
            available = true;
            
            // 获取OffsetProvider类（注意：这是一个接口，不是具体的类）
            ModuleLogger.debug(LOG_MODULE, "正在加载OffsetProvider接口...");
            try {
                offsetProviderClass = Class.forName("dev.tr7zw.skinlayers.api.OffsetProvider");
                ModuleLogger.debug(LOG_MODULE, "✓ OffsetProvider接口加载成功");
            } catch (ClassNotFoundException e) {
                ModuleLogger.warn(LOG_MODULE, "⚠ OffsetProvider接口不存在，某些高级功能将被禁用");
                ModuleLogger.warn(LOG_MODULE, "  这不会影响基本的3D网格创建，但可能影响位置偏移功能");
                offsetProviderClass = null; // 标记为不可用
            }

            available = true;
            ModuleLogger.info(LOG_MODULE, "✓ 成功初始化3D皮肤层API反射（OffsetProvider: {}）",
                offsetProviderClass != null ? "可用" : "不可用");

            // 检查是否可以为3D Skin Layers提供外部文件支持
            try {
                boolean externalSupportEnabled = checkExternalFileSupport();
                if (externalSupportEnabled) {
                    ModuleLogger.info(LOG_MODULE, "✓ 外部PNG文件支持已启用，3D皮肤层可以访问外部纹理");
                } else {
                    ModuleLogger.warn(LOG_MODULE, "⚠ 外部PNG文件支持未完全启用，某些功能可能受限");
                }
            } catch (Exception e) {
                ModuleLogger.warn(LOG_MODULE, "⚠ 检查外部文件支持时出错: {}", e.getMessage());
            }

            return true;
            
        } catch (ClassNotFoundException e) {
            ModuleLogger.error(LOG_MODULE, "✗ 类未找到: {} - 请检查3D皮肤层mod版本是否正确", e.getMessage());
            ModuleLogger.error(LOG_MODULE, "   期望的API包结构: dev.tr7zw.skinlayers.api.*");
            ModuleLogger.error(LOG_MODULE, "   可能的原因: 1) mod版本不匹配 2) API已更改 3) mod未正确加载");
            ModuleLogger.error(LOG_MODULE, "   建议的兼容版本: skinlayers3d-fabric-1.6.x (for MC 1.20.1)");
            ModuleLogger.error(LOG_MODULE, "   当前支持的方法签名: 8参数和9参数版本");
            ModuleLogger.error(LOG_MODULE, "   如果问题持续，请检查mod版本或报告给开发者");
            available = false;
            return false;
        } catch (NoSuchMethodException e) {
            ModuleLogger.error(LOG_MODULE, "✗ 方法未找到: {}", e.getMessage());
            available = false;
            return false;
        } catch (Exception e) {
            ModuleLogger.error(LOG_MODULE, "✗ 初始化3D皮肤层API反射失败", e);
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
                ModuleLogger.debug(LOG_MODULE, "3D皮肤层mod未加载");
                hasLoggedIsAvailable = true;
            }
            return false;
        }

        if (!initialize()) {
            // 只在第一次调用时记录日志
            if (!hasLoggedIsAvailable) {
                ModuleLogger.debug(LOG_MODULE, "3D皮肤层API初始化失败");
                hasLoggedIsAvailable = true;
            }
            return false;
        }

        // 只在第一次调用时记录日志
        if (!hasLoggedIsAvailable) {
            ModuleLogger.debug(LOG_MODULE, "已初始化，可用状态: {}", available);
            ModuleLogger.debug(LOG_MODULE, "3D皮肤层API状态: {}", available ? "可用" : "不可用");
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
            ModuleLogger.debug(LOG_MODULE, "API不可用，无法创建3D网格");
            return null;
        }

        try {
            ModuleLogger.debug(LOG_MODULE, "正在创建3D网格: {}x{}x{}, UV({},{}), topPivot={}, rotationOffset={}",
                    width, height, depth, textureU, textureV, topPivot, rotationOffset);

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
                ModuleLogger.warn(LOG_MODULE, "create3DMesh返回null");
            } else {
                ModuleLogger.debug(LOG_MODULE, "✓ 3D网格创建成功: {}", mesh.getClass().getName());
            }
            return mesh;
        } catch (Exception e) {
            ModuleLogger.error(LOG_MODULE, "✗ 创建3D网格失败", e);

            // 检查是否是配置相关的问题，如果是则标记API不可用
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("ModBase.config") && errorMessage.contains("null")) {
                ModuleLogger.error(LOG_MODULE, "检测到3D皮肤层配置问题，自动禁用API可用性");
                available = false; // 标记API不可用，避免后续尝试
            }

            return null;
        }
    }
    
    /**
     * 从资源位置加载皮肤纹理
     * 优先从外部PNG文件加载，如果失败则尝试从资源包加载
     */
    private static NativeImage loadSkinTexture(Identifier skinLocation) {
        ModuleLogger.debug(LOG_MODULE, "正在加载皮肤纹理: {}", skinLocation);

        // 首先尝试从外部PNG文件加载（玩偶系统使用的纹理）
        Path externalFilePath = ExternalTextureLoader.getTexturePath(skinLocation);
        if (externalFilePath != null && Files.exists(externalFilePath)) {
            ModuleLogger.debug(LOG_MODULE, "✓ 找到外部PNG文件，正在从文件系统加载: {}", externalFilePath);
            try {
                NativeImage skin = NativeImage.read(Files.newInputStream(externalFilePath));

                int width = skin.getWidth();
                int height = skin.getHeight();
                ModuleLogger.debug(LOG_MODULE, "外部皮肤尺寸: {}x{}", width, height);

                // 检查是否为64x64皮肤（3D皮肤层只支持64x64）
                if (width == 64 && height == 64) {
                    ModuleLogger.debug(LOG_MODULE, "✓ 外部皮肤尺寸符合要求（64x64）");
                    return skin;
                } else {
                    ModuleLogger.warn(LOG_MODULE, "✗ 外部皮肤 {} 不是64x64（实际: {}x{}），无法使用3D渲染",
                            skinLocation, width, height);
                    skin.close();
                    return null;
                }
            } catch (Exception e) {
                ModuleLogger.error(LOG_MODULE, "✗ 从外部文件加载皮肤纹理失败: {} -> {}", skinLocation, externalFilePath, e);
            }
        } else {
            ModuleLogger.debug(LOG_MODULE, "未找到外部PNG文件，将尝试从资源包加载");
        }

        // 如果外部文件加载失败，尝试从资源包加载（兼容标准Minecraft皮肤）
        try {
            Optional<Resource> resource = MinecraftClient.getInstance()
                    .getResourceManager().getResource(skinLocation);

            if (resource.isPresent()) {
                ModuleLogger.debug(LOG_MODULE, "✓ 资源包中找到资源，正在读取...");
                // 在1.20.1中，Resource使用getInputStream()方法
                NativeImage skin = NativeImage.read(resource.get().getInputStream());

                int width = skin.getWidth();
                int height = skin.getHeight();
                ModuleLogger.debug(LOG_MODULE, "资源包皮肤尺寸: {}x{}", width, height);

                // 检查是否为64x64皮肤（3D皮肤层只支持64x64）
                if (width == 64 && height == 64) {
                    ModuleLogger.debug(LOG_MODULE, "✓ 资源包皮肤尺寸符合要求（64x64）");
                    return skin;
                } else {
                    ModuleLogger.warn(LOG_MODULE, "✗ 资源包皮肤 {} 不是64x64（实际: {}x{}），无法使用3D渲染",
                            skinLocation, width, height);
                    skin.close();
                    return null;
                }
            } else {
                ModuleLogger.warn(LOG_MODULE, "✗ 资源包中也未找到资源: {}", skinLocation);
            }
        } catch (Exception e) {
            ModuleLogger.error(LOG_MODULE, "✗ 从资源包加载皮肤纹理失败: {}", skinLocation, e);
        }

        return null;
    }

    /**
     * 检查外部文件支持状态
     * 验证我们的纹理系统是否能为3D Skin Layers提供外部文件支持
     */
    private static boolean checkExternalFileSupport() {
        ModuleLogger.debug(LOG_MODULE, "正在检查外部文件支持状态...");

        try {
            // 检查是否有已加载的外部纹理
            Map<Identifier, Path> loadedTextures = ExternalTextureLoader.getAllLoadedTextures();
            if (loadedTextures.isEmpty()) {
                ModuleLogger.debug(LOG_MODULE, "未检测到已加载的外部纹理文件");
                return false;
            }

            ModuleLogger.debug(LOG_MODULE, "✓ 检测到 {} 个已加载的外部纹理文件", loadedTextures.size());

            // 检查纹理管理器是否可用
            var textureManager = MinecraftClient.getInstance().getTextureManager();
            if (textureManager == null) {
                ModuleLogger.warn(LOG_MODULE, "纹理管理器不可用");
                return false;
            }

            // 验证我们是否能正确加载外部纹理
            for (Map.Entry<Identifier, Path> entry : loadedTextures.entrySet()) {
                Identifier textureId = entry.getKey();
                Path filePath = entry.getValue();

                if (Files.exists(filePath)) {
                    ModuleLogger.debug(LOG_MODULE, "✓ 验证外部纹理可用: {} -> {}", textureId, filePath.getFileName());
                } else {
                    ModuleLogger.warn(LOG_MODULE, "✗ 外部纹理文件不存在: {} -> {}", textureId, filePath);
                }
            }

            ModuleLogger.debug(LOG_MODULE, "✓ 外部文件支持检查完成");
            return true;

        } catch (Exception e) {
            ModuleLogger.error(LOG_MODULE, "✗ 检查外部文件支持时出错", e);
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

        ModuleLogger.debug(LOG_MODULE, "正在为3D皮肤层预加载外部纹理...");

        try {
            var textureManager = MinecraftClient.getInstance().getTextureManager();
            Map<Identifier, Path> loadedTextures = ExternalTextureLoader.getAllLoadedTextures();

            int successCount = 0;
            for (Map.Entry<Identifier, Path> entry : loadedTextures.entrySet()) {
                Identifier textureId = entry.getKey();
                Path filePath = entry.getValue();

                if (Files.exists(filePath)) {
                    // 确保纹理已注册到纹理管理器
                    boolean registered = ExternalTextureLoader.loadTexture(textureId, textureManager);
                    if (registered) {
                        ModuleLogger.debug(LOG_MODULE, "✓ 外部纹理已预注册到3D皮肤层: {}", textureId);
                        successCount++;
                    } else {
                        ModuleLogger.warn(LOG_MODULE, "✗ 外部纹理预注册失败: {}", textureId);
                    }
                } else {
                    ModuleLogger.warn(LOG_MODULE, "✗ 外部纹理文件不存在，跳过预注册: {} -> {}", textureId, filePath);
                }
            }

            ModuleLogger.info(LOG_MODULE, "✓ 外部纹理预加载完成，已为3D皮肤层注册 {} 个纹理", successCount);
            
            // 标记为已预加载
            texturesPreloaded = true;

        } catch (Exception e) {
            ModuleLogger.error(LOG_MODULE, "✗ 预加载外部纹理时出错", e);
        }
    }

    /**
     * 为玩偶设置3D皮肤层
     * 
     * @param skinLocation 皮肤资源位置
     * @param thinArms 是否为细手臂模型
     * @return Doll3DSkinData对象，如果失败返回null
     */
    public static Doll3DSkinData setup3dLayers(Identifier skinLocation, boolean thinArms) {
        // 只在第一次调用时记录日志，避免每帧都输出导致卡顿
        if (!hasLoggedSetup3dLayers) {
            ModuleLogger.info(LOG_MODULE, "开始设置3D皮肤层: {}, thinArms={}", skinLocation, thinArms);
            hasLoggedSetup3dLayers = true;
        }
        
        if (!isAvailable()) {
            // 只在第一次调用时记录日志
            if (!hasLoggedSetup3dLayers) {
                ModuleLogger.warn(LOG_MODULE, "✗ API不可用，无法设置3D皮肤层");
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
                ModuleLogger.debug(LOG_MODULE, "✓ 使用缓存的3D皮肤数据");
            }
            return cached;
        }
        
        // 加载皮肤纹理
        ModuleLogger.debug(LOG_MODULE, "加载皮肤纹理...");
        NativeImage skin = loadSkinTexture(skinLocation);
        if (skin == null) {
            ModuleLogger.warn(LOG_MODULE, "✗ 无法加载皮肤纹理，设置失败");
            return null;
        }
        
        try {
            ModuleLogger.debug(LOG_MODULE, "开始创建3D网格...");
            Doll3DSkinData data = new Doll3DSkinData();
            
            // 创建各个部位的3D网格（参考SkinUtil.setup3dLayers的实现）
            ModuleLogger.debug(LOG_MODULE, "创建左腿网格...");
            Object leftLegMesh = create3DMesh(skin, 4, 12, 4, 0, 48, true, 0f);
            data.setLeftLegMesh(leftLegMesh);
            
            ModuleLogger.debug(LOG_MODULE, "创建右腿网格...");
            Object rightLegMesh = create3DMesh(skin, 4, 12, 4, 0, 32, true, 0f);
            data.setRightLegMesh(rightLegMesh);
            
            // 手臂（根据thinArms选择不同的宽度）
            if (thinArms) {
                ModuleLogger.debug(LOG_MODULE, "创建细手臂网格...");
                // 细手臂：宽度3
                Object leftArmMesh = create3DMesh(skin, 3, 12, 4, 48, 48, true, -2f);
                Object rightArmMesh = create3DMesh(skin, 3, 12, 4, 40, 32, true, -2f);
                data.setLeftArmMesh(leftArmMesh);
                data.setRightArmMesh(rightArmMesh);
            } else {
                ModuleLogger.debug(LOG_MODULE, "创建粗手臂网格...");
                // 粗手臂：宽度4
                Object leftArmMesh = create3DMesh(skin, 4, 12, 4, 48, 48, true, -2f);
                Object rightArmMesh = create3DMesh(skin, 4, 12, 4, 40, 32, true, -2f);
                data.setLeftArmMesh(leftArmMesh);
                data.setRightArmMesh(rightArmMesh);
            }
            
            ModuleLogger.debug(LOG_MODULE, "创建身体网格...");
            Object torsoMesh = create3DMesh(skin, 8, 12, 4, 16, 32, true, 0);
            data.setTorsoMesh(torsoMesh);
            
            ModuleLogger.debug(LOG_MODULE, "创建头部网格...");
            Object headMesh = create3DMesh(skin, 8, 8, 8, 32, 0, false, 0.6f);
            data.setHeadMesh(headMesh);
            
            data.setCurrentSkin(skinLocation);
            data.setThinArms(thinArms);
            
            // 检查数据有效性
            if (data.hasValidData()) {
                ModuleLogger.info(LOG_MODULE, "✓ 3D皮肤层设置成功，有效网格数: {}", 
                        countValidMeshes(data));
            } else {
                ModuleLogger.warn(LOG_MODULE, "✗ 3D皮肤层设置完成但无有效网格");
            }
            
            // 缓存结果
            CACHE.put(cacheKey, data);
            ModuleLogger.debug(LOG_MODULE, "数据已缓存");
            
            return data;
            
        } catch (Exception e) {
            ModuleLogger.error(LOG_MODULE, "✗ 设置3D皮肤层失败: {}", skinLocation, e);
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
            ModuleLogger.debug(LOG_MODULE, "OffsetProvider不可用，跳过: {}", name);
            return null;
        }

        try {
            java.lang.reflect.Field field = offsetProviderClass.getField(name);
            field.setAccessible(true); // 绕过访问控制
            Object result = field.get(null);
            ModuleLogger.debug(LOG_MODULE, "获取OffsetProvider成功: {} = {}", name, result);
            return result;
        } catch (Exception e) {
            ModuleLogger.warn(LOG_MODULE, "获取OffsetProvider失败: {} - {}", name, e.getMessage());
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
