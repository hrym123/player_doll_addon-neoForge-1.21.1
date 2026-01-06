package com.lanye.dolladdon.api.action;

import com.lanye.dolladdon.api.pose.DollPose;
import net.minecraft.util.math.MathHelper;

/**
 * 简单的玩偶动作实现
 * 支持线性插值在关键帧之间过渡
 */
public class SimpleDollAction implements DollAction {
    private final String name;
    private final String displayName;
    private final boolean looping; // 向后兼容字段
    private final ActionMode mode;
    private final ActionKeyframe[] keyframes; // 原始关键帧（相对tick）
    private final ProcessedKeyframe[] processedKeyframes; // 处理后的关键帧（绝对tick）
    private final int duration;
    private final int firstKeyframeDuration; // 第一个关键帧的持续时间（用于从当前姿态插值）
    
    public SimpleDollAction(String name, boolean looping, ActionKeyframe[] keyframes) {
        this(name, null, looping, keyframes);
    }
    
    public SimpleDollAction(String name, String displayName, boolean looping, ActionKeyframe[] keyframes) {
        this(name, displayName, looping ? ActionMode.LOOP : ActionMode.ONCE, keyframes);
    }
    
    public SimpleDollAction(String name, String displayName, ActionMode mode, ActionKeyframe[] keyframes) {
        this.name = name;
        this.displayName = displayName != null ? displayName : name;
        this.mode = mode;
        this.looping = (mode == ActionMode.LOOP); // 向后兼容
        this.keyframes = keyframes;
        
        // 将相对tick转换为绝对tick（累加）
        // tick的含义：从前一个关键帧到这个关键帧需要的时间
        if (keyframes.length == 0) {
            this.processedKeyframes = new ProcessedKeyframe[0];
            this.duration = 0;
            this.firstKeyframeDuration = 0;
        } else {
            this.processedKeyframes = new ProcessedKeyframe[keyframes.length];
            int absoluteTick = 0;
            
            // 第一个关键帧的tick表示从当前姿态到第一个关键帧姿态需要的时间
            int firstTick = keyframes[0].getTick();
            // firstKeyframeDuration需要准确反映原始值（可能是0），用于判断是否需要插值
            this.firstKeyframeDuration = Math.max(firstTick, 0); // 允许为0（表示立即切换）
            // 第一个关键帧的绝对tick：如果firstTick为0，则设为0（立即到达）；否则设为firstTick
            if (firstTick <= 0) {
                // tick=0或负数，表示立即到达第一个关键帧，绝对tick设为0
                absoluteTick = 0;
            } else {
                // tick>0，需要时间过渡，绝对tick设为firstTick
                absoluteTick = firstTick;
            }
            processedKeyframes[0] = new ProcessedKeyframe(absoluteTick, keyframes[0].getPose());
            
            // 后续关键帧的tick累加
            for (int i = 1; i < keyframes.length; i++) {
                int relativeTick = keyframes[i].getTick();
                absoluteTick += relativeTick;
                processedKeyframes[i] = new ProcessedKeyframe(absoluteTick, keyframes[i].getPose());
            }
            
            // 总时长是最后一个关键帧的绝对tick
            this.duration = absoluteTick;
        }
    }
    
    /**
     * 获取第一个关键帧的持续时间（用于从当前姿态插值）
     */
    public int getFirstKeyframeDuration() {
        return firstKeyframeDuration;
    }
    
    /**
     * 获取第一个关键帧的姿态
     */
    public DollPose getFirstKeyframePose() {
        if (processedKeyframes.length == 0) {
            return null;
        }
        return processedKeyframes[0].pose();
    }
    
    /**
     * 获取最后一个关键帧的姿态（不经过插值）
     */
    public DollPose getLastKeyframePose() {
        if (processedKeyframes.length == 0) {
            return null;
        }
        return processedKeyframes[processedKeyframes.length - 1].pose();
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
    public boolean isLooping() {
        return looping;
    }
    
    @Override
    public ActionMode getMode() {
        return mode;
    }
    
    @Override
    public int getDuration() {
        return duration;
    }
    
    @Override
    public DollPose getPoseAt(int tick) {
        if (processedKeyframes.length == 0) {
            return null;
        }
        
        // 根据模式处理tick
        int actualTick;
        if (mode == ActionMode.LOOP) {
            // 循环模式：当 tick == duration 时，返回最后一个关键帧（用于平滑循环）
            // 当 tick > duration 时，使用模运算回到开始
            if (tick >= duration) {
                actualTick = duration - 1; // 返回最后一个关键帧
            } else {
                actualTick = tick;
            }
        } else {
            actualTick = Math.min(tick, duration - 1);
        }
        
        // 如果只有一个关键帧，直接返回
        if (processedKeyframes.length == 1) {
            return processedKeyframes[0].pose();
        }
        
        // 找到当前tick所在的关键帧区间（使用绝对tick）
        ProcessedKeyframe prevKeyframe = null;
        ProcessedKeyframe nextKeyframe = null;
        
        for (int i = 0; i < processedKeyframes.length; i++) {
            if (processedKeyframes[i].absoluteTick() <= actualTick) {
                prevKeyframe = processedKeyframes[i];
            }
            if (processedKeyframes[i].absoluteTick() >= actualTick && nextKeyframe == null) {
                nextKeyframe = processedKeyframes[i];
                break;
            }
        }
        
        // 如果没有找到前一个关键帧，使用第一个
        if (prevKeyframe == null) {
            prevKeyframe = processedKeyframes[0];
        }
        
        // 如果没有找到下一个关键帧，使用最后一个
        if (nextKeyframe == null) {
            nextKeyframe = processedKeyframes[processedKeyframes.length - 1];
        }
        
        // 如果前后关键帧相同，直接返回
        if (prevKeyframe == nextKeyframe) {
            return prevKeyframe.pose();
        }
        
        // 计算插值比例（使用绝对tick）
        int tickDiff = nextKeyframe.absoluteTick() - prevKeyframe.absoluteTick();
        float t = tickDiff > 0 ? (float)(actualTick - prevKeyframe.absoluteTick()) / tickDiff : 0.0F;
        t = MathHelper.clamp(t, 0.0F, 1.0F);
        
        // 在两个姿态之间插值
        return interpolatePoses(prevKeyframe.pose(), nextKeyframe.pose(), t);
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
                MathHelper.lerp(t, arr1[0], arr2[0]),
                MathHelper.lerp(t, arr1[1], arr2[1]),
                MathHelper.lerp(t, arr1[2], arr2[2])
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
        
        @Override
        public float[] getPosition() {
            return interpolate(pose1.getPosition(), pose2.getPosition());
        }
        
        @Override
        public float[] getScale() {
            return interpolate(pose1.getScale(), pose2.getScale());
        }
        
        @Override
        public float[] getHeadPosition() {
            return interpolate(pose1.getHeadPosition(), pose2.getHeadPosition());
        }
        
        @Override
        public float[] getHeadScale() {
            return interpolate(pose1.getHeadScale(), pose2.getHeadScale());
        }
        
        @Override
        public float[] getHatPosition() {
            return interpolate(pose1.getHatPosition(), pose2.getHatPosition());
        }
        
        @Override
        public float[] getHatScale() {
            return interpolate(pose1.getHatScale(), pose2.getHatScale());
        }
        
        @Override
        public float[] getBodyPosition() {
            return interpolate(pose1.getBodyPosition(), pose2.getBodyPosition());
        }
        
        @Override
        public float[] getBodyScale() {
            return interpolate(pose1.getBodyScale(), pose2.getBodyScale());
        }
        
        @Override
        public float[] getRightArmPosition() {
            return interpolate(pose1.getRightArmPosition(), pose2.getRightArmPosition());
        }
        
        @Override
        public float[] getRightArmScale() {
            return interpolate(pose1.getRightArmScale(), pose2.getRightArmScale());
        }
        
        @Override
        public float[] getLeftArmPosition() {
            return interpolate(pose1.getLeftArmPosition(), pose2.getLeftArmPosition());
        }
        
        @Override
        public float[] getLeftArmScale() {
            return interpolate(pose1.getLeftArmScale(), pose2.getLeftArmScale());
        }
        
        @Override
        public float[] getRightLegPosition() {
            return interpolate(pose1.getRightLegPosition(), pose2.getRightLegPosition());
        }
        
        @Override
        public float[] getRightLegScale() {
            return interpolate(pose1.getRightLegScale(), pose2.getRightLegScale());
        }
        
        @Override
        public float[] getLeftLegPosition() {
            return interpolate(pose1.getLeftLegPosition(), pose2.getLeftLegPosition());
        }
        
        @Override
        public float[] getLeftLegScale() {
            return interpolate(pose1.getLeftLegScale(), pose2.getLeftLegScale());
        }
    }
}

