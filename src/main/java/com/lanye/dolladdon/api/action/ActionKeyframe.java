package com.lanye.dolladdon.api.action;

import com.lanye.dolladdon.api.pose.DollPose;

/**
 * 动作关键帧
 * 定义在特定时间点的姿态
 */
public class ActionKeyframe {
    private final int tick;
    private final DollPose pose;
    
    public ActionKeyframe(int tick, DollPose pose) {
        this.tick = tick;
        this.pose = pose;
    }
    
    public int getTick() {
        return tick;
    }
    
    public DollPose getPose() {
        return pose;
    }
}

