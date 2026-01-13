package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 为 BaseDollEntity 添加日志的 Mixin
 * 在不修改原始类的情况下注入日志代码
 */
@Mixin(BaseDollEntity.class)
public class BaseDollEntityMixin {
    
    /**
     * 在 interact 方法开始处注入日志
     * 
     * @param player 玩家
     * @param hand 交互的手
     * @param cir 回调信息（用于获取返回值）
     */
    @Inject(
        method = "interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At("HEAD"),
        cancellable = false
    )
    private void onInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        BaseDollEntity entity = (BaseDollEntity) (Object) this;
        ItemStack heldStack = player != null ? player.getItemInHand(hand) : null;
        
        ModuleLogger.debug(
            LogModuleConfig.MODULE_ENTITY_INTERACT,
            "BaseDollEntity.interact called: player={}, hand={}, item={}, server={}, entityID={}, entityPosition=({}, {}, {}), sneaking={}",
            player != null ? player.getName().getString() : "null",
            hand,
            heldStack != null && !heldStack.isEmpty() ? heldStack.getItem().toString() : "empty",
            !entity.level().isClientSide(),
            entity.getId(),
            String.format("%.2f", entity.getX()),
            String.format("%.2f", entity.getY()),
            String.format("%.2f", entity.getZ()),
            player != null && player.isShiftKeyDown()
        );
    }
    
    /**
     * 在 interact 方法返回前注入日志（记录返回值）
     */
    @Inject(
        method = "interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At("RETURN"),
        cancellable = false
    )
    private void onInteractReturn(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        BaseDollEntity entity = (BaseDollEntity) (Object) this;
        
        ModuleLogger.debug(
            LogModuleConfig.MODULE_ENTITY_INTERACT,
            "BaseDollEntity.interact returned: {}, player={}, server={}, entityID={}",
            cir.getReturnValue(),
            player != null ? player.getName().getString() : "null",
            !entity.level().isClientSide(),
            entity.getId()
        );
    }
    
    /**
     * 在 setAction 方法调用时注入日志
     */
    @Inject(
        method = "setAction(Lcom/lanye/dolladdon/api/action/DollAction;)V",
        at = @At("HEAD"),
        cancellable = false,
        remap = false
    )
    private void onSetActionHead(com.lanye.dolladdon.api.action.DollAction action, CallbackInfo ci) {
        BaseDollEntity entity = (BaseDollEntity) (Object) this;
        String actionName = action != null ? action.getName() : "null";
        String actionDisplayName = null;
        try {
            if (action != null) {
                actionDisplayName = action.getDisplayName();
            }
        } catch (Exception e) {
            // 忽略错误
        }
        
        ModuleLogger.debug(
            LogModuleConfig.MODULE_ENTITY_ACTION,
            "BaseDollEntity.setAction called: entityID={}, server={}, actionName={}, actionDisplayName={}, actionNull={}",
            entity.getId(),
            !entity.level().isClientSide(),
            actionName,
            actionDisplayName != null ? actionDisplayName : "null",
            action == null
        );
    }
    
    /**
     * 在 setAction 方法返回后注入日志（检查动作是否真的被设置）
     */
    @Inject(
        method = "setAction(Lcom/lanye/dolladdon/api/action/DollAction;)V",
        at = @At("RETURN"),
        cancellable = false,
        remap = false
    )
    private void onSetActionReturn(com.lanye.dolladdon.api.action.DollAction action, CallbackInfo ci) {
        BaseDollEntity entity = (BaseDollEntity) (Object) this;
        String currentActionName = null;
        try {
            com.lanye.dolladdon.api.action.DollAction currentAction = entity.getCurrentAction();
            if (currentAction != null) {
                currentActionName = currentAction.getName();
            }
        } catch (Exception e) {
            // 忽略错误
        }
        
        ModuleLogger.debug(
            LogModuleConfig.MODULE_ENTITY_ACTION,
            "BaseDollEntity.setAction returned: entityID={}, server={}, currentActionName={}, actionWasSet={}",
            entity.getId(),
            !entity.level().isClientSide(),
            currentActionName != null ? currentActionName : "null",
            currentActionName != null
        );
    }
    
}
