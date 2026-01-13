package com.lanye.dolladdon.mixin.logging;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * 模块化日志工具类
 * 统一管理 logging 模块的所有日志输出
 * 
 * 使用方式：
 * 1. 在 Mixin 类中调用 ModuleLogger.debug/info/warn/error() 方法
 * 2. 传入模块名称和日志消息
 * 3. 系统会根据 LogModuleConfig 中的配置自动判断是否输出
 * 
 * 示例：
 * ```java
 * ModuleLogger.info(LogModuleConfig.MODULE_ENTITY, "Entity created: {}", entityId);
 * ```
 */
public class ModuleLogger {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 静态初始化块：在类加载时执行，用于测试日志系统是否正常工作
    static {
        // 直接使用底层 Logger 输出测试日志（不经过模块系统，确保一定能看到）
        LOGGER.debug("========================================");
        LOGGER.debug("[Logging System Test] ModuleLogger class loaded successfully");
        LOGGER.debug("[Logging System Test] Logger instance: {}", LOGGER.getClass().getName());
        LOGGER.debug("========================================");
        
        // 测试模块日志系统（使用 DEBUG 级别）
        try {
            ModuleLogger.info(LogModuleConfig.MODULE_INIT, "Logging system test: ModuleLogger initialized successfully");
            ModuleLogger.debug(LogModuleConfig.MODULE_INIT, "Logging system test: DEBUG level test (may not appear if module level is WARN or higher)");
            ModuleLogger.warn(LogModuleConfig.MODULE_INIT, "Logging system test: WARN level test");
        } catch (Exception e) {
            LOGGER.error("[Logging System Test] Failed to test module logging system", e);
        }
    }
    
    /**
     * 获取底层 Logger 实例（用于特殊场景）
     * @return Logger 实例
     */
    public static Logger getLogger() {
        return LOGGER;
    }
    
    /**
     * 记录调试级别日志
     * @param moduleName 模块名称（使用 LogModuleConfig 中的常量）
     * @param message 日志消息（支持 SLF4J 风格的占位符 {}）
     * @param args 参数
     */
    public static void debug(String moduleName, String message, Object... args) {
        if (shouldLog(moduleName, LogLevel.DEBUG)) {
            String formattedMessage = formatMessage(moduleName, message);
            LOGGER.debug(formattedMessage, args);
        }
    }
    
    /**
     * 记录信息级别日志
     * @param moduleName 模块名称
     * @param message 日志消息
     * @param args 参数
     */
    public static void info(String moduleName, String message, Object... args) {
        if (shouldLog(moduleName, LogLevel.INFO)) {
            String formattedMessage = formatMessage(moduleName, message);
            LOGGER.info(formattedMessage, args);
        }
    }
    
    /**
     * 记录警告级别日志
     * @param moduleName 模块名称
     * @param message 日志消息
     * @param args 参数
     */
    public static void warn(String moduleName, String message, Object... args) {
        if (shouldLog(moduleName, LogLevel.WARN)) {
            String formattedMessage = formatMessage(moduleName, message);
            LOGGER.warn(formattedMessage, args);
        }
    }
    
    /**
     * 记录错误级别日志
     * @param moduleName 模块名称
     * @param message 日志消息
     * @param args 参数
     */
    public static void error(String moduleName, String message, Object... args) {
        if (shouldLog(moduleName, LogLevel.ERROR)) {
            String formattedMessage = formatMessage(moduleName, message);
            LOGGER.error(formattedMessage, args);
        }
    }
    
    /**
     * 记录错误级别日志（带异常）
     * @param moduleName 模块名称
     * @param message 日志消息
     * @param throwable 异常
     */
    public static void error(String moduleName, String message, Throwable throwable) {
        if (shouldLog(moduleName, LogLevel.ERROR)) {
            String formattedMessage = formatMessage(moduleName, message);
            LOGGER.error(formattedMessage, throwable);
        }
    }
    
    /**
     * 记录错误级别日志（带异常和参数）
     * @param moduleName 模块名称
     * @param message 日志消息
     * @param throwable 异常
     * @param args 参数
     */
    public static void error(String moduleName, String message, Throwable throwable, Object... args) {
        if (shouldLog(moduleName, LogLevel.ERROR)) {
            String formattedMessage = formatMessage(moduleName, message);
            LOGGER.error(formattedMessage, throwable);
        }
    }
    
    /**
     * 判断是否应该输出日志
     * @param moduleName 模块名称
     * @param level 日志级别
     * @return true 如果应该输出，false 否则
     */
    private static boolean shouldLog(String moduleName, LogLevel level) {
        LogLevel moduleLevel = LogModuleConfig.getModuleLevel(moduleName);
        return moduleLevel.shouldLog(level);
    }
    
    /**
     * 格式化日志消息
     * @param moduleName 模块名称
     * @param message 原始消息
     * @return 格式化后的消息
     */
    private static String formatMessage(String moduleName, String message) {
        String template = LogModuleConfig.getLogTemplate(moduleName);
        // 替换模板中的第一个 {} 为消息内容
        if (template.contains("{}")) {
            return template.replaceFirst("\\{\\}", message);
        }
        return template + " " + message;
    }
}
