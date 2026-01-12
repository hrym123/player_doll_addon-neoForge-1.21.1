package com.lanye.dolladdon.api.action;

/**
 * 动作播放模式
 */
public enum ActionMode {
    /**
     * 循环：动作播放完毕后继续从第一个关键帧开始
     */
    LOOP,
    
    /**
     * 保持：动作播放完毕后保持最后一个关键帧的姿态
     */
    HOLD,
    
    /**
     * 一次性：动作播放完毕后回归当前姿态（standing）
     */
    ONCE
}
