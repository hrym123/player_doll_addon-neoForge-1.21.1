package com.lanye.dolladdon.util.skinlayers3d;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.util.logging.LogLevel;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 3D皮肤层专用日志工具
 * 使用ModuleLogger统一管理日志，确保[3D皮肤层]日志始终输出
 * 
 * @deprecated 建议直接使用 ModuleLogger，此类仅作为便捷包装器保留
 */
@Deprecated
public class SkinLayersLogger {
    private static final String MODULE_NAME = "3d_skin_layers";
    private static final Logger LOGGER = ModuleLogger.getLogger(MODULE_NAME);
    
    /**
     * 设置日志级别：除了3D皮肤层模块外，其他模块只输出WARN及以上级别
     */
    public static void configureLogLevels() {
        // 使用LogModuleConfig统一配置
        Map<String, LogLevel> moduleLevels = new HashMap<>();
        moduleLevels.put(PlayerDollAddon.MODID + "." + MODULE_NAME, LogLevel.DEBUG);
        moduleLevels.put("com.lanye.dolladdon.util.skinlayers3d.Doll3DSkinUtil", LogLevel.DEBUG);
        moduleLevels.put("com.lanye.dolladdon.util.skinlayers3d.Doll3DSkinData", LogLevel.DEBUG);
        moduleLevels.put("com.lanye.dolladdon.base.render.BaseDollRenderer", LogLevel.WARN);
        
        // 配置底层日志框架
        ModuleLogger.configureFrameworkLogLevels(LogLevel.WARN, moduleLevels);
        
        // 通过LogModuleConfig配置模块级别
        LogModuleConfig.setGlobalLevel(LogLevel.WARN);
        LogModuleConfig.setModuleLevel(MODULE_NAME, LogLevel.DEBUG);
        
        LOGGER.info("日志级别配置完成：3D皮肤层=DEBUG，其他模块=WARN");
    }
    
    public static void debug(String message) {
        ModuleLogger.debug(MODULE_NAME, "[3D皮肤层] " + message);
    }
    
    public static void debug(String message, Object... args) {
        ModuleLogger.debug(MODULE_NAME, "[3D皮肤层] " + message, args);
    }
    
    public static void info(String message) {
        ModuleLogger.info(MODULE_NAME, "[3D皮肤层] " + message);
    }
    
    public static void info(String message, Object... args) {
        ModuleLogger.info(MODULE_NAME, "[3D皮肤层] " + message, args);
    }
    
    public static void warn(String message) {
        ModuleLogger.warn(MODULE_NAME, "[3D皮肤层] " + message);
    }
    
    public static void warn(String message, Object... args) {
        ModuleLogger.warn(MODULE_NAME, "[3D皮肤层] " + message, args);
    }
    
    public static void error(String message) {
        ModuleLogger.error(MODULE_NAME, "[3D皮肤层] " + message);
    }
    
    public static void error(String message, Object... args) {
        ModuleLogger.error(MODULE_NAME, "[3D皮肤层] " + message, args);
    }
    
    public static void error(String message, Throwable throwable) {
        ModuleLogger.error(MODULE_NAME, "[3D皮肤层] " + message, throwable);
    }
}
