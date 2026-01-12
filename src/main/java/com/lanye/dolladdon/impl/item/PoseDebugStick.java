package com.lanye.dolladdon.impl.item;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.client.data.PoseDebugStickData;
import com.lanye.dolladdon.util.pose.PoseActionManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * 姿态调试棒
 * 潜行时滑动滚轮切换姿态，右键玩偶应用当前姿态
 */
public class PoseDebugStick extends Item {
    private static final String NBT_KEY_POSE = "SelectedPose";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public PoseDebugStick() {
        super(new Item.Properties());
    }
    
    /**
     * 保存调试棒数据到玩家物品栏（在玩家退出时调用）
     */
    public static void saveToInventory(Player player) {
        if (player == null || player.level().isClientSide()) {
            return;
        }
        
        // 从内存获取数据
        String poseName = PoseDebugStickData.getSelectedPose(player);
        if (poseName == null || poseName.isEmpty()) {
            return;
        }
        
        // 保存到物品栏中所有姿态调试棒
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof PoseDebugStick) {
                savePoseToStack(stack, poseName);
            }
        }
    }
    
    /**
     * 从玩家物品栏恢复调试棒数据（在玩家登录时调用）
     */
    public static void restoreFromInventory(Player player) {
        if (player == null) {
            return;
        }
        
        // 扫描玩家物品栏中的所有姿态调试棒
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof PoseDebugStick) {
                String poseName = getSelectedPoseFromStack(stack);
                if (poseName != null && !poseName.isEmpty()) {
                    PoseDebugStickData.setSelectedPose(player, poseName);
                    break; // 找到第一个有数据的调试棒就停止
                }
            }
        }
    }
    
    /**
     * 从 ItemStack NBT 读取姿态名称（不更新全局数据）
     */
    private static String getSelectedPoseFromStack(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PoseDebugStick)) {
            return null;
        }
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var nbt = customData.copyTag();
            if (nbt != null && nbt.contains(NBT_KEY_POSE)) {
                return nbt.getString(NBT_KEY_POSE);
            }
        }
        return null;
    }
    
    /**
     * 获取当前选中的姿态名称（优先从全局数据读取，然后从 ItemStack NBT 读取）
     */
    public static String getSelectedPose(Player player, ItemStack stack) {
        // 优先从全局数据读取（客户端和服务端共享）
        if (player != null) {
            String pose = PoseDebugStickData.getSelectedPose(player);
            if (pose != null) {
                return pose;
            }
        }
        
        // 从 ItemStack NBT 读取（向后兼容）
        if (!stack.isEmpty() && stack.getItem() instanceof PoseDebugStick) {
            String poseName = getSelectedPoseFromStack(stack);
            if (poseName != null) {
                // 同时更新到全局数据
                if (player != null) {
                    PoseDebugStickData.setSelectedPose(player, poseName);
                }
                return poseName;
            }
        }
        return null;
    }
    
    /**
     * 设置选中的姿态名称（同时保存到全局数据和 ItemStack NBT）
     */
    public static void setSelectedPose(Player player, ItemStack stack, String poseName) {
        // 保存到全局数据（客户端和服务端共享）
        if (player != null) {
            PoseDebugStickData.setSelectedPose(player, poseName);
        }
        
        // 保存到当前 ItemStack NBT
        savePoseToStack(stack, poseName);
        
        // 同步到玩家物品栏中所有姿态调试棒（确保数据一致性）
        if (player != null) {
            syncPoseToAllSticks(player, poseName);
        }
    }
    
    /**
     * 保存姿态到 ItemStack NBT
     */
    private static void savePoseToStack(ItemStack stack, String poseName) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PoseDebugStick)) {
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
        if (poseName != null && !poseName.isEmpty()) {
            nbt.putString(NBT_KEY_POSE, poseName);
        } else {
            nbt.remove(NBT_KEY_POSE);
        }
        // 使用反射创建 CustomData 对象
        try {
            Object customDataComponent = createCustomData(nbt);
            if (customDataComponent != null) {
                java.lang.reflect.Method setMethod = ItemStack.class.getMethod("set", 
                    net.minecraft.core.component.DataComponentType.class, Object.class);
                setMethod.invoke(stack, DataComponents.CUSTOM_DATA, customDataComponent);
            } else {
                LOGGER.error("创建 CustomData 对象失败");
            }
        } catch (Exception e) {
            LOGGER.error("设置 CustomData 失败", e);
        }
    }
    
    /**
     * 同步姿态到玩家物品栏中所有姿态调试棒
     */
    private static void syncPoseToAllSticks(Player player, String poseName) {
        if (player == null) {
            return;
        }
        
        // 同步到物品栏中所有姿态调试棒
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof PoseDebugStick) {
                savePoseToStack(stack, poseName);
            }
        }
    }
    
    /**
     * 获取当前选中的姿态名称（仅从 ItemStack NBT 读取，用于兼容）
     */
    @Deprecated
    public static String getSelectedPose(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PoseDebugStick)) {
            return null;
        }
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var nbt = customData.copyTag();
            if (nbt != null && nbt.contains(NBT_KEY_POSE)) {
                return nbt.getString(NBT_KEY_POSE);
            }
        }
        return null;
    }
    
    /**
     * 设置选中的姿态名称（仅保存到 ItemStack NBT，用于兼容）
     */
    @Deprecated
    public static void setSelectedPose(ItemStack stack, String poseName) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PoseDebugStick)) {
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
        if (poseName != null && !poseName.isEmpty()) {
            nbt.putString(NBT_KEY_POSE, poseName);
        } else {
            nbt.remove(NBT_KEY_POSE);
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
     * 发送消息到动作栏（物品栏上方）
     */
    private static void sendActionBarMessage(Player player, Component message) {
        if (player instanceof ServerPlayer serverPlayer) {
            // 服务端：使用数据包发送到动作栏（overlay=true 表示动作栏）
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSystemChatPacket(message, true));
        } else {
            // 客户端：直接显示（actionBar=true 表示动作栏）
            player.displayClientMessage(message, true);
        }
    }
    
    /**
     * 应用姿态到玩偶实体（由事件处理器调用）
     */
    public static InteractionResult applyPoseToEntity(ItemStack stack, Player user, BaseDollEntity dollEntity, Level world) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        
        // 从全局数据或 ItemStack NBT 读取选中的姿态
        String selectedPoseName = getSelectedPose(user, stack);
        
        if (selectedPoseName == null || selectedPoseName.isEmpty()) {
            sendActionBarMessage(user, Component.literal("请先选择一个姿态（潜行时滑动滚轮）"));
            return InteractionResult.FAIL;
        }
        
        // 使用 setPoseByName 方法设置姿态并更新索引
        boolean success = dollEntity.setPoseByName(selectedPoseName);
        if (!success) {
            sendActionBarMessage(user, Component.literal("姿态不存在或设置失败: " + selectedPoseName));
            LOGGER.warn("姿态调试棒: 姿态不存在或设置失败: {}", selectedPoseName);
            return InteractionResult.FAIL;
        }
        
        // 获取姿态显示名称
        var pose = PoseActionManager.getPose(selectedPoseName);
        String displayName = pose != null && pose.getDisplayName() != null ? pose.getDisplayName() : selectedPoseName;
        
        sendActionBarMessage(user, Component.literal("已应用姿态: " + displayName));
        world.playSound(null, dollEntity.getX(), dollEntity.getY(), dollEntity.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 1.2F);
        
        return InteractionResult.SUCCESS;
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        
        // 显示当前选中的姿态
        String selectedPoseName = getSelectedPose(user, stack);
        if (selectedPoseName != null && !selectedPoseName.isEmpty()) {
            var pose = PoseActionManager.getPose(selectedPoseName);
            if (pose != null) {
                String displayName = pose.getDisplayName();
                sendActionBarMessage(user, Component.literal("当前姿态: " + displayName));
            } else {
                sendActionBarMessage(user, Component.literal("当前姿态: " + selectedPoseName + " (不存在)"));
            }
        } else {
            sendActionBarMessage(user, Component.literal("未选择姿态（潜行时滑动滚轮切换）"));
        }
        
        return InteractionResultHolder.success(stack);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // 在工具提示中显示当前选中的姿态
        // 注意：在工具提示中无法访问 Player，所以使用 ItemStack NBT
        String selectedPoseName = getSelectedPose(stack);
        if (selectedPoseName != null && !selectedPoseName.isEmpty()) {
            var pose = PoseActionManager.getPose(selectedPoseName);
            if (pose != null) {
                String displayName = pose.getDisplayName();
                tooltip.add(Component.literal("当前姿态: " + displayName));
            } else {
                tooltip.add(Component.literal("当前姿态: " + selectedPoseName + " (不存在)"));
            }
        } else {
            tooltip.add(Component.literal("未选择姿态"));
        }
    }
    
    @Override
    public boolean isFoil(ItemStack stack) {
        // 如果有选中的姿态，显示附魔光效
        // 注意：isFoil 方法在客户端调用，无法访问 Player
        // 所以这里使用 ItemStack NBT 作为后备
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var nbt = customData.copyTag();
            if (nbt != null && nbt.contains(NBT_KEY_POSE)) {
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
        
        Exception lastException = null;
        for (String className : possiblePaths) {
            try {
                Class<?> customDataClass = Class.forName(className);
                java.lang.reflect.Method ofMethod = customDataClass.getMethod("of", CompoundTag.class);
                Object result = ofMethod.invoke(null, nbt);
                if (result != null) {
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
            LOGGER.error("所有 CustomData 类路径都失败，最后一个错误: {}", lastException.getMessage(), lastException);
        } else {
            LOGGER.error("无法创建 CustomData 对象，所有类路径都未找到");
        }
        return null;
    }
}
