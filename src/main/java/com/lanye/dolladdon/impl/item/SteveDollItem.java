package com.lanye.dolladdon.impl.item;

import com.lanye.dolladdon.base.item.BaseDollItem;
import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.impl.entity.SteveDollEntity;
import net.minecraft.world.level.Level;

/**
 * 史蒂夫玩偶物品
 * 固定使用Steve模型（粗手臂）和Steve默认皮肤
 */
public class SteveDollItem extends BaseDollItem {
    
    @Override
    protected BaseDollEntity createDollEntity(Level level, double x, double y, double z) {
        return new SteveDollEntity(level, x, y, z);
    }
}

