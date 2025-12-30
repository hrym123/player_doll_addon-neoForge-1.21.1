package com.lanye.dolladdon.util;

import com.lanye.dolladdon.PlayerDollAddon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 模块化日志管理器
 * 支持为不同模块单独控制日志开关，实现一键开关功能
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * // 1. 定义模块名称常量
 * private static final String LOG_MODULE_ENTITY = "entity";
 * 
 * // 2. 获取日志对象
 * private static final Logger LOGGER = ModuleLogger.getLogger(LOG_MODULE_ENTITY);
 * 
 * // 3. 使用日志（自动根据模块开关决定是否输出）
 * ModuleLogger.debug(LOG_MODULE_ENTITY, "调试信息");
 * ModuleLogger.info(LOG_MODULE_ENTITY, "信息: {}", value);
 * ModuleLogger.warn(LOG_MODULE_ENTITY, "警告: {}", error);
 * ModuleLogger.error(LOG_MODULE_ENTITY, "错误: {}", exception);
 * 
 * // 4. 控制日志开关（在代码中或配置文件中）
 * ModuleLogger.setModuleEnabled("entity", false);  // 禁用entity模块日志
 * ModuleLogger.setGlobalEnabled(false);             // 一键禁用所有模块日志
 * ModuleLogger.disableAll();                        // 一键禁用所有模块日志（便捷方法）
 * ModuleLogger.enableAll();                         // 一键启用所有模块日志（便捷方法）
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
 *   <li>error级别的日志始终输出，不受开关控制（确保错误能被及时发现）</li>
 *   <li>debug/info/warn级别的日志受模块开关和全局开关控制</li>
 *   <li>全局开关优先级高于模块开关</li>
 * </ul>
 */
public class ModuleLogger {
    private static final Map<String, Boolean> moduleStates = new HashMap<>();
    private static final Map<String, Logger> loggers = new HashMap<>();
    
    // 全局日志开关：设置为false可一键禁用所有模块日志
    private static boolean globalEnabled = true;
    
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
            // 默认启用该模块
            if (!moduleStates.containsKey(moduleName)) {
                moduleStates.put(moduleName, true);
            }
        }
        return loggers.get(moduleName);
    }
    
    /**
     * 检查指定模块的日志是否启用
     * @param moduleName 模块名称
     * @return 如果全局开关和模块开关都启用，返回true
     */
    public static boolean isEnabled(String moduleName) {
        return globalEnabled && moduleStates.getOrDefault(moduleName, true);
    }
    
    /**
     * 设置指定模块的日志开关
     * @param moduleName 模块名称
     * @param enabled 是否启用
     */
    public static void setModuleEnabled(String moduleName, boolean enabled) {
        moduleStates.put(moduleName, enabled);
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
     * 一键启用所有模块日志
     */
    public static void enableAll() {
        setGlobalEnabled(true);
    }
    
    /**
     * 记录调试日志（仅在模块启用时记录）
     * @param moduleName 模块名称
     * @param message 日志消息
     */
    public static void debug(String moduleName, String message) {
        if (isEnabled(moduleName)) {
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
        if (isEnabled(moduleName)) {
            getLogger(moduleName).debug(message, args);
        }
    }
    
    /**
     * 记录信息日志（仅在模块启用时记录）
     * @param moduleName 模块名称
     * @param message 日志消息
     */
    public static void info(String moduleName, String message) {
        if (isEnabled(moduleName)) {
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
        if (isEnabled(moduleName)) {
            getLogger(moduleName).info(message, args);
        }
    }
    
    /**
     * 记录警告日志（仅在模块启用时记录）
     * @param moduleName 模块名称
     * @param message 日志消息
     */
    public static void warn(String moduleName, String message) {
        if (isEnabled(moduleName)) {
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
        if (isEnabled(moduleName)) {
            getLogger(moduleName).warn(message, args);
        }
    }
    
    /**
     * 记录错误日志（错误日志始终记录，不受开关控制）
     * @param moduleName 模块名称
     * @param message 日志消息
     */
    public static void error(String moduleName, String message) {
        getLogger(moduleName).error(message);
    }
    
    /**
     * 记录错误日志（带参数，错误日志始终记录）
     * @param moduleName 模块名称
     * @param message 日志消息模板
     * @param args 参数
     */
    public static void error(String moduleName, String message, Object... args) {
        getLogger(moduleName).error(message, args);
    }
    
    /**
     * 记录错误日志（带异常，错误日志始终记录）
     * @param moduleName 模块名称
     * @param message 日志消息
     * @param throwable 异常对象
     */
    public static void error(String moduleName, String message, Throwable throwable) {
        getLogger(moduleName).error(message, throwable);
    }
}

