package com.lanye.dolladdon.util.logging;

/**
 * 日志级别枚举
 * 用于控制模块日志的输出级别
 */
public enum LogLevel {
    DEBUG(0),   // 输出所有级别（debug, info, warn, error）
    INFO(1),   // 输出 info, warn, error
    WARN(2),   // 输出 warn, error
    ERROR(3),   // 只输出 error
    OFF(4);     // 不输出任何日志（包括 error）
    
    private final int level;
    
    LogLevel(int level) {
        this.level = level;
    }
    
    public int getLevel() {
        return level;
    }
    
    /**
     * 检查当前级别是否应该输出指定级别的日志
     * @param targetLevel 目标日志级别
     * @return 是否应该输出
     */
    public boolean shouldLog(LogLevel targetLevel) {
        if (this == OFF) {
            // OFF 级别不输出任何日志（包括 error）
            return false;
        }
        return targetLevel.getLevel() >= this.getLevel();
    }
}

