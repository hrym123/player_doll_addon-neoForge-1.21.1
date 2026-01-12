package com.lanye.dolladdon.impl.entity;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 自定义纹理玩偶实体
 * 使用外部 PNG 文件作为纹理
 */
public class CustomTextureDollEntity extends BaseDollEntity {
    private final ResourceLocation textureIdentifier;
    private final String registryName;
    
    public CustomTextureDollEntity(EntityType<? extends CustomTextureDollEntity> entityType, Level world, ResourceLocation textureIdentifier, String registryName) {
        super(entityType, world);
        this.textureIdentifier = textureIdentifier;
        this.registryName = registryName;
    }
    
    public CustomTextureDollEntity(Level world, double x, double y, double z, ResourceLocation textureIdentifier, String registryName) {
        super(getEntityType(registryName), world, x, y, z);
        this.textureIdentifier = textureIdentifier;
        this.registryName = registryName;
    }
    
    @Override
    protected ItemStack getDollItemStack() {
        return new ItemStack(ModItems.getCustomTextureDollItem(registryName));
    }
    
    /**
     * 获取纹理标识符
     */
    public ResourceLocation getTextureIdentifier() {
        return textureIdentifier;
    }
    
    /**
     * 获取注册名称
     */
    public String getRegistryName() {
        return registryName;
    }
    
    /**
     * 获取实体类型（从 ModEntities 获取）
     */
    private static EntityType<? extends CustomTextureDollEntity> getEntityType(String registryName) {
        return ModEntities.getCustomTextureDollEntityType(registryName);
    }
}
