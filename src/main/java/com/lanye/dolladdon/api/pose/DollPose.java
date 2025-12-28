package com.lanye.dolladdon.api.pose;

/**
 * 玩偶姿态接口
 * 定义玩偶模型各部分的旋转角度
 * 
 * 开发者可以实现此接口来创建自定义姿态
 */
public interface DollPose {
    /**
     * 获取姿态名称
     * @return 姿态名称
     */
    String getName();
    
    /**
     * 获取姿态的中文显示名称
     * @return 中文名称，如果没有则返回英文名称
     */
    default String getDisplayName() {
        return getName();
    }
    
    /**
     * 获取头部旋转角度（弧度）
     * @return [x, y, z] 旋转角度
     */
    float[] getHeadRotation();
    
    /**
     * 获取帽子旋转角度（弧度）
     * @return [x, y, z] 旋转角度
     */
    float[] getHatRotation();
    
    /**
     * 获取身体旋转角度（弧度）
     * @return [x, y, z] 旋转角度
     */
    float[] getBodyRotation();
    
    /**
     * 获取右臂旋转角度（弧度）
     * @return [x, y, z] 旋转角度
     */
    float[] getRightArmRotation();
    
    /**
     * 获取左臂旋转角度（弧度）
     * @return [x, y, z] 旋转角度
     */
    float[] getLeftArmRotation();
    
    /**
     * 获取右腿旋转角度（弧度）
     * @return [x, y, z] 旋转角度
     */
    float[] getRightLegRotation();
    
    /**
     * 获取左腿旋转角度（弧度）
     * @return [x, y, z] 旋转角度
     */
    float[] getLeftLegRotation();
    
    /**
     * 获取位置偏移（相对于默认位置）
     * @return [x, y, z] 位置偏移
     */
    default float[] getPosition() {
        return new float[]{0.0f, 0.0f, 0.0f};
    }
    
    /**
     * 获取缩放大小（相对于默认大小）
     * @return [x, y, z] 缩放大小，默认[1.0, 1.0, 1.0]
     */
    default float[] getScale() {
        return new float[]{1.0f, 1.0f, 1.0f};
    }
    
    /**
     * 获取头部位置偏移
     * @return [x, y, z] 位置偏移，默认[0.0, 0.0, 0.0]
     */
    default float[] getHeadPosition() {
        return new float[]{0.0f, 0.0f, 0.0f};
    }
    
    /**
     * 获取头部缩放大小
     * @return [x, y, z] 缩放大小，默认[1.0, 1.0, 1.0]
     */
    default float[] getHeadScale() {
        return new float[]{1.0f, 1.0f, 1.0f};
    }
    
    /**
     * 获取帽子位置偏移
     * @return [x, y, z] 位置偏移，默认[0.0, 0.0, 0.0]
     */
    default float[] getHatPosition() {
        return new float[]{0.0f, 0.0f, 0.0f};
    }
    
    /**
     * 获取帽子缩放大小
     * @return [x, y, z] 缩放大小，默认[1.0, 1.0, 1.0]
     */
    default float[] getHatScale() {
        return new float[]{1.0f, 1.0f, 1.0f};
    }
    
    /**
     * 获取身体位置偏移
     * @return [x, y, z] 位置偏移，默认[0.0, 0.0, 0.0]
     */
    default float[] getBodyPosition() {
        return new float[]{0.0f, 0.0f, 0.0f};
    }
    
    /**
     * 获取身体缩放大小
     * @return [x, y, z] 缩放大小，默认[1.0, 1.0, 1.0]
     */
    default float[] getBodyScale() {
        return new float[]{1.0f, 1.0f, 1.0f};
    }
    
    /**
     * 获取右臂位置偏移
     * @return [x, y, z] 位置偏移，默认[0.0, 0.0, 0.0]
     */
    default float[] getRightArmPosition() {
        return new float[]{0.0f, 0.0f, 0.0f};
    }
    
    /**
     * 获取右臂缩放大小
     * @return [x, y, z] 缩放大小，默认[1.0, 1.0, 1.0]
     */
    default float[] getRightArmScale() {
        return new float[]{1.0f, 1.0f, 1.0f};
    }
    
    /**
     * 获取左臂位置偏移
     * @return [x, y, z] 位置偏移，默认[0.0, 0.0, 0.0]
     */
    default float[] getLeftArmPosition() {
        return new float[]{0.0f, 0.0f, 0.0f};
    }
    
    /**
     * 获取左臂缩放大小
     * @return [x, y, z] 缩放大小，默认[1.0, 1.0, 1.0]
     */
    default float[] getLeftArmScale() {
        return new float[]{1.0f, 1.0f, 1.0f};
    }
    
    /**
     * 获取右腿位置偏移
     * @return [x, y, z] 位置偏移，默认[0.0, 0.0, 0.0]
     */
    default float[] getRightLegPosition() {
        return new float[]{0.0f, 0.0f, 0.0f};
    }
    
    /**
     * 获取右腿缩放大小
     * @return [x, y, z] 缩放大小，默认[1.0, 1.0, 1.0]
     */
    default float[] getRightLegScale() {
        return new float[]{1.0f, 1.0f, 1.0f};
    }
    
    /**
     * 获取左腿位置偏移
     * @return [x, y, z] 位置偏移，默认[0.0, 0.0, 0.0]
     */
    default float[] getLeftLegPosition() {
        return new float[]{0.0f, 0.0f, 0.0f};
    }
    
    /**
     * 获取左腿缩放大小
     * @return [x, y, z] 缩放大小，默认[1.0, 1.0, 1.0]
     */
    default float[] getLeftLegScale() {
        return new float[]{1.0f, 1.0f, 1.0f};
    }
}

