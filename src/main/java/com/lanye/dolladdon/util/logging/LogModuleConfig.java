package com.lanye.dolladdon.util.logging;

import java.util.HashMap;
import java.util.Map;

/**
 * 日志模块配置类
 * 集中管理所有模块的声明、日志级别和模板
 * 
 * <p>所有日志相关的类都应该从这个类读取模块配置</p>
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * // 1. 使用模块常量
 * String moduleName = LogModuleConfig.MODULE_ENTITY;
 * 
 * // 2. 获取模块的日志级别
 * LogLevel level = LogModuleConfig.getModuleLevel(moduleName);
 * 
 * // 3. 获取日志模板
 * String template = LogModuleConfig.getLogTemplate(moduleName);
 * 
 * // 4. 使用ModuleLogger记录日志
 * ModuleLogger.info(moduleName, template, args);
 * }</pre>
 */
public class LogModuleConfig {
    
    // ==================== 模块常量声明 ====================
    
    /**
     * 实体基础模块
     */
    public static final String MODULE_ENTITY = "entity";
    
    /**
     * 实体姿态模块
     */
    public static final String MODULE_ENTITY_POSE = "entity.pose";
    
    /**
     * 实体动作模块
     */
    public static final String MODULE_ENTITY_ACTION = "entity.action";
    
    /**
     * 实体交互模块
     */
    public static final String MODULE_ENTITY_INTERACT = "entity.interact";
    
    /**
     * 实体NBT模块
     */
    public static final String MODULE_ENTITY_NBT = "entity.nbt";
    
    /**
     * 渲染模块
     */
    public static final String MODULE_RENDER = "render";
    
    /**
     * 3D皮肤层模块
     */
    public static final String MODULE_3D_SKIN_LAYERS = "3d_skin_layers";
    
    /**
     * 资源管理模块
     */
    public static final String MODULE_RESOURCE = "resource";
    
    /**
     * 姿态加载器模块
     */
    public static final String MODULE_POSE_LOADER = "pose.loader";
    
    /**
     * 动作加载器模块
     */
    public static final String MODULE_ACTION_LOADER = "action.loader";
    
    /**
     * 资源生成器模块
     */
    public static final String MODULE_RESOURCE_GENERATOR = "resource.generator";
    
    /**
     * 纹理扫描器模块
     */
    public static final String MODULE_TEXTURE_SCANNER = "texture.scanner";
    
    // ==================== 模块默认日志级别配置 ====================
    
    private static final Map<String, LogLevel> DEFAULT_MODULE_LEVELS = new HashMap<>();
    
    static {
        // 实体相关模块：默认INFO级别
        DEFAULT_MODULE_LEVELS.put(MODULE_ENTITY, LogLevel.WARN);
        DEFAULT_MODULE_LEVELS.put(MODULE_ENTITY_POSE, LogLevel.WARN);
        DEFAULT_MODULE_LEVELS.put(MODULE_ENTITY_ACTION, LogLevel.WARN);
        DEFAULT_MODULE_LEVELS.put(MODULE_ENTITY_INTERACT, LogLevel.WARN);
        DEFAULT_MODULE_LEVELS.put(MODULE_ENTITY_NBT, LogLevel.WARN);
        
        // 渲染模块：默认WARN级别
        DEFAULT_MODULE_LEVELS.put(MODULE_RENDER, LogLevel.WARN);
        
        // 3D皮肤层模块：默认DEBUG级别（用于调试）
        DEFAULT_MODULE_LEVELS.put(MODULE_3D_SKIN_LAYERS, LogLevel.DEBUG);
        
        // 资源管理模块：默认INFO级别
        DEFAULT_MODULE_LEVELS.put(MODULE_RESOURCE, LogLevel.WARN);
        DEFAULT_MODULE_LEVELS.put(MODULE_RESOURCE_GENERATOR, LogLevel.WARN);
        DEFAULT_MODULE_LEVELS.put(MODULE_TEXTURE_SCANNER, LogLevel.WARN);
        
        // 加载器模块：默认INFO级别
        DEFAULT_MODULE_LEVELS.put(MODULE_POSE_LOADER, LogLevel.WARN);
        DEFAULT_MODULE_LEVELS.put(MODULE_ACTION_LOADER, LogLevel.WARN);
    }
    
    // ==================== 日志模板配置 ====================
    
    private static final Map<String, String> LOG_TEMPLATES = new HashMap<>();
    
    static {
        // 实体模块模板
        LOG_TEMPLATES.put(MODULE_ENTITY, "[实体] {}");
        LOG_TEMPLATES.put(MODULE_ENTITY_POSE, "[实体-姿态] {}");
        LOG_TEMPLATES.put(MODULE_ENTITY_ACTION, "[实体-动作] {}");
        LOG_TEMPLATES.put(MODULE_ENTITY_INTERACT, "[实体-交互] {}");
        LOG_TEMPLATES.put(MODULE_ENTITY_NBT, "[实体-NBT] {}");
        
        // 渲染模块模板
        LOG_TEMPLATES.put(MODULE_RENDER, "[渲染] {}");
        
        // 3D皮肤层模块模板
        LOG_TEMPLATES.put(MODULE_3D_SKIN_LAYERS, "[3D皮肤层] {}");
        
        // 资源管理模块模板
        LOG_TEMPLATES.put(MODULE_RESOURCE, "[资源] {}");
        LOG_TEMPLATES.put(MODULE_RESOURCE_GENERATOR, "[资源生成] {}");
        LOG_TEMPLATES.put(MODULE_TEXTURE_SCANNER, "[纹理扫描] {}");
        
        // 加载器模块模板
        LOG_TEMPLATES.put(MODULE_POSE_LOADER, "[姿态加载] {}");
        LOG_TEMPLATES.put(MODULE_ACTION_LOADER, "[动作加载] {}");
    }
    
    // ==================== 公共方法 ====================
    
    /**
     * 获取模块的默认日志级别
     * 
     * @param moduleName 模块名称
     * @return 日志级别，如果模块未配置则返回默认级别（INFO）
     */
    public static LogLevel getModuleLevel(String moduleName) {
        return DEFAULT_MODULE_LEVELS.getOrDefault(moduleName, LogLevel.INFO);
    }
    
    /**
     * 设置模块的日志级别
     * 这是唯一允许修改日志级别的方法
     * 
     * @param moduleName 模块名称
     * @param level 日志级别
     */
    public static void setModuleLevel(String moduleName, LogLevel level) {
        DEFAULT_MODULE_LEVELS.put(moduleName, level);
        // 直接调用 ModuleLogger 的内部方法
        ModuleLogger.setModuleLevelInternal(moduleName, level);
    }
    
    /**
     * 获取模块的日志模板
     * 
     * @param moduleName 模块名称
     * @return 日志模板，如果模块未配置则返回默认模板 "{}"
     */
    public static String getLogTemplate(String moduleName) {
        return LOG_TEMPLATES.getOrDefault(moduleName, "{}");
    }
    
    /**
     * 设置模块的日志模板
     * 
     * @param moduleName 模块名称
     * @param template 日志模板（使用 {} 作为占位符）
     */
    public static void setLogTemplate(String moduleName, String template) {
        LOG_TEMPLATES.put(moduleName, template);
    }
    
    /**
     * 注册新模块
     * 
     * @param moduleName 模块名称
     * @param defaultLevel 默认日志级别
     * @param template 日志模板
     */
    public static void registerModule(String moduleName, LogLevel defaultLevel, String template) {
        DEFAULT_MODULE_LEVELS.put(moduleName, defaultLevel);
        LOG_TEMPLATES.put(moduleName, template);
        // 同步到ModuleLogger（使用内部方法）
        ModuleLogger.setModuleLevelInternal(moduleName, defaultLevel);
    }
    
    /**
     * 初始化所有模块的日志级别
     * 应该在应用启动时调用，确保所有模块使用配置的默认级别
     */
    public static void initializeModuleLevels() {
        for (Map.Entry<String, LogLevel> entry : DEFAULT_MODULE_LEVELS.entrySet()) {
            // 使用内部方法直接设置
            ModuleLogger.setModuleLevelInternal(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * 设置全局日志开关
     * 
     * @param enabled 是否启用所有模块日志
     */
    public static void setGlobalEnabled(boolean enabled) {
        ModuleLogger.setGlobalEnabledInternal(enabled);
    }
    
    /**
     * 获取全局日志开关状态
     * 
     * @return 全局开关状态
     */
    public static boolean isGlobalEnabled() {
        return ModuleLogger.isGlobalEnabled();
    }
    
    /**
     * 设置所有模块的日志级别为指定级别
     * 
     * @param level 日志级别
     */
    public static void setGlobalLevel(LogLevel level) {
        for (String moduleName : DEFAULT_MODULE_LEVELS.keySet()) {
            setModuleLevel(moduleName, level);
        }
    }
    
    /**
     * 一键禁用所有模块日志
     */
    public static void disableAll() {
        setGlobalEnabled(false);
    }
    
    /**
     * 一键启用所有模块日志（恢复到默认级别）
     */
    public static void enableAll() {
        setGlobalEnabled(true);
        initializeModuleLevels();
    }
    
    /**
     * 获取所有已注册的模块名称
     * 
     * @return 模块名称集合
     */
    public static java.util.Set<String> getAllModules() {
        return DEFAULT_MODULE_LEVELS.keySet();
    }
    
    /**
     * 检查模块是否已注册
     * 
     * @param moduleName 模块名称
     * @return 是否已注册
     */
    public static boolean isModuleRegistered(String moduleName) {
        return DEFAULT_MODULE_LEVELS.containsKey(moduleName);
    }
    
    /**
     * 获取模块的完整日志消息（应用模板）
     * 
     * @param moduleName 模块名称
     * @param message 原始消息
     * @return 应用模板后的完整消息
     */
    public static String formatLogMessage(String moduleName, String message) {
        String template = getLogTemplate(moduleName);
        return template.replace("{}", message);
    }
    
    /**
     * 获取模块的完整日志消息（应用模板，支持参数）
     * 
     * @param moduleName 模块名称
     * @param message 原始消息模板
     * @param args 参数
     * @return 应用模板后的完整消息
     */
    public static String formatLogMessage(String moduleName, String message, Object... args) {
        String template = getLogTemplate(moduleName);
        // 先格式化参数
        String formattedMessage = formatMessage(message, args);
        // 再应用模块模板
        return template.replace("{}", formattedMessage);
    }
    
    /**
     * 格式化消息（简单的占位符替换）
     * 注意：这是一个简单的实现，不支持复杂的格式化
     */
    private static String formatMessage(String template, Object... args) {
        if (args == null || args.length == 0) {
            return template;
        }
        
        String result = template;
        for (Object arg : args) {
            // 简单替换第一个 {}
            int index = result.indexOf("{}");
            if (index >= 0) {
                result = result.substring(0, index) + 
                        (arg != null ? arg.toString() : "null") + 
                        result.substring(index + 2);
            }
        }
        return result;
    }
}
