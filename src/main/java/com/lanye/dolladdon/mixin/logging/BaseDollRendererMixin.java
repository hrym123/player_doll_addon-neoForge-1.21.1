package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.base.render.BaseDollRenderer;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 为 BaseDollRenderer 添加日志的 Mixin
 * 追踪皮肤纹理获取过程
 */
@Mixin(BaseDollRenderer.class)
public class BaseDollRendererMixin {
    
    /**
     * 在 getSkinLocation 方法开始时注入日志
     */
    @Inject(
        method = "getSkinLocation",
        at = @At("HEAD"),
        remap = false
    )
    private <T extends BaseDollEntity> void onGetSkinLocationStart(
        T entity,
        CallbackInfoReturnable<ResourceLocation> cir
    ) {
        // 读取实体的 persistentData
        var persistentData = entity.getPersistentData();
        String persistentSkinPath = persistentData.contains("SkinPath", net.minecraft.nbt.Tag.TAG_STRING)
            ? persistentData.getString("SkinPath") : "null";
        
        // 读取实体的 skinPath 字段（通过 getSkinPath 方法）
        String entitySkinPath = entity.getSkinPath();
        
        ModuleLogger.debug(
            LogModuleConfig.MODULE_RENDER,
            "getSkinLocation开始: entityID={}, persistentData.SkinPath={}, entity.getSkinPath()={}, isClientSide={}",
            entity.getId(),
            persistentSkinPath,
            entitySkinPath != null ? entitySkinPath : "null",
            entity.level().isClientSide()
        );
    }
    
    /**
     * 在 getSkinLocation 方法返回时注入日志
     */
    @Inject(
        method = "getSkinLocation",
        at = @At("RETURN"),
        remap = false
    )
    private <T extends BaseDollEntity> void onGetSkinLocationReturn(
        T entity,
        CallbackInfoReturnable<ResourceLocation> cir
    ) {
        ResourceLocation texture = cir.getReturnValue();
        
        // 读取实体的 persistentData
        var persistentData = entity.getPersistentData();
        String persistentSkinPath = persistentData.contains("SkinPath", net.minecraft.nbt.Tag.TAG_STRING)
            ? persistentData.getString("SkinPath") : "null";
        
        // 读取实体的 skinPath 字段（通过 getSkinPath 方法）
        String entitySkinPath = entity.getSkinPath();
        
        // 检查纹理是否存在（如果纹理不为null）
        boolean textureExists = false;
        if (texture != null) {
            try {
                var minecraft = net.minecraft.client.Minecraft.getInstance();
                if (minecraft != null && minecraft.getResourceManager() != null) {
                    textureExists = minecraft.getResourceManager().getResource(texture).isPresent();
                }
            } catch (Exception e) {
                // 忽略异常
            }
        }
        
        ModuleLogger.debug(
            LogModuleConfig.MODULE_RENDER,
            "getSkinLocation返回: entityID={}, persistentData.SkinPath={}, entity.getSkinPath()={}, 返回纹理={}, 纹理存在={}",
            entity.getId(),
            persistentSkinPath,
            entitySkinPath != null ? entitySkinPath : "null",
            texture != null ? texture.toString() : "null",
            textureExists
        );
    }
}
