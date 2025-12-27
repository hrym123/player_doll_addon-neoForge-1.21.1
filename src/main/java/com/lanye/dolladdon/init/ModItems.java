package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.dynamic.DynamicDollItem;
import com.lanye.dolladdon.impl.item.AlexDollItem;
import com.lanye.dolladdon.impl.item.SteveDollItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PlayerDollAddon.MODID);
    
    // 史蒂夫玩偶物品（固定模型：粗手臂 + Steve默认皮肤）
    public static final DeferredItem<SteveDollItem> STEVE_DOLL = ITEMS.register("steve_doll", SteveDollItem::new);
    
    // 艾利克斯玩偶物品（固定模型：细手臂 + Alex默认皮肤）
    public static final DeferredItem<AlexDollItem> ALEX_DOLL = ITEMS.register("alex_doll", AlexDollItem::new);
    
    // 动态注册的玩偶物品（从文件加载）
    public static final Map<String, DeferredItem<DynamicDollItem>> DYNAMIC_DOLLS = new HashMap<>();
    
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
}

