package com.lanye.dolladdon.api.pose;

/**
 * 简单的玩偶姿态实现
 * 使用数组存储各部分的旋转角度
 */
public class SimpleDollPose implements DollPose {
    private final String name;
    private final String displayName;
    private final float[] headRotation;
    private final float[] hatRotation;
    private final float[] bodyRotation;
    private final float[] rightArmRotation;
    private final float[] leftArmRotation;
    private final float[] rightLegRotation;
    private final float[] leftLegRotation;
    private final float[] position;
    private final float[] scale;
    
    // 各部件的位置和缩放
    private final float[] headPosition;
    private final float[] headScale;
    private final float[] hatPosition;
    private final float[] hatScale;
    private final float[] bodyPosition;
    private final float[] bodyScale;
    private final float[] rightArmPosition;
    private final float[] rightArmScale;
    private final float[] leftArmPosition;
    private final float[] leftArmScale;
    private final float[] rightLegPosition;
    private final float[] rightLegScale;
    private final float[] leftLegPosition;
    private final float[] leftLegScale;
    
    public SimpleDollPose(String name,
                         float[] headRotation,
                         float[] hatRotation,
                         float[] bodyRotation,
                         float[] rightArmRotation,
                         float[] leftArmRotation,
                         float[] rightLegRotation,
                         float[] leftLegRotation) {
        this(name, null, headRotation, hatRotation, bodyRotation, rightArmRotation, leftArmRotation, rightLegRotation, leftLegRotation, null, null);
    }
    
    public SimpleDollPose(String name,
                         String displayName,
                         float[] headRotation,
                         float[] hatRotation,
                         float[] bodyRotation,
                         float[] rightArmRotation,
                         float[] leftArmRotation,
                         float[] rightLegRotation,
                         float[] leftLegRotation) {
        this(name, displayName, headRotation, hatRotation, bodyRotation, rightArmRotation, leftArmRotation, rightLegRotation, leftLegRotation, null, null);
    }
    
    public SimpleDollPose(String name,
                         String displayName,
                         float[] headRotation,
                         float[] hatRotation,
                         float[] bodyRotation,
                         float[] rightArmRotation,
                         float[] leftArmRotation,
                         float[] rightLegRotation,
                         float[] leftLegRotation,
                         float[] position,
                         float[] scale) {
        this(name, displayName, headRotation, hatRotation, bodyRotation, rightArmRotation, leftArmRotation, rightLegRotation, leftLegRotation, position, scale,
             null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
    
    public SimpleDollPose(String name,
                         String displayName,
                         float[] headRotation,
                         float[] hatRotation,
                         float[] bodyRotation,
                         float[] rightArmRotation,
                         float[] leftArmRotation,
                         float[] rightLegRotation,
                         float[] leftLegRotation,
                         float[] position,
                         float[] scale,
                         float[] headPosition,
                         float[] headScale,
                         float[] hatPosition,
                         float[] hatScale,
                         float[] bodyPosition,
                         float[] bodyScale,
                         float[] rightArmPosition,
                         float[] rightArmScale,
                         float[] leftArmPosition,
                         float[] leftArmScale,
                         float[] rightLegPosition,
                         float[] rightLegScale,
                         float[] leftLegPosition,
                         float[] leftLegScale) {
        this.name = name;
        this.displayName = displayName != null ? displayName : name;
        this.headRotation = headRotation != null ? headRotation : new float[]{0, 0, 0};
        this.hatRotation = hatRotation != null ? hatRotation : new float[]{0, 0, 0};
        this.bodyRotation = bodyRotation != null ? bodyRotation : new float[]{0, 0, 0};
        this.rightArmRotation = rightArmRotation != null ? rightArmRotation : new float[]{0, 0, 0};
        this.leftArmRotation = leftArmRotation != null ? leftArmRotation : new float[]{0, 0, 0};
        this.rightLegRotation = rightLegRotation != null ? rightLegRotation : new float[]{0, 0, 0};
        this.leftLegRotation = leftLegRotation != null ? leftLegRotation : new float[]{0, 0, 0};
        this.position = position != null ? position : new float[]{0.0f, 0.0f, 0.0f};
        this.scale = scale != null ? scale : new float[]{1.0f, 1.0f, 1.0f};
        
        // 各部件的位置和缩放
        this.headPosition = headPosition != null ? headPosition : new float[]{0.0f, 0.0f, 0.0f};
        this.headScale = headScale != null ? headScale : new float[]{1.0f, 1.0f, 1.0f};
        this.hatPosition = hatPosition != null ? hatPosition : new float[]{0.0f, 0.0f, 0.0f};
        this.hatScale = hatScale != null ? hatScale : new float[]{1.0f, 1.0f, 1.0f};
        this.bodyPosition = bodyPosition != null ? bodyPosition : new float[]{0.0f, 0.0f, 0.0f};
        this.bodyScale = bodyScale != null ? bodyScale : new float[]{1.0f, 1.0f, 1.0f};
        this.rightArmPosition = rightArmPosition != null ? rightArmPosition : new float[]{0.0f, 0.0f, 0.0f};
        this.rightArmScale = rightArmScale != null ? rightArmScale : new float[]{1.0f, 1.0f, 1.0f};
        this.leftArmPosition = leftArmPosition != null ? leftArmPosition : new float[]{0.0f, 0.0f, 0.0f};
        this.leftArmScale = leftArmScale != null ? leftArmScale : new float[]{1.0f, 1.0f, 1.0f};
        this.rightLegPosition = rightLegPosition != null ? rightLegPosition : new float[]{0.0f, 0.0f, 0.0f};
        this.rightLegScale = rightLegScale != null ? rightLegScale : new float[]{1.0f, 1.0f, 1.0f};
        this.leftLegPosition = leftLegPosition != null ? leftLegPosition : new float[]{0.0f, 0.0f, 0.0f};
        this.leftLegScale = leftLegScale != null ? leftLegScale : new float[]{1.0f, 1.0f, 1.0f};
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public float[] getHeadRotation() {
        return headRotation.clone();
    }
    
    @Override
    public float[] getHatRotation() {
        return hatRotation.clone();
    }
    
    @Override
    public float[] getBodyRotation() {
        return bodyRotation.clone();
    }
    
    @Override
    public float[] getRightArmRotation() {
        return rightArmRotation.clone();
    }
    
    @Override
    public float[] getLeftArmRotation() {
        return leftArmRotation.clone();
    }
    
    @Override
    public float[] getRightLegRotation() {
        return rightLegRotation.clone();
    }
    
    @Override
    public float[] getLeftLegRotation() {
        return leftLegRotation.clone();
    }
    
    @Override
    public float[] getPosition() {
        return position.clone();
    }
    
    @Override
    public float[] getScale() {
        return scale.clone();
    }
    
    @Override
    public float[] getHeadPosition() {
        return headPosition.clone();
    }
    
    @Override
    public float[] getHeadScale() {
        return headScale.clone();
    }
    
    @Override
    public float[] getHatPosition() {
        return hatPosition.clone();
    }
    
    @Override
    public float[] getHatScale() {
        return hatScale.clone();
    }
    
    @Override
    public float[] getBodyPosition() {
        return bodyPosition.clone();
    }
    
    @Override
    public float[] getBodyScale() {
        return bodyScale.clone();
    }
    
    @Override
    public float[] getRightArmPosition() {
        return rightArmPosition.clone();
    }
    
    @Override
    public float[] getRightArmScale() {
        return rightArmScale.clone();
    }
    
    @Override
    public float[] getLeftArmPosition() {
        return leftArmPosition.clone();
    }
    
    @Override
    public float[] getLeftArmScale() {
        return leftArmScale.clone();
    }
    
    @Override
    public float[] getRightLegPosition() {
        return rightLegPosition.clone();
    }
    
    @Override
    public float[] getRightLegScale() {
        return rightLegScale.clone();
    }
    
    @Override
    public float[] getLeftLegPosition() {
        return leftLegPosition.clone();
    }
    
    @Override
    public float[] getLeftLegScale() {
        return leftLegScale.clone();
    }
    
    /**
     * 创建默认站立姿态
     */
    public static SimpleDollPose createDefaultStandingPose() {
        return new SimpleDollPose(
            "standing",
            "站立",
            new float[]{0, 0, 0},
            new float[]{0, 0, 0},
            new float[]{0, 0, 0},
            new float[]{-0.6981317F, 0, 0}, // 约 -40 度
            new float[]{0.6981317F, 0, 0},  // 约 40 度
            new float[]{0, 0, 0},
            new float[]{0, 0, 0}
        );
    }
}

