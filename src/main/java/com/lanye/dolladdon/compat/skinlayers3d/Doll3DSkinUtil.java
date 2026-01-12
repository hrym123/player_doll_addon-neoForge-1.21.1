package com.lanye.dolladdon.compat.skinlayers3d;

import com.lanye.dolladdon.util.resource.ExternalTextureLoader;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

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
    // Logger removed - logging handled by Mixin
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
            // Debug logging handled by Mixin
            return available;
        }
        
        initialized = true;
        
        if (!SkinLayersDetector.IS_3D_SKIN_LAYERS_LOADED) {
            // Warning logging handled by Mixin
            return false;
        }
        
        // Info logging handled by Mixin
        
        try {
            // 获取SkinLayersAPI类
            // Debug logging handled by Mixin
            Class<?> apiClass = Class.forName("dev.tr7zw.skinlayers.api.SkinLayersAPI");
            // Debug logging handled by Mixin
            
            // 获取getMeshHelper方法
            // Debug logging handled by Mixin
            Method getMeshHelperMethod = apiClass.getMethod("getMeshHelper");
            getMeshHelperMethod.setAccessible(true); // 绕过访问控制
            meshHelper = getMeshHelperMethod.invoke(null);
            
            if (meshHelper == null) {
                // Error logging handled by Mixin
                return false;
            }
            // Debug logging handled by Mixin
            
            // 获取MeshHelper类
            meshHelperClass = meshHelper.getClass();
            
            // 获取create3DMesh方法
            // 尝试8参数版本（兼容旧版本）
            // Debug logging handled by Mixin
            try {
                create3DMeshMethod = meshHelperClass.getMethod("create3DMesh",
                        NativeImage.class, int.class, int.class, int.class,
                        int.class, int.class, boolean.class, float.class);
                // 绕过访问控制，允许调用私有内部类的方法
                create3DMeshMethod.setAccessible(true);
                // Debug logging handled by Mixin
            } catch (NoSuchMethodException e) {
                // 尝试9参数版本（新版本）
                // Debug logging handled by Mixin
                create3DMeshMethod = meshHelperClass.getMethod("create3DMesh",
                        NativeImage.class, int.class, int.class, int.class,
                        int.class, int.class, boolean.class, float.class, boolean.class);
                // 绕过访问控制，允许调用私有内部类的方法
                create3DMeshMethod.setAccessible(true);
                // Debug logging handled by Mixin
            }
            
            // 获取Mesh接口类
            // Debug logging handled by Mixin
            meshClass = Class.forName("dev.tr7zw.skinlayers.api.Mesh");
            // Debug logging handled by Mixin

            // 推迟API可用性测试到第一次实际使用时进行
            // 避免在mod初始化阶段测试，此时3D皮肤层mod的配置可能还未完全初始化
            // 这解决了 ModBase.config 为 null 的问题
            // Debug logging handled by Mixin
            // Info logging handled by Mixin
            available = true;
            
            // 获取OffsetProvider类（注意：这是一个接口，不是具体的类）
            // Debug logging handled by Mixin
            try {
                offsetProviderClass = Class.forName("dev.tr7zw.skinlayers.api.OffsetProvider");
                // Debug logging handled by Mixin
            } catch (ClassNotFoundException e) {
                // Warning logging handled by Mixin
                // Warning logging handled by Mixin
                offsetProviderClass = null; // 标记为不可用
            }

            available = true;
            // Info logging handled by Mixin

            // 检查是否可以为3D Skin Layers提供外部文件支持
            try {
                boolean externalSupportEnabled = checkExternalFileSupport();
                if (externalSupportEnabled) {
                    // Info logging handled by Mixin
                } else {
                    // Warning logging handled by Mixin
                }
            } catch (Exception e) {
                // Warning logging handled by Mixin
            }

            return true;
            
        } catch (ClassNotFoundException e) {
            // Error logging handled by Mixin
            // Error logging handled by Mixin
            // Error logging handled by Mixin
            // Error logging handled by Mixin
            // Error logging handled by Mixin
            // Error logging handled by Mixin
            available = false;
            return false;
        } catch (NoSuchMethodException e) {
            // Error logging handled by Mixin
            available = false;
            return false;
        } catch (Exception e) {
            // Error logging handled by Mixin
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
                // Debug logging handled by Mixin
                hasLoggedIsAvailable = true;
            }
            return false;
        }

        if (!initialize()) {
            // 只在第一次调用时记录日志
            if (!hasLoggedIsAvailable) {
                // Debug logging handled by Mixin
                hasLoggedIsAvailable = true;
            }
            return false;
        }

        // 只在第一次调用时记录日志
        if (!hasLoggedIsAvailable) {
            // Debug logging handled by Mixin
            // Debug logging handled by Mixin
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
            // Debug logging handled by Mixin
            return null;
        }

        try {
            // Debug logging handled by Mixin

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
                // Warning logging handled by Mixin
            } else {
                // Debug logging handled by Mixin
            }
            return mesh;
        } catch (Exception e) {
            // Error logging handled by Mixin

            // 检查是否是配置相关的问题，如果是则标记API不可用
            String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("ModBase.config") && errorMessage.contains("null")) {
                // Error logging handled by Mixin
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
        // Debug logging handled by Mixin

        // 首先尝试从外部PNG文件加载（玩偶系统使用的纹理）
        Path externalFilePath = ExternalTextureLoader.getTexturePath(skinLocation);
        if (externalFilePath != null && Files.exists(externalFilePath)) {
            // Debug logging handled by Mixin
            try {
                NativeImage skin = NativeImage.read(Files.newInputStream(externalFilePath));

                int width = skin.getWidth();
                int height = skin.getHeight();
                // Debug logging handled by Mixin

                // 检查是否为64x64皮肤（3D皮肤层只支持64x64）
                if (width == 64 && height == 64) {
                    // Debug logging handled by Mixin
                    return skin;
                } else {
                    // Warning logging handled by Mixin
                    skin.close();
                    return null;
                }
            } catch (Exception e) {
                // Error logging handled by Mixin
            }
        } else {
            // Debug logging handled by Mixin
        }

        // 如果外部文件加载失败，尝试从资源包加载（兼容标准Minecraft皮肤）
        try {
            Optional<Resource> resource = Minecraft.getInstance()
                    .getResourceManager().getResource(skinLocation);

            if (resource.isPresent()) {
                // Debug logging handled by Mixin
                NativeImage skin = NativeImage.read(resource.get().open());

                int width = skin.getWidth();
                int height = skin.getHeight();
                // Debug logging handled by Mixin

                // 检查是否为64x64皮肤（3D皮肤层只支持64x64）
                if (width == 64 && height == 64) {
                    // Debug logging handled by Mixin
                    return skin;
                } else {
                    // Warning logging handled by Mixin
                    skin.close();
                    return null;
                }
            } else {
                // Warning logging handled by Mixin
            }
        } catch (Exception e) {
            // Error logging handled by Mixin
        }

        return null;
    }

    /**
     * 检查外部文件支持状态
     * 验证我们的纹理系统是否能为3D Skin Layers提供外部文件支持
     */
    private static boolean checkExternalFileSupport() {
        // Debug logging handled by Mixin

        try {
            // 检查是否有已加载的外部纹理
            Map<ResourceLocation, Path> loadedTextures = ExternalTextureLoader.getAllLoadedTextures();
            if (loadedTextures.isEmpty()) {
                // Debug logging handled by Mixin
                return false;
            }

            // Debug logging handled by Mixin

            // 检查纹理管理器是否可用
            var textureManager = Minecraft.getInstance().getTextureManager();
            if (textureManager == null) {
                // Warning logging handled by Mixin
                return false;
            }

            // 验证我们是否能正确加载外部纹理
            for (Map.Entry<ResourceLocation, Path> entry : loadedTextures.entrySet()) {
                ResourceLocation textureId = entry.getKey();
                Path filePath = entry.getValue();

                if (Files.exists(filePath)) {
                    // Debug logging handled by Mixin
                } else {
                    // Warning logging handled by Mixin
                }
            }

            // Debug logging handled by Mixin
            return true;

        } catch (Exception e) {
            // Error logging handled by Mixin
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

        // Debug logging handled by Mixin

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
                        // Debug logging handled by Mixin
                        successCount++;
                    } else {
                        // Warning logging handled by Mixin
                    }
                } else {
                    // Warning logging handled by Mixin
                }
            }

            // Info logging handled by Mixin
            
            // 标记为已预加载
            texturesPreloaded = true;

        } catch (Exception e) {
            // Error logging handled by Mixin
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
            // Info logging handled by Mixin
            hasLoggedSetup3dLayers = true;
        }
        
        if (!isAvailable()) {
            // 只在第一次调用时记录日志
            if (!hasLoggedSetup3dLayers) {
                // Warning logging handled by Mixin
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
                // Debug logging handled by Mixin
            }
            return cached;
        }
        
        // 加载皮肤纹理
        // Debug logging handled by Mixin
        NativeImage skin = loadSkinTexture(skinLocation);
        if (skin == null) {
            // Warning logging handled by Mixin
            return null;
        }
        
        try {
            // Debug logging handled by Mixin
            Doll3DSkinData data = new Doll3DSkinData();
            
            // 创建各个部位的3D网格（参考SkinUtil.setup3dLayers的实现）
            // Debug logging handled by Mixin
            Object leftLegMesh = create3DMesh(skin, 4, 12, 4, 0, 48, true, 0f);
            data.setLeftLegMesh(leftLegMesh);
            
            // Debug logging handled by Mixin
            Object rightLegMesh = create3DMesh(skin, 4, 12, 4, 0, 32, true, 0f);
            data.setRightLegMesh(rightLegMesh);
            
            // 手臂（根据thinArms选择不同的宽度）
            if (thinArms) {
                // Debug logging handled by Mixin
                // 细手臂：宽度3
                Object leftArmMesh = create3DMesh(skin, 3, 12, 4, 48, 48, true, -2f);
                Object rightArmMesh = create3DMesh(skin, 3, 12, 4, 40, 32, true, -2f);
                data.setLeftArmMesh(leftArmMesh);
                data.setRightArmMesh(rightArmMesh);
            } else {
                // Debug logging handled by Mixin
                // 粗手臂：宽度4
                Object leftArmMesh = create3DMesh(skin, 4, 12, 4, 48, 48, true, -2f);
                Object rightArmMesh = create3DMesh(skin, 4, 12, 4, 40, 32, true, -2f);
                data.setLeftArmMesh(leftArmMesh);
                data.setRightArmMesh(rightArmMesh);
            }
            
            // Debug logging handled by Mixin
            Object torsoMesh = create3DMesh(skin, 8, 12, 4, 16, 32, true, 0);
            data.setTorsoMesh(torsoMesh);
            
            // Debug logging handled by Mixin
            Object headMesh = create3DMesh(skin, 8, 8, 8, 32, 0, false, 0.6f);
            data.setHeadMesh(headMesh);
            
            data.setCurrentSkin(skinLocation);
            data.setThinArms(thinArms);
            
            // 检查数据有效性
            if (data.hasValidData()) {
                // Info logging handled by Mixin
            } else {
                // Warning logging handled by Mixin
            }
            
            // 缓存结果
            CACHE.put(cacheKey, data);
            // Debug logging handled by Mixin
            
            return data;
            
        } catch (Exception e) {
            // Error logging handled by Mixin
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
            // Debug logging handled by Mixin
            return null;
        }

        try {
            java.lang.reflect.Field field = offsetProviderClass.getField(name);
            field.setAccessible(true); // 绕过访问控制
            Object result = field.get(null);
            // Debug logging handled by Mixin
            return result;
        } catch (Exception e) {
            // Warning logging handled by Mixin
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
