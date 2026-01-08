package com.lanye.dolladdon.impl.item;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.util.ActionDebugStickData;
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

import java.util.List;

/**
 * 动作调试棒
 * 潜行时滑动滚轮切换动作，右键玩偶应用当前动作
 */
public class ActionDebugStick extends Item {
    private static final String NBT_KEY_ACTION = "SelectedAction";
    private static final String PLAYER_DATA_KEY = "player_doll_selected_action";
    private static final String LOG_MODULE = LogModuleConfig.MODULE_ENTITY_ACTION;
    
    public ActionDebugStick() {
        super(new Item.Settings());
    }
    
    /**
     * 获取当前选中的动作名称（优先从全局数据读取，然后从 ItemStack NBT 读取）
     */
    public static String getSelectedAction(PlayerEntity player, ItemStack stack) {
        // 优先从全局数据读取（客户端和服务端共享）
        if (player != null) {
            String action = ActionDebugStickData.getSelectedAction(player);
            if (action != null) {
                return action;
            }
        }
        
        // 从 ItemStack NBT 读取（向后兼容）
        if (!stack.isEmpty() && stack.getItem() instanceof ActionDebugStick) {
            NbtCompound nbt = stack.getNbt();
            if (nbt != null && nbt.contains(NBT_KEY_ACTION)) {
                String actionName = nbt.getString(NBT_KEY_ACTION);
                // 同时更新到全局数据
                if (player != null && actionName != null) {
                    ActionDebugStickData.setSelectedAction(player, actionName);
                }
                return actionName;
            }
        }
        return null;
    }
    
    /**
     * 设置选中的动作名称（同时保存到全局数据和 ItemStack NBT）
     */
    public static void setSelectedAction(PlayerEntity player, ItemStack stack, String actionName) {
        // 保存到全局数据（客户端和服务端共享）
        if (player != null) {
            ActionDebugStickData.setSelectedAction(player, actionName);
        }
        
        // 保存到 ItemStack NBT（用于持久化）
        if (!stack.isEmpty() && stack.getItem() instanceof ActionDebugStick) {
            NbtCompound nbt = stack.getOrCreateNbt();
            if (actionName != null && !actionName.isEmpty()) {
                nbt.putString(NBT_KEY_ACTION, actionName);
            } else {
                nbt.remove(NBT_KEY_ACTION);
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
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(NBT_KEY_ACTION)) {
            return nbt.getString(NBT_KEY_ACTION);
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
        NbtCompound nbt = stack.getOrCreateNbt();
        if (actionName != null && !actionName.isEmpty()) {
            nbt.putString(NBT_KEY_ACTION, actionName);
        } else {
            nbt.remove(NBT_KEY_ACTION);
        }
    }
    
    /**
     * 应用动作到玩偶实体（由事件处理器调用）
     */
    public static ActionResult applyActionToEntity(ItemStack stack, PlayerEntity user, BaseDollEntity dollEntity, World world) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        
        // 从全局数据或 ItemStack NBT 读取选中的动作
        String selectedActionName = getSelectedAction(user, stack);
        // 调试日志
        ModuleLogger.debug(LOG_MODULE, "动作调试棒应用动作: ItemStack NBT={}, 选中的动作={}", 
            stack.getNbt(), selectedActionName);
        
        if (selectedActionName == null || selectedActionName.isEmpty()) {
            user.sendMessage(Text.literal("请先选择一个动作（潜行时滑动滚轮）"), true);
            return ActionResult.FAIL;
        }
        
        var action = PoseActionManager.getAction(selectedActionName);
        if (action == null) {
            user.sendMessage(Text.literal("动作不存在: " + selectedActionName), true);
            ModuleLogger.warn(LOG_MODULE, "动作调试棒: 动作不存在: {}", selectedActionName);
            return ActionResult.FAIL;
        }
        
        dollEntity.setAction(action);
        String displayName = action.getDisplayName();
        user.sendMessage(Text.literal("已应用动作: " + displayName), true);
        world.playSound(null, dollEntity.getX(), dollEntity.getY(), dollEntity.getZ(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5F, 1.2F);
        ModuleLogger.debug(LOG_MODULE, "动作调试棒: 玩家 {} 对玩偶 {} 应用动作 {}", user.getName().getString(), dollEntity.getId(), selectedActionName);
        
        return ActionResult.SUCCESS;
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        // 显示当前选中的动作
        String selectedActionName = getSelectedAction(user, stack);
        if (selectedActionName != null && !selectedActionName.isEmpty()) {
            var action = PoseActionManager.getAction(selectedActionName);
            if (action != null) {
                String displayName = action.getDisplayName();
                user.sendMessage(Text.literal("当前动作: " + displayName), true);
            } else {
                user.sendMessage(Text.literal("当前动作: " + selectedActionName + " (不存在)"), true);
            }
        } else {
            user.sendMessage(Text.literal("未选择动作（潜行时滑动滚轮切换）"), true);
        }
        
        return TypedActionResult.success(stack);
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        // 如果有选中的动作，显示附魔光效
        // 注意：hasGlint 方法在客户端调用，无法访问 PlayerEntity
        // 所以这里使用 ItemStack NBT 作为后备
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(NBT_KEY_ACTION)) {
            return true;
        }
        return false;
    }
    
}
