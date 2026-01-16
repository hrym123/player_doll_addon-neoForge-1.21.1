package com.lanye.dolladdon.util.command;

import com.lanye.dolladdon.PlayerDoll;
import com.lanye.dolladdon.init.ModItems;
import com.lanye.dolladdon.util.factory.DollItemFactory;
import com.lanye.dolladdon.util.neoForge.DynamicDollLoader;
import com.lanye.dolladdon.util.neoForge.DynamicModelGenerator;
import com.lanye.dolladdon.util.neoForge.DynamicTextureManager;
import com.lanye.dolladdon.util.resource.PlayerSkinDownloader;
import com.lanye.dolladdon.util.resource.SkinFileNamingUtil;
import net.minecraft.resources.ResourceLocation;
import java.util.stream.Stream;
import net.minecraft.world.item.ItemStack;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * 玩偶皮肤指令
 * 用于获取玩家皮肤并注册成玩偶
 */
public class DollSkinCommand {
    
    /**
     * 玩家名称建议提供器
     * 用于Tab补全时显示在线玩家列表
     */
    private static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTIONS = 
        (context, builder) -> {
            CommandSourceStack source = context.getSource();
            if (source.getEntity() instanceof ServerPlayer) {
                // 获取服务器上的所有在线玩家
                return SharedSuggestionProvider.suggest(
                    source.getServer().getPlayerList().getPlayers().stream()
                        .map(player -> player.getName().getString()),
                    builder
                );
            }
            return Suggestions.empty();
        };
    
    /**
     * 皮肤文件名建议提供器
     * 用于Tab补全时显示可用的皮肤文件列表
     */
    private static final SuggestionProvider<CommandSourceStack> SKIN_FILE_SUGGESTIONS = 
        (context, builder) -> {
            CommandSourceStack source = context.getSource();
            try {
                Path pngDir = PlayerSkinDownloader.getPngDirectory();
                if (java.nio.file.Files.exists(pngDir) && java.nio.file.Files.isDirectory(pngDir)) {
                    try (Stream<Path> paths = java.nio.file.Files.list(pngDir)) {
                        return SharedSuggestionProvider.suggest(
                            paths
                                .filter(java.nio.file.Files::isRegularFile)
                                .filter(path -> path.toString().toLowerCase().endsWith(".png"))
                                .map(path -> path.getFileName().toString())
                                .sorted(),
                            builder
                        );
                    }
                }
            } catch (Exception e) {
                // 忽略错误，返回空建议
            }
            return Suggestions.empty();
        };
    
    /**
     * 注册指令
     * 
     * @param dispatcher 指令分发器
     * @param context 指令构建上下文
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        // 注册 /dollskin 命令（支持指定玩家或获取自己的皮肤）
        dispatcher.register(
            Commands.literal("dollskin")
                .requires(source -> source.hasPermission(2)) // 需要OP权限
                .executes(ctx -> execute(ctx, null)) // 无参数：获取自己的皮肤
                .then(
                    Commands.literal("list")
                        .executes(ctx -> executeList(ctx)) // 列出所有可用的皮肤图片
                )
                .then(
                    Commands.literal("use")
                        .then(
                            Commands.argument("filename", StringArgumentType.greedyString())
                                .suggests(SKIN_FILE_SUGGESTIONS) // Tab补全：显示可用皮肤文件列表
                                .executes(ctx -> executeUse(ctx, StringArgumentType.getString(ctx, "filename")))
                        )
                )
                .then(
                    Commands.literal("nbt")
                        .executes(ctx -> executeNBT(ctx, false)) // 显示当前手持物品的NBT数据
                        .then(
                            Commands.literal("save")
                                .executes(ctx -> executeNBT(ctx, true)) // 将NBT数据保存到文件
                        )
                )
                .then(
                    Commands.argument("player", StringArgumentType.word())
                        .suggests(PLAYER_SUGGESTIONS) // Tab补全：显示在线玩家列表
                        .executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "player")))
                )
        );
    }
    
    /**
     * 执行指令
     * 
     * @param context 指令上下文
     * @param playerName 玩家名称（可选，如果为null则使用执行指令的玩家）
     * @return 执行结果（1表示成功，0表示失败）
     */
    private static int execute(CommandContext<CommandSourceStack> context, String playerName) {
        CommandSourceStack source = context.getSource();
        
        try {
            ServerPlayer targetPlayer = null;
            boolean isMyDollSkin = (playerName == null || playerName.isEmpty());
            
            // 如果提供了玩家名，查找对应玩家
            if (playerName != null && !playerName.isEmpty()) {
                targetPlayer = source.getServer().getPlayerList().getPlayerByName(playerName);
                if (targetPlayer == null) {
                    source.sendFailure(Component.literal("§c[玩偶皮肤] 玩家不存在或不在线: " + playerName));
                    return 0;
                }
            } else {
                // 如果没有提供玩家名，使用执行指令的玩家
                if (source.getEntity() instanceof ServerPlayer player) {
                    targetPlayer = player;
                } else {
                    source.sendFailure(Component.literal("§c[玩偶皮肤] 此指令只能由玩家执行，或需要指定玩家名"));
                    source.sendFailure(Component.literal("§7提示: 使用 /dollskin <玩家名> 来获取指定玩家的皮肤"));
                    return 0;
                }
            }
            
            if (targetPlayer == null) {
                source.sendFailure(Component.literal("§c[玩偶皮肤] 无法获取玩家信息"));
                return 0;
            }
            
            // 获取玩家信息
            final String targetPlayerName = targetPlayer.getName().getString();
            final java.util.UUID targetPlayerUUID = targetPlayer.getUUID();
            boolean isAlexModel = com.lanye.dolladdon.util.resource.PlayerSkinUtil.isAlexModel(
                targetPlayerUUID, 
                targetPlayerName
            );
            
            // 生成文件名（仅根据玩家名和模型类型）
            final String fileName = SkinFileNamingUtil.generateFileName(targetPlayerName, isAlexModel);
            
            // 获取保存路径
            Path pngDir = PlayerSkinDownloader.getPngDirectory();
            final Path targetPath = pngDir.resolve(fileName);
            final ServerPlayer finalTargetPlayer = targetPlayer;
            
            // 发送开始提示
            if (isMyDollSkin) {
                source.sendSuccess(() -> Component.literal("§a[玩偶皮肤] 正在获取您的皮肤并创建玩偶..."), false);
            } else {
                source.sendSuccess(() -> Component.literal("§a[玩偶皮肤] 正在获取玩家 §e" + targetPlayerName + " §a的皮肤..."), false);
            }
            
            // 检查文件是否已存在
            boolean fileExists = java.nio.file.Files.exists(targetPath);
            if (fileExists) {
                // 文件已存在，将备份原文件
                source.sendSuccess(() -> Component.literal("§e[玩偶皮肤] 检测到已存在的皮肤文件，将备份后覆盖: §7" + fileName), false);
            }
            
            // 异步下载并保存皮肤，避免阻塞服务器主线程
            source.sendSuccess(() -> Component.literal("§7[玩偶皮肤] 正在下载皮肤文件（异步执行，请稍候...）"), false);
            
            // 使用服务器的工作线程池异步执行下载任务
            // 注意：使用 final 变量以确保 lambda 表达式可以正确捕获
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                // 如果文件已存在，先备份原文件到备份目录
                if (fileExists && java.nio.file.Files.exists(targetPath)) {
                    try {
                        // 创建备份目录（如果不存在）
                        Path backupDir = pngDir.resolve("backup");
                        if (!java.nio.file.Files.exists(backupDir)) {
                            java.nio.file.Files.createDirectories(backupDir);
                            com.lanye.dolladdon.util.logging.ModuleLogger.debug(
                                com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                                "execute: 已创建备份目录: {}",
                                backupDir
                            );
                        }
                        
                        // 生成备份文件名（添加时间戳）
                        String timestamp = java.time.LocalDateTime.now().format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                        );
                        String backupFileName = fileName.substring(0, fileName.length() - 4) + "_backup_" + timestamp + ".png";
                        Path backupPath = backupDir.resolve(backupFileName);
                        
                        // 将原文件移动到备份目录
                        java.nio.file.Files.move(targetPath, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        
                        com.lanye.dolladdon.util.logging.ModuleLogger.debug(
                            com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                            "execute: 已备份原文件到备份目录: {} -> {}",
                            fileName, backupPath
                        );
                    } catch (Exception e) {
                        com.lanye.dolladdon.util.logging.ModuleLogger.warn(
                            com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                            "execute: 备份文件失败: {}",
                            e.getMessage(), e
                        );
                    }
                }
                
                return PlayerSkinDownloader.downloadPlayerSkin(
                    targetPlayerUUID, 
                    targetPlayerName, 
                    targetPath, 
                    true // 允许覆盖（已备份原文件）
                );
            }, source.getServer()).thenAcceptAsync(result -> {
                // 在主线程中发送结果消息
                if (result.isSuccess()) {
                    source.sendSuccess(() -> Component.literal("§a[玩偶皮肤] ✓ 皮肤文件已保存: §e" + fileName), true);
                    if (result.isFallback()) {
                        source.sendSuccess(() -> Component.literal("§e[玩偶皮肤] 提示: 使用了回退方案（开发模式或离线模式）"), false);
                    }
                    
                    // 注册纹理到DynamicTextureManager（确保纹理可以被访问）
                    try {
                        ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
                            "player_doll", 
                            "png/" + fileName
                        );
                        DynamicTextureManager.registerTexture(textureLocation, targetPath);
                        source.sendSuccess(() -> Component.literal("§a[玩偶皮肤] ✓ 纹理已注册到资源管理器"), false);
                    } catch (Exception e) {
                        // 注册失败不影响主流程，纹理可能已经通过其他方式注册
                    }
                    
                    // 创建带NBT的玩偶物品并给予玩家
                    try {
                        ItemStack dollItem = createDollItemWithNBT(
                            targetPlayerName, 
                            targetPlayerUUID, 
                            fileName, 
                            isAlexModel
                        );
                        
                        // 给予玩家物品
                        ServerPlayer commandPlayer = null;
                        if (source.getEntity() instanceof ServerPlayer player) {
                            commandPlayer = player;
                        } else {
                            // 如果不是玩家执行，给予目标玩家
                            commandPlayer = finalTargetPlayer;
                        }
                        
                        if (commandPlayer != null) {
                            if (commandPlayer.getInventory().add(dollItem)) {
                                source.sendSuccess(() -> Component.literal("§a[玩偶皮肤] ✓ 玩偶物品已添加到您的背包"), true);
                                source.sendSuccess(() -> Component.literal("§7[玩偶皮肤] 提示: 放置玩偶后，它将显示玩家 §e" + targetPlayerName + " §7的皮肤"), false);
                            } else {
                                // 如果背包满了，尝试掉落物品
                                commandPlayer.drop(dollItem, false);
                                source.sendSuccess(() -> Component.literal("§e[玩偶皮肤] 背包已满，玩偶物品已掉落在地"), true);
                                source.sendSuccess(() -> Component.literal("§7[玩偶皮肤] 提示: 请拾取掉落的玩偶物品"), false);
                            }
                        }
                    } catch (Exception e) {
                        // 如果给予物品失败，不影响主流程
                        source.sendFailure(Component.literal("§c[玩偶皮肤] 无法自动给予玩偶物品"));
                        source.sendSuccess(() -> Component.literal("§7[玩偶皮肤] 提示: 请手动在创造模式物品栏中获取玩偶物品"), false);
                    }
                } else {
                    String errorMsg = "§c[玩偶皮肤] ✗ 下载或保存皮肤失败: " + targetPlayerName;
                    if (result.getErrorMessage() != null) {
                        errorMsg += " §7(" + result.getErrorMessage() + ")";
                    }
                    source.sendFailure(Component.literal(errorMsg));
                    source.sendFailure(Component.literal("§7[玩偶皮肤] 提示: 请检查网络连接或玩家皮肤服务是否可用"));
                }
            }, source.getServer());
            
            // 立即返回，不等待下载完成
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 执行指令时发生错误: " + e.getMessage()));
            source.sendFailure(Component.literal("§7[玩偶皮肤] 提示: 请检查日志文件以获取详细错误信息"));
            // Error logging handled by Mixin
            return 0;
        }
    }
    
    /**
     * 创建带NBT的玩偶物品
     * 使用工厂类统一创建，确保NBT结构标准化，从而可以叠加
     * 
     * @param playerName 玩家名称
     * @param playerUUID 玩家UUID（用于生成文件名，不保存到NBT）
     * @param fileName 皮肤文件名
     * @param isAlexModel 是否为Alex模型
     * @return 带NBT的玩偶物品
     */
    private static ItemStack createDollItemWithNBT(String playerName, java.util.UUID playerUUID, 
                                                   String fileName, boolean isAlexModel) {
        // 如果 fileName 已经是完整的 ResourceLocation 格式（包含 :），直接使用
        // 否则添加前缀
        String skinPath = fileName.contains(":") ? fileName : "player_doll:png/" + fileName;
        
        // 使用工厂类创建物品，确保NBT结构标准化
        // 注意：playerUUID 仅用于生成文件名，不保存到物品NBT中
        // 不再使用 DisplayName，直接使用 PlayerName 显示
        return DollItemFactory.createCustomTextureDoll(
            skinPath,
            isAlexModel,
            playerName
        );
    }
    
    /**
     * 执行 list 子命令
     * 列出所有可用的皮肤图片文件
     * 
     * @param context 指令上下文
     * @return 执行结果（1表示成功，0表示失败）
     */
    private static int executeList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            Path pngDir = PlayerSkinDownloader.getPngDirectory();
            
            if (!java.nio.file.Files.exists(pngDir) || !java.nio.file.Files.isDirectory(pngDir)) {
                source.sendFailure(Component.literal("§c[玩偶皮肤] 皮肤目录不存在: " + pngDir));
                return 0;
            }
            
            source.sendSuccess(() -> Component.literal("§a[玩偶皮肤] 可用的皮肤图片列表:"), false);
            source.sendSuccess(() -> Component.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
            
            try (Stream<Path> paths = java.nio.file.Files.list(pngDir)) {
                java.util.List<Path> pngFiles = paths
                    .filter(java.nio.file.Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".png"))
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());
                
                if (pngFiles.isEmpty()) {
                    source.sendSuccess(() -> Component.literal("§7  暂无可用皮肤图片"), false);
                    source.sendSuccess(() -> Component.literal("§7  提示: 使用 /dollskin <玩家名> 下载皮肤"), false);
                } else {
                    java.util.concurrent.atomic.AtomicInteger index = new java.util.concurrent.atomic.AtomicInteger(1);
                    for (Path pngFile : pngFiles) {
                        final String fileName = pngFile.getFileName().toString();
                        final String playerName = SkinFileNamingUtil.extractPlayerNameFromFileName(fileName);
                        final boolean isAlex = SkinFileNamingUtil.extractModelTypeFromFileName(fileName);
                        final String modelType = isAlex ? "§bAlex" : "§6Steve";
                        
                        // 获取文件大小
                        final long fileSize = java.nio.file.Files.size(pngFile);
                        final String fileSizeStr = formatFileSize(fileSize);
                        
                        // 获取文件修改时间
                        final java.time.Instant lastModified = java.nio.file.Files.getLastModifiedTime(pngFile).toInstant();
                        final String timeStr = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(java.time.ZoneId.systemDefault())
                            .format(lastModified);
                        
                        final int currentIndex = index.getAndIncrement();
                        source.sendSuccess(() -> Component.literal(
                            String.format("§7  %d. §e%s §7- 玩家: §a%s §7- 模型: %s §7- 大小: §7%s §7- 时间: §7%s", 
                                currentIndex, fileName, 
                                playerName != null ? playerName : "未知", 
                                modelType, fileSizeStr, timeStr)
                        ), false);
                    }
                    
                    source.sendSuccess(() -> Component.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
                    source.sendSuccess(() -> Component.literal("§7提示: 使用 §e/dollskin use <文件名> §7来使用指定的皮肤图片创建玩偶"), false);
                    source.sendSuccess(() -> Component.literal("§7例如: §e/dollskin use Slan_ye_eab2a8a3.png"), false);
                }
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 列出皮肤文件时发生错误: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 执行 use 子命令
     * 使用指定的皮肤图片文件创建玩偶
     * 
     * @param context 指令上下文
     * @param fileName 皮肤文件名
     * @return 执行结果（1表示成功，0表示失败）
     */
    private static int executeUse(CommandContext<CommandSourceStack> context, String fileName) {
        CommandSourceStack source = context.getSource();
        
        try {
            // 清理文件名：去除前后空格，只保留文件名部分（去除路径）
            fileName = fileName.trim();
            if (fileName.contains("/") || fileName.contains("\\")) {
                // 如果包含路径分隔符，只取文件名部分
                fileName = java.nio.file.Paths.get(fileName).getFileName().toString();
            }
            
            // 确保文件名以 .png 结尾
            if (!fileName.toLowerCase().endsWith(".png")) {
                fileName = fileName + ".png";
            }
            
            Path pngDir = PlayerSkinDownloader.getPngDirectory();
            Path skinFile = pngDir.resolve(fileName);
            
            // 检查文件是否存在
            if (!java.nio.file.Files.exists(skinFile) || !java.nio.file.Files.isRegularFile(skinFile)) {
                source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 皮肤文件不存在: " + fileName));
                source.sendFailure(Component.literal("§7提示: 使用 §e/dollskin list §7查看所有可用皮肤文件"));
                return 0;
            }
            
            // 从文件名提取信息（支持新格式和旧格式）
            String playerName = SkinFileNamingUtil.extractPlayerNameFromFileName(fileName);
            boolean isAlexModel = SkinFileNamingUtil.extractModelTypeFromFileName(fileName);
            
            if (playerName == null || playerName.isEmpty()) {
                source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 无法从文件名提取玩家名称: " + fileName));
                return 0;
            }
            
            // 生成用于获取皮肤的UUID（仅用于下载皮肤，不保存到物品NBT）
            // 尝试从在线玩家中查找匹配的UUID
            java.util.UUID playerUUID = null;
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                if (player.getName().getString().equals(playerName)) {
                    playerUUID = player.getUUID();
                    break;
                }
            }
            
            // 如果找不到匹配的在线玩家，使用基于玩家名的UUID（用于离线模式）
            if (playerUUID == null) {
                playerUUID = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
            }
            
            // 确保变量是 final 的，以便在 lambda 中使用
            final String finalFileName = fileName;
            final String finalPlayerName = playerName;
            
            source.sendSuccess(() -> Component.literal("§a[玩偶皮肤] 正在使用皮肤文件创建玩偶: §e" + finalFileName), false);
            
            // 注册纹理到DynamicTextureManager
            ResourceLocation registeredTextureLocation = null;
            try {
                // 验证文件是否存在且可读
                if (!java.nio.file.Files.exists(skinFile)) {
                    source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 皮肤文件不存在: " + finalFileName));
                    return 0;
                }
                
                if (!java.nio.file.Files.isRegularFile(skinFile)) {
                    source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 皮肤文件不是常规文件: " + finalFileName));
                    return 0;
                }
                
                // 创建 ResourceLocation，确保路径格式正确
                // ResourceLocation 路径只允许 [a-z0-9/._-] 字符
                // 如果文件名包含非 ASCII 字符，需要进行编码处理
                String texturePath = "png/" + finalFileName;
                
                // 验证路径格式（ResourceLocation 要求路径不能包含某些特殊字符）
                if (texturePath.contains("..") || texturePath.contains("//")) {
                    source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 无效的纹理路径: " + texturePath));
                    return 0;
                }
                
                // 检查文件名是否包含非 ASCII 字符（ResourceLocation 不支持）
                // ResourceLocation 路径只允许 [a-z0-9/._-] 字符（注意：只允许小写字母，不允许大写字母）
                // URL 编码会引入 % 字符，也不被允许，所以直接使用哈希值
                boolean containsNonAscii = finalFileName.chars().anyMatch(ch -> ch > 127 || (ch < 32 && ch != 9 && ch != 10 && ch != 13));
                // 检查是否包含大写字母（ResourceLocation 路径不允许大写字母）
                boolean containsUpperCase = finalFileName.chars().anyMatch(ch -> ch >= 'A' && ch <= 'Z');
                // 也检查是否包含不允许的字符（除了小写字母、数字、点、下划线、连字符）
                boolean containsInvalidChars = finalFileName.chars().anyMatch(ch -> {
                    return !((ch >= 'a' && ch <= 'z') || 
                            (ch >= '0' && ch <= '9') || 
                            ch == '.' || ch == '_' || ch == '-');
                });
                
                String safeFileName = null;
                if (containsNonAscii || containsUpperCase || containsInvalidChars) {
                    // 如果包含非 ASCII 字符或不允许的字符，使用文件名哈希值
                    // 使用 MD5 哈希值以确保唯一性和符合 ResourceLocation 规范
                    try {
                        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                        byte[] hashBytes = md.digest(finalFileName.getBytes("UTF-8"));
                        StringBuilder hashString = new StringBuilder();
                        for (byte b : hashBytes) {
                            hashString.append(String.format("%02x", b));
                        }
                        // 使用前16个字符作为文件名（32个字符太长）
                        safeFileName = "skin_" + hashString.substring(0, 16) + ".png";
                    } catch (Exception e) {
                        // 如果 MD5 失败，使用 hashCode（可能冲突但更简单）
                        int fileNameHash = finalFileName.hashCode();
                        safeFileName = "skin_" + Integer.toHexString(Math.abs(fileNameHash)) + ".png";
                    }
                    texturePath = "png/" + safeFileName;
                    
                    // 使用路径映射，不需要复制文件
                    // 原始文件路径保持不变，ResourceLocation使用哈希文件名
                    final String finalSafeFileName = safeFileName;
                    source.sendSuccess(() -> Component.literal("§e[玩偶皮肤] 提示: 文件名包含特殊字符，已使用路径映射: " + finalSafeFileName), false);
                }
                
                ResourceLocation textureLocation;
                try {
                    textureLocation = ResourceLocation.fromNamespaceAndPath(
                        "player_doll", 
                        texturePath
                    );
                } catch (IllegalArgumentException e) {
                    // 如果仍然失败，使用文件名哈希值作为后备方案
                    // 使用 MD5 哈希值以确保唯一性和符合 ResourceLocation 规范
                    if (safeFileName == null) {
                        try {
                            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                            byte[] hashBytes = md.digest(finalFileName.getBytes("UTF-8"));
                            StringBuilder hashString = new StringBuilder();
                            for (byte b : hashBytes) {
                                hashString.append(String.format("%02x", b));
                            }
                            // 使用前16个字符作为文件名（32个字符太长）
                            safeFileName = "skin_" + hashString.substring(0, 16) + ".png";
                        } catch (Exception ex) {
                            // 如果 MD5 失败，使用 hashCode（确保是正数）
                            int fileNameHash = Math.abs(finalFileName.hashCode());
                            safeFileName = "skin_" + Integer.toHexString(fileNameHash) + ".png";
                        }
                    }
                    texturePath = "png/" + safeFileName;
                    
                    // 使用路径映射，不需要复制文件
                    textureLocation = ResourceLocation.fromNamespaceAndPath(
                        "player_doll", 
                        texturePath
                    );
                    final String finalSafeFileName = safeFileName;
                    source.sendSuccess(() -> Component.literal("§e[玩偶皮肤] 提示: 使用路径映射作为纹理路径: " + finalSafeFileName), false);
                }
                
                // 注册纹理
                final ResourceLocation finalTextureLocation = textureLocation; // 创建 final 副本用于 lambda
                // 使用原始文件路径注册纹理（不复制文件）
                DynamicTextureManager.registerTexture(textureLocation, skinFile);
                
                // 如果使用了哈希文件名（包含特殊字符），注册路径映射
                if (safeFileName != null) {
                    DynamicTextureManager.registerPathMapping(finalFileName, textureLocation);
                    com.lanye.dolladdon.util.logging.ModuleLogger.debug(
                        com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                        "已注册路径映射: {} -> {}",
                        finalFileName, textureLocation
                    );
                }
                
                registeredTextureLocation = textureLocation; // 保存注册的纹理位置
                source.sendSuccess(() -> Component.literal("§a[玩偶皮肤] ✓ 纹理已注册: " + finalTextureLocation), false);
            } catch (IllegalArgumentException e) {
                source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 纹理路径格式错误: " + e.getMessage()));
                source.sendFailure(Component.literal("§7提示: 文件名可能包含无效字符，请检查文件名"));
                return 0;
            } catch (Exception e) {
                source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 纹理注册失败: " + e.getClass().getSimpleName()));
                source.sendFailure(Component.literal("§7错误详情: " + e.getMessage()));
                // 记录详细错误信息到日志
                com.lanye.dolladdon.util.logging.ModuleLogger.error(
                    com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                    "纹理注册失败 - 文件名: {}, 路径: {}, 错误: {}",
                    finalFileName, skinFile, e.getMessage(), e
                );
                return 0;
            }
            
            // 创建带NBT的玩偶物品
            // 使用注册纹理时使用的路径（确保一致性）
            final String nbtTexturePath = registeredTextureLocation != null 
                ? registeredTextureLocation.toString() 
                : "player_doll:png/" + finalFileName;
            
            try {
                ItemStack dollItem = createDollItemWithNBT(
                    finalPlayerName, 
                    playerUUID, 
                    nbtTexturePath,  // 使用注册纹理时使用的路径
                    isAlexModel
                );
                
                // 给予玩家物品
                ServerPlayer commandPlayer = null;
                if (source.getEntity() instanceof ServerPlayer player) {
                    commandPlayer = player;
                }
                
                if (commandPlayer != null) {
                    if (commandPlayer.getInventory().add(dollItem)) {
                        source.sendSuccess(() -> Component.literal("§a[玩偶皮肤] ✓ 玩偶物品已添加到您的背包"), true);
                        source.sendSuccess(() -> Component.literal("§7[玩偶皮肤] 提示: 放置玩偶后，它将显示玩家 §e" + finalPlayerName + " §7的皮肤"), false);
                    } else {
                        // 如果背包满了，尝试掉落物品
                        commandPlayer.drop(dollItem, false);
                        source.sendSuccess(() -> Component.literal("§e[玩偶皮肤] 背包已满，玩偶物品已掉落在地"), true);
                        source.sendSuccess(() -> Component.literal("§7[玩偶皮肤] 提示: 请拾取掉落的玩偶物品"), false);
                    }
                } else {
                    source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 此指令只能由玩家执行"));
                    return 0;
                }
            } catch (Exception e) {
                source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 创建玩偶物品时发生错误: " + e.getMessage()));
                return 0;
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 执行指令时发生错误: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 格式化文件大小
     * 
     * @param bytes 字节数
     * @return 格式化后的文件大小字符串
     */
    private static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 执行 nbt 子命令
     * 显示当前手持物品的NBT数据，格式化输出便于复制
     * 可以将NBT数据保存到文件以便复制
     * 
     * @param context 指令上下文
     * @param saveToFile 是否保存到文件
     * @return 执行结果（1表示成功，0表示失败）
     */
    private static int executeNBT(CommandContext<CommandSourceStack> context, boolean saveToFile) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§c[玩偶皮肤] 此指令只能由玩家执行"));
            return 0;
        }
        
        ItemStack stack = player.getMainHandItem();
        
        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("§c[玩偶皮肤] 您的手中没有物品"));
            source.sendSuccess(() -> Component.literal("§7提示: 请手持要查看NBT的物品"), false);
            return 0;
        }
        
        // 获取物品的NBT数据
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        
        source.sendSuccess(() -> Component.literal("§a[玩偶皮肤] 物品NBT数据:"), false);
        source.sendSuccess(() -> Component.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        
        // 显示物品基本信息
        source.sendSuccess(() -> Component.literal("§7物品ID: §e" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())), false);
        source.sendSuccess(() -> Component.literal("§7数量: §e" + stack.getCount()), false);
        
        if (customData != null) {
            var dataTag = customData.copyTag();
            if (dataTag != null) {
                // 格式化为多行显示，便于阅读和复制
                String nbtString = dataTag.toString();
                
                // 如果NBT太长，分段显示
                if (nbtString.length() > 300) {
                    source.sendSuccess(() -> Component.literal("§7NBT数据（点击下方文本可复制）:"), false);
                    
                    // 将NBT字符串按行分割显示
                    String[] lines = nbtString.split("(?<=\\{)|(?=\\{)|(?<=\\})|(?=\\})|(?<=,)|(?=,)|(?<=\\[)|(?=\\[)|(?<=\\])|(?=\\])");
                    StringBuilder currentLine = new StringBuilder("§7");
                    int lineLength = 0;
                    
                    for (String segment : lines) {
                        if (currentLine.length() + segment.length() > 100 && lineLength > 0) {
                            // 创建 final 变量供 lambda 使用
                            final String lineToSend = currentLine.toString();
                            source.sendSuccess(() -> Component.literal(lineToSend), false);
                            currentLine = new StringBuilder("§7").append(segment);
                            lineLength = segment.length();
                        } else {
                            currentLine.append(segment);
                            lineLength += segment.length();
                        }
                    }
                    
                    if (currentLine.length() > 2) {
                        // 创建 final 变量供 lambda 使用
                        final String finalLine = currentLine.toString();
                        source.sendSuccess(() -> Component.literal(finalLine), false);
                    }
                } else {
                    source.sendSuccess(() -> Component.literal("§7NBT数据: §e" + nbtString), false);
                }
                
                // 如果包含EntityData，显示结构化信息
                if (dataTag.contains("EntityData")) {
                    var entityTag = dataTag.getCompound("EntityData");
                    source.sendSuccess(() -> Component.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
                    source.sendSuccess(() -> Component.literal("§7实体数据 (EntityData):"), false);
                    
                    if (entityTag.contains("SkinPath", net.minecraft.nbt.Tag.TAG_STRING)) {
                        source.sendSuccess(() -> Component.literal("§7  SkinPath: §e" + entityTag.getString("SkinPath")), false);
                    }
                    if (entityTag.contains("IsAlexModel", net.minecraft.nbt.Tag.TAG_BYTE)) {
                        boolean isAlex = entityTag.getBoolean("IsAlexModel");
                        source.sendSuccess(() -> Component.literal("§7  IsAlexModel: §e" + isAlex), false);
                    }
                    if (entityTag.contains("PlayerName", net.minecraft.nbt.Tag.TAG_STRING)) {
                        source.sendSuccess(() -> Component.literal("§7  PlayerName: §e" + entityTag.getString("PlayerName")), false);
                    }
                    if (entityTag.contains("DisplayName", net.minecraft.nbt.Tag.TAG_STRING)) {
                        source.sendSuccess(() -> Component.literal("§7  DisplayName: §e" + entityTag.getString("DisplayName")), false);
                    }
                }
            } else {
                source.sendSuccess(() -> Component.literal("§7NBT数据: §e(null)"), false);
            }
        } else {
            source.sendSuccess(() -> Component.literal("§7NBT数据: §e(无)"), false);
        }
        
        source.sendSuccess(() -> Component.literal("§7━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"), false);
        
        // 如果请求保存到文件
        if (saveToFile) {
            try {
                // 获取NBT数据字符串
                String nbtString = "";
                if (customData != null) {
                    var dataTag = customData.copyTag();
                    if (dataTag != null) {
                        nbtString = dataTag.toString();
                    }
                }
                
                // 保存到文件
                // 获取游戏目录
                java.nio.file.Path gameDir;
                try {
                    Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
                    java.lang.reflect.Method gameDirMethod = fmlPathsClass.getMethod("getGamePath");
                    gameDir = (java.nio.file.Path) gameDirMethod.invoke(null);
                } catch (Exception e) {
                    gameDir = java.nio.file.Paths.get(".").toAbsolutePath().normalize();
                }
                
                // 创建 nbt_save 目录（player_doll/nbt_save/）
                java.nio.file.Path nbtDir = gameDir.resolve("player_doll/nbt_save");
                if (!java.nio.file.Files.exists(nbtDir)) {
                    java.nio.file.Files.createDirectories(nbtDir);
                }
                
                // 生成文件名（使用时间戳）
                String fileName = "item_nbt_" + java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                ) + ".txt";
                java.nio.file.Path nbtFile = nbtDir.resolve(fileName);
                
                // 构建完整的NBT数据文本
                StringBuilder fileContent = new StringBuilder();
                fileContent.append("物品NBT数据\n");
                fileContent.append("==========\n\n");
                fileContent.append("物品ID: ").append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())).append("\n");
                fileContent.append("数量: ").append(stack.getCount()).append("\n\n");
                fileContent.append("完整NBT数据:\n");
                fileContent.append(nbtString).append("\n\n");
                
                // 如果包含EntityData，添加结构化信息
                if (customData != null) {
                    var dataTag = customData.copyTag();
                    if (dataTag != null && dataTag.contains("EntityData")) {
                        var entityTag = dataTag.getCompound("EntityData");
                        fileContent.append("实体数据 (EntityData):\n");
                        if (entityTag.contains("SkinPath", net.minecraft.nbt.Tag.TAG_STRING)) {
                            fileContent.append("  SkinPath: ").append(entityTag.getString("SkinPath")).append("\n");
                        }
                        if (entityTag.contains("IsAlexModel", net.minecraft.nbt.Tag.TAG_BYTE)) {
                            fileContent.append("  IsAlexModel: ").append(entityTag.getBoolean("IsAlexModel")).append("\n");
                        }
                        if (entityTag.contains("PlayerName", net.minecraft.nbt.Tag.TAG_STRING)) {
                            fileContent.append("  PlayerName: ").append(entityTag.getString("PlayerName")).append("\n");
                        }
                        if (entityTag.contains("DisplayName", net.minecraft.nbt.Tag.TAG_STRING)) {
                            fileContent.append("  DisplayName: ").append(entityTag.getString("DisplayName")).append("\n");
                        }
                    }
                }
                
                // 写入文件
                java.nio.file.Files.writeString(nbtFile, fileContent.toString(), java.nio.charset.StandardCharsets.UTF_8);
                
                source.sendSuccess(() -> Component.literal("§a[玩偶皮肤] ✓ NBT数据已保存到文件"), true);
                source.sendSuccess(() -> Component.literal("§7文件路径: §e" + nbtFile.toString()), false);
                source.sendSuccess(() -> Component.literal("§7提示: 可以打开文件复制NBT数据"), false);
                
            } catch (Exception e) {
                source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 保存NBT文件失败: " + e.getMessage()));
                com.lanye.dolladdon.util.logging.ModuleLogger.error(
                    com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                    "executeNBT: 保存NBT文件失败",
                    e
                );
            }
        } else {
            source.sendSuccess(() -> Component.literal("§7提示: 使用 §e/dollskin nbt save §7将NBT数据保存到文件以便复制"), false);
            source.sendSuccess(() -> Component.literal("§7或者使用原版命令: §e/data get entity @s SelectedItem"), false);
        }
        
        return 1;
    }
    
    /**
     * 创建CustomData对象
     * 使用反射调用CustomData.of()静态方法，支持多个可能的类路径
     * 
     * @param nbt NBT标签
     * @return CustomData对象，如果创建失败返回null
     * @deprecated 请使用 {@link com.lanye.dolladdon.util.factory.DollItemFactory#createCustomData(net.minecraft.nbt.CompoundTag)} 替代
     */
    @Deprecated(forRemoval = false)
    public static Object createCustomData(net.minecraft.nbt.CompoundTag nbt) {
        String[] possiblePaths = {
            "net.minecraft.core.component.types.CustomData",
            "net.minecraft.core.component.CustomData",
            "net.minecraft.world.item.component.CustomData"
        };
        
        Exception lastException = null;
        for (String className : possiblePaths) {
            try {
                Class<?> customDataClass = Class.forName(className);
                java.lang.reflect.Method ofMethod = customDataClass.getMethod("of", net.minecraft.nbt.CompoundTag.class);
                Object result = ofMethod.invoke(null, nbt);
                if (result != null) {
                    com.lanye.dolladdon.util.logging.ModuleLogger.debug(
                        com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                        "createCustomData: 成功创建CustomData对象 - 类路径: {}",
                        className
                    );
                    return result;
                }
            } catch (ClassNotFoundException e) {
                lastException = e;
                continue;
            } catch (Exception e) {
                lastException = e;
                continue;
            }
        }
        
        if (lastException != null) {
            com.lanye.dolladdon.util.logging.ModuleLogger.error(
                com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                "createCustomData: 所有路径都失败 - 最后错误: {}",
                lastException.getMessage(), lastException
            );
        } else {
            com.lanye.dolladdon.util.logging.ModuleLogger.error(
                com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                "createCustomData: 所有路径都失败 - 未知错误"
            );
        }
        return null;
    }
}
