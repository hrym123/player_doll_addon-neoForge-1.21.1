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

