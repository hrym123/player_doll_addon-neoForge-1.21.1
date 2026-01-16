package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为 ItemStack.applyComponents 添加日志的 Mixin
 * 追踪玩偶物品的NBT设置过程
 * 通过追踪 applyComponents 调用来记录 customData 的设置
 */
@Mixin(ItemStack.class)
public class DollSkinCommandMixin {
    
    /**
     * 在 applyComponents 方法调用后注入日志
     * 检查是否设置了 customData 组件，并记录 EntityData 内容
     * 仅保留警告日志，debug日志已清理
     */
    @Inject(
        method = "applyComponents(Lnet/minecraft/core/component/DataComponentPatch;)V",
        at = @At("RETURN")
    )
    private void onApplyComponents(net.minecraft.core.component.DataComponentPatch patch, CallbackInfo ci) {
        ItemStack stack = (ItemStack) (Object) this;
        
        // 检查是否是玩偶物品
        boolean isDollItem = stack.getItem() instanceof com.lanye.dolladdon.base.item.BaseDollItem;
        
        // 只记录玩偶物品的警告日志（debug日志已清理）
        if (isDollItem) {
            var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (customData != null) {
                var dataTag = customData.copyTag();
                if (dataTag == null || !dataTag.contains("EntityData")) {
                    ModuleLogger.warn(
                        LogModuleConfig.MODULE_COMMAND,
                        "applyComponents完成: 玩偶物品有customData但没有EntityData标签"
                    );
                }
            } else {
                ModuleLogger.warn(
                    LogModuleConfig.MODULE_COMMAND,
                    "applyComponents完成: 玩偶物品没有customData组件"
                );
            }
        }
    }
}
