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
    
    // setAction 相关调试日志已关闭 - 问题010已修复
    
    /**
     * 在 restoreFromNBT 方法中，在调用 setSkinFromNBT 后注入日志
     */
    @Inject(
        method = "restoreFromNBT(Lnet/minecraft/nbt/CompoundTag;)V",
        at = @At(
            value = "INVOKE",
            target = "Lcom/lanye/dolladdon/base/entity/BaseDollEntity;setSkinFromNBT(Lnet/minecraft/nbt/CompoundTag;)V",
            shift = At.Shift.AFTER
        )
    )
    private void onAfterSetSkinFromNBT(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        BaseDollEntity entity = (BaseDollEntity) (Object) this;
        String skinPath = entity.getSkinPath();
        boolean isAlexModel = entity.isAlexModel();
        
        ModuleLogger.debug(
            LogModuleConfig.MODULE_ENTITY_NBT,
            "restoreFromNBT: 已设置皮肤 - SkinPath={}, IsAlexModel={}, entityID={}",
            skinPath != null ? skinPath : "null",
            isAlexModel,
            entity.getId()
        );
    }
    
    /**
     * 在 setSkinFromNBT 方法中注入日志
     */
    @Inject(
        method = "setSkinFromNBT(Lnet/minecraft/nbt/CompoundTag;)V",
        at = @At("RETURN")
    )
    private void onSetSkinFromNBTReturn(net.minecraft.nbt.CompoundTag nbt, CallbackInfo ci) {
        BaseDollEntity entity = (BaseDollEntity) (Object) this;
        String skinPath = entity.getSkinPath();
        boolean isAlexModel = entity.isAlexModel();
        
        // 检查 persistentData 是否已同步
        var persistentData = entity.getPersistentData();
        String persistentSkinPath = persistentData.contains("SkinPath", net.minecraft.nbt.Tag.TAG_STRING)
            ? persistentData.getString("SkinPath") : "null";
        
        ModuleLogger.debug(
            LogModuleConfig.MODULE_ENTITY_NBT,
            "setSkinFromNBT完成: SkinPath={}, IsAlexModel={}, persistentData.SkinPath={}, entityID={}",
            skinPath != null ? skinPath : "null",
            isAlexModel,
            persistentSkinPath,
            entity.getId()
        );
    }
    
}
