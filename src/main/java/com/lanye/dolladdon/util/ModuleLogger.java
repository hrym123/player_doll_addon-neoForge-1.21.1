package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 模块化日志管理器
 * 支持为不同模块单独控制日志级别，实现一键开关和级别控制功能
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * // 1. 定义模块名称常量
 * private static final String LOG_MODULE_ENTITY = "entity";
 * 
 * // 2. 获取日志对象
 * private static final Logger LOGGER = ModuleLogger.getLogger(LOG_MODULE_ENTITY);
 * 
 * // 3. 使用日志（自动根据模块级别决定是否输出）
 * ModuleLogger.debug(LOG_MODULE_ENTITY, "调试信息");
 * ModuleLogger.info(LOG_MODULE_ENTITY, "信息: {}", value);
 * ModuleLogger.warn(LOG_MODULE_ENTITY, "警告: {}", error);
 * ModuleLogger.error(LOG_MODULE_ENTITY, "错误: {}", exception);
 * 
 * // 4. 控制日志级别（在代码中或配置文件中）
 * ModuleLogger.setModuleLevel("entity.pose", LogLevel.DEBUG);  // 设置为debug级别，输出所有日志
 * ModuleLogger.setModuleLevel("entity.pose", LogLevel.INFO);  // 设置为info级别，输出info及以上
 * ModuleLogger.setModuleLevel("entity.pose", LogLevel.WARN);  // 设置为warn级别，只输出warn和error
 * ModuleLogger.setModuleLevel("entity.pose", LogLevel.ERROR);  // 设置为error级别，只输出error
 * ModuleLogger.setModuleLevel("entity.pose", LogLevel.OFF);   // 设置为off级别，不输出任何日志
 * 
 * // 5. 兼容旧版本的开关方法（已废弃，建议使用级别控制）
 * ModuleLogger.setModuleEnabled("entity", false);  // 等同于 LogLevel.OFF
 * ModuleLogger.setGlobalEnabled(false);             // 一键禁用所有模块日志
 * ModuleLogger.disableAll();                        // 一键禁用所有模块日志（便捷方法）
 * ModuleLogger.enableAll();                         // 一键启用所有模块日志（默认DEBUG级别）
 * }</pre>
 * 
 * <p>模块命名建议：</p>
 * <ul>
 *   <li>使用点分隔符组织模块层级，如 "entity", "entity.pose", "entity.action"</li>
 *   <li>模块名称应该清晰、简洁，便于识别和管理</li>
 * </ul>
 * 
 * <p>注意事项：</p>
 * <ul>
 *   <li>日志级别优先级：DEBUG < INFO < WARN < ERROR < OFF</li>
 *   <li>设置为某个级别会输出该级别及以上的所有日志</li>
 *   <li>例如：设置为 INFO 级别会输出 INFO、WARN、ERROR 级别的日志</li>
 *   <li>全局开关优先级高于模块级别设置</li>
 *   <li>OFF 级别会禁用所有日志，包括 ERROR（如果需要始终输出 ERROR，请使用 ERROR 级别）</li>
 * </ul>
 */
public class ModuleLogger {
    private static final Map<String, LogLevel> moduleLevels = new HashMap<>();
    private static final Map<String, Boolean> moduleStates = new HashMap<>(); // 兼容旧版本
    private static final Map<String, Logger> loggers = new HashMap<>();
    
    // 全局日志开关：设置为false可一键禁用所有模块日志
    private static boolean globalEnabled = true;
    
    // 默认日志级别
    private static final LogLevel DEFAULT_LEVEL = LogLevel.DEBUG;
    
    // 默认模块状态（可以在初始化时设置）
    static {
        // 默认启用所有模块的日志
        // 可以通过配置文件或代码修改
    }
    
    /**
     * 获取指定模块的日志对象
     * @param moduleName 模块名称（如 "entity", "render", "pose" 等）
     * @return Logger对象
     */
    public static Logger getLogger(String moduleName) {
        if (!loggers.containsKey(moduleName)) {
            Logger logger = LoggerFactory.getLogger(PlayerDollAddon.MODID + "." + moduleName);
            loggers.put(moduleName, logger);
            // 默认启用该模块，使用默认级别
            if (!moduleLevels.containsKey(moduleName)) {
                moduleLevels.put(moduleName, DEFAULT_LEVEL);
            }
            // 兼容旧版本
            if (!moduleStates.containsKey(moduleName)) {
                moduleStates.put(moduleName, true);
            }
        }
        return loggers.get(moduleName);
    }
    
    /**
     * 获取指定模块的日志级别
     * @param moduleName 模块名称
     * @return 日志级别，如果未设置则返回默认级别
     */
    public static LogLevel getModuleLevel(String moduleName) {
        return moduleLevels.getOrDefault(moduleName, DEFAULT_LEVEL);
    }
    
    /**
     * 设置指定模块的日志级别
     * @param moduleName 模块名称
     * @param level 日志级别
     */
    public static void setModuleLevel(String moduleName, LogLevel level) {
        moduleLevels.put(moduleName, level);
        // 同步更新旧版本的开关状态（兼容性）
        moduleStates.put(moduleName, level != LogLevel.OFF);
    }
    
    /**
     * 检查指定模块是否应该输出指定级别的日志
     * @param moduleName 模块名称
     * @param level 日志级别
     * @return 如果应该输出，返回true
     */
    private static boolean shouldLog(String moduleName, LogLevel level) {
        if (!globalEnabled) {
            return false;
        }
        LogLevel moduleLevel = getModuleLevel(moduleName);
        return moduleLevel.shouldLog(level);
    }
    
    /**
     * 检查指定模块的日志是否启用（兼容旧版本）
     * @param moduleName 模块名称
     * @return 如果全局开关和模块开关都启用，返回true
     * @deprecated 建议使用 {@link #shouldLog(String, LogLevel)} 或直接使用级别控制
     */
    @Deprecated
    public static boolean isEnabled(String moduleName) {
        return globalEnabled && moduleStates.getOrDefault(moduleName, true);
    }
    
    /**
     * 设置指定模块的日志开关（兼容旧版本）
     * @param moduleName 模块名称
     * @param enabled 是否启用
     * @deprecated 建议使用 {@link #setModuleLevel(String, LogLevel)} 进行级别控制
     */
    @Deprecated
    public static void setModuleEnabled(String moduleName, boolean enabled) {
        moduleStates.put(moduleName, enabled);
        // 同步更新级别
        if (enabled) {
            moduleLevels.put(moduleName, DEFAULT_LEVEL);
        } else {
            moduleLevels.put(moduleName, LogLevel.OFF);
        }
    }
    
    /**
     * 设置全局日志开关
     * @param enabled 是否启用所有模块日志
     */
    public static void setGlobalEnabled(boolean enabled) {
        globalEnabled = enabled;
    }
    
    /**
     * 获取全局日志开关状态
     * @return 全局开关状态
     */
    public static boolean isGlobalEnabled() {
        return globalEnabled;
    }
    
    /**
     * 一键禁用所有模块日志
     */
    public static void disableAll() {
        setGlobalEnabled(false);
    }
    
    /**
     * 一键启用所有模块日志（设置为默认级别 DEBUG）
     */
    public static void enableAll() {
        setGlobalEnabled(true);
        // 将所有模块设置为默认级别
        for (String moduleName : moduleLevels.keySet()) {
            moduleLevels.put(moduleName, DEFAULT_LEVEL);
            moduleStates.put(moduleName, true);
        }
    }
    
    /**
     * 设置全局日志级别（为所有模块设置相同的级别）
     * @param level 日志级别
     */
    public static void setGlobalLevel(LogLevel level) {
        for (String moduleName : loggers.keySet()) {
            setModuleLevel(moduleName, level);
        }
    }
    
    /**
     * 记录调试日志（根据模块级别决定是否输出）
     * @param moduleName 模块名称
     * @param message 日志消息
     */
    public static void debug(String moduleName, String message) {
        if (shouldLog(moduleName, LogLevel.DEBUG)) {
            getLogger(moduleName).debug(message);
        }
    }
    
    /**
     * 记录调试日志（带参数）
     * @param moduleName 模块名称
     * @param message 日志消息模板
     * @param args 参数
     */
    public static void debug(String moduleName, String message, Object... args) {
        if (shouldLog(moduleName, LogLevel.DEBUG)) {
            getLogger(moduleName).debug(message, args);
        }
    }
    
    /**
     * 记录信息日志（根据模块级别决定是否输出）
     * @param moduleName 模块名称
     * @param message 日志消息
     */
    public static void info(String moduleName, String message) {
        if (shouldLog(moduleName, LogLevel.INFO)) {
            getLogger(moduleName).info(message);
        }
    }
    
    /**
     * 记录信息日志（带参数）
     * @param moduleName 模块名称
     * @param message 日志消息模板
     * @param args 参数
     */
    public static void info(String moduleName, String message, Object... args) {
        if (shouldLog(moduleName, LogLevel.INFO)) {
            getLogger(moduleName).info(message, args);
        }
    }
    
    /**
     * 记录警告日志（根据模块级别决定是否输出）
     * @param moduleName 模块名称
     * @param message 日志消息
     */
    public static void warn(String moduleName, String message) {
        if (shouldLog(moduleName, LogLevel.WARN)) {
            getLogger(moduleName).warn(message);
        }
    }
    
    /**
     * 记录警告日志（带参数）
     * @param moduleName 模块名称
     * @param message 日志消息模板
     * @param args 参数
     */
    public static void warn(String moduleName, String message, Object... args) {
        if (shouldLog(moduleName, LogLevel.WARN)) {
            getLogger(moduleName).warn(message, args);
        }
    }
    
    /**
     * 记录错误日志（根据模块级别决定是否输出，OFF级别除外）
     * @param moduleName 模块名称
     * @param message 日志消息
     */
    public static void error(String moduleName, String message) {
        if (shouldLog(moduleName, LogLevel.ERROR)) {
            getLogger(moduleName).error(message);
        }
    }
    
    /**
     * 记录错误日志（带参数，根据模块级别决定是否输出）
     * @param moduleName 模块名称
     * @param message 日志消息模板
     * @param args 参数
     */
    public static void error(String moduleName, String message, Object... args) {
        if (shouldLog(moduleName, LogLevel.ERROR)) {
            getLogger(moduleName).error(message, args);
        }
    }
    
    /**
     * 记录错误日志（带异常，根据模块级别决定是否输出）
     * @param moduleName 模块名称
     * @param message 日志消息
     * @param throwable 异常对象
     */
    public static void error(String moduleName, String message, Throwable throwable) {
        if (shouldLog(moduleName, LogLevel.ERROR)) {
            getLogger(moduleName).error(message, throwable);
        }
    }
}

