package com.lanye.dolladdon.util.logging;

import com.lanye.dolladdon.PlayerDollAddon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 模块化日志管理器
 * 支持为不同模块单独控制日志级别，实现一键开关和级别控制功能
 * 
 * <p><strong>重要提示：</strong>所有日志级别的修改都应该通过 {@link LogModuleConfig} 进行。
 * 此类中的设置方法已标记为 {@code @Deprecated}，仅保留用于向后兼容。</p>
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * // 1. 使用 LogModuleConfig 中定义的模块常量
 * private static final String LOG_MODULE_ENTITY = LogModuleConfig.MODULE_ENTITY;
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
 * // 4. 控制日志级别（必须通过 LogModuleConfig）
 * LogModuleConfig.setModuleLevel("entity.pose", LogLevel.DEBUG);  // 设置为debug级别，输出所有日志
 * LogModuleConfig.setModuleLevel("entity.pose", LogLevel.INFO);  // 设置为info级别，输出info及以上
 * LogModuleConfig.setModuleLevel("entity.pose", LogLevel.WARN);  // 设置为warn级别，只输出warn和error
 * LogModuleConfig.setModuleLevel("entity.pose", LogLevel.ERROR);  // 设置为error级别，只输出error
 * LogModuleConfig.setModuleLevel("entity.pose", LogLevel.OFF);   // 设置为off级别，不输出任何日志
 * 
 * // 5. 全局控制（必须通过 LogModuleConfig）
 * LogModuleConfig.setGlobalLevel(LogLevel.WARN);  // 设置所有模块为WARN级别
 * LogModuleConfig.disableAll();                    // 一键禁用所有模块日志
 * LogModuleConfig.enableAll();                     // 一键启用所有模块日志（恢复到默认级别）
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
    
    // 默认日志级别（已关闭DEBUG级别，仅输出INFO及以上级别）
    private static final LogLevel DEFAULT_LEVEL = LogLevel.INFO;
    
    // 默认模块状态（可以在初始化时设置）
    static {
        // 默认启用所有模块的日志
        // 可以通过配置文件或代码修改
    }
    
    /**
     * 配置底层日志框架的日志级别（如果支持）
     * 这个方法会尝试配置logback等底层日志框架，实现真正的日志级别控制
     * 使用反射来避免编译时依赖logback
     * 
     * @param rootLevel 根日志级别（影响所有日志），如果为null则不设置
     * @param moduleLevels 模块特定的日志级别映射，key为模块名或类名，value为日志级别
     */
    public static void configureFrameworkLogLevels(LogLevel rootLevel, Map<String, LogLevel> moduleLevels) {
        try {
            // 尝试获取logback的LoggerContext（使用反射避免编译时依赖）
            Object loggerFactory = LoggerFactory.getILoggerFactory();
            Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
            
            if (loggerContextClass.isInstance(loggerFactory)) {
                Object loggerContext = loggerFactory;
                
                // 加载必要的类和方法（只加载一次）
                Class<?> loggerClass = Class.forName("ch.qos.logback.classic.Logger");
                Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
                java.lang.reflect.Method getLoggerMethod = loggerContextClass.getMethod("getLogger", String.class);
                java.lang.reflect.Method setLevelMethod = loggerClass.getMethod("setLevel", levelClass);
                java.lang.reflect.Method setAdditiveMethod = loggerClass.getMethod("setAdditive", boolean.class);
                
                // 设置根日志级别
                if (rootLevel != null) {
                    Object rootLogger = getLoggerMethod.invoke(loggerContext, org.slf4j.Logger.ROOT_LOGGER_NAME);
                    Object logbackLevel = convertToLogbackLevel(rootLevel);
                    if (logbackLevel != null) {
                        setLevelMethod.invoke(rootLogger, logbackLevel);
                    }
                }
                
                // 为各个模块设置日志级别
                if (moduleLevels != null) {
                    for (Map.Entry<String, LogLevel> entry : moduleLevels.entrySet()) {
                        String moduleName = entry.getKey();
                        LogLevel level = entry.getValue();
                        Object logger = getLoggerMethod.invoke(loggerContext, moduleName);
                        Object logbackLevel = convertToLogbackLevel(level);
                        if (logbackLevel != null) {
                            setLevelMethod.invoke(logger, logbackLevel);
                        }
                        // 如果模块名是完整的类名，设置不继承父logger的级别
                        if (moduleName.contains(".") && !moduleName.startsWith(PlayerDollAddon.MODID + ".")) {
                            setAdditiveMethod.invoke(logger, false);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            // logback类不存在，忽略
        } catch (NoClassDefFoundError e) {
            // logback类不存在，忽略
        } catch (Exception e) {
            // 配置失败，忽略
        }
    }
    
    /**
     * 将LogLevel转换为logback的Level（使用反射避免编译时依赖）
     */
    private static Object convertToLogbackLevel(LogLevel level) {
        try {
            Class<?> levelClass = Class.forName("ch.qos.logback.classic.Level");
            String levelName;
            
            if (level == null) {
                levelName = "INFO";
            } else {
                switch (level) {
                    case DEBUG:
                        levelName = "DEBUG";
                        break;
                    case INFO:
                        levelName = "INFO";
                        break;
                    case WARN:
                        levelName = "WARN";
                        break;
                    case ERROR:
                        levelName = "ERROR";
                        break;
                    case OFF:
                        levelName = "OFF";
                        break;
                    default:
                        levelName = "INFO";
                        break;
                }
            }
            
            // 使用静态字段获取Level实例
            java.lang.reflect.Field levelField = levelClass.getField(levelName);
            return levelField.get(null);
        } catch (Exception e) {
            // 如果反射失败，返回null（调用者会处理）
            return null;
        }
    }
    
    /**
     * 配置日志级别：除了指定模块外，其他模块只输出WARN及以上级别
     * 
     * @param debugModules 需要输出DEBUG级别的模块列表（模块名或类名）
     */
    public static void configureLogLevelsForDebugModules(String... debugModules) {
        Map<String, LogLevel> moduleLevels = new HashMap<>();
        
        // 为指定的调试模块设置DEBUG级别
        for (String module : debugModules) {
            moduleLevels.put(module, LogLevel.DEBUG);
        }
        
        // 设置根日志级别为WARN
        configureFrameworkLogLevels(LogLevel.WARN, moduleLevels);
        
        // 同时更新ModuleLogger的模块级别（通过 LogModuleConfig）
        for (String module : debugModules) {
            LogModuleConfig.setModuleLevel(module, LogLevel.DEBUG);
        }
        // 设置全局级别为 WARN（通过 LogModuleConfig）
        for (String moduleName : loggers.keySet()) {
            if (!java.util.Arrays.asList(debugModules).contains(moduleName)) {
                LogModuleConfig.setModuleLevel(moduleName, LogLevel.WARN);
            }
        }
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
            // 优先从 LogModuleConfig 读取配置，如果未配置则使用默认级别
            if (!moduleLevels.containsKey(moduleName)) {
                LogLevel configLevel = LogModuleConfig.getModuleLevel(moduleName);
                moduleLevels.put(moduleName, configLevel);
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
     * 优先从 LogModuleConfig 读取配置
     * 
     * @param moduleName 模块名称
     * @return 日志级别，如果未设置则返回默认级别
     */
    public static LogLevel getModuleLevel(String moduleName) {
        // 如果已经在运行时设置过，使用运行时设置
        if (moduleLevels.containsKey(moduleName)) {
            return moduleLevels.get(moduleName);
        }
        // 否则从 LogModuleConfig 读取
        return LogModuleConfig.getModuleLevel(moduleName);
    }
    
    /**
     * 设置指定模块的日志级别（内部方法，仅允许 LogModuleConfig 调用）
     * 
     * <p><strong>注意：</strong>此方法已改为包私有，外部代码不应直接调用。
     * 请使用 {@link LogModuleConfig#setModuleLevel(String, LogLevel)} 来修改日志级别。</p>
     * 
     * @param moduleName 模块名称
     * @param level 日志级别
     */
    static void setModuleLevelInternal(String moduleName, LogLevel level) {
        moduleLevels.put(moduleName, level);
        // 同步更新旧版本的开关状态（兼容性）
        moduleStates.put(moduleName, level != LogLevel.OFF);
    }
    
    /**
     * 设置指定模块的日志级别
     * 
     * @deprecated 请使用 {@link LogModuleConfig#setModuleLevel(String, LogLevel)} 来修改日志级别。
     *             所有日志级别的修改都应该通过 LogModuleConfig 进行。
     * 
     * @param moduleName 模块名称
     * @param level 日志级别
     */
    @Deprecated
    public static void setModuleLevel(String moduleName, LogLevel level) {
        // 重定向到 LogModuleConfig
        LogModuleConfig.setModuleLevel(moduleName, level);
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
     * 
     * @deprecated 请使用 {@link LogModuleConfig#setModuleLevel(String, LogLevel)} 来修改日志级别。
     *             所有日志级别的修改都应该通过 LogModuleConfig 进行。
     * 
     * @param moduleName 模块名称
     * @param enabled 是否启用
     */
    @Deprecated
    public static void setModuleEnabled(String moduleName, boolean enabled) {
        // 重定向到 LogModuleConfig
        LogModuleConfig.setModuleLevel(moduleName, enabled ? LogLevel.INFO : LogLevel.OFF);
    }
    
    /**
     * 设置全局日志开关（内部方法，仅允许 LogModuleConfig 调用）
     * 
     * <p><strong>注意：</strong>此方法已改为包私有，外部代码不应直接调用。
     * 请使用 {@link LogModuleConfig} 来修改日志配置。</p>
     * 
     * @param enabled 是否启用所有模块日志
     */
    static void setGlobalEnabledInternal(boolean enabled) {
        globalEnabled = enabled;
    }
    
    /**
     * 设置全局日志开关
     * 
     * @deprecated 请使用 {@link LogModuleConfig} 来修改日志配置。
     *             所有日志级别的修改都应该通过 LogModuleConfig 进行。
     * 
     * @param enabled 是否启用所有模块日志
     */
    @Deprecated
    public static void setGlobalEnabled(boolean enabled) {
        // 重定向到 LogModuleConfig（如果 LogModuleConfig 有对应方法）
        setGlobalEnabledInternal(enabled);
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
     * 
     * @deprecated 请使用 {@link LogModuleConfig} 来修改日志配置。
     *             所有日志级别的修改都应该通过 LogModuleConfig 进行。
     */
    @Deprecated
    public static void disableAll() {
        setGlobalEnabledInternal(false);
    }
    
    /**
     * 一键启用所有模块日志（设置为默认级别）
     * 
     * @deprecated 请使用 {@link LogModuleConfig#initializeModuleLevels()} 来初始化日志级别。
     *             所有日志级别的修改都应该通过 LogModuleConfig 进行。
     */
    @Deprecated
    public static void enableAll() {
        setGlobalEnabledInternal(true);
        // 重新初始化所有模块级别
        LogModuleConfig.initializeModuleLevels();
    }
    
    /**
     * 设置全局日志级别（为所有模块设置相同的级别）
     * 
     * @deprecated 请使用 {@link LogModuleConfig} 来修改日志配置。
     *             所有日志级别的修改都应该通过 LogModuleConfig 进行。
     * 
     * @param level 日志级别
     */
    @Deprecated
    public static void setGlobalLevel(LogLevel level) {
        // 重定向到 LogModuleConfig
        for (String moduleName : loggers.keySet()) {
            LogModuleConfig.setModuleLevel(moduleName, level);
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

