package com.lanye.dolladdon.util.factory;

import com.lanye.dolladdon.init.ModItems;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 玩偶物品工厂类
 * 提供统一的玩偶物品创建方法，确保NBT结构标准化
 * 保证指向同一个材质文件的玩偶可以叠加
 */
public class DollItemFactory {
    
    /**
     * 创建自定义纹理玩偶物品
     * 使用标准化的NBT结构，确保相同材质的物品可以叠加
     * 
     * @param skinPath 皮肤路径（ResourceLocation格式，如 "player_doll:png/xxx.png"）
     * @param isAlexModel 是否为Alex模型
     * @param playerName 玩家名称（必需，用于显示）
     * @return 带NBT的玩偶物品
     */
    public static ItemStack createCustomTextureDoll(
            String skinPath,
            boolean isAlexModel,
            String playerName) {
        
        ItemStack dollItem = new ItemStack(ModItems.CUSTOM_TEXTURE_DOLL.get(), 1);
        
        // 创建标准化的NBT结构
        // 字段顺序：SkinPath -> IsAlexModel -> PlayerName
        // 确保所有创建方式生成的NBT结构完全一致
        CompoundTag entityDataTag = createStandardizedEntityDataTag(
            skinPath, isAlexModel, playerName
        );
        
        // 如果没有实体数据，直接返回物品（无NBT）
        if (entityDataTag.isEmpty()) {
            return dollItem;
        }
        
        // 将EntityData放入customData
        CompoundTag customDataTag = new CompoundTag();
        customDataTag.put("EntityData", entityDataTag);
        
        // 创建CustomData对象并设置到ItemStack
        Object customData = createCustomData(customDataTag);
        if (customData == null) {
            ModuleLogger.warn(
                LogModuleConfig.MODULE_FACTORY,
                "createCustomTextureDoll: 无法创建CustomData对象，返回无NBT物品"
            );
            return dollItem;
        }
        
        // 使用applyComponents设置组件
        try {
            DataComponentPatch.Builder builder = DataComponentPatch.builder();
            
            // 使用反射调用set方法，避免泛型类型检查
            java.lang.reflect.Method setMethod = builder.getClass().getDeclaredMethod("set", 
                net.minecraft.core.component.DataComponentType.class, Object.class);
            setMethod.setAccessible(true);
            setMethod.invoke(builder, DataComponents.CUSTOM_DATA, customData);
            
            dollItem.applyComponents(builder.build());
            
            // 验证NBT是否成功设置
            var verifyCustomData = dollItem.get(DataComponents.CUSTOM_DATA);
            if (verifyCustomData == null) {
                ModuleLogger.error(
                    LogModuleConfig.MODULE_FACTORY,
                    "createCustomTextureDoll: applyComponents后验证失败 - customData为null"
                );
            } else {
                var verifyDataTag = verifyCustomData.copyTag();
                if (verifyDataTag == null || !verifyDataTag.contains("EntityData")) {
                    ModuleLogger.error(
                        LogModuleConfig.MODULE_FACTORY,
                        "createCustomTextureDoll: applyComponents后验证失败 - EntityData标签缺失"
                    );
                } else {
                    ModuleLogger.debug(
                        LogModuleConfig.MODULE_FACTORY,
                        "createCustomTextureDoll: NBT设置成功 - SkinPath={}",
                        verifyDataTag.getCompound("EntityData").getString("SkinPath")
                    );
                }
            }
        } catch (Exception e) {
            // 如果applyComponents失败，尝试使用ItemStack的set方法（如果可用）
            ModuleLogger.warn(
                LogModuleConfig.MODULE_FACTORY,
                "createCustomTextureDoll: applyComponents失败，尝试备用方法 - 错误: {}",
                e.getMessage()
            );
            try {
                java.lang.reflect.Method setMethod = ItemStack.class.getMethod("set", 
                    net.minecraft.core.component.DataComponentType.class, Object.class);
                setMethod.invoke(dollItem, DataComponents.CUSTOM_DATA, customData);
                
                // 验证备用方法是否成功
                var verifyCustomData = dollItem.get(DataComponents.CUSTOM_DATA);
                if (verifyCustomData == null) {
                    ModuleLogger.error(
                        LogModuleConfig.MODULE_FACTORY,
                        "createCustomTextureDoll: 备用方法也失败 - customData为null"
                    );
                } else {
                    ModuleLogger.debug(
                        LogModuleConfig.MODULE_FACTORY,
                        "createCustomTextureDoll: 备用方法成功设置NBT"
                    );
                }
            } catch (Exception e2) {
                // 如果都失败，记录错误但不抛出异常（让游戏继续运行）
                ModuleLogger.error(
                    LogModuleConfig.MODULE_FACTORY,
                    "createCustomTextureDoll: 所有方法都失败 - 第一次错误: {}, 第二次错误: {}",
                    e.getMessage(), e2.getMessage(), e2
                );
            }
        }
        
        return dollItem;
    }
    
    /**
     * 创建标准化的EntityData标签
     * 字段顺序固定：SkinPath -> IsAlexModel -> PlayerName
     * 确保所有创建方式生成的NBT结构完全一致，从而可以叠加
     * 
     * @param skinPath 皮肤路径
     * @param isAlexModel 是否为Alex模型
     * @param playerName 玩家名称（必需，用于显示）
     * @return 标准化的EntityData标签
     */
    private static CompoundTag createStandardizedEntityDataTag(
            String skinPath,
            boolean isAlexModel,
            String playerName) {
        
        CompoundTag entityTag = new CompoundTag();
        
        // 必须字段：SkinPath、IsAlexModel 和 PlayerName
        if (skinPath == null || skinPath.isEmpty()) {
            return entityTag; // 如果没有皮肤路径，返回空标签
        }
        
        if (playerName == null || playerName.isEmpty()) {
            ModuleLogger.warn(
                LogModuleConfig.MODULE_FACTORY,
                "createStandardizedEntityDataTag: playerName为空，返回空标签"
            );
            return entityTag;
        }
        
        // 按固定顺序添加字段，确保NBT结构一致
        entityTag.putString("SkinPath", skinPath);
        entityTag.putBoolean("IsAlexModel", isAlexModel);
        entityTag.putString("PlayerName", playerName);
        
        // 注意：不再保存 DisplayName，直接使用 PlayerName 显示
        
        return entityTag;
    }
    
    /**
     * 创建CustomData对象
     * 使用反射调用CustomData.of()静态方法，支持多个可能的类路径
     * 
     * @param nbt NBT标签
     * @return CustomData对象，如果创建失败返回null
     */
    public static Object createCustomData(CompoundTag nbt) {
        String[] possiblePaths = {
            "net.minecraft.core.component.types.CustomData",
            "net.minecraft.core.component.CustomData",
            "net.minecraft.world.item.component.CustomData"
        };
        
        Exception lastException = null;
        for (String className : possiblePaths) {
            try {
                Class<?> customDataClass = Class.forName(className);
                java.lang.reflect.Method ofMethod = customDataClass.getMethod("of", CompoundTag.class);
                Object result = ofMethod.invoke(null, nbt);
                if (result != null) {
                    ModuleLogger.debug(
                        LogModuleConfig.MODULE_FACTORY,
                        "createCustomData: 成功创建CustomData对象 - 类路径: {}",
                        className
                    );
                    return result;
                }
            } catch (ClassNotFoundException e) {
                lastException = e;
                continue;
            } catch (Exception e) {
                lastException = e;
                continue;
            }
        }
        
        if (lastException != null) {
            ModuleLogger.error(
                LogModuleConfig.MODULE_FACTORY,
                "createCustomData: 所有路径都失败 - 最后错误: {}",
                lastException.getMessage(), lastException
            );
        } else {
            ModuleLogger.error(
                LogModuleConfig.MODULE_FACTORY,
                "createCustomData: 所有路径都失败 - 未知错误"
            );
        }
        return null;
    }
    
    /**
     * 从ItemStack中提取皮肤路径
     * 用于判断两个物品是否指向同一个材质文件
     * 
     * @param stack 物品堆栈
     * @return 皮肤路径，如果不存在则返回null
     */
    @Nullable
    public static String extractSkinPath(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        
        var dataTag = customData.copyTag();
        if (dataTag == null || !dataTag.contains("EntityData")) {
            return null;
        }
        
        CompoundTag entityTag = dataTag.getCompound("EntityData");
        if (!entityTag.contains("SkinPath", net.minecraft.nbt.Tag.TAG_STRING)) {
            return null;
        }
        
        return entityTag.getString("SkinPath");
    }
    
    /**
     * 从ItemStack中提取标准化实体数据标签
     * 用于比较两个物品是否指向同一个材质文件
     * 
     * @param stack 物品堆栈
     * @return 实体数据标签，如果不存在则返回null
     */
    @Nullable
    public static CompoundTag extractEntityDataTag(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        
        var dataTag = customData.copyTag();
        if (dataTag == null || !dataTag.contains("EntityData")) {
            return null;
        }
        
        return dataTag.getCompound("EntityData");
    }
}
