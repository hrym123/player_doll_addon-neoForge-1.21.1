package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
        
        LoggingUtil.info(
            "[Mixin日志] BaseDollEntity.interact 被调用: " +
            "玩家={}, 手={}, 物品={}, 服务端={}, 实体ID={}, 实体位置=({}, {}, {}), 潜行={}",
            player != null ? player.getName().getString() : "null",
            hand,
            heldStack != null && !heldStack.isEmpty() ? heldStack.getItem().toString() : "空",
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
        
        LoggingUtil.info(
            "[Mixin日志] BaseDollEntity.interact 返回: {}, 玩家={}, 服务端={}, 实体ID={}",
            cir.getReturnValue(),
            player != null ? player.getName().getString() : "null",
            !entity.level().isClientSide(),
            entity.getId()
        );
    }
}
