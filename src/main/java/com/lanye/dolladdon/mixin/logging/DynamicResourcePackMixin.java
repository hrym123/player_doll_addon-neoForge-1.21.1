package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.util.neoForge.DynamicResourcePack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;

/**
 * 为 DynamicResourcePack 添加日志的 Mixin
 * 追踪动态资源包的资源加载过程
 */
@Mixin(value = DynamicResourcePack.class, remap = false)
public class DynamicResourcePackMixin {
    
    /**
     * 在 getResource 方法中添加日志（仅记录请求，避免日志过多）
     */
    private static int getResourceCallCount = 0;
    
    @Inject(
        method = "getResource",
        at = @At("HEAD"),
        remap = false
    )
    private void onGetResource(PackType type, ResourceLocation location, CallbackInfoReturnable<IoSupplier<InputStream>> cir) {
        getResourceCallCount++;
        // 只记录前20次调用，避免日志过多
        if (getResourceCallCount <= 20) {
            System.out.println("[PlayerDoll] [DEBUG] DynamicResourcePack.getResource 请求: " + location + " (调用次数: " + getResourceCallCount + ")");
        } else if (getResourceCallCount == 21) {
            System.out.println("[PlayerDoll] [DEBUG] DynamicResourcePack.getResource 后续调用将不再记录日志（避免日志过多）");
        }
    }
    
    /**
     * 在 listResources 方法开始时添加日志
     * 注意：ResourceOutput 是 PackResources 接口中的嵌套类型
     */
    @Inject(
        method = "listResources",
        at = @At("HEAD"),
        remap = false
    )
    private void onListResources(PackType type, String namespace, String path, PackResources.ResourceOutput output, CallbackInfo ci) {
        System.out.println("[PlayerDoll] [DEBUG] DynamicResourcePack.listResources: namespace=" + namespace + ", path=" + path);
    }
}
