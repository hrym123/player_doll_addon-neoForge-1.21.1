package com.lanye.dolladdon.util.command;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.util.neoForge.DynamicDollLoader;
import com.lanye.dolladdon.util.resource.PlayerSkinDownloader;
import com.lanye.dolladdon.util.resource.SkinFileNamingUtil;
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
     * 注册指令
     * 
     * @param dispatcher 指令分发器
     * @param context 指令构建上下文
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("dollskin")
                .requires(source -> source.hasPermission(2)) // 需要OP权限
                .executes(ctx -> execute(ctx, null)) // 无参数：获取自己的皮肤
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
            
            // 如果提供了玩家名，查找对应玩家
            if (playerName != null && !playerName.isEmpty()) {
                targetPlayer = source.getServer().getPlayerList().getPlayerByName(playerName);
                if (targetPlayer == null) {
                    source.sendFailure(Component.literal("玩家不存在或不在线: " + playerName));
                    return 0;
                }
            } else {
                // 如果没有提供玩家名，使用执行指令的玩家
                if (source.getEntity() instanceof ServerPlayer player) {
                    targetPlayer = player;
                } else {
                    source.sendFailure(Component.literal("此指令只能由玩家执行，或需要指定玩家名"));
                    return 0;
                }
            }
            
            if (targetPlayer == null) {
                source.sendFailure(Component.literal("无法获取玩家信息"));
                return 0;
            }
            
            // 获取玩家信息
            final String targetPlayerName = targetPlayer.getName().getString();
            final java.util.UUID targetPlayerUUID = targetPlayer.getUUID();
            boolean isAlexModel = com.lanye.dolladdon.util.resource.PlayerSkinUtil.isAlexModel(
                targetPlayerUUID, 
                targetPlayerName
            );
            
            // 生成文件名（包含UUID以避免重名）
            final String fileName = SkinFileNamingUtil.generateFileName(targetPlayerName, isAlexModel, targetPlayerUUID);
            
            // 获取保存路径
            Path pngDir = PlayerSkinDownloader.getPngDirectory();
            final Path targetPath = pngDir.resolve(fileName);
            
            // 检查文件是否已存在
            // 如果文件已存在，检查是否是同一个玩家的文件（通过UUID判断）
            boolean fileExists = java.nio.file.Files.exists(targetPath);
            if (fileExists) {
                // 从已存在的文件名中提取UUID短版本
                String existingUuidShort = extractUuidFromFileName(fileName);
                // 获取当前玩家的UUID短版本
                String currentUuidShort = getUuidShort(targetPlayerUUID);
                
                // 如果文件名包含相同的UUID短版本，说明是同一个玩家，允许覆盖
                // 否则，可能是不同玩家但名字相同，不允许覆盖
                if (existingUuidShort != null && existingUuidShort.equalsIgnoreCase(currentUuidShort)) {
                    // 同一个玩家，允许覆盖（在downloadPlayerSkin中处理）
                    source.sendSuccess(() -> Component.literal("文件已存在，将覆盖: " + fileName), false);
                } else {
                    // 不同玩家，不允许覆盖
                    source.sendFailure(Component.literal("文件已存在且可能属于其他玩家: " + fileName + "。如需覆盖，请先删除该文件。"));
                    return 0;
                }
            }
            
            // 异步下载并保存皮肤，避免阻塞服务器主线程
            source.sendSuccess(() -> Component.literal("正在下载玩家皮肤: " + targetPlayerName + "（异步执行，请稍候...）"), false);
            
            // 使用服务器的工作线程池异步执行下载任务
            // 注意：使用 final 变量以确保 lambda 表达式可以正确捕获
            final boolean allowOverwrite = fileExists; // 如果文件已存在且是同一个玩家，允许覆盖
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                return PlayerSkinDownloader.downloadPlayerSkin(
                    targetPlayerUUID, 
                    targetPlayerName, 
                    targetPath, 
                    allowOverwrite // 如果是同一个玩家的文件，允许覆盖
                );
            }, source.getServer()).thenAcceptAsync(result -> {
                // 在主线程中发送结果消息
                if (result.isSuccess()) {
                    source.sendSuccess(() -> Component.literal("皮肤已保存: " + fileName), true);
                    if (result.isFallback()) {
                        source.sendSuccess(() -> Component.literal("注意：使用了回退方案（开发模式或离线模式）"), false);
                    }
                    source.sendSuccess(() -> Component.literal("注意：需要重启游戏或重新加载资源包才能使用新注册的玩偶"), false);
                } else {
                    String errorMsg = "下载或保存皮肤失败: " + targetPlayerName;
                    if (result.getErrorMessage() != null) {
                        errorMsg += " (" + result.getErrorMessage() + ")";
                    }
                    source.sendFailure(Component.literal(errorMsg));
                }
            }, source.getServer());
            
            // 立即返回，不等待下载完成
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("执行指令时发生错误: " + e.getMessage()));
            // Error logging handled by Mixin
            return 0;
        }
    }
    
    /**
     * 从文件名中提取UUID短版本
     * 文件名格式：[S|A]<玩家名>_<UUID短版本>.png
     * 
     * @param fileName 文件名
     * @return UUID短版本，如果无法提取则返回null
     */
    private static String extractUuidFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        // 移除扩展名
        if (fileName.endsWith(".png")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }
        
        // 查找最后一个下划线的位置（玩家名和UUID之间的分隔符）
        int lastUnderscore = fileName.lastIndexOf('_');
        if (lastUnderscore == -1 || lastUnderscore == fileName.length() - 1) {
            return null;
        }
        
        // 提取UUID短版本（最后一个下划线之后的部分）
        String uuidShort = fileName.substring(lastUnderscore + 1);
        
        // 验证是否是有效的UUID短版本（8位十六进制）
        if (uuidShort.length() == 8 && uuidShort.matches("[0-9a-fA-F]{8}")) {
            return uuidShort.toLowerCase();
        }
        
        return null;
    }
    
    /**
     * 获取UUID的短版本（前8位，不含连字符）
     * 用于文件名中避免重名
     * 
     * @param uuid 玩家UUID
     * @return UUID的短版本（8位十六进制字符串）
     */
    private static String getUuidShort(java.util.UUID uuid) {
        if (uuid == null) {
            return "00000000";
        }
        // 获取UUID的字符串表示，去掉连字符，取前8位
        String uuidString = uuid.toString().replace("-", "");
        return uuidString.substring(0, Math.min(8, uuidString.length())).toLowerCase();
    }
}
