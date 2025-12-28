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
}

