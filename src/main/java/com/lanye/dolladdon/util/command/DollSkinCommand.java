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
            String targetPlayerName = targetPlayer.getName().getString();
            boolean isAlexModel = com.lanye.dolladdon.util.resource.PlayerSkinUtil.isAlexModel(
                targetPlayer.getUUID(), 
                targetPlayerName
            );
            
            // 生成文件名
            String fileName = SkinFileNamingUtil.generateFileName(targetPlayerName, isAlexModel);
            
            // 获取保存路径
            Path pngDir = PlayerSkinDownloader.getPngDirectory();
            Path targetPath = pngDir.resolve(fileName);
            
            // 检查文件是否已存在
            boolean fileExists = java.nio.file.Files.exists(targetPath);
            if (fileExists) {
                source.sendFailure(Component.literal("文件已存在: " + fileName + "。如需覆盖，请先删除该文件。"));
                return 0;
            }
            
            // 下载并保存皮肤
            source.sendSuccess(() -> Component.literal("正在下载玩家皮肤: " + targetPlayerName), false);
            
            boolean success = PlayerSkinDownloader.downloadPlayerSkin(
                targetPlayer, 
                targetPath, 
                false // 不覆盖已存在的文件
            );
            
            if (!success) {
                source.sendFailure(Component.literal("下载或保存皮肤失败: " + targetPlayerName));
                return 0;
            }
            
            // 触发动态玩偶重新扫描
            // 注意：由于动态玩偶是在模组初始化时注册的，我们需要重新扫描并注册
            // 但是，在运行时动态注册新实体比较复杂，这里我们只保存文件
            // 玩家需要重启游戏或重新加载资源包才能看到新注册的玩偶
            // 或者，我们可以尝试在运行时注册（但这需要更复杂的实现）
            
            // 尝试重新扫描目录（这不会自动注册新实体，但可以更新纹理）
            // 注意：DynamicDollLoader.scanDirectory() 只是扫描文件，不会注册实体
            // 要在运行时注册新实体，需要更复杂的实现
            
            source.sendSuccess(() -> Component.literal("皮肤已保存: " + fileName), true);
            source.sendSuccess(() -> Component.literal("注意：需要重启游戏或重新加载资源包才能使用新注册的玩偶"), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("执行指令时发生错误: " + e.getMessage()));
            // Error logging handled by Mixin
            return 0;
        }
    }
}
