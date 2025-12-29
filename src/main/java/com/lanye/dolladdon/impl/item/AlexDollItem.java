package com.lanye.dolladdon.impl.item;

import com.lanye.dolladdon.base.item.BaseDollItem;
import com.lanye.dolladdon.impl.entity.AlexDollEntity;
import com.lanye.dolladdon.base.entity.BaseDollEntity;
import net.minecraft.world.World;

/**
 * 艾利克斯玩偶物品
 * 固定使用Alex模型（细手臂）和Alex默认皮肤
 */
public class AlexDollItem extends BaseDollItem {
    
    @Override
    protected BaseDollEntity createDollEntity(World world, double x, double y, double z) {
        return new AlexDollEntity(world, x, y, z);
    }
}

