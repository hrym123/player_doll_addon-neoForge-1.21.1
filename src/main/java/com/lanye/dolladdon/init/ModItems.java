package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDoll;
import com.lanye.dolladdon.dynamic.DynamicDollItem;
import com.lanye.dolladdon.impl.item.ActionDebugStick;
import com.lanye.dolladdon.impl.item.AlexDollItem;
import com.lanye.dolladdon.impl.item.CustomTextureDollItem;
import com.lanye.dolladdon.impl.item.PoseDebugStick;
import com.lanye.dolladdon.impl.item.SteveDollItem;
import com.lanye.dolladdon.util.resource.PngTextureScanner;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModItems {
    // Logger removed - logging handled by Mixin
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PlayerDoll.MODID);
    
    // 史蒂夫玩偶物品（固定模型：粗手臂 + Steve默认皮肤）
    public static final DeferredItem<SteveDollItem> STEVE_DOLL = ITEMS.register("steve_doll", SteveDollItem::new);
    
    // 艾利克斯玩偶物品（固定模型：细手臂 + Alex默认皮肤）
    public static final DeferredItem<AlexDollItem> ALEX_DOLL = ITEMS.register("alex_doll", AlexDollItem::new);
    
    // 动作调试棒（潜行时滑动滚轮切换动作，右键玩偶应用当前动作）
    public static final DeferredItem<ActionDebugStick> ACTION_DEBUG_STICK = ITEMS.register("action_debug_stick", ActionDebugStick::new);
    
    // 姿态调试棒（潜行时滑动滚轮切换姿态，右键玩偶应用当前姿态）
    public static final DeferredItem<PoseDebugStick> POSE_DEBUG_STICK = ITEMS.register("pose_debug_stick", PoseDebugStick::new);
    
    // 自定义纹理玩偶物品（统一物品，通过NBT存储皮肤路径）
    public static final DeferredItem<CustomTextureDollItem> CUSTOM_TEXTURE_DOLL = ITEMS.register("custom_texture_doll", CustomTextureDollItem::new);
    
    // 动态注册的玩偶物品（从文件加载）- 已废弃，保留用于向后兼容
    @Deprecated
    public static final Map<String, DeferredItem<DynamicDollItem>> DYNAMIC_DOLLS = new HashMap<>();
    
    // 自定义纹理玩偶物品映射表（注册名称 -> 物品持有者）
    public static final Map<String, DeferredItem<CustomTextureDollItem>> CUSTOM_TEXTURE_DOLL_ITEMS = new HashMap<>();
    
    /**
     * 动态注册玩偶物品
     * @param registryName 注册名称
     * @param entityHolder 实体类型持有者（延迟获取）
     * @param textureLocation 纹理位置
     * @param isAlexModel 是否为Alex模型
     * @param displayName 显示名称
     * @return 注册的物品持有者
     */
    public static DeferredItem<DynamicDollItem> registerDynamicDoll(String registryName, 
                                                                     net.neoforged.neoforge.registries.DeferredHolder<EntityType<?>, EntityType<com.lanye.dolladdon.dynamic.DynamicDollEntity>> entityHolder,
                                                                     ResourceLocation textureLocation, 
                                                                     boolean isAlexModel,
                                                                     String displayName) {
        DeferredItem<DynamicDollItem> holder = ITEMS.register(
                registryName,
                () -> new DynamicDollItem(entityHolder.get(), textureLocation, isAlexModel, displayName)
        );
        DYNAMIC_DOLLS.put(registryName, holder);
        return holder;
    }
    
    /**
     * 注册自定义纹理玩偶物品
     * @param registryName 注册名称
     * @param textureId 纹理标识符
     * @return 注册的物品持有者
     * @deprecated 已废弃，不再为每个皮肤注册新物品，使用统一的 CUSTOM_TEXTURE_DOLL
     */
    @Deprecated
    public static DeferredItem<CustomTextureDollItem> registerCustomTextureDollItem(String registryName, ResourceLocation textureId) {
        // 注意：此方法已废弃，保留用于向后兼容
        // 现在使用统一的 CUSTOM_TEXTURE_DOLL 物品，通过NBT存储皮肤路径
        DeferredItem<CustomTextureDollItem> holder = ITEMS.register(
                "custom_doll_" + registryName,
                () -> new CustomTextureDollItem()
        );
        CUSTOM_TEXTURE_DOLL_ITEMS.put(registryName, holder);
        
        // 验证注册（在DeferredRegister注册后，通过检查映射表验证）
        // 注意：DeferredRegister的验证需要在注册完成后进行，这里先存储到映射表
        return holder;
    }
    
    /**
     * 获取自定义纹理玩偶物品
     * @param registryName 注册名称
     * @return 物品，如果不存在则返回 null
     */
    public static Item getCustomTextureDollItem(String registryName) {
        DeferredItem<CustomTextureDollItem> holder = CUSTOM_TEXTURE_DOLL_ITEMS.get(registryName);
        return holder != null ? holder.get() : null;
    }
    
    /**
     * 获取所有自定义纹理玩偶物品
     * @return 物品映射表
     */
    public static Map<String, Item> getAllCustomTextureDollItems() {
        Map<String, Item> result = new HashMap<>();
        for (Map.Entry<String, DeferredItem<CustomTextureDollItem>> entry : CUSTOM_TEXTURE_DOLL_ITEMS.entrySet()) {
            result.put(entry.getKey(), entry.getValue().get());
        }
        return result;
    }
    
    /**
     * 注册所有自定义纹理玩偶物品（从PNG文件扫描）
     */
    public static void registerCustomTextureDollItems() {
        List<PngTextureScanner.PngTextureInfo> pngFiles = PngTextureScanner.scanPngFiles();
        
        for (PngTextureScanner.PngTextureInfo pngInfo : pngFiles) {
            try {
                String registryName = pngInfo.getRegistryName();
                ResourceLocation textureId = pngInfo.getTextureIdentifier();
                ResourceLocation itemId = ResourceLocation.fromNamespaceAndPath(PlayerDoll.MODID, "custom_doll_" + registryName);
                
                // 注册物品
                DeferredItem<CustomTextureDollItem> registeredHolder = registerCustomTextureDollItem(registryName, textureId);
                
                // 验证注册（在DeferredRegister中，验证通过检查映射表）
                DeferredItem<CustomTextureDollItem> verifyHolder = CUSTOM_TEXTURE_DOLL_ITEMS.get(registryName);
                if (verifyHolder == registeredHolder) {
                    // Debug logging handled by Mixin
                } else {
                    // Error logging handled by Mixin
                }
            } catch (Exception e) {
                // Error logging handled by Mixin
            }
        }
    }
}

