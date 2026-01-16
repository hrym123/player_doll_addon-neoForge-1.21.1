package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.base.item.BaseDollItem;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 为 BaseDollItem 添加日志的 Mixin
 * 追踪从物品创建实体时的NBT读取过程
 */
@Mixin(BaseDollItem.class)
public class BaseDollItemMixin {
    
    /**
     * 在 useOn 方法中，在调用 restoreFromNBT 前注入日志
     * 仅保留警告日志，debug日志已清理
     */
    @Inject(
        method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
        at = @At(
            value = "INVOKE",
            target = "Lcom/lanye/dolladdon/base/entity/BaseDollEntity;restoreFromNBT(Lnet/minecraft/nbt/CompoundTag;)V",
            shift = At.Shift.BEFORE
        )
    )
    private void onBeforeRestoreFromNBT(
        net.minecraft.world.item.context.UseOnContext context,
        CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir
    ) {
        net.minecraft.world.item.ItemStack stack = context.getItemInHand();
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        
        if (customData != null) {
            var dataTag = customData.copyTag();
            if (dataTag == null || !dataTag.contains("EntityData")) {
                ModuleLogger.warn(
                    LogModuleConfig.MODULE_COMMAND,
                    "从物品创建实体: customData中没有EntityData标签"
                );
            }
        } else {
            ModuleLogger.warn(
                LogModuleConfig.MODULE_COMMAND,
                "从物品创建实体: 物品没有customData组件"
            );
        }
    }
}
