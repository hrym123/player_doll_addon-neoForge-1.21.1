package com.lanye.dolladdon.util.skinlayers3d;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.PlayerDollAddonClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩偶3D皮肤工具类
 * 通过反射调用3D皮肤层mod的API来创建3D网格
 */
public class Doll3DSkinUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(Doll3DSkinUtil.class);
    
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
    
    /**
     * 缓存键
     */
    private static class CacheKey {
        private final Identifier skinLocation;
        private final boolean thinArms;
        
        public CacheKey(Identifier skinLocation, boolean thinArms) {
            this.skinLocation = skinLocation;
            this.thinArms = thinArms;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CacheKey cacheKey = (CacheKey) o;
            return thinArms == cacheKey.thinArms && 
                   skinLocation.equals(cacheKey.skinLocation);
        }
        
        @Override
        public int hashCode() {
            return skinLocation.hashCode() * 31 + (thinArms ? 1 : 0);
        }
    }
    
    /**
     * 初始化反射相关对象
     */
    private static boolean initialize() {
        if (initialized) {
            SkinLayersLogger.debug("已初始化，可用状态: {}", available);
            return available;
        }
        
        initialized = true;
        
        if (!PlayerDollAddonClient.IS_3D_SKIN_LAYERS_LOADED) {
            SkinLayersLogger.warn("mod未加载，无法初始化");
            return false;
        }
        
        SkinLayersLogger.info("开始初始化API反射...");
        
        try {
            // 获取SkinLayersAPI类
            SkinLayersLogger.debug("正在加载SkinLayersAPI类...");
            Class<?> apiClass = Class.forName("dev.tr7zw.skinlayers.api.SkinLayersAPI");
            SkinLayersLogger.debug("✓ SkinLayersAPI类加载成功");
            
            // 获取getMeshHelper方法
            SkinLayersLogger.debug("正在获取getMeshHelper方法...");
            Method getMeshHelperMethod = apiClass.getMethod("getMeshHelper");
            meshHelper = getMeshHelperMethod.invoke(null);
            
            if (meshHelper == null) {
                SkinLayersLogger.error("✗ 无法获取MeshHelper实例（返回null）");
                return false;
            }
            SkinLayersLogger.debug("✓ MeshHelper实例获取成功: {}", meshHelper.getClass().getName());
            
            // 获取MeshHelper类
            meshHelperClass = meshHelper.getClass();
            
            // 获取create3DMesh方法
            // 方法签名：create3DMesh(NativeImage, int, int, int, int, int, boolean, float)
            SkinLayersLogger.debug("正在获取create3DMesh方法...");
            create3DMeshMethod = meshHelperClass.getMethod("create3DMesh",
                    NativeImage.class, int.class, int.class, int.class,
                    int.class, int.class, boolean.class, float.class);
            SkinLayersLogger.debug("✓ create3DMesh方法获取成功");
            
            // 获取Mesh接口类
            SkinLayersLogger.debug("正在加载Mesh接口类...");
            meshClass = Class.forName("dev.tr7zw.skinlayers.api.Mesh");
            SkinLayersLogger.debug("✓ Mesh接口类加载成功");
            
            // 获取OffsetProvider类
            SkinLayersLogger.debug("正在加载OffsetProvider类...");
            offsetProviderClass = Class.forName("dev.tr7zw.skinlayers.api.OffsetProvider");
            SkinLayersLogger.debug("✓ OffsetProvider类加载成功");
            
            available = true;
            SkinLayersLogger.info("✓ 成功初始化3D皮肤层API反射");
            return true;
            
        } catch (ClassNotFoundException e) {
            SkinLayersLogger.error("✗ 类未找到: {}", e.getMessage());
            available = false;
            return false;
        } catch (NoSuchMethodException e) {
            SkinLayersLogger.error("✗ 方法未找到: {}", e.getMessage());
            available = false;
            return false;
        } catch (Exception e) {
            SkinLayersLogger.error("✗ 初始化3D皮肤层API反射失败", e);
            available = false;
            return false;
        }
    }
    
    /**
     * 检查3D皮肤层功能是否可用
     */
    public static boolean isAvailable() {
        if (!PlayerDollAddonClient.IS_3D_SKIN_LAYERS_LOADED) {
            return false;
        }
        return initialize() && available;
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
            SkinLayersLogger.debug("API不可用，无法创建3D网格");
            return null;
        }
        
        try {
            SkinLayersLogger.debug("正在创建3D网格: {}x{}x{}, UV({},{}), topPivot={}, rotationOffset={}",
                    width, height, depth, textureU, textureV, topPivot, rotationOffset);
            Object mesh = create3DMeshMethod.invoke(meshHelper, skin, width, height, depth,
                    textureU, textureV, topPivot, rotationOffset);
            if (mesh == null) {
                SkinLayersLogger.warn("create3DMesh返回null");
            } else {
                SkinLayersLogger.debug("✓ 3D网格创建成功: {}", mesh.getClass().getName());
            }
            return mesh;
        } catch (Exception e) {
            SkinLayersLogger.error("✗ 创建3D网格失败", e);
            return null;
        }
    }
    
    /**
     * 从资源位置加载皮肤纹理
     */
    private static NativeImage loadSkinTexture(Identifier skinLocation) {
        SkinLayersLogger.debug("正在加载皮肤纹理: {}", skinLocation);
        try {
            Optional<Resource> resource = MinecraftClient.getInstance()
                    .getResourceManager().getResource(skinLocation);
            
            if (resource.isPresent()) {
                SkinLayersLogger.debug("✓ 资源找到，正在读取...");
                // 在1.20.1中，Resource使用getInputStream()方法
                NativeImage skin = NativeImage.read(resource.get().getInputStream());
                
                int width = skin.getWidth();
                int height = skin.getHeight();
                SkinLayersLogger.debug("皮肤尺寸: {}x{}", width, height);
                
                // 检查是否为64x64皮肤（3D皮肤层只支持64x64）
                if (width == 64 && height == 64) {
                    SkinLayersLogger.debug("✓ 皮肤尺寸符合要求（64x64）");
                    return skin;
                } else {
                    SkinLayersLogger.warn("✗ 皮肤 {} 不是64x64（实际: {}x{}），无法使用3D渲染", 
                            skinLocation, width, height);
                    skin.close();
                    return null;
                }
            } else {
                SkinLayersLogger.warn("✗ 资源未找到: {}", skinLocation);
            }
        } catch (Exception e) {
            SkinLayersLogger.error("✗ 加载皮肤纹理失败: {}", skinLocation, e);
        }
        
        return null;
    }
    
    /**
     * 为玩偶设置3D皮肤层
     * 
     * @param skinLocation 皮肤资源位置
     * @param thinArms 是否为细手臂模型
     * @return Doll3DSkinData对象，如果失败返回null
     */
    public static Doll3DSkinData setup3dLayers(Identifier skinLocation, boolean thinArms) {
        SkinLayersLogger.info("开始设置3D皮肤层: {}, thinArms={}", skinLocation, thinArms);
        
        if (!isAvailable()) {
            SkinLayersLogger.warn("✗ API不可用，无法设置3D皮肤层");
            return null;
        }
        
        // 检查缓存
        CacheKey cacheKey = new CacheKey(skinLocation, thinArms);
        Doll3DSkinData cached = CACHE.get(cacheKey);
        if (cached != null && cached.getCurrentSkin() != null && 
            cached.getCurrentSkin().equals(skinLocation) && 
            cached.isThinArms() == thinArms) {
            SkinLayersLogger.debug("✓ 使用缓存的3D皮肤数据");
            return cached;
        }
        
        // 加载皮肤纹理
        SkinLayersLogger.debug("加载皮肤纹理...");
        NativeImage skin = loadSkinTexture(skinLocation);
        if (skin == null) {
            SkinLayersLogger.warn("✗ 无法加载皮肤纹理，设置失败");
            return null;
        }
        
        try {
            SkinLayersLogger.debug("开始创建3D网格...");
            Doll3DSkinData data = new Doll3DSkinData();
            
            // 创建各个部位的3D网格（参考SkinUtil.setup3dLayers的实现）
            SkinLayersLogger.debug("创建左腿网格...");
            Object leftLegMesh = create3DMesh(skin, 4, 12, 4, 0, 48, true, 0f);
            data.setLeftLegMesh(leftLegMesh);
            
            SkinLayersLogger.debug("创建右腿网格...");
            Object rightLegMesh = create3DMesh(skin, 4, 12, 4, 0, 32, true, 0f);
            data.setRightLegMesh(rightLegMesh);
            
            // 手臂（根据thinArms选择不同的宽度）
            if (thinArms) {
                SkinLayersLogger.debug("创建细手臂网格...");
                // 细手臂：宽度3
                Object leftArmMesh = create3DMesh(skin, 3, 12, 4, 48, 48, true, -2f);
                Object rightArmMesh = create3DMesh(skin, 3, 12, 4, 40, 32, true, -2f);
                data.setLeftArmMesh(leftArmMesh);
                data.setRightArmMesh(rightArmMesh);
            } else {
                SkinLayersLogger.debug("创建粗手臂网格...");
                // 粗手臂：宽度4
                Object leftArmMesh = create3DMesh(skin, 4, 12, 4, 48, 48, true, -2f);
                Object rightArmMesh = create3DMesh(skin, 4, 12, 4, 40, 32, true, -2f);
                data.setLeftArmMesh(leftArmMesh);
                data.setRightArmMesh(rightArmMesh);
            }
            
            SkinLayersLogger.debug("创建身体网格...");
            Object torsoMesh = create3DMesh(skin, 8, 12, 4, 16, 32, true, 0);
            data.setTorsoMesh(torsoMesh);
            
            SkinLayersLogger.debug("创建头部网格...");
            Object headMesh = create3DMesh(skin, 8, 8, 8, 32, 0, false, 0.6f);
            data.setHeadMesh(headMesh);
            
            data.setCurrentSkin(skinLocation);
            data.setThinArms(thinArms);
            
            // 检查数据有效性
            if (data.hasValidData()) {
                SkinLayersLogger.info("✓ 3D皮肤层设置成功，有效网格数: {}", 
                        countValidMeshes(data));
            } else {
                SkinLayersLogger.warn("✗ 3D皮肤层设置完成但无有效网格");
            }
            
            // 缓存结果
            CACHE.put(cacheKey, data);
            SkinLayersLogger.debug("数据已缓存");
            
            return data;
            
        } catch (Exception e) {
            SkinLayersLogger.error("✗ 设置3D皮肤层失败: {}", skinLocation, e);
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
        if (!isAvailable()) {
            return null;
        }
        
        try {
            Field field = offsetProviderClass.getField(name);
            return field.get(null);
        } catch (Exception e) {
            SkinLayersLogger.error("获取OffsetProvider失败: {}", name, e);
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
