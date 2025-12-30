package com.lanye.dolladdon.impl.render;

import com.lanye.dolladdon.base.render.BaseDollItemRenderer;
import com.lanye.dolladdon.util.PlayerSkinUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/**
 * 艾利克斯玩偶物品渲染器
 * 固定使用Alex模型（细手臂）和Alex默认皮肤
 */
public class AlexDollItemRenderer extends BaseDollItemRenderer {
    
    public AlexDollItemRenderer(MinecraftClient client, @SuppressWarnings("unused") Object unused) {
        super(client, true); // true 表示使用细手臂模型（Alex）
    }
    
    @Override
    protected Identifier getSkinLocation() {
        return PlayerSkinUtil.getAlexSkin();
    }
}

