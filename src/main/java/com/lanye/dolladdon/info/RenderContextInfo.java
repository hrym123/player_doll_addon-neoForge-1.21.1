package com.lanye.dolladdon.info;

import net.minecraft.client.render.VertexConsumer;

/**
 * 渲染上下文信息类
 * 用于存储和传递渲染所需的上下文参数
 * 
 * <p>此类封装了渲染时的光照、覆盖纹理和顶点消费者等参数，使代码更加清晰和易于维护。</p>
 */
public class RenderContextInfo {
    /** 光照信息 */
    private final int light;
    
    /** 覆盖纹理 */
    private final int overlay;
    
    /** 顶点消费者 */
    private final VertexConsumer vertexConsumer;
    
    /**
     * 构造函数
     * 
     * @param light 光照信息
     * @param overlay 覆盖纹理
     * @param vertexConsumer 顶点消费者
     */
    public RenderContextInfo(int light, int overlay, VertexConsumer vertexConsumer) {
        this.light = light;
        this.overlay = overlay;
        this.vertexConsumer = vertexConsumer;
    }
    
    /**
     * 获取光照信息
     * 
     * @return 光照信息
     */
    public int getLight() {
        return light;
    }
    
    /**
     * 获取覆盖纹理
     * 
     * @return 覆盖纹理
     */
    public int getOverlay() {
        return overlay;
    }
    
    /**
     * 获取顶点消费者
     * 
     * @return 顶点消费者
     */
    public VertexConsumer getVertexConsumer() {
        return vertexConsumer;
    }
    
    @Override
    public String toString() {
        return String.format(
            "RenderContextInfo{light=%d, overlay=%d, vertexConsumer=%s}",
            light, overlay, vertexConsumer != null ? vertexConsumer.getClass().getSimpleName() : "null"
        );
    }
}
