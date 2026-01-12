package com.lanye.dolladdon.model;

import net.minecraft.client.renderer.RenderType;

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
    private final net.minecraft.client.renderer.MultiBufferSource bufferSource;
    
    /** 渲染类型 */
    private final RenderType renderType;
    
    /**
     * 构造函数
     * 
     * @param light 光照信息
     * @param overlay 覆盖纹理
     * @param bufferSource 缓冲区源
     * @param renderType 渲染类型
     */
    public RenderContextInfo(int light, int overlay, net.minecraft.client.renderer.MultiBufferSource bufferSource, RenderType renderType) {
        this.light = light;
        this.overlay = overlay;
        this.bufferSource = bufferSource;
        this.renderType = renderType;
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
     * 获取缓冲区源
     * 
     * @return 缓冲区源
     */
    public net.minecraft.client.renderer.MultiBufferSource getBufferSource() {
        return bufferSource;
    }
    
    /**
     * 获取渲染类型
     * 
     * @return 渲染类型
     */
    public RenderType getRenderType() {
        return renderType;
    }
    
    /**
     * 获取顶点消费者
     * 
     * @return 顶点消费者
     */
    public com.mojang.blaze3d.vertex.VertexConsumer getVertexConsumer() {
        return bufferSource.getBuffer(renderType);
    }
    
    @Override
    public String toString() {
        return String.format(
            "RenderContextInfo{light=%d, overlay=%d, bufferSource=%s, renderType=%s}",
            light, overlay, bufferSource != null ? bufferSource.getClass().getSimpleName() : "null",
            renderType != null ? renderType.toString() : "null"
        );
    }
}
