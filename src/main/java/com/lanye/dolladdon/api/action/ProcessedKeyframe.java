package com.lanye.dolladdon.api.action;

import com.lanye.dolladdon.api.pose.DollPose;

/**
 * 处理后的关键帧（内部使用）
 * tick 已转换为绝对时间点
 */
record ProcessedKeyframe(int absoluteTick, DollPose pose) {
}
