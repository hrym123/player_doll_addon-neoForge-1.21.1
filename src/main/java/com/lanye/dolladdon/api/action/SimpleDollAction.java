package com.lanye.dolladdon.api.action;

import com.lanye.dolladdon.api.pose.DollPose;
import net.minecraft.util.Mth;

/**
 * 简单的玩偶动作实现
 * 支持线性插值在关键帧之间过渡
 */
public class SimpleDollAction implements DollAction {
    private final String name;
    private final boolean looping;
    private final ActionKeyframe[] keyframes;
    private final int duration;
    
    public SimpleDollAction(String name, boolean looping, ActionKeyframe[] keyframes) {
        this.name = name;
        this.looping = looping;
        this.keyframes = keyframes;
        
        // 计算总时长（最后一个关键帧的tick + 1）
        int maxTick = 0;
        for (ActionKeyframe keyframe : keyframes) {
            if (keyframe.getTick() > maxTick) {
                maxTick = keyframe.getTick();
            }
        }
        this.duration = maxTick + 1;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean isLooping() {
        return looping;
    }
    
    @Override
    public int getDuration() {
        return duration;
    }
    
    @Override
    public DollPose getPoseAt(int tick) {
        if (keyframes.length == 0) {
            return null;
        }
        
        // 如果循环，将tick限制在总时长内
        int actualTick = looping ? (tick % duration) : Math.min(tick, duration - 1);
        
        // 如果只有一个关键帧，直接返回
        if (keyframes.length == 1) {
            return keyframes[0].getPose();
        }
        
        // 找到当前tick所在的关键帧区间
        ActionKeyframe prevKeyframe = null;
        ActionKeyframe nextKeyframe = null;
        
        for (int i = 0; i < keyframes.length; i++) {
            if (keyframes[i].getTick() <= actualTick) {
                prevKeyframe = keyframes[i];
            }
            if (keyframes[i].getTick() >= actualTick && nextKeyframe == null) {
                nextKeyframe = keyframes[i];
                break;
            }
        }
        
        // 如果没有找到前一个关键帧，使用第一个
        if (prevKeyframe == null) {
            prevKeyframe = keyframes[0];
        }
        
        // 如果没有找到下一个关键帧，使用最后一个
        if (nextKeyframe == null) {
            nextKeyframe = keyframes[keyframes.length - 1];
        }
        
        // 如果前后关键帧相同，直接返回
        if (prevKeyframe == nextKeyframe) {
            return prevKeyframe.getPose();
        }
        
        // 计算插值比例
        int tickDiff = nextKeyframe.getTick() - prevKeyframe.getTick();
        float t = tickDiff > 0 ? (float)(actualTick - prevKeyframe.getTick()) / tickDiff : 0.0F;
        t = Mth.clamp(t, 0.0F, 1.0F);
        
        // 在两个姿态之间插值
        return interpolatePoses(prevKeyframe.getPose(), nextKeyframe.getPose(), t);
    }
    
    @Override
    public ActionKeyframe[] getKeyframes() {
        return keyframes.clone();
    }
    
    /**
     * 在两个姿态之间插值
     */
    private DollPose interpolatePoses(DollPose pose1, DollPose pose2, float t) {
        return new InterpolatedPose(pose1, pose2, t);
    }
    
    /**
     * 插值姿态的内部实现
     */
    private static class InterpolatedPose implements DollPose {
        private final DollPose pose1;
        private final DollPose pose2;
        private final float t;
        
        public InterpolatedPose(DollPose pose1, DollPose pose2, float t) {
            this.pose1 = pose1;
            this.pose2 = pose2;
            this.t = t;
        }
        
        @Override
        public String getName() {
            return "interpolated";
        }
        
        private float[] interpolate(float[] arr1, float[] arr2) {
            return new float[]{
                Mth.lerp(t, arr1[0], arr2[0]),
                Mth.lerp(t, arr1[1], arr2[1]),
                Mth.lerp(t, arr1[2], arr2[2])
            };
        }
        
        @Override
        public float[] getHeadRotation() {
            return interpolate(pose1.getHeadRotation(), pose2.getHeadRotation());
        }
        
        @Override
        public float[] getHatRotation() {
            return interpolate(pose1.getHatRotation(), pose2.getHatRotation());
        }
        
        @Override
        public float[] getBodyRotation() {
            return interpolate(pose1.getBodyRotation(), pose2.getBodyRotation());
        }
        
        @Override
        public float[] getRightArmRotation() {
            return interpolate(pose1.getRightArmRotation(), pose2.getRightArmRotation());
        }
        
        @Override
        public float[] getLeftArmRotation() {
            return interpolate(pose1.getLeftArmRotation(), pose2.getLeftArmRotation());
        }
        
        @Override
        public float[] getRightLegRotation() {
            return interpolate(pose1.getRightLegRotation(), pose2.getRightLegRotation());
        }
        
        @Override
        public float[] getLeftLegRotation() {
            return interpolate(pose1.getLeftLegRotation(), pose2.getLeftLegRotation());
        }
    }
}

