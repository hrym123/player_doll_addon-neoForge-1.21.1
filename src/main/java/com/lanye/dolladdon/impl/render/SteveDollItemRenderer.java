package com.lanye.dolladdon.impl.render;

import com.lanye.dolladdon.base.render.BaseDollItemRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/**
 * 史蒂夫玩偶物品渲染器
 * 固定使用Steve模型（粗手臂）和Steve默认皮肤
 */
public class SteveDollItemRenderer extends BaseDollItemRenderer {
    
    public SteveDollItemRenderer(MinecraftClient client, @SuppressWarnings("unused") Object unused) {
        super(client, false); // false 表示使用粗手臂模型（Steve）
    }
    
    @Override
    protected Identifier getSkinLocation() {
        return PlayerSkinUtil.getSteveSkin();
    }
}

