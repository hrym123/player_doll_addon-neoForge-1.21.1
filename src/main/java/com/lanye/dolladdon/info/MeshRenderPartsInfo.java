package com.lanye.dolladdon.info;

/**
 * 3D网格身体各部位渲染信息类
 * 用于封装所有身体部位的3D网格渲染信息
 * 
 * <p>此类封装了所有身体部位的3D网格渲染参数，使代码更加清晰和易于维护。</p>
 */
public class MeshRenderPartsInfo {
    /** 头部（帽子）变换信息 */
    private final PartTransformInfo hat;
    
    /** 右臂变换信息 */
    private final PartTransformInfo rightArm;
    
    /** 左臂变换信息 */
    private final PartTransformInfo leftArm;
    
    /** 身体变换信息 */
    private final PartTransformInfo body;
    
    /** 右腿变换信息 */
    private final PartTransformInfo rightLeg;
    
    /** 左腿变换信息 */
    private final PartTransformInfo leftLeg;
    
    /**
     * 构造函数
     * 
     * @param hat 头部（帽子）变换信息
     * @param rightArm 右臂变换信息
     * @param leftArm 左臂变换信息
     * @param body 身体变换信息
     * @param rightLeg 右腿变换信息
     * @param leftLeg 左腿变换信息
     */
    public MeshRenderPartsInfo(PartTransformInfo hat,
                               PartTransformInfo rightArm,
                               PartTransformInfo leftArm,
                               PartTransformInfo body,
                               PartTransformInfo rightLeg,
                               PartTransformInfo leftLeg) {
        this.hat = hat;
        this.rightArm = rightArm;
        this.leftArm = leftArm;
        this.body = body;
        this.rightLeg = rightLeg;
        this.leftLeg = leftLeg;
    }
    
    /**
     * 从数组创建（便捷构造方法）
     * 
     * @param hatPosition 头部位置 [X, Y, Z]
     * @param hatScale 头部缩放 [X, Y, Z]
     * @param hatRotation 头部旋转 [X, Y, Z]（弧度）
     * @param rightArmPosition 右臂位置 [X, Y, Z]
     * @param rightArmScale 右臂缩放 [X, Y, Z]
     * @param rightArmRotation 右臂旋转 [X, Y, Z]（弧度）
     * @param leftArmPosition 左臂位置 [X, Y, Z]
     * @param leftArmScale 左臂缩放 [X, Y, Z]
     * @param leftArmRotation 左臂旋转 [X, Y, Z]（弧度）
     * @param bodyPosition 身体位置 [X, Y, Z]
     * @param bodyScale 身体缩放 [X, Y, Z]
     * @param bodyRotation 身体旋转 [X, Y, Z]（弧度）
     * @param rightLegPosition 右腿位置 [X, Y, Z]
     * @param rightLegScale 右腿缩放 [X, Y, Z]
     * @param rightLegRotation 右腿旋转 [X, Y, Z]（弧度）
     * @param leftLegPosition 左腿位置 [X, Y, Z]
     * @param leftLegScale 左腿缩放 [X, Y, Z]
     * @param leftLegRotation 左腿旋转 [X, Y, Z]（弧度）
     * @return 3D网格身体各部位渲染信息对象
     */
    public static MeshRenderPartsInfo of(
            float[] hatPosition, float[] hatScale, float[] hatRotation,
            float[] rightArmPosition, float[] rightArmScale, float[] rightArmRotation,
            float[] leftArmPosition, float[] leftArmScale, float[] leftArmRotation,
            float[] bodyPosition, float[] bodyScale, float[] bodyRotation,
            float[] rightLegPosition, float[] rightLegScale, float[] rightLegRotation,
            float[] leftLegPosition, float[] leftLegScale, float[] leftLegRotation) {
        return new MeshRenderPartsInfo(
            PartTransformInfo.of(hatPosition, hatScale, hatRotation),
            PartTransformInfo.of(rightArmPosition, rightArmScale, rightArmRotation),
            PartTransformInfo.of(leftArmPosition, leftArmScale, leftArmRotation),
            PartTransformInfo.of(bodyPosition, bodyScale, bodyRotation),
            PartTransformInfo.of(rightLegPosition, rightLegScale, rightLegRotation),
            PartTransformInfo.of(leftLegPosition, leftLegScale, leftLegRotation)
        );
    }
    
    /**
     * 获取头部（帽子）变换信息
     * 
     * @return 头部（帽子）变换信息
     */
    public PartTransformInfo getHat() {
        return hat;
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
     * 获取左臂变换信息
     * 
     * @return 左臂变换信息
     */
    public PartTransformInfo getLeftArm() {
        return leftArm;
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
     * 获取右腿变换信息
     * 
     * @return 右腿变换信息
     */
    public PartTransformInfo getRightLeg() {
        return rightLeg;
    }
    
    /**
     * 获取左腿变换信息
     * 
     * @return 左腿变换信息
     */
    public PartTransformInfo getLeftLeg() {
        return leftLeg;
    }
    
    @Override
    public String toString() {
        return String.format(
            "MeshRenderPartsInfo{hat=%s, rightArm=%s, leftArm=%s, body=%s, rightLeg=%s, leftLeg=%s}",
            hat, rightArm, leftArm, body, rightLeg, leftLeg
        );
    }
}
