package com.lanye.dolladdon.impl.render;

import com.lanye.dolladdon.base.render.BaseDollItemRenderer;
import com.lanye.dolladdon.util.resource.PlayerSkinUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

/**
 * 标准玩偶物品渲染器
 * 用于渲染固定的标准玩偶物品（Alex 或 Steve）
 * 通过构造参数区分不同的模型和皮肤
 */
public class StandardDollItemRenderer extends BaseDollItemRenderer {
    private final Identifier skinLocation;
    
    /**
     * 创建标准玩偶物品渲染器
     * 
     * @param client Minecraft 客户端实例
     * @param unused 未使用的参数（用于兼容 BuiltinItemRendererRegistry 的接口）
     * @param isAlexModel true 表示使用 Alex 模型（细手臂），false 表示使用 Steve 模型（粗手臂）
     */
    public StandardDollItemRenderer(MinecraftClient client, @SuppressWarnings("unused") Object unused, boolean isAlexModel) {
        super(client, isAlexModel);
        this.skinLocation = isAlexModel ? PlayerSkinUtil.getAlexSkin() : PlayerSkinUtil.getSteveSkin();
    }
    
    @Override
    protected Identifier getSkinLocation() {
        return skinLocation;
    }
}
