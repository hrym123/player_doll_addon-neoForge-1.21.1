package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.impl.item.ActionDebugStick;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 为 ActionDebugStick 添加日志的 Mixin
 * 在不修改原始类的情况下注入日志代码
 */
@Mixin(ActionDebugStick.class)
public class ActionDebugStickMixin {
    
    /**
     * 在 applyActionToEntity 方法开始处注入日志
     * 
     * @param stack 物品堆
     * @param user 玩家
     * @param dollEntity 玩偶实体
     * @param world 世界
     * @param cir 回调信息（用于获取返回值）
     */
    @Inject(
        method = "applyActionToEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lcom/lanye/dolladdon/base/entity/BaseDollEntity;Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/InteractionResult;",
        at = @At("HEAD"),
        cancellable = false,
        remap = false
    )
    private static void onApplyActionToEntityHead(
        ItemStack stack,
        Player user,
        BaseDollEntity dollEntity,
        Level world,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        // 获取选中的动作名称（用于调试）
        String selectedAction = null;
        try {
            if (user != null && stack != null && !stack.isEmpty()) {
                // 使用反射调用 getSelectedAction 方法
                java.lang.reflect.Method getSelectedActionMethod = 
                    com.lanye.dolladdon.impl.item.ActionDebugStick.class.getMethod(
                        "getSelectedAction", 
                        net.minecraft.world.entity.player.Player.class, 
                        net.minecraft.world.item.ItemStack.class
                    );
                selectedAction = (String) getSelectedActionMethod.invoke(null, user, stack);
            }
        } catch (Exception e) {
            // 忽略反射错误
        }
        
        ModuleLogger.debug(
            LogModuleConfig.MODULE_DEBUG_STICK_ACTION,
            "ActionDebugStick.applyActionToEntity called: player={}, server={}, entityID={}, entityPosition=({}, {}, {}), item={}, selectedAction={}",
            user != null ? user.getName().getString() : "null",
            !world.isClientSide(),
            dollEntity != null ? dollEntity.getId() : -1,
            dollEntity != null ? String.format("%.2f", dollEntity.getX()) : "null",
            dollEntity != null ? String.format("%.2f", dollEntity.getY()) : "null",
            dollEntity != null ? String.format("%.2f", dollEntity.getZ()) : "null",
            stack != null && !stack.isEmpty() ? stack.getItem().toString() : "empty",
            selectedAction != null ? selectedAction : "null"
        );
    }
    
    /**
     * 在 applyActionToEntity 方法返回前注入日志（记录返回值）
     */
    @Inject(
        method = "applyActionToEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lcom/lanye/dolladdon/base/entity/BaseDollEntity;Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/InteractionResult;",
        at = @At("RETURN"),
        cancellable = false,
        remap = false
    )
    private static void onApplyActionToEntityReturn(
        ItemStack stack,
        Player user,
        BaseDollEntity dollEntity,
        Level world,
        CallbackInfoReturnable<InteractionResult> cir
    ) {
        // 如果是服务端且返回了SUCCESS，检查动作是否真的被应用了
        String actionApplied = null;
        if (!world.isClientSide() && cir.getReturnValue() == InteractionResult.SUCCESS && dollEntity != null) {
            try {
                // 使用反射获取实体的当前动作（方法名是 getCurrentAction，不是 getAction）
                java.lang.reflect.Method getCurrentActionMethod = 
                    com.lanye.dolladdon.base.entity.BaseDollEntity.class.getMethod("getCurrentAction");
                Object action = getCurrentActionMethod.invoke(dollEntity);
                if (action != null) {
                    java.lang.reflect.Method getDisplayNameMethod = action.getClass().getMethod("getDisplayName");
                    actionApplied = (String) getDisplayNameMethod.invoke(action);
                }
            } catch (Exception e) {
                // 记录反射错误以便调试
                ModuleLogger.debug(
                    LogModuleConfig.MODULE_DEBUG_STICK_ACTION,
                    "Failed to get action via reflection: {}",
                    e.getMessage()
                );
            }
        }
        
        ModuleLogger.debug(
            LogModuleConfig.MODULE_DEBUG_STICK_ACTION,
            "ActionDebugStick.applyActionToEntity returned: {}, player={}, server={}, actionApplied={}",
            cir.getReturnValue(),
            user != null ? user.getName().getString() : "null",
            !world.isClientSide(),
            actionApplied != null ? actionApplied : "N/A"
        );
    }
}
