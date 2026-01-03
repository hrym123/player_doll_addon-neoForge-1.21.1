package com.lanye.dolladdon.api.action;

import com.lanye.dolladdon.api.pose.DollPose;

/**
 * 玩偶动作接口
 * 定义玩偶的动画序列，可以包含多个姿态和过渡
 * 
 * 开发者可以实现此接口来创建自定义动作
 */
public interface DollAction {
    /**
     * 获取动作名称
     * @return 动作名称
     */
    String getName();
    
    /**
     * 获取动作的显示名称（用于UI显示）
     * @return 显示名称，如果没有则返回动作名称
     */
    default String getDisplayName() {
        return getName();
    }
    
    /**
     * 获取动作是否循环播放（向后兼容方法）
     * @return 是否循环
     * @deprecated 使用 {@link #getMode()} 替代
     */
    @Deprecated
    boolean isLooping();
    
    /**
     * 获取动作播放模式
     * @return 动作播放模式
     */
    default ActionMode getMode() {
        // 默认实现：根据 isLooping() 返回对应的模式
        return isLooping() ? ActionMode.LOOP : ActionMode.ONCE;
    }
    
    /**
     * 获取动作总时长（tick数）
     * @return 总时长
     */
    int getDuration();
    
    /**
     * 获取指定时间点的姿态
     * @param tick 当前tick（从0开始）
     * @return 该时间点的姿态，如果超出范围返回null
     */
    DollPose getPoseAt(int tick);
    
    /**
     * 获取动作的关键帧列表
     * @return 关键帧数组，每个元素为 [tick, pose]
     */
    ActionKeyframe[] getKeyframes();
}

