package com.lanye.dolladdon.impl.item;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.client.data.PoseDebugStickData;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import com.lanye.dolladdon.util.pose.PoseActionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * 姿态调试棒
 * 潜行时滑动滚轮切换姿态，右键玩偶应用当前姿态
 */
public class PoseDebugStick extends Item {
    private static final String NBT_KEY_POSE = "SelectedPose";
    private static final String LOG_MODULE = LogModuleConfig.MODULE_ENTITY_POSE;
    
    public PoseDebugStick() {
        super(new Item.Settings());
    }
    
    /**
     * 获取当前选中的姿态名称（优先从全局数据读取，然后从 ItemStack NBT 读取）
     */
    public static String getSelectedPose(PlayerEntity player, ItemStack stack) {
        // 优先从全局数据读取（客户端和服务端共享）
        if (player != null) {
            String pose = PoseDebugStickData.getSelectedPose(player);
            if (pose != null) {
                return pose;
            }
        }
        
        // 从 ItemStack NBT 读取（向后兼容）
        if (!stack.isEmpty() && stack.getItem() instanceof PoseDebugStick) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null && nbt.contains(NBT_KEY_POSE)) {
                String poseName = nbt.getString(NBT_KEY_POSE);
                // 同时更新到全局数据
                if (player != null && poseName != null) {
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
    public static void setSelectedPose(PlayerEntity player, ItemStack stack, String poseName) {
        // 保存到全局数据（客户端和服务端共享）
        if (player != null) {
            PoseDebugStickData.setSelectedPose(player, poseName);
        }
        
        // 保存到 ItemStack NBT（用于持久化）
        if (!stack.isEmpty() && stack.getItem() instanceof PoseDebugStick) {
            NbtCompound nbt = stack.getOrCreateNbt();
            if (poseName != null && !poseName.isEmpty()) {
                nbt.putString(NBT_KEY_POSE, poseName);
            } else {
                nbt.remove(NBT_KEY_POSE);
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
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(NBT_KEY_POSE)) {
            return nbt.getString(NBT_KEY_POSE);
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
        NbtCompound nbt = stack.getOrCreateNbt();
        if (poseName != null && !poseName.isEmpty()) {
            nbt.putString(NBT_KEY_POSE, poseName);
        } else {
            nbt.remove(NBT_KEY_POSE);
        }
    }
    
    /**
     * 应用姿态到玩偶实体（由事件处理器调用）
     */
    public static ActionResult applyPoseToEntity(ItemStack stack, PlayerEntity user, BaseDollEntity dollEntity, World world) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        
        // 从全局数据或 ItemStack NBT 读取选中的姿态
        String selectedPoseName = getSelectedPose(user, stack);
        // 调试日志
        ModuleLogger.debug(LOG_MODULE, "姿态调试棒应用姿态: ItemStack NBT={}, 选中的姿态={}", 
            stack.getNbt(), selectedPoseName);
        
        if (selectedPoseName == null || selectedPoseName.isEmpty()) {
            user.sendMessage(Text.literal("请先选择一个姿态（潜行时滑动滚轮）"), true);
            return ActionResult.FAIL;
        }
        
        // 使用 setPoseByName 方法设置姿态并更新索引
        boolean success = dollEntity.setPoseByName(selectedPoseName);
        if (!success) {
            user.sendMessage(Text.literal("姿态不存在或设置失败: " + selectedPoseName), true);
            ModuleLogger.warn(LOG_MODULE, "姿态调试棒: 姿态不存在或设置失败: {}", selectedPoseName);
            return ActionResult.FAIL;
        }
        
        // 获取姿态显示名称
        var pose = PoseActionManager.getPose(selectedPoseName);
        String displayName = pose != null && pose.getDisplayName() != null ? pose.getDisplayName() : selectedPoseName;
        
        user.sendMessage(Text.literal("已应用姿态: " + displayName), true);
        world.playSound(null, dollEntity.getX(), dollEntity.getY(), dollEntity.getZ(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5F, 1.2F);
        ModuleLogger.debug(LOG_MODULE, "姿态调试棒: 玩家 {} 对玩偶 {} 应用姿态 {}", 
            user.getName().getString(), dollEntity.getId(), selectedPoseName);
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        // 显示当前选中的姿态
        String selectedPoseName = getSelectedPose(user, stack);
        if (selectedPoseName != null && !selectedPoseName.isEmpty()) {
            var pose = PoseActionManager.getPose(selectedPoseName);
            if (pose != null) {
                String displayName = pose.getDisplayName();
                user.sendMessage(Text.literal("当前姿态: " + displayName), true);
            } else {
                user.sendMessage(Text.literal("当前姿态: " + selectedPoseName + " (不存在)"), true);
            }
        } else {
            user.sendMessage(Text.literal("未选择姿态（潜行时滑动滚轮切换）"), true);
        }
        
        return TypedActionResult.success(stack);
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        // 如果有选中的姿态，显示附魔光效
        // 注意：hasGlint 方法在客户端调用，无法访问 PlayerEntity
        // 所以这里使用 ItemStack NBT 作为后备
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(NBT_KEY_POSE)) {
            return true;
        }
        return false;
    }
    
}
