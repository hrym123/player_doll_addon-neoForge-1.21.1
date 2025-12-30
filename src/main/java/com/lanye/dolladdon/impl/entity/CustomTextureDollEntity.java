package com.lanye.dolladdon.impl.entity;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * 自定义纹理玩偶实体
 * 使用外部 PNG 文件作为纹理
 */
public class CustomTextureDollEntity extends BaseDollEntity {
    private final Identifier textureIdentifier;
    private final String registryName;
    
    public CustomTextureDollEntity(EntityType<? extends CustomTextureDollEntity> entityType, World world, Identifier textureIdentifier, String registryName) {
        super(entityType, world);
        this.textureIdentifier = textureIdentifier;
        this.registryName = registryName;
    }
    
    public CustomTextureDollEntity(World world, double x, double y, double z, Identifier textureIdentifier, String registryName) {
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
    public Identifier getTextureIdentifier() {
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

