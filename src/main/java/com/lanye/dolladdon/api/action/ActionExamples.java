package com.lanye.dolladdon.api.action;

import com.lanye.dolladdon.api.pose.DollPose;
import com.lanye.dolladdon.api.pose.SimpleDollPose;

/**
 * 动作示例类
 * 展示如何通过代码创建自定义动作
 */
public class ActionExamples {
    
    /**
     * 创建一个简单的挥手动作
     */
    public static DollAction createWaveAction() {
        // 创建关键帧
        ActionKeyframe[] keyframes = new ActionKeyframe[]{
            // 第0帧：起始姿态（站立）
            new ActionKeyframe(0, SimpleDollPose.createDefaultStandingPose()),
            
            // 第5帧：手臂向上
            new ActionKeyframe(5, new SimpleDollPose(
                "wave_up",
                new float[]{0, 0, 0}, // head
                new float[]{0, 0, 0}, // hat
                new float[]{0, 0, 0}, // body
                new float[]{-1.5708F, 0, 0}, // rightArm (向上90度)
                new float[]{0.6981317F, 0, 0}, // leftArm (保持原样)
                new float[]{0, 0, 0}, // rightLeg
                new float[]{0, 0, 0}  // leftLeg
            )),
            
            // 第10帧：回到起始姿态
            new ActionKeyframe(10, SimpleDollPose.createDefaultStandingPose())
        };
        
        // 创建动作（不循环）
        return new SimpleDollAction("wave", false, keyframes);
    }
    
    /**
     * 创建一个循环的跳舞动作
     */
    public static DollAction createDanceAction() {
        ActionKeyframe[] keyframes = new ActionKeyframe[]{
            // 第0帧：手臂向左
            new ActionKeyframe(0, new SimpleDollPose(
                "dance_left",
                new float[]{0, 0, 0},
                new float[]{0, 0, 0},
                new float[]{0, 0, 0},
                new float[]{-1.5708F, 0, 0}, // rightArm 向上
                new float[]{1.5708F, 0, 0},   // leftArm 向上
                new float[]{0, 0, 0},
                new float[]{0, 0, 0}
            )),
            
            // 第10帧：手臂向右
            new ActionKeyframe(10, new SimpleDollPose(
                "dance_right",
                new float[]{0, 0, 0},
                new float[]{0, 0, 0},
                new float[]{0, 0, 0},
                new float[]{1.5708F, 0, 0},   // rightArm 向上
                new float[]{-1.5708F, 0, 0},  // leftArm 向上
                new float[]{0, 0, 0},
                new float[]{0, 0, 0}
            )),
            
            // 第20帧：回到起始姿态（形成循环）
            new ActionKeyframe(20, new SimpleDollPose(
                "dance_left",
                new float[]{0, 0, 0},
                new float[]{0, 0, 0},
                new float[]{0, 0, 0},
                new float[]{-1.5708F, 0, 0},
                new float[]{1.5708F, 0, 0},
                new float[]{0, 0, 0},
                new float[]{0, 0, 0}
            ))
        };
        
        // 创建循环动作
        return new SimpleDollAction("dance", true, keyframes);
    }
    
    /**
     * 创建一个自定义动作的示例
     * 展示如何创建更复杂的动作
     */
    public static DollAction createCustomAction() {
        // 你可以创建任意数量的关键帧
        ActionKeyframe[] keyframes = new ActionKeyframe[]{
            new ActionKeyframe(0, createPose1()),
            new ActionKeyframe(5, createPose2()),
            new ActionKeyframe(10, createPose3()),
            new ActionKeyframe(15, createPose2()),
            new ActionKeyframe(20, createPose1())
        };
        
        return new SimpleDollAction("custom", true, keyframes);
    }
    
    private static DollPose createPose1() {
        return new SimpleDollPose(
            "pose1",
            new float[]{0, 0, 0},
            new float[]{0, 0, 0},
            new float[]{0, 0, 0},
            new float[]{-0.5F, 0, 0},
            new float[]{0.5F, 0, 0},
            new float[]{0, 0, 0},
            new float[]{0, 0, 0}
        );
    }
    
    private static DollPose createPose2() {
        return new SimpleDollPose(
            "pose2",
            new float[]{0, 0, 0},
            new float[]{0, 0, 0},
            new float[]{0, 0, 0},
            new float[]{-1.0F, 0, 0},
            new float[]{1.0F, 0, 0},
            new float[]{0, 0, 0},
            new float[]{0, 0, 0}
        );
    }
    
    private static DollPose createPose3() {
        return new SimpleDollPose(
            "pose3",
            new float[]{0, 0, 0.2F},
            new float[]{0, 0, 0.2F},
            new float[]{0, 0, 0},
            new float[]{-1.5F, 0, 0},
            new float[]{1.5F, 0, 0},
            new float[]{0, 0, 0},
            new float[]{0, 0, 0}
        );
    }
}

