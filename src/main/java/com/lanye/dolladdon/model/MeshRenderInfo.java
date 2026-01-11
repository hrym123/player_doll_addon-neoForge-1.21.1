package com.lanye.dolladdon.model;

import net.minecraft.client.model.ModelPart;

/**
 * 3D网格渲染信息类
 * 用于封装单个3D网格部件渲染所需的所有信息
 * 
 * <p>此类封装了3D网格渲染参数，使代码更加清晰和易于维护。</p>
 */
public class MeshRenderInfo {
    /** 模型部件 */
    private final ModelPart modelPart;
    
    /** 3D网格对象 */
    private final Object mesh;
    
    /** 偏移提供者名称（用于日志） */
    private final String offsetProviderName;
    
    /** 渲染上下文信息 */
    private final RenderContextInfo contextInfo;
    
    /** 部件变换信息（包含位置、缩放、旋转） */
    private final PartTransformInfo transformInfo;
    
    /**
     * 构造函数
     * 
     * @param modelPart 模型部件
     * @param mesh 3D网格对象
     * @param offsetProviderName 偏移提供者名称
     * @param contextInfo 渲染上下文信息
     * @param transformInfo 部件变换信息
     */
    public MeshRenderInfo(ModelPart modelPart,
                         Object mesh,
                         String offsetProviderName,
                         RenderContextInfo contextInfo,
                         PartTransformInfo transformInfo) {
        this.modelPart = modelPart;
        this.mesh = mesh;
        this.offsetProviderName = offsetProviderName;
        this.contextInfo = contextInfo;
        this.transformInfo = transformInfo;
    }
    
    /**
     * 从数组创建（便捷构造方法）
     * 
     * @param modelPart 模型部件
     * @param mesh 3D网格对象
     * @param offsetProviderName 偏移提供者名称
     * @param light 光照信息
     * @param overlay 覆盖纹理
     * @param vertexConsumer 顶点消费者
     * @param position 位置 [X, Y, Z]
     * @param scale 缩放 [X, Y, Z]
     * @param rotation 旋转 [X, Y, Z]（弧度）
     * @return 3D网格渲染信息对象
     */
    public static MeshRenderInfo of(ModelPart modelPart,
                                    Object mesh,
                                    String offsetProviderName,
                                    int light,
                                    int overlay,
                                    net.minecraft.client.render.VertexConsumer vertexConsumer,
                                    float[] position,
                                    float[] scale,
                                    float[] rotation) {
        RenderContextInfo contextInfo = new RenderContextInfo(light, overlay, vertexConsumer);
        PartTransformInfo transformInfo = PartTransformInfo.of(position, scale, rotation);
        return new MeshRenderInfo(modelPart, mesh, offsetProviderName, contextInfo, transformInfo);
    }
    
    /**
     * 获取模型部件
     * 
     * @return 模型部件
     */
    public ModelPart getModelPart() {
        return modelPart;
    }
    
    /**
     * 获取3D网格对象
     * 
     * @return 3D网格对象
     */
    public Object getMesh() {
        return mesh;
    }
    
    /**
     * 获取偏移提供者名称
     * 
     * @return 偏移提供者名称
     */
    public String getOffsetProviderName() {
        return offsetProviderName;
    }
    
    /**
     * 获取渲染上下文信息
     * 
     * @return 渲染上下文信息
     */
    public RenderContextInfo getContextInfo() {
        return contextInfo;
    }
    
    /**
     * 获取部件变换信息
     * 
     * @return 部件变换信息
     */
    public PartTransformInfo getTransformInfo() {
        return transformInfo;
    }
    
    @Override
    public String toString() {
        return String.format(
            "MeshRenderInfo{part=%s, mesh=%s, name='%s', context=%s, transform=%s}",
            modelPart != null ? modelPart.getClass().getSimpleName() : "null",
            mesh != null ? mesh.getClass().getSimpleName() : "null",
            offsetProviderName,
            contextInfo,
            transformInfo
        );
    }
}
