package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.PlayerDollAddon;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为 PlayerDollAddon 的事件处理方法添加日志的 Mixin
 */
@Mixin(PlayerDollAddon.class)
public class PlayerDollAddonEventMixin {
    
    /**
     * 在 onEntityInteract 方法开始处注入日志
     */
    @Inject(
        method = "onEntityInteract",
        at = @At("HEAD"),
        remap = false
    )
    private void onEntityInteractHead(PlayerInteractEvent.EntityInteract event, CallbackInfo ci) {
        if (event.getTarget() != null) {
            ModuleLogger.debug(
                LogModuleConfig.MODULE_ENTITY_INTERACT,
                "Player {} interacting with entity: type={}, ID={}, hand={}, server={}, entityClass={}",
                event.getEntity().getName().getString(), 
                event.getTarget().getType().toString(),
                event.getTarget().getId(),
                event.getHand(), 
                !event.getLevel().isClientSide(),
                event.getTarget().getClass().getName()
            );
        }
    }
    
    /**
     * 在 onEntityInteract 方法返回前注入日志（如果发生异常）
     */
    @Inject(
        method = "onEntityInteract",
        at = @At("RETURN"),
        remap = false
    )
    private void onEntityInteractReturn(PlayerInteractEvent.EntityInteract event, CallbackInfo ci) {
        // 交互结果日志已在 BaseDollEntityMixin 中处理
    }
    
    /**
     * 在 onPlayerLoggedIn 方法中注入日志
     */
    @Inject(
        method = "onPlayerLoggedIn",
        at = @At("HEAD"),
        remap = false
    )
    private void onPlayerLoggedInHead(PlayerEvent.PlayerLoggedInEvent event, CallbackInfo ci) {
        ModuleLogger.debug(
            LogModuleConfig.MODULE_MAIN,
            "Player {} logged in",
            event.getEntity() != null ? event.getEntity().getName().getString() : "null"
        );
    }
    
    /**
     * 在 onPlayerLoggedOut 方法中注入日志
     */
    @Inject(
        method = "onPlayerLoggedOut",
        at = @At("HEAD"),
        remap = false
    )
    private void onPlayerLoggedOutHead(PlayerEvent.PlayerLoggedOutEvent event, CallbackInfo ci) {
        ModuleLogger.debug(
            LogModuleConfig.MODULE_MAIN,
            "Player {} logged out",
            event.getEntity() != null ? event.getEntity().getName().getString() : "null"
        );
    }
}
