package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.base.render.BaseDollRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 为 BaseDollRenderer 添加日志的 Mixin
 * 追踪皮肤纹理获取过程
 */
@Mixin(value = BaseDollRenderer.class, remap = false)
public class BaseDollRendererMixin {
    
    /**
     * 在 getSkinLocation 方法返回时添加日志（仅在前几次调用时记录，避免日志过多）
     * 注意：使用 ThreadLocal 避免多线程问题
     */
    private static final ThreadLocal<Integer> getSkinLocationCallCount = ThreadLocal.withInitial(() -> 0);
    
    @Inject(
        method = "getSkinLocation",
        at = @At("RETURN"),
        remap = false
    )
    private void onGetSkinLocationReturn(CallbackInfoReturnable<ResourceLocation> cir) {
        int count = getSkinLocationCallCount.get();
        count++;
        getSkinLocationCallCount.set(count);
        
        // 只记录前10次调用，避免日志过多
        if (count <= 10) {
            ResourceLocation location = cir.getReturnValue();
            System.out.println("[PlayerDoll] [DEBUG] BaseDollRenderer.getSkinLocation 返回: " + location + " (调用次数: " + count + ")");
        } else if (count == 11) {
            System.out.println("[PlayerDoll] [DEBUG] BaseDollRenderer.getSkinLocation 后续调用将不再记录日志（避免日志过多）");
        }
    }
}
