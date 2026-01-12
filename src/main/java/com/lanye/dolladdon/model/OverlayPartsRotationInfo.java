package com.lanye.dolladdon.model;

/**
 * 外层部件旋转信息类
 * 用于存储和传递外层部件（袖子、裤子、夹克）的旋转角度
 * 
 * <p>此类封装了所有外层部件的旋转参数，使代码更加清晰和易于维护。</p>
 */
public class OverlayPartsRotationInfo {
    /** 身体旋转 [X, Y, Z]（度） */
    private final float[] bodyRotation;
    
    /** 左臂旋转 [X, Y, Z]（度） */
    private final float[] leftArmRotation;
    
    /** 右臂旋转 [X, Y, Z]（度） */
    private final float[] rightArmRotation;
    
    /** 左腿旋转 [X, Y, Z]（度） */
    private final float[] leftLegRotation;
    
    /** 右腿旋转 [X, Y, Z]（度） */
    private final float[] rightLegRotation;
    
    /**
     * 构造函数
     * 
     * @param bodyRotX 身体的X旋转（度）
     * @param bodyRotY 身体的Y旋转（度）
     * @param bodyRotZ 身体的Z旋转（度）
     * @param leftArmRotX 左臂的X旋转（度）
     * @param leftArmRotY 左臂的Y旋转（度）
     * @param leftArmRotZ 左臂的Z旋转（度）
     * @param rightArmRotX 右臂的X旋转（度）
     * @param rightArmRotY 右臂的Y旋转（度）
     * @param rightArmRotZ 右臂的Z旋转（度）
     * @param leftLegRotX 左腿的X旋转（度）
     * @param leftLegRotY 左腿的Y旋转（度）
     * @param leftLegRotZ 左腿的Z旋转（度）
     * @param rightLegRotX 右腿的X旋转（度）
     * @param rightLegRotY 右腿的Y旋转（度）
     * @param rightLegRotZ 右腿的Z旋转（度）
     */
    public OverlayPartsRotationInfo(
            float bodyRotX, float bodyRotY, float bodyRotZ,
            float leftArmRotX, float leftArmRotY, float leftArmRotZ,
            float rightArmRotX, float rightArmRotY, float rightArmRotZ,
            float leftLegRotX, float leftLegRotY, float leftLegRotZ,
            float rightLegRotX, float rightLegRotY, float rightLegRotZ) {
        this.bodyRotation = new float[]{bodyRotX, bodyRotY, bodyRotZ};
        this.leftArmRotation = new float[]{leftArmRotX, leftArmRotY, leftArmRotZ};
        this.rightArmRotation = new float[]{rightArmRotX, rightArmRotY, rightArmRotZ};
        this.leftLegRotation = new float[]{leftLegRotX, leftLegRotY, leftLegRotZ};
        this.rightLegRotation = new float[]{rightLegRotX, rightLegRotY, rightLegRotZ};
    }
    
    /**
     * 使用数组构造
     * 
     * @param bodyRotation 身体旋转 [X, Y, Z]（度）
     * @param leftArmRotation 左臂旋转 [X, Y, Z]（度）
     * @param rightArmRotation 右臂旋转 [X, Y, Z]（度）
     * @param leftLegRotation 左腿旋转 [X, Y, Z]（度）
     * @param rightLegRotation 右腿旋转 [X, Y, Z]（度）
     */
    public OverlayPartsRotationInfo(
            float[] bodyRotation,
            float[] leftArmRotation,
            float[] rightArmRotation,
            float[] leftLegRotation,
            float[] rightLegRotation) {
        this.bodyRotation = bodyRotation != null ? bodyRotation.clone() : new float[]{0, 0, 0};
        this.leftArmRotation = leftArmRotation != null ? leftArmRotation.clone() : new float[]{0, 0, 0};
        this.rightArmRotation = rightArmRotation != null ? rightArmRotation.clone() : new float[]{0, 0, 0};
        this.leftLegRotation = leftLegRotation != null ? leftLegRotation.clone() : new float[]{0, 0, 0};
        this.rightLegRotation = rightLegRotation != null ? rightLegRotation.clone() : new float[]{0, 0, 0};
    }
    
    /**
     * 创建零旋转信息（所有部件旋转为0）
     * 
     * @return 零旋转信息对象
     */
    public static OverlayPartsRotationInfo zero() {
        return new OverlayPartsRotationInfo(
            0, 0, 0,
            0, 0, 0,
            0, 0, 0,
            0, 0, 0,
            0, 0, 0
        );
    }
    
    /**
     * 获取身体旋转
     * 
     * @return 身体旋转数组 [X, Y, Z]（度）
     */
    public float[] getBodyRotation() {
        return bodyRotation.clone();
    }
    
    /**
     * 获取左臂旋转
     * 
     * @return 左臂旋转数组 [X, Y, Z]（度）
     */
    public float[] getLeftArmRotation() {
        return leftArmRotation.clone();
    }
    
    /**
     * 获取右臂旋转
     * 
     * @return 右臂旋转数组 [X, Y, Z]（度）
     */
    public float[] getRightArmRotation() {
        return rightArmRotation.clone();
    }
    
    /**
     * 获取左腿旋转
     * 
     * @return 左腿旋转数组 [X, Y, Z]（度）
     */
    public float[] getLeftLegRotation() {
        return leftLegRotation.clone();
    }
    
    /**
     * 获取右腿旋转
     * 
     * @return 右腿旋转数组 [X, Y, Z]（度）
     */
    public float[] getRightLegRotation() {
        return rightLegRotation.clone();
    }
    
    /**
     * 获取身体旋转（内部使用，不克隆，直接返回内部数组引用）
     * 注意：此方法返回内部数组的直接引用，不应修改返回的数组
     * 
     * @return 身体旋转数组 [X, Y, Z]（度）
     */
    public float[] getBodyRotationInternal() {
        return bodyRotation;
    }
    
    /**
     * 获取左臂旋转（内部使用，不克隆，直接返回内部数组引用）
     * 注意：此方法返回内部数组的直接引用，不应修改返回的数组
     * 
     * @return 左臂旋转数组 [X, Y, Z]（度）
     */
    public float[] getLeftArmRotationInternal() {
        return leftArmRotation;
    }
    
    /**
     * 获取右臂旋转（内部使用，不克隆，直接返回内部数组引用）
     * 注意：此方法返回内部数组的直接引用，不应修改返回的数组
     * 
     * @return 右臂旋转数组 [X, Y, Z]（度）
     */
    public float[] getRightArmRotationInternal() {
        return rightArmRotation;
    }
    
    /**
     * 获取左腿旋转（内部使用，不克隆，直接返回内部数组引用）
     * 注意：此方法返回内部数组的直接引用，不应修改返回的数组
     * 
     * @return 左腿旋转数组 [X, Y, Z]（度）
     */
    public float[] getLeftLegRotationInternal() {
        return leftLegRotation;
    }
    
    /**
     * 获取右腿旋转（内部使用，不克隆，直接返回内部数组引用）
     * 注意：此方法返回内部数组的直接引用，不应修改返回的数组
     * 
     * @return 右腿旋转数组 [X, Y, Z]（度）
     */
    public float[] getRightLegRotationInternal() {
        return rightLegRotation;
    }
    
    @Override
    public String toString() {
        return String.format(
            "OverlayPartsRotationInfo{body=[%.2f, %.2f, %.2f], leftArm=[%.2f, %.2f, %.2f], rightArm=[%.2f, %.2f, %.2f], leftLeg=[%.2f, %.2f, %.2f], rightLeg=[%.2f, %.2f, %.2f]}",
            bodyRotation[0], bodyRotation[1], bodyRotation[2],
            leftArmRotation[0], leftArmRotation[1], leftArmRotation[2],
            rightArmRotation[0], rightArmRotation[1], rightArmRotation[2],
            leftLegRotation[0], leftLegRotation[1], leftLegRotation[2],
            rightLegRotation[0], rightLegRotation[1], rightLegRotation[2]
        );
    }
}
