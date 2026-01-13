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
    
    /**
     * 在 tick 方法开始处注入日志，追踪动作状态
     */
    @Inject(
        method = "tick()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;tick()V",
            shift = At.Shift.AFTER
        ),
        cancellable = false
    )
    private void onTickStart(CallbackInfo ci) {
        BaseDollEntity entity = (BaseDollEntity) (Object) this;
        boolean isClient = entity.level().isClientSide();
        
        // 在客户端，记录同步的动作数据
        if (isClient) {
            try {
                java.lang.reflect.Field dataActionNameField = BaseDollEntity.class.getDeclaredField("DATA_ACTION_NAME");
                dataActionNameField.setAccessible(true);
                net.minecraft.network.syncher.EntityDataAccessor<String> dataActionName = 
                    (net.minecraft.network.syncher.EntityDataAccessor<String>) dataActionNameField.get(null);
                
                java.lang.reflect.Field dataActionTickField = BaseDollEntity.class.getDeclaredField("DATA_ACTION_TICK");
                dataActionTickField.setAccessible(true);
                net.minecraft.network.syncher.EntityDataAccessor<Integer> dataActionTick = 
                    (net.minecraft.network.syncher.EntityDataAccessor<Integer>) dataActionTickField.get(null);
                
                String syncedActionName = entity.getEntityData().get(dataActionName);
                int syncedActionTick = entity.getEntityData().get(dataActionTick);
                
                // 获取当前姿态
                com.lanye.dolladdon.api.pose.DollPose currentPose = null;
                try {
                    java.lang.reflect.Method getCurrentPoseMethod = BaseDollEntity.class.getMethod("getCurrentPose");
                    currentPose = (com.lanye.dolladdon.api.pose.DollPose) getCurrentPoseMethod.invoke(entity);
                } catch (Exception e) {
                    // 忽略错误
                }
                
                ModuleLogger.debug(
                    LogModuleConfig.MODULE_ENTITY_ACTION,
                    "BaseDollEntity.tick (CLIENT): entityID={}, syncedActionName={}, syncedActionTick={}, currentPose={}",
                    entity.getId(),
                    syncedActionName != null && !syncedActionName.isEmpty() ? syncedActionName : "null",
                    syncedActionTick,
                    currentPose != null ? currentPose.getName() : "null"
                );
            } catch (Exception e) {
                // 忽略反射错误
            }
        }
        
        // 在服务端，使用反射获取 actionTick 字段和 currentAction
        if (!isClient) {
            int actionTick = 0;
            com.lanye.dolladdon.api.action.DollAction currentAction = null;
            try {
                currentAction = entity.getCurrentAction();
                if (currentAction != null) {
                    java.lang.reflect.Field actionTickField = BaseDollEntity.class.getDeclaredField("actionTick");
                    actionTickField.setAccessible(true);
                    actionTick = actionTickField.getInt(entity);
                }
            } catch (Exception e) {
                // 忽略反射错误
            }
            
            if (currentAction != null) {
                boolean isLooping = false;
                int duration = 0;
                try {
                    isLooping = currentAction.isLooping();
                    duration = currentAction.getDuration();
                } catch (Exception e) {
                    // 忽略错误
                }
                
                ModuleLogger.debug(
                    LogModuleConfig.MODULE_ENTITY_ACTION,
                    "BaseDollEntity.tick (SERVER): entityID={}, currentAction={}, actionTick={}, isLooping={}, duration={}, willClear={}",
                    entity.getId(),
                    currentAction.getName(),
                    actionTick,
                    isLooping,
                    duration,
                    !isLooping && actionTick >= duration
                );
            }
        }
    }
    
    /**
     * 在 tick 方法中动作被清空后注入日志（在 currentAction = null 之后）
     */
    @Inject(
        method = "tick()V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/lanye/dolladdon/util/pose/PoseActionManager;getPose(Ljava/lang/String;)Lcom/lanye/dolladdon/api/pose/DollPose;",
            shift = At.Shift.AFTER,
            remap = false
        ),
        cancellable = false
    )
    private void onTickAfterActionCleared(CallbackInfo ci) {
        BaseDollEntity entity = (BaseDollEntity) (Object) this;
        
        // 检查 currentAction 是否为 null（动作可能刚被清空）
        com.lanye.dolladdon.api.action.DollAction currentAction = null;
        try {
            currentAction = entity.getCurrentAction();
        } catch (Exception e) {
            // 忽略错误
        }
        
        // 如果 currentAction 为 null，记录日志
        // 注意：这个注入点可能在动作被清空后执行，也可能在其他地方执行
        // 所以我们需要检查是否真的被清空了
        if (currentAction == null) {
            ModuleLogger.debug(
                LogModuleConfig.MODULE_ENTITY_ACTION,
                "BaseDollEntity.tick: entityID={}, server={}, currentAction is null (may have been cleared)",
                entity.getId(),
                !entity.level().isClientSide()
            );
        }
    }
}
