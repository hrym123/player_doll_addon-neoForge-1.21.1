package com.lanye.dolladdon.model;

/**
 * 身体各部位变换信息类
 * 用于存储和传递身体所有部位的变换信息（位置、缩放）
 * 
 * <p>此类封装了身体所有部位的变换参数，使代码更加清晰和易于维护。</p>
 */
public class BodyPartsTransformInfo {
    /** 头部变换信息 */
    private final PartTransformInfo head;
    
    /** 身体变换信息 */
    private final PartTransformInfo body;
    
    /** 左臂变换信息 */
    private final PartTransformInfo leftArm;
    
    /** 右臂变换信息 */
    private final PartTransformInfo rightArm;
    
    /** 左腿变换信息 */
    private final PartTransformInfo leftLeg;
    
    /** 右腿变换信息 */
    private final PartTransformInfo rightLeg;
    
    /**
     * 构造函数
     * 
     * @param head 头部变换信息
     * @param body 身体变换信息
     * @param leftArm 左臂变换信息
     * @param rightArm 右臂变换信息
     * @param leftLeg 左腿变换信息
     * @param rightLeg 右腿变换信息
     */
    public BodyPartsTransformInfo(PartTransformInfo head, PartTransformInfo body,
                                   PartTransformInfo leftArm, PartTransformInfo rightArm,
                                   PartTransformInfo leftLeg, PartTransformInfo rightLeg) {
        this.head = head;
        this.body = body;
        this.leftArm = leftArm;
        this.rightArm = rightArm;
        this.leftLeg = leftLeg;
        this.rightLeg = rightLeg;
    }
    
    /**
     * 从数组创建（便捷构造方法）
     * 
     * @param headPosition 头部位置 [X, Y, Z]
     * @param headScale 头部缩放 [X, Y, Z]
     * @param bodyPosition 身体位置 [X, Y, Z]
     * @param bodyScale 身体缩放 [X, Y, Z]
     * @param leftArmPosition 左臂位置 [X, Y, Z]
     * @param leftArmScale 左臂缩放 [X, Y, Z]
     * @param rightArmPosition 右臂位置 [X, Y, Z]
     * @param rightArmScale 右臂缩放 [X, Y, Z]
     * @param leftLegPosition 左腿位置 [X, Y, Z]
     * @param leftLegScale 左腿缩放 [X, Y, Z]
     * @param rightLegPosition 右腿位置 [X, Y, Z]
     * @param rightLegScale 右腿缩放 [X, Y, Z]
     * @return 身体各部位变换信息对象
     */
    public static BodyPartsTransformInfo of(
            float[] headPosition, float[] headScale,
            float[] bodyPosition, float[] bodyScale,
            float[] leftArmPosition, float[] leftArmScale,
            float[] rightArmPosition, float[] rightArmScale,
            float[] leftLegPosition, float[] leftLegScale,
            float[] rightLegPosition, float[] rightLegScale) {
        return new BodyPartsTransformInfo(
            PartTransformInfo.of(headPosition, headScale),
            PartTransformInfo.of(bodyPosition, bodyScale),
            PartTransformInfo.of(leftArmPosition, leftArmScale),
            PartTransformInfo.of(rightArmPosition, rightArmScale),
            PartTransformInfo.of(leftLegPosition, leftLegScale),
            PartTransformInfo.of(rightLegPosition, rightLegScale)
        );
    }
    
    /**
     * 获取头部变换信息
     * 
     * @return 头部变换信息
     */
    public PartTransformInfo getHead() {
        return head;
    }
    
    /**
     * 获取身体变换信息
     * 
     * @return 身体变换信息
     */
    public PartTransformInfo getBody() {
        return body;
    }
    
    /**
     * 获取左臂变换信息
     * 
     * @return 左臂变换信息
     */
    public PartTransformInfo getLeftArm() {
        return leftArm;
    }
    
    /**
     * 获取右臂变换信息
     * 
     * @return 右臂变换信息
     */
    public PartTransformInfo getRightArm() {
        return rightArm;
    }
    
    /**
     * 获取左腿变换信息
     * 
     * @return 左腿变换信息
     */
    public PartTransformInfo getLeftLeg() {
        return leftLeg;
    }
    
    /**
     * 获取右腿变换信息
     * 
     * @return 右腿变换信息
     */
    public PartTransformInfo getRightLeg() {
        return rightLeg;
    }
    
    @Override
    public String toString() {
        return "BodyPartsTransformInfo{head=" + head + ", body=" + body + 
               ", leftArm=" + leftArm + ", rightArm=" + rightArm + 
               ", leftLeg=" + leftLeg + ", rightLeg=" + rightLeg + "}";
    }
}
