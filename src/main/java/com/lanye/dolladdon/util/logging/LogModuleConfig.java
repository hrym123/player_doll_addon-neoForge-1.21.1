package com.lanye.dolladdon.util.logging;

/**
 * 日志模块配置类
 * 集中管理所有模块的声明、日志级别和模板
 * 
 * <p><strong>重要：</strong>此类的所有配置都是写死的，不可修改！
 * 配置在编译时确定，运行时无法更改。</p>
 * 
 * <p>使用示例：</p>
 * <pre>{@code
 * // 1. 使用模块常量
 * String moduleName = LogModuleConfig.MODULE_ENTITY;
 * 
 * // 2. 读取配置（唯一允许的操作）
 * LogLevel level = LogModuleConfig.getModuleLevel(moduleName);
 * String template = LogModuleConfig.getLogTemplate(moduleName);
 * 
 * // 3. 使用ModuleLogger记录日志
 * ModuleLogger.info(moduleName, "日志消息");
 *
 * // ❌ 注意：配置不可修改！
 * // LogModuleConfig.setModuleLevel(moduleName, LogLevel.DEBUG); // 编译错误！
 * }</pre>
 */
public class LogModuleConfig {
    
    // ==================== 模块常量声明 ====================
    
    /**
     * 主模块（用于主初始化日志）
     */
    public static final String MODULE_MAIN = "main";
    
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
     * 3D渲染偏移模块
     */
    public static final String MODULE_RENDER_3D_OFFSET = "render.3d_offset";
    
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
    
    // ==================== 模块默认日志级别配置（写死，不可修改） ====================
    
    // 主模块：默认WARN级别
    private static final LogLevel LEVEL_MAIN = LogLevel.WARN;
    
    // 实体相关模块：默认WARN级别
    private static final LogLevel LEVEL_ENTITY = LogLevel.WARN;
    private static final LogLevel LEVEL_ENTITY_POSE = LogLevel.WARN;
    private static final LogLevel LEVEL_ENTITY_ACTION = LogLevel.WARN;
    private static final LogLevel LEVEL_ENTITY_INTERACT = LogLevel.WARN;
    private static final LogLevel LEVEL_ENTITY_NBT = LogLevel.WARN;
        
    // 渲染模块：默认WARN级别
    private static final LogLevel LEVEL_RENDER = LogLevel.WARN;
        
    // 3D皮肤层模块：默认DEBUG级别（特殊配置）
    private static final LogLevel LEVEL_3D_SKIN_LAYERS = LogLevel.WARN;
    
    // 3D渲染偏移模块：默认WARN级别
    private static final LogLevel LEVEL_RENDER_3D_OFFSET = LogLevel.DEBUG;
        
    // 资源管理模块：默认WARN级别
    private static final LogLevel LEVEL_RESOURCE = LogLevel.WARN;
    private static final LogLevel LEVEL_RESOURCE_GENERATOR = LogLevel.WARN;
    private static final LogLevel LEVEL_TEXTURE_SCANNER = LogLevel.WARN;
        
    // 加载器模块：默认WARN级别
    private static final LogLevel LEVEL_POSE_LOADER = LogLevel.WARN;
    private static final LogLevel LEVEL_ACTION_LOADER = LogLevel.WARN;
    
    // ==================== 日志模板配置（写死，不可修改） ====================
    
        // 主模块模板
    private static final String TEMPLATE_MAIN = "[主模块] {}";
        
        // 实体模块模板
    private static final String TEMPLATE_ENTITY = "[实体] {}";
    private static final String TEMPLATE_ENTITY_POSE = "[实体-姿态] {}";
    private static final String TEMPLATE_ENTITY_ACTION = "[实体-动作] {}";
    private static final String TEMPLATE_ENTITY_INTERACT = "[实体-交互] {}";
    private static final String TEMPLATE_ENTITY_NBT = "[实体-NBT] {}";
        
        // 渲染模块模板
    private static final String TEMPLATE_RENDER = "[渲染] {}";
        
        // 3D皮肤层模块模板
    private static final String TEMPLATE_3D_SKIN_LAYERS = "[3D皮肤层] {}";
    
        // 3D渲染偏移模块模板
    private static final String TEMPLATE_RENDER_3D_OFFSET = "[3D渲染偏移] {}";
        
        // 资源管理模块模板
    private static final String TEMPLATE_RESOURCE = "[资源] {}";
    private static final String TEMPLATE_RESOURCE_GENERATOR = "[资源生成] {}";
    private static final String TEMPLATE_TEXTURE_SCANNER = "[纹理扫描] {}";
        
        // 加载器模块模板
    private static final String TEMPLATE_POSE_LOADER = "[姿态加载] {}";
    private static final String TEMPLATE_ACTION_LOADER = "[动作加载] {}";
    
    // ==================== 配置读取方法 ====================
    
    /**
     * 获取模块的默认日志级别
     * 配置写死，不可修改
     * 
     * @param moduleName 模块名称
     * @return 日志级别，如果模块未配置则返回默认级别（INFO）
     */
    public static LogLevel getModuleLevel(String moduleName) {
        if (moduleName == null) {
            return LogLevel.INFO;
    }
    
        switch (moduleName) {
            case MODULE_MAIN: return LEVEL_MAIN;
            case MODULE_ENTITY: return LEVEL_ENTITY;
            case MODULE_ENTITY_POSE: return LEVEL_ENTITY_POSE;
            case MODULE_ENTITY_ACTION: return LEVEL_ENTITY_ACTION;
            case MODULE_ENTITY_INTERACT: return LEVEL_ENTITY_INTERACT;
            case MODULE_ENTITY_NBT: return LEVEL_ENTITY_NBT;
            case MODULE_RENDER: return LEVEL_RENDER;
            case MODULE_3D_SKIN_LAYERS: return LEVEL_3D_SKIN_LAYERS;
            case MODULE_RENDER_3D_OFFSET: return LEVEL_RENDER_3D_OFFSET;
            case MODULE_RESOURCE: return LEVEL_RESOURCE;
            case MODULE_POSE_LOADER: return LEVEL_POSE_LOADER;
            case MODULE_ACTION_LOADER: return LEVEL_ACTION_LOADER;
            case MODULE_RESOURCE_GENERATOR: return LEVEL_RESOURCE_GENERATOR;
            case MODULE_TEXTURE_SCANNER: return LEVEL_TEXTURE_SCANNER;
            default: return LogLevel.INFO;
        }
    }
    
    /**
     * 获取模块的日志模板
     * 配置写死，不可修改
     * 
     * @param moduleName 模块名称
     * @return 日志模板，如果模块未配置则返回默认模板 "{}"
     */
    public static String getLogTemplate(String moduleName) {
        if (moduleName == null) {
            return "{}";
    }
    
        switch (moduleName) {
            case MODULE_MAIN: return TEMPLATE_MAIN;
            case MODULE_ENTITY: return TEMPLATE_ENTITY;
            case MODULE_ENTITY_POSE: return TEMPLATE_ENTITY_POSE;
            case MODULE_ENTITY_ACTION: return TEMPLATE_ENTITY_ACTION;
            case MODULE_ENTITY_INTERACT: return TEMPLATE_ENTITY_INTERACT;
            case MODULE_ENTITY_NBT: return TEMPLATE_ENTITY_NBT;
            case MODULE_RENDER: return TEMPLATE_RENDER;
            case MODULE_3D_SKIN_LAYERS: return TEMPLATE_3D_SKIN_LAYERS;
            case MODULE_RENDER_3D_OFFSET: return TEMPLATE_RENDER_3D_OFFSET;
            case MODULE_RESOURCE: return TEMPLATE_RESOURCE;
            case MODULE_POSE_LOADER: return TEMPLATE_POSE_LOADER;
            case MODULE_ACTION_LOADER: return TEMPLATE_ACTION_LOADER;
            case MODULE_RESOURCE_GENERATOR: return TEMPLATE_RESOURCE_GENERATOR;
            case MODULE_TEXTURE_SCANNER: return TEMPLATE_TEXTURE_SCANNER;
            default: return "{}";
        }
    }
    
    /**
     * 获取所有模块名称
     * 配置写死，不可修改
     * 
     * @return 所有模块名称的数组
     */
    public static String[] getAllModuleNames() {
        return new String[] {
            MODULE_MAIN,
            MODULE_ENTITY,
            MODULE_ENTITY_POSE,
            MODULE_ENTITY_ACTION,
            MODULE_ENTITY_INTERACT,
            MODULE_ENTITY_NBT,
            MODULE_RENDER,
            MODULE_3D_SKIN_LAYERS,
            MODULE_RENDER_3D_OFFSET,
            MODULE_RESOURCE,
            MODULE_POSE_LOADER,
            MODULE_ACTION_LOADER,
            MODULE_RESOURCE_GENERATOR,
            MODULE_TEXTURE_SCANNER
        };
    }
}