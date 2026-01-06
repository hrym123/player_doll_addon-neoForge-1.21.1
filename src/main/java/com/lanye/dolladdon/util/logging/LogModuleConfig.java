package com.lanye.dolladdon.util.logging;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 日志模块配置类
 * <p>
 * 集中管理所有日志模块的配置信息，包括模块名称、默认日志级别和日志模板。
 * 所有配置在编译时确定，运行时不可修改。
 * </p>
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * // 使用模块常量
 * ModuleLogger.info(LogModuleConfig.MODULE_ENTITY, "实体已创建");
 * 
 * // 获取模块配置
 * LogLevel level = LogModuleConfig.getModuleLevel("entity");
 * String template = LogModuleConfig.getLogTemplate("entity");
 * }</pre>
 */
public class LogModuleConfig {
    
    /**
     * 模块信息记录类
     * 
     * @param name 模块名称（唯一标识）
     * @param level 默认日志级别
     * @param template 日志模板，使用 {} 作为占位符
     */
    public record ModuleInfo(String name, LogLevel level, String template) {}
    
    // ==================== 模块常量 ====================
    
    /** 主模块（用于主初始化日志） */
    public static final String MODULE_MAIN = "main";
    
    /** 实体基础模块 */
    public static final String MODULE_ENTITY = "entity";
    
    /** 实体姿态模块 */
    public static final String MODULE_ENTITY_POSE = "entity.pose";
    
    /** 实体动作模块 */
    public static final String MODULE_ENTITY_ACTION = "entity.action";
    
    /** 实体交互模块 */
    public static final String MODULE_ENTITY_INTERACT = "entity.interact";
    
    /** 实体NBT模块 */
    public static final String MODULE_ENTITY_NBT = "entity.nbt";
    
    /** 渲染模块 */
    public static final String MODULE_RENDER = "render";
    
    /** 3D皮肤层模块 */
    public static final String MODULE_3D_SKIN_LAYERS = "3d_skin_layers";
    
    /** 3D渲染偏移模块 */
    public static final String MODULE_RENDER_3D_OFFSET = "render.3d_offset";
    
    /** 资源管理模块 */
    public static final String MODULE_RESOURCE = "resource";
    
    /** 资源生成器模块 */
    public static final String MODULE_RESOURCE_GENERATOR = "resource.generator";
    
    /** 纹理扫描器模块 */
    public static final String MODULE_TEXTURE_SCANNER = "texture.scanner";
    
    /** 姿态加载器模块 */
    public static final String MODULE_POSE_LOADER = "pose.loader";
    
    /** 动作加载器模块 */
    public static final String MODULE_ACTION_LOADER = "action.loader";
    
    // ==================== 模块配置存储 ====================
    
    /**
     * 所有模块配置的不可变映射表
     * Key: 模块名称, Value: 模块信息
     */
    private static final Map<String, ModuleInfo> MODULES;
    
    /**
     * 静态初始化块：初始化所有模块配置
     * 配置在编译时确定，运行时不可修改
     */
    static {
        Map<String, ModuleInfo> modules = new HashMap<>();
        
        // 主模块
        modules.put(MODULE_MAIN, new ModuleInfo(MODULE_MAIN, LogLevel.WARN, "[主模块] {}"));
        
        // 实体相关模块
        modules.put(MODULE_ENTITY, new ModuleInfo(MODULE_ENTITY, LogLevel.WARN, "[实体] {}"));
        modules.put(MODULE_ENTITY_POSE, new ModuleInfo(MODULE_ENTITY_POSE, LogLevel.DEBUG, "[实体-姿态] {}"));
        modules.put(MODULE_ENTITY_ACTION, new ModuleInfo(MODULE_ENTITY_ACTION, LogLevel.DEBUG, "[实体-动作] {}"));
        modules.put(MODULE_ENTITY_INTERACT, new ModuleInfo(MODULE_ENTITY_INTERACT, LogLevel.WARN, "[实体-交互] {}"));
        modules.put(MODULE_ENTITY_NBT, new ModuleInfo(MODULE_ENTITY_NBT, LogLevel.WARN, "[实体-NBT] {}"));
        
        // 渲染模块
        modules.put(MODULE_RENDER, new ModuleInfo(MODULE_RENDER, LogLevel.WARN, "[渲染] {}"));
        modules.put(MODULE_3D_SKIN_LAYERS, new ModuleInfo(MODULE_3D_SKIN_LAYERS, LogLevel.WARN, "[3D皮肤层] {}"));
        modules.put(MODULE_RENDER_3D_OFFSET, new ModuleInfo(MODULE_RENDER_3D_OFFSET, LogLevel.WARN, "[3D渲染偏移] {}"));
        
        // 资源管理模块
        modules.put(MODULE_RESOURCE, new ModuleInfo(MODULE_RESOURCE, LogLevel.WARN, "[资源] {}"));
        modules.put(MODULE_RESOURCE_GENERATOR, new ModuleInfo(MODULE_RESOURCE_GENERATOR, LogLevel.WARN, "[资源生成] {}"));
        modules.put(MODULE_TEXTURE_SCANNER, new ModuleInfo(MODULE_TEXTURE_SCANNER, LogLevel.WARN, "[纹理扫描] {}"));
        
        // 加载器模块
        modules.put(MODULE_POSE_LOADER, new ModuleInfo(MODULE_POSE_LOADER, LogLevel.WARN, "[姿态加载] {}"));
        modules.put(MODULE_ACTION_LOADER, new ModuleInfo(MODULE_ACTION_LOADER, LogLevel.DEBUG, "[动作加载] {}"));
        
        MODULES = Collections.unmodifiableMap(modules);
    }
    
    // ==================== 配置查询方法 ====================
    
    /**
     * 获取指定模块的默认日志级别
     * 
     * @param moduleName 模块名称
     * @return 日志级别，如果模块不存在则返回 {@link LogLevel#INFO}
     */
    public static LogLevel getModuleLevel(String moduleName) {
        ModuleInfo info = MODULES.get(moduleName);
        return info != null ? info.level() : LogLevel.INFO;
    }
    
    /**
     * 获取指定模块的日志模板
     * 
     * @param moduleName 模块名称
     * @return 日志模板，如果模块不存在则返回默认模板 "{}"
     */
    public static String getLogTemplate(String moduleName) {
        ModuleInfo info = MODULES.get(moduleName);
        return info != null ? info.template() : "{}";
    }
    
    /**
     * 获取所有模块名称
     * 
     * @return 所有模块名称的数组
     */
    public static String[] getAllModuleNames() {
        return MODULES.keySet().toArray(new String[0]);
    }
    
    /**
     * 获取所有模块信息
     * 
     * @return 所有模块信息的不可变列表
     */
    public static List<ModuleInfo> getAllModules() {
        return MODULES.values().stream().collect(Collectors.toUnmodifiableList());
    }
}