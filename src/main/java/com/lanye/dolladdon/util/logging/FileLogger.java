package com.lanye.dolladdon.util.logging;

import com.lanye.dolladdon.PlayerDollAddon;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 文件日志记录器
 * 为本项目的日志系统提供单独的文件输出功能
 *
 * <p>特性：</p>
 * <ul>
 *   <li>同时输出到控制台和日志文件</li>
 *   <li>支持按日期分割日志文件</li>
 *   <li>支持按模块分类的日志文件</li>
 *   <li>线程安全的文件写入</li>
 *   <li>自动创建日志目录</li>
 *   <li>文件大小限制和轮转</li>
 * </ul>
 */
public class FileLogger {
    private static final String LOG_DIR_NAME = "logs";
    private static final String MOD_LOG_PREFIX = "player_doll_addon";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_BACKUP_FILES = 5;

    // 日期格式
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    // 文件写入器缓存
    private static final ConcurrentMap<String, FileWriterInfo> fileWriters = new ConcurrentHashMap<>();

    // 全局配置
    private static boolean fileLoggingEnabled = true;
    private static boolean separateModuleFiles = false; // 是否为每个模块创建单独的文件
    private static Path logDirectory;

    static {
        initializeLogDirectory();
    }

    /**
     * 初始化日志目录
     */
    private static void initializeLogDirectory() {
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir();
            logDirectory = gameDir.resolve(LOG_DIR_NAME).resolve(MOD_LOG_PREFIX);

            // 创建日志目录
            if (!Files.exists(logDirectory)) {
                Files.createDirectories(logDirectory);
                ModuleLogger.info(LogConfigManager.MODULE_MAIN, "[文件日志] 创建日志目录: {}", logDirectory);
            }
        } catch (Exception e) {
            System.err.println("[PlayerDollAddon] 无法创建日志目录: " + e.getMessage());
            logDirectory = Paths.get(System.getProperty("user.home"), "player_doll_logs");
            try {
                Files.createDirectories(logDirectory);
            } catch (Exception ex) {
                System.err.println("[PlayerDollAddon] 无法创建备用日志目录: " + ex.getMessage());
                logDirectory = Paths.get(".");
            }
        }
    }

    /**
     * 文件写入器信息
     */
    private static class FileWriterInfo {
        PrintWriter writer;
        final ReentrantLock lock;
        final Path filePath;
        long lastSizeCheck;
        int backupIndex;

        FileWriterInfo(PrintWriter writer, Path filePath) {
            this.writer = writer;
            this.lock = new ReentrantLock();
            this.filePath = filePath;
            this.lastSizeCheck = System.currentTimeMillis();
            this.backupIndex = 0;
        }
    }

    /**
     * 获取日志文件名
     */
    private static String getLogFileName(String moduleName) {
        String dateStr = DATE_FORMAT.format(new Date());
        if (separateModuleFiles && moduleName != null && !moduleName.isEmpty()) {
            return MOD_LOG_PREFIX + "-" + moduleName.replace(".", "_") + "-" + dateStr + ".log";
        } else {
            return MOD_LOG_PREFIX + "-" + dateStr + ".log";
        }
    }

    /**
     * 获取或创建文件写入器
     */
    private static FileWriterInfo getOrCreateFileWriter(String moduleName) {
        String fileName = getLogFileName(moduleName);
        return fileWriters.computeIfAbsent(fileName, key -> {
            try {
                Path logFile = logDirectory.resolve(key);
                PrintWriter writer = new PrintWriter(new FileWriter(logFile.toFile(), true), true);
                return new FileWriterInfo(writer, logFile);
            } catch (IOException e) {
                System.err.println("[PlayerDollAddon] 无法创建日志文件写入器: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * 检查并处理文件大小限制
     */
    private static void checkFileSize(FileWriterInfo writerInfo) {
        if (writerInfo == null) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - writerInfo.lastSizeCheck < 60000) { // 每分钟检查一次
            return;
        }

        writerInfo.lastSizeCheck = currentTime;

        try {
            long fileSize = Files.size(writerInfo.filePath);
            if (fileSize > MAX_FILE_SIZE) {
                rotateLogFile(writerInfo);
            }
        } catch (IOException e) {
            // 忽略文件大小检查错误
        }
    }

    /**
     * 轮转日志文件
     */
    private static void rotateLogFile(FileWriterInfo writerInfo) {
        writerInfo.lock.lock();
        try {
            writerInfo.writer.close();

            // 删除最旧的备份文件
            Path oldestBackup = writerInfo.filePath.getParent().resolve(
                writerInfo.filePath.getFileName() + "." + MAX_BACKUP_FILES);
            if (Files.exists(oldestBackup)) {
                Files.delete(oldestBackup);
            }

            // 轮转现有备份文件
            for (int i = MAX_BACKUP_FILES - 1; i >= 1; i--) {
                Path currentBackup = writerInfo.filePath.getParent().resolve(
                    writerInfo.filePath.getFileName() + "." + i);
                Path nextBackup = writerInfo.filePath.getParent().resolve(
                    writerInfo.filePath.getFileName() + "." + (i + 1));
                if (Files.exists(currentBackup)) {
                    Files.move(currentBackup, nextBackup);
                }
            }

            // 将当前文件重命名为 .1 备份
            Path backupFile = writerInfo.filePath.getParent().resolve(
                writerInfo.filePath.getFileName() + ".1");
            Files.move(writerInfo.filePath, backupFile);

            // 创建新的日志文件
            PrintWriter newWriter = new PrintWriter(new FileWriter(writerInfo.filePath.toFile(), false), true);
            writerInfo.writer = newWriter;

            writeHeader(writerInfo.writer, writerInfo.filePath.getFileName().toString());

        } catch (IOException e) {
            System.err.println("[PlayerDollAddon] 日志文件轮转失败: " + e.getMessage());
        } finally {
            writerInfo.lock.unlock();
        }
    }

    /**
     * 写入文件头信息
     */
    private static void writeHeader(PrintWriter writer, String fileName) {
        writer.println("===================================================================================");
        writer.println("Player Doll Addon - 日志文件: " + fileName);
        writer.println("创建时间: " + TIME_FORMAT.format(new Date()));
        writer.println("===================================================================================");
        writer.println();
    }

    /**
     * 记录日志到文件
     */
    public static void log(String moduleName, LogLevel level, String message) {
        if (!fileLoggingEnabled) return;

        FileWriterInfo writerInfo = getOrCreateFileWriter(moduleName);
        if (writerInfo == null) return;

        checkFileSize(writerInfo);

        writerInfo.lock.lock();
        try {
            String timestamp = TIME_FORMAT.format(new Date());
            String logLine = String.format("[%s] [%s] [%s] %s",
                timestamp, level.name(), moduleName, message);
            writerInfo.writer.println(logLine);
        } finally {
            writerInfo.lock.unlock();
        }
    }

    /**
     * 记录异常日志到文件
     */
    public static void log(String moduleName, LogLevel level, String message, Throwable throwable) {
        if (!fileLoggingEnabled) return;

        FileWriterInfo writerInfo = getOrCreateFileWriter(moduleName);
        if (writerInfo == null) return;

        checkFileSize(writerInfo);

        writerInfo.lock.lock();
        try {
            String timestamp = TIME_FORMAT.format(new Date());
            String logLine = String.format("[%s] [%s] [%s] %s",
                timestamp, level.name(), moduleName, message);
            writerInfo.writer.println(logLine);

            if (throwable != null) {
                writerInfo.writer.println("异常详情:");
                throwable.printStackTrace(writerInfo.writer);
            }
        } finally {
            writerInfo.lock.unlock();
        }
    }

    /**
     * 设置是否启用文件日志
     */
    public static void setFileLoggingEnabled(boolean enabled) {
        fileLoggingEnabled = enabled;
        if (enabled) {
            ModuleLogger.info(LogConfigManager.MODULE_MAIN, "[文件日志] 文件日志输出已启用");
        } else {
            ModuleLogger.info(LogConfigManager.MODULE_MAIN, "[文件日志] 文件日志输出已禁用");
        }
    }

    /**
     * 获取文件日志启用状态
     */
    public static boolean isFileLoggingEnabled() {
        return fileLoggingEnabled;
    }

    /**
     * 设置是否为每个模块创建单独的日志文件
     */
    public static void setSeparateModuleFiles(boolean separate) {
        separateModuleFiles = separate;
        if (separate) {
            ModuleLogger.info(LogConfigManager.MODULE_MAIN, "[文件日志] 已启用按模块分离的日志文件");
        } else {
            ModuleLogger.info(LogConfigManager.MODULE_MAIN, "[文件日志] 已禁用按模块分离的日志文件");
        }
    }

    /**
     * 获取日志目录路径
     */
    public static Path getLogDirectory() {
        return logDirectory;
    }

    /**
     * 关闭所有文件写入器
     * 在程序关闭时调用
     */
    public static void shutdown() {
        for (FileWriterInfo writerInfo : fileWriters.values()) {
            writerInfo.lock.lock();
            try {
                writerInfo.writer.close();
            } finally {
                writerInfo.lock.unlock();
            }
        }
        fileWriters.clear();
        ModuleLogger.info(LogConfigManager.MODULE_MAIN, "[文件日志] 所有日志文件已关闭");
    }

    /**
     * 获取当前活动的日志文件列表
     */
    public static String[] getActiveLogFiles() {
        return fileWriters.keySet().toArray(new String[0]);
    }
}