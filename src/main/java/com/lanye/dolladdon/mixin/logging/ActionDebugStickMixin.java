package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.impl.item.ActionDebugStick;
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
        ModuleLogger.debug(
            LogModuleConfig.MODULE_DEBUG_STICK_ACTION,
            "ActionDebugStick.applyActionToEntity called: player={}, server={}, entityID={}, entityPosition=({}, {}, {}), item={}",
            user != null ? user.getName().getString() : "null",
            !world.isClientSide(),
            dollEntity != null ? dollEntity.getId() : -1,
            dollEntity != null ? String.format("%.2f", dollEntity.getX()) : "null",
            dollEntity != null ? String.format("%.2f", dollEntity.getY()) : "null",
            dollEntity != null ? String.format("%.2f", dollEntity.getZ()) : "null",
            stack != null && !stack.isEmpty() ? stack.getItem().toString() : "empty"
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
        ModuleLogger.debug(
            LogModuleConfig.MODULE_DEBUG_STICK_ACTION,
            "ActionDebugStick.applyActionToEntity returned: {}, player={}, server={}",
            cir.getReturnValue(),
            user != null ? user.getName().getString() : "null",
            !world.isClientSide()
        );
    }
}
