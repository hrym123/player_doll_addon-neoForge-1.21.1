package com.lanye.dolladdon.util.logging;

/**
 * 日志模块配置中心
 * 集中管理所有模块常量、默认日志级别和日志模板
 * 
 * 设计原则：
 * 1. 所有配置写死在代码中，编译时确定
 * 2. 运行时只读，不可修改
 * 3. 支持通过注释快速禁用整个模块的日志
 * 
 * 快速禁用模块日志的方法：
 * 1. 将模块的默认级别设置为 LogLevel.OFF
 * 2. 或删除对应的 Mixin 类文件
 * 3. 或从 player_doll.mixins.json 中移除对应的 Mixin 配置
 */
public class LogModuleConfig {
    
    // ==================== 模块常量声明 ====================
    
    /** 主模块 */
    public static final String MODULE_MAIN = "main";
    
    /** 实体基础模块 */
    public static final String MODULE_ENTITY = "entity";
    
    /** 实体交互模块 */
    public static final String MODULE_ENTITY_INTERACT = "entity.interact";
    
    /** 实体姿态模块 */
    public static final String MODULE_ENTITY_POSE = "entity.pose";
    
    /** 实体动作模块 - 可通过设置级别为 OFF 一键关闭所有动作相关日志 */
    public static final String MODULE_ENTITY_ACTION = "entity.action";
    
    /** 实体NBT模块 */
    public static final String MODULE_ENTITY_NBT = "entity.nbt";
    
    /** 渲染模块 */
    public static final String MODULE_RENDER = "render";
    
    /** 3D皮肤层模块 */
    public static final String MODULE_3D_SKIN_LAYERS = "3d_skin_layers";
    
    /** 资源管理模块 */
    public static final String MODULE_RESOURCE = "resource";
    
    /** 姿态加载器模块 */
    public static final String MODULE_POSE_LOADER = "pose_loader";
    
    /** 动作加载器模块 - 可通过设置级别为 OFF 一键关闭所有动作加载日志 */
    public static final String MODULE_ACTION_LOADER = "action_loader";
    
    /** 资源生成器模块 */
    public static final String MODULE_RESOURCE_GENERATOR = "resource_generator";
    
    /** 纹理扫描器模块 */
    public static final String MODULE_TEXTURE_SCANNER = "texture_scanner";
    
    /** 调试棒模块 - 动作调试棒 */
    public static final String MODULE_DEBUG_STICK_ACTION = "debug_stick.action";
    
    /** 调试棒模块 - 姿态调试棒 */
    public static final String MODULE_DEBUG_STICK_POSE = "debug_stick.pose";
    
    /** 命令模块 - 玩偶皮肤指令 */
    public static final String MODULE_COMMAND = "command";
    
    // ==================== 默认日志级别配置 ====================
    // 注意：修改这里的级别即可控制对应模块的日志输出
    // 设置为 LogLevel.OFF 即可完全禁用该模块的日志
    
    private static final LogLevel LEVEL_MAIN = LogLevel.WARN;  // 主模块日志级别
    private static final LogLevel LEVEL_ENTITY = LogLevel.WARN;  // 实体基础模块日志级别
    private static final LogLevel LEVEL_ENTITY_INTERACT = LogLevel.DEBUG;  // 实体交互模块日志级别，当前正在调试交互问题
    private static final LogLevel LEVEL_ENTITY_POSE = LogLevel.WARN;  // 实体姿态模块日志级别
    private static final LogLevel LEVEL_ENTITY_ACTION = LogLevel.DEBUG;  // 实体动作模块日志级别，当前正在调试动作问题
    private static final LogLevel LEVEL_ENTITY_NBT = LogLevel.DEBUG;  // 实体NBT模块日志级别，当前正在调试皮肤问题
    private static final LogLevel LEVEL_RENDER = LogLevel.DEBUG;  // 渲染模块日志级别，当前正在调试皮肤问题
    private static final LogLevel LEVEL_3D_SKIN_LAYERS = LogLevel.WARN;  // 3D皮肤层模块日志级别
    private static final LogLevel LEVEL_RESOURCE = LogLevel.DEBUG;  // 资源管理模块日志级别，当前正在调试皮肤问题
    private static final LogLevel LEVEL_POSE_LOADER = LogLevel.WARN;  // 姿态加载器模块日志级别
    private static final LogLevel LEVEL_ACTION_LOADER = LogLevel.WARN;  // 动作加载器模块日志级别，改为 OFF 可一键关闭所有动作加载日志
    private static final LogLevel LEVEL_RESOURCE_GENERATOR = LogLevel.WARN;  // 资源生成器模块日志级别
    private static final LogLevel LEVEL_TEXTURE_SCANNER = LogLevel.WARN;  // 纹理扫描器模块日志级别
    private static final LogLevel LEVEL_DEBUG_STICK_ACTION = LogLevel.DEBUG;  // 动作调试棒模块日志级别，当前正在调试的模块
    private static final LogLevel LEVEL_DEBUG_STICK_POSE = LogLevel.WARN;  // 姿态调试棒模块日志级别
    private static final LogLevel LEVEL_COMMAND = LogLevel.DEBUG;  // 命令模块日志级别，当前正在调试皮肤问题
    
    // ==================== 日志模板配置 ====================
    
    private static final String TEMPLATE_MAIN = "[主模块] {}";
    private static final String TEMPLATE_ENTITY = "[实体] {}";
    private static final String TEMPLATE_ENTITY_INTERACT = "[实体-交互] {}";
    private static final String TEMPLATE_ENTITY_POSE = "[实体-姿态] {}";
    private static final String TEMPLATE_ENTITY_ACTION = "[实体-动作] {}";
    private static final String TEMPLATE_ENTITY_NBT = "[实体-NBT] {}";
    private static final String TEMPLATE_RENDER = "[渲染] {}";
    private static final String TEMPLATE_3D_SKIN_LAYERS = "[3D皮肤层] {}";
    private static final String TEMPLATE_RESOURCE = "[资源] {}";
    private static final String TEMPLATE_POSE_LOADER = "[姿态加载] {}";
    private static final String TEMPLATE_ACTION_LOADER = "[动作加载] {}";
    private static final String TEMPLATE_RESOURCE_GENERATOR = "[资源生成] {}";
    private static final String TEMPLATE_TEXTURE_SCANNER = "[纹理扫描] {}";
    private static final String TEMPLATE_DEBUG_STICK_ACTION = "[动作调试棒] {}";
    private static final String TEMPLATE_DEBUG_STICK_POSE = "[姿态调试棒] {}";
    private static final String TEMPLATE_COMMAND = "[命令] {}";
    
    // ==================== 配置读取方法 ====================
    
    /**
     * 获取模块的默认日志级别
     * @param moduleName 模块名称
     * @return 日志级别，如果模块不存在则返回 WARN
     */
    public static LogLevel getModuleLevel(String moduleName) {
        return switch (moduleName) {
            case MODULE_MAIN -> LEVEL_MAIN;
            case MODULE_ENTITY -> LEVEL_ENTITY;
            case MODULE_ENTITY_INTERACT -> LEVEL_ENTITY_INTERACT;
            case MODULE_ENTITY_POSE -> LEVEL_ENTITY_POSE;
            case MODULE_ENTITY_ACTION -> LEVEL_ENTITY_ACTION;
            case MODULE_ENTITY_NBT -> LEVEL_ENTITY_NBT;
            case MODULE_RENDER -> LEVEL_RENDER;
            case MODULE_3D_SKIN_LAYERS -> LEVEL_3D_SKIN_LAYERS;
            case MODULE_RESOURCE -> LEVEL_RESOURCE;
            case MODULE_POSE_LOADER -> LEVEL_POSE_LOADER;
            case MODULE_ACTION_LOADER -> LEVEL_ACTION_LOADER;
            case MODULE_RESOURCE_GENERATOR -> LEVEL_RESOURCE_GENERATOR;
            case MODULE_TEXTURE_SCANNER -> LEVEL_TEXTURE_SCANNER;
            case MODULE_DEBUG_STICK_ACTION -> LEVEL_DEBUG_STICK_ACTION;
            case MODULE_DEBUG_STICK_POSE -> LEVEL_DEBUG_STICK_POSE;
            case MODULE_COMMAND -> LEVEL_COMMAND;
            default -> LogLevel.WARN;  // 默认返回 WARN
        };
    }
    
    /**
     * 获取模块的日志模板
     * @param moduleName 模块名称
     * @return 日志模板，如果模块不存在则返回默认模板
     */
    public static String getLogTemplate(String moduleName) {
        return switch (moduleName) {
            case MODULE_MAIN -> TEMPLATE_MAIN;
            case MODULE_ENTITY -> TEMPLATE_ENTITY;
            case MODULE_ENTITY_INTERACT -> TEMPLATE_ENTITY_INTERACT;
            case MODULE_ENTITY_POSE -> TEMPLATE_ENTITY_POSE;
            case MODULE_ENTITY_ACTION -> TEMPLATE_ENTITY_ACTION;
            case MODULE_ENTITY_NBT -> TEMPLATE_ENTITY_NBT;
            case MODULE_RENDER -> TEMPLATE_RENDER;
            case MODULE_3D_SKIN_LAYERS -> TEMPLATE_3D_SKIN_LAYERS;
            case MODULE_RESOURCE -> TEMPLATE_RESOURCE;
            case MODULE_POSE_LOADER -> TEMPLATE_POSE_LOADER;
            case MODULE_ACTION_LOADER -> TEMPLATE_ACTION_LOADER;
            case MODULE_RESOURCE_GENERATOR -> TEMPLATE_RESOURCE_GENERATOR;
            case MODULE_TEXTURE_SCANNER -> TEMPLATE_TEXTURE_SCANNER;
            case MODULE_DEBUG_STICK_ACTION -> TEMPLATE_DEBUG_STICK_ACTION;
            case MODULE_DEBUG_STICK_POSE -> TEMPLATE_DEBUG_STICK_POSE;
            case MODULE_COMMAND -> TEMPLATE_COMMAND;
            default -> "[{}] {}";  // 默认模板
        };
    }
}
