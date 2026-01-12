package com.lanye.dolladdon.impl.item;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.client.data.ActionDebugStickData;
import com.lanye.dolladdon.util.pose.PoseActionManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * 动作调试棒
 * 潜行时滑动滚轮切换动作，右键玩偶应用当前动作
 */
public class ActionDebugStick extends Item {
    private static final String NBT_KEY_ACTION = "SelectedAction";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public ActionDebugStick() {
        super(new Item.Properties());
    }
    
    /**
     * 获取当前选中的动作名称（优先从全局数据读取，然后从 ItemStack NBT 读取）
     */
    public static String getSelectedAction(Player player, ItemStack stack) {
        // 优先从全局数据读取（客户端和服务端共享）
        if (player != null) {
            String action = ActionDebugStickData.getSelectedAction(player);
            if (action != null) {
                return action;
            }
        }
        
        // 从 ItemStack NBT 读取（向后兼容）
        if (!stack.isEmpty() && stack.getItem() instanceof ActionDebugStick) {
            var customData = stack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                var nbt = customData.copyTag();
                if (nbt != null && nbt.contains(NBT_KEY_ACTION)) {
                    String actionName = nbt.getString(NBT_KEY_ACTION);
                    // 同时更新到全局数据
                    if (player != null && actionName != null) {
                        ActionDebugStickData.setSelectedAction(player, actionName);
                    }
                    return actionName;
                }
            }
        }
        return null;
    }
    
    /**
     * 设置选中的动作名称（同时保存到全局数据和 ItemStack NBT）
     */
    public static void setSelectedAction(Player player, ItemStack stack, String actionName) {
        // 保存到全局数据（客户端和服务端共享）
        if (player != null) {
            ActionDebugStickData.setSelectedAction(player, actionName);
        }
        
        // 保存到 ItemStack NBT（用于持久化）
        if (!stack.isEmpty() && stack.getItem() instanceof ActionDebugStick) {
            var existingData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag nbt;
            if (existingData != null) {
                var existingTag = existingData.copyTag();
                nbt = existingTag != null ? existingTag : new CompoundTag();
            } else {
                nbt = new CompoundTag();
            }
            if (actionName != null && !actionName.isEmpty()) {
                nbt.putString(NBT_KEY_ACTION, actionName);
            } else {
                nbt.remove(NBT_KEY_ACTION);
            }
            // 使用反射创建 CustomData 对象
            try {
                Object customDataComponent = createCustomData(nbt);
                if (customDataComponent != null) {
                    java.lang.reflect.Method setMethod = ItemStack.class.getMethod("set", 
                        net.minecraft.core.component.DataComponentType.class, Object.class);
                    setMethod.invoke(stack, DataComponents.CUSTOM_DATA, customDataComponent);
                }
            } catch (Exception e) {
                LOGGER.error("设置 CustomData 失败", e);
            }
        }
    }
    
    /**
     * 获取当前选中的动作名称（仅从 ItemStack NBT 读取，用于兼容）
     */
    @Deprecated
    public static String getSelectedAction(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ActionDebugStick)) {
            return null;
        }
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var nbt = customData.copyTag();
            if (nbt != null && nbt.contains(NBT_KEY_ACTION)) {
                return nbt.getString(NBT_KEY_ACTION);
            }
        }
        return null;
    }
    
    /**
     * 设置选中的动作名称（仅保存到 ItemStack NBT，用于兼容）
     */
    @Deprecated
    public static void setSelectedAction(ItemStack stack, String actionName) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ActionDebugStick)) {
            return;
        }
        var existingData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag nbt;
        if (existingData != null) {
            var existingTag = existingData.copyTag();
            nbt = existingTag != null ? existingTag : new CompoundTag();
        } else {
            nbt = new CompoundTag();
        }
        if (actionName != null && !actionName.isEmpty()) {
            nbt.putString(NBT_KEY_ACTION, actionName);
        } else {
            nbt.remove(NBT_KEY_ACTION);
        }
        // 使用反射创建 CustomData 对象
        try {
            Object customDataComponent = createCustomData(nbt);
            if (customDataComponent != null) {
                java.lang.reflect.Method setMethod = ItemStack.class.getMethod("set", 
                    net.minecraft.core.component.DataComponentType.class, Object.class);
                setMethod.invoke(stack, DataComponents.CUSTOM_DATA, customDataComponent);
            }
        } catch (Exception e) {
            LOGGER.error("设置 CustomData 失败", e);
        }
    }
    
    /**
     * 应用动作到玩偶实体（由事件处理器调用）
     */
    public static InteractionResult applyActionToEntity(ItemStack stack, Player user, BaseDollEntity dollEntity, Level world) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        // 从全局数据或 ItemStack NBT 读取选中的动作
        String selectedActionName = getSelectedAction(user, stack);
        // 调试日志
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        LOGGER.debug("动作调试棒应用动作: ItemStack NBT={}, 选中的动作={}", 
            customData != null ? customData.copyTag() : null, selectedActionName);
        
        if (selectedActionName == null || selectedActionName.isEmpty()) {
            user.sendSystemMessage(Component.literal("请先选择一个动作（潜行时滑动滚轮）"));
            return InteractionResult.FAIL;
        }
        
        var action = PoseActionManager.getAction(selectedActionName);
        if (action == null) {
            user.sendSystemMessage(Component.literal("动作不存在: " + selectedActionName));
            LOGGER.warn("动作调试棒: 动作不存在: {}", selectedActionName);
            return InteractionResult.FAIL;
        }
        
        dollEntity.setAction(action);
        String displayName = action.getName();
        user.sendSystemMessage(Component.literal("已应用动作: " + displayName));
        world.playSound(null, dollEntity.getX(), dollEntity.getY(), dollEntity.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.2F);
        LOGGER.debug("动作调试棒: 玩家 {} 对玩偶 {} 应用动作 {}", user.getName().getString(), dollEntity.getId(), selectedActionName);
        
        return InteractionResult.SUCCESS;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        
        // 显示当前选中的动作
        String selectedActionName = getSelectedAction(user, stack);
        if (selectedActionName != null && !selectedActionName.isEmpty()) {
            var action = PoseActionManager.getAction(selectedActionName);
            if (action != null) {
                String displayName = action.getName();
                user.sendSystemMessage(Component.literal("当前动作: " + displayName));
            } else {
                user.sendSystemMessage(Component.literal("当前动作: " + selectedActionName + " (不存在)"));
            }
        } else {
            user.sendSystemMessage(Component.literal("未选择动作（潜行时滑动滚轮切换）"));
        }
        
        return InteractionResultHolder.success(stack);
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        // 如果有选中的动作，显示附魔光效
        // 注意：isFoil 方法在客户端调用，无法访问 Player
        // 所以这里使用 ItemStack NBT 作为后备
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var nbt = customData.copyTag();
            if (nbt != null && nbt.contains(NBT_KEY_ACTION)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 创建 CustomData 对象（使用反射，兼容不同的包路径）
     */
    private static Object createCustomData(CompoundTag nbt) {
        String[] possiblePaths = {
            "net.minecraft.core.component.types.CustomData",
            "net.minecraft.core.component.CustomData",
            "net.minecraft.world.item.component.CustomData"
        };
        
        for (String className : possiblePaths) {
            try {
                Class<?> customDataClass = Class.forName(className);
                java.lang.reflect.Method ofMethod = customDataClass.getMethod("of", CompoundTag.class);
                return ofMethod.invoke(null, nbt);
            } catch (ClassNotFoundException e) {
                continue;
            } catch (Exception e) {
                continue;
            }
        }
        return null;
    }
}
