package com.lanye.dolladdon.mixin.logging;

import com.lanye.dolladdon.PlayerDollAddon;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * 日志工具类
 * 统一管理 logging 模块的所有日志输出
 */
public class LoggingUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * 获取日志记录器
     * @return Logger 实例
     */
    public static Logger getLogger() {
        return LOGGER;
    }
    
    /**
     * 记录信息级别日志
     * @param message 日志消息
     * @param args 参数
     */
    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }
    
    /**
     * 记录调试级别日志
     * @param message 日志消息
     * @param args 参数
     */
    public static void debug(String message, Object... args) {
        LOGGER.debug(message, args);
    }
    
    /**
     * 记录警告级别日志
     * @param message 日志消息
     * @param args 参数
     */
    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }
    
    /**
     * 记录错误级别日志
     * @param message 日志消息
     * @param args 参数
     */
    public static void error(String message, Object... args) {
        LOGGER.error(message, args);
    }
    
    /**
     * 记录错误级别日志（带异常）
     * @param message 日志消息
     * @param throwable 异常
     */
    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }
    
    /**
     * 记录错误级别日志（带异常和参数）
     * @param message 日志消息
     * @param throwable 异常
     * @param args 参数
     */
    public static void error(String message, Throwable throwable, Object... args) {
        LOGGER.error(message, throwable);
    }
}
