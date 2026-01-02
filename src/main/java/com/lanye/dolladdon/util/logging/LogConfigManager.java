package com.lanye.dolladdon.util.logging;

/**
 * 日志配置管理器
 * 提供日志系统的初始化和管理功能
 *
 * <p>配置读取请直接使用 {@link LogModuleConfig}</p>
 * <p>此类的作用是提供初始化和管理功能，避免配置读取时的重复导入</p>
 */
public class LogConfigManager {

    // ==================== 初始化和管理方法 ====================

    /**
     * 初始化所有模块的日志级别
     * 应该在应用启动时调用，确保所有模块使用配置的默认级别
     */
    public static void initializeModuleLevels() {
        String[] modules = LogModuleConfig.getAllModuleNames();
        for (String module : modules) {
            LogLevel level = LogModuleConfig.getModuleLevel(module);
            // 使用内部方法直接设置
            ModuleLogger.setModuleLevelInternal(module, level);
        }
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
     * 初始化文件日志系统
     * 应该在 mod 初始化时调用
     * 配置写死：启用文件日志，不分离模块文件
     */
    public static void initializeFileLogging() {
        // 写死配置：始终启用文件日志
        FileLogger.setFileLoggingEnabled(true);

        // 写死配置：不分离模块文件（所有日志写入同一个文件）
        FileLogger.setSeparateModuleFiles(false);

        ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "[日志配置] 文件日志系统初始化完成");
        ModuleLogger.info(LogModuleConfig.MODULE_MAIN, "[日志配置] 日志文件位置: {}", FileLogger.getLogDirectory());
    }

    /**
     * 获取日志目录路径
     * @return 日志目录路径
     */
    public static java.nio.file.Path getLogDirectory() {
        return FileLogger.getLogDirectory();
    }

}