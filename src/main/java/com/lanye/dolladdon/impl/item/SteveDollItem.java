package com.lanye.dolladdon.impl.item;

import com.lanye.dolladdon.base.item.BaseDollItem;
import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.impl.entity.SteveDollEntity;
import com.lanye.dolladdon.init.ModEntities;
import net.minecraft.world.World;

/**
 * 史蒂夫玩偶物品
 * 固定使用Steve模型（粗手臂）和Steve默认皮肤
 */
public class SteveDollItem extends BaseDollItem {
    
    @Override
    protected BaseDollEntity createDollEntity(World world, double x, double y, double z) {
        return new SteveDollEntity(ModEntities.STEVE_DOLL, world, x, y, z);
    }
}

