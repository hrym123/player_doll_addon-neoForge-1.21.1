package com.lanye.dolladdon.util.logging;

/**
 * 日志级别枚举
 * 定义五级日志级别，支持优先级比较
 */
public enum LogLevel {
    /**
     * DEBUG级别：输出所有级别日志（debug, info, warn, error）
     */
    DEBUG(0),
    
    /**
     * INFO级别：输出 info, warn, error
     */
    INFO(1),
    
    /**
     * WARN级别：输出 warn, error
     */
    WARN(2),
    
    /**
     * ERROR级别：只输出 error
     */
    ERROR(3),
    
    /**
     * OFF级别：不输出任何日志（包括 error）
     */
    OFF(4);
    
    private final int level;
    
    LogLevel(int level) {
        this.level = level;
    }
    
    /**
     * 获取级别数值
     * @return 级别数值
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * 判断是否应该输出指定级别的日志
     * @param targetLevel 目标日志级别
     * @return true 如果应该输出，false 否则
     */
    public boolean shouldLog(LogLevel targetLevel) {
        if (this == OFF) {
            return false;
        }
        if (targetLevel == OFF) {
            return false;
        }
        return targetLevel.level >= this.level;
    }
}
