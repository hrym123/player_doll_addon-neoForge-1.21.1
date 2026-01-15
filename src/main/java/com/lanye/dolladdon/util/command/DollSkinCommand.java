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
            
            // 生成文件名
            final String fileName = SkinFileNamingUtil.generateFileName(targetPlayerName, isAlexModel);
            
            // 获取保存路径
            Path pngDir = PlayerSkinDownloader.getPngDirectory();
            final Path targetPath = pngDir.resolve(fileName);
            
            // 检查文件是否已存在
            boolean fileExists = java.nio.file.Files.exists(targetPath);
            if (fileExists) {
                source.sendFailure(Component.literal("文件已存在: " + fileName + "。如需覆盖，请先删除该文件。"));
                return 0;
            }
            
            // 异步下载并保存皮肤，避免阻塞服务器主线程
            source.sendSuccess(() -> Component.literal("正在下载玩家皮肤: " + targetPlayerName + "（异步执行，请稍候...）"), false);
            
            // 使用服务器的工作线程池异步执行下载任务
            // 注意：使用 final 变量以确保 lambda 表达式可以正确捕获
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                return PlayerSkinDownloader.downloadPlayerSkin(
                    targetPlayerUUID, 
                    targetPlayerName, 
                    targetPath, 
                    false // 不覆盖已存在的文件
                );
            }, source.getServer()).thenAcceptAsync(success -> {
                // 在主线程中发送结果消息
                if (success) {
                    source.sendSuccess(() -> Component.literal("皮肤已保存: " + fileName), true);
                    source.sendSuccess(() -> Component.literal("注意：需要重启游戏或重新加载资源包才能使用新注册的玩偶"), false);
                } else {
                    source.sendFailure(Component.literal("下载或保存皮肤失败: " + targetPlayerName));
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
}
