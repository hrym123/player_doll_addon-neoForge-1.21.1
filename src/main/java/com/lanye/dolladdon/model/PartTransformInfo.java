package com.lanye.dolladdon.model;

/**
 * 部件变换信息类
 * 用于存储和传递部件的位置、缩放和旋转信息
 * 
 * <p>此类封装了部件的变换参数（位置、缩放、旋转），使代码更加清晰和易于维护。</p>
 */
public class PartTransformInfo {
    /** 位置偏移 [X, Y, Z] */
    private final float[] position;
    
    /** 缩放 [X, Y, Z] */
    private final float[] scale;
    
    /** 旋转 [X, Y, Z]（度），可选，如果为null则表示不使用旋转 */
    private final float[] rotation;
    
    /**
     * 构造函数（不包含旋转）
     * 
     * @param position 位置偏移 [X, Y, Z]
     * @param scale 缩放 [X, Y, Z]
     */
    public PartTransformInfo(float[] position, float[] scale) {
        this.position = position != null ? position.clone() : new float[]{0, 0, 0};
        this.scale = scale != null ? scale.clone() : new float[]{1, 1, 1};
        this.rotation = null;
    }
    
    /**
     * 构造函数（包含旋转）
     * 
     * @param position 位置偏移 [X, Y, Z]
     * @param scale 缩放 [X, Y, Z]
     * @param rotation 旋转 [X, Y, Z]（度）
     */
    public PartTransformInfo(float[] position, float[] scale, float[] rotation) {
        this.position = position != null ? position.clone() : new float[]{0, 0, 0};
        this.scale = scale != null ? scale.clone() : new float[]{1, 1, 1};
        this.rotation = rotation != null ? rotation.clone() : null;
    }
    
    /**
     * 创建变换信息（静态工厂方法）
     * 
     * @param position 位置偏移 [X, Y, Z]
     * @param scale 缩放 [X, Y, Z]
     * @return 变换信息对象
     */
    public static PartTransformInfo of(float[] position, float[] scale) {
        return new PartTransformInfo(position, scale);
    }
    
    /**
     * 创建包含旋转的变换信息（静态工厂方法）
     * 
     * @param position 位置偏移 [X, Y, Z]
     * @param scale 缩放 [X, Y, Z]
     * @param rotation 旋转 [X, Y, Z]（度）
     * @return 变换信息对象
     */
    public static PartTransformInfo of(float[] position, float[] scale, float[] rotation) {
        return new PartTransformInfo(position, scale, rotation);
    }
    
    /**
     * 创建零变换信息（位置和旋转为0，缩放为1）
     * 
     * @return 零变换信息对象
     */
    public static PartTransformInfo zero() {
        return new PartTransformInfo(
            new float[]{0, 0, 0},
            new float[]{1, 1, 1},
            new float[]{0, 0, 0}
        );
    }
    
    /**
     * 创建无旋转的零变换信息
     * 
     * @return 零变换信息对象（无旋转）
     */
    public static PartTransformInfo zeroNoRotation() {
        return new PartTransformInfo(
            new float[]{0, 0, 0},
            new float[]{1, 1, 1}
        );
    }
    
    /**
     * 获取位置偏移
     * 
     * @return 位置偏移数组 [X, Y, Z]
     */
    public float[] getPosition() {
        return position.clone();
    }
    
    /**
     * 获取缩放
     * 
     * @return 缩放数组 [X, Y, Z]
     */
    public float[] getScale() {
        return scale.clone();
    }
    
    /**
     * 获取旋转
     * 
     * @return 旋转数组 [X, Y, Z]（度），如果未设置则返回null
     */
    public float[] getRotation() {
        return rotation != null ? rotation.clone() : null;
    }
    
    /**
     * 是否包含旋转信息
     * 
     * @return true 如果包含旋转信息
     */
    public boolean hasRotation() {
        return rotation != null;
    }
    
    /**
     * 获取位置偏移（内部使用，不克隆，直接返回内部数组引用）
     * 注意：此方法返回内部数组的直接引用，不应修改返回的数组
     * 
     * @return 位置偏移数组 [X, Y, Z]
     */
    public float[] getPositionInternal() {
        return position;
    }
    
    /**
     * 获取缩放（内部使用，不克隆，直接返回内部数组引用）
     * 注意：此方法返回内部数组的直接引用，不应修改返回的数组
     * 
     * @return 缩放数组 [X, Y, Z]
     */
    public float[] getScaleInternal() {
        return scale;
    }
    
    /**
     * 获取旋转（内部使用，不克隆，直接返回内部数组引用）
     * 注意：此方法返回内部数组的直接引用，不应修改返回的数组
     * 
     * @return 旋转数组 [X, Y, Z]（度），如果未设置则返回null
     */
    public float[] getRotationInternal() {
        return rotation;
    }
    
    @Override
    public String toString() {
        if (rotation != null) {
            return String.format(
                "PartTransformInfo{position=[%.2f, %.2f, %.2f], scale=[%.2f, %.2f, %.2f], rotation=[%.2f, %.2f, %.2f]}",
                position[0], position[1], position[2],
                scale[0], scale[1], scale[2],
                rotation[0], rotation[1], rotation[2]
            );
        } else {
            return String.format(
                "PartTransformInfo{position=[%.2f, %.2f, %.2f], scale=[%.2f, %.2f, %.2f]}",
                position[0], position[1], position[2],
                scale[0], scale[1], scale[2]
            );
        }
    }
}
