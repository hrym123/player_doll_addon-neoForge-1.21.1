package com.lanye.dolladdon.impl.item;

import com.lanye.dolladdon.base.item.BaseDollItem;
import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.impl.entity.CustomTextureDollEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * 自定义纹理玩偶物品
 * 使用外部 PNG 文件作为纹理
 */
public class CustomTextureDollItem extends BaseDollItem {
    private final Identifier textureIdentifier;
    private final String registryName;
    
    public CustomTextureDollItem(Identifier textureIdentifier, String registryName) {
        super();
        this.textureIdentifier = textureIdentifier;
        this.registryName = registryName;
    }
    
    @Override
    protected BaseDollEntity createDollEntity(World world, double x, double y, double z) {
        return new CustomTextureDollEntity(world, x, y, z, textureIdentifier, registryName);
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
}

