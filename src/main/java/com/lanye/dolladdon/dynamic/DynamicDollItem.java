package com.lanye.dolladdon.dynamic;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * 动态玩偶物品
 * 用于从文件加载的玩偶
 */
public class DynamicDollItem extends com.lanye.dolladdon.base.item.BaseDollItem {
    private final EntityType<DynamicDollEntity> entityType;
    private final Identifier textureLocation;
    private final boolean isAlexModel;
    private final String displayName;
    
    public DynamicDollItem(EntityType<DynamicDollEntity> entityType, Identifier textureLocation, boolean isAlexModel, String displayName) {
        super();
        this.entityType = entityType;
        this.textureLocation = textureLocation;
        this.isAlexModel = isAlexModel;
        this.displayName = displayName;
    }
    
    @Override
    public Text getName(ItemStack stack) {
        return Text.literal(displayName);
    }
    
    @Override
    protected BaseDollEntity createDollEntity(World world, double x, double y, double z) {
        return new DynamicDollEntity(entityType, world, x, y, z);
    }
    
    public Identifier getTextureLocation() {
        return textureLocation;
    }
    
    public boolean isAlexModel() {
        return isAlexModel;
    }
}

