package com.lanye.dolladdon.command;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.config.ModConfig;
import com.lanye.dolladdon.util.PlayerSkinUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 玩家玩偶命令
 * 用于获取当前玩家的皮肤模型并创建物品
 */
public class PlayerDollCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("playerdoll")
                .requires(source -> source.hasPermission(2)) // 需要 OP 权限
                .then(Commands.literal("get")
                        .executes(PlayerDollCommand::getSelfDoll) // 获取自己的玩偶
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(PlayerDollCommand::getPlayerDoll) // 获取指定玩家的玩偶
                        )
                )
        );
    }
    
    /**
     * 获取自己的玩偶
     */
    /**
     * 处理 "/playerdoll get" 命令，给予自己一个自己的玩家玩偶
     * @param context 命令上下文
     * @return 命令执行返回值（成功为1，失败为0）
     * @throws CommandSyntaxException 如果命令执行出现语法错误
     */
    private static int getSelfDoll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // 获取命令来源
        CommandSourceStack source = context.getSource();

        // 获取执行该命令的玩家
        ServerPlayer player = source.getPlayerOrException();

        // 调用方法，为玩家自己赠送玩家玩偶物品
        return givePlayerDoll(source, player, player);
    }
    /**
     * 获取指定玩家的玩偶
     */
    private static int getPlayerDoll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer executor = source.getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        
        return givePlayerDoll(source, executor, target);
    }
    
    /**
     * 给予玩家玩偶物品
     */
    private static int givePlayerDoll(CommandSourceStack source, Player executor, Player target) {
        try {
            java.util.UUID playerUUID = target.getUUID();
            String playerName = target.getName().getString();
            
            if (ModConfig.isTestMode()) {
                PlayerDollAddon.LOGGER.info("[PlayerDollCommand] 开始创建玩家玩偶 - UUID: {}, 名称: {}", playerUUID, playerName);
            }
            
            // 预加载玩家皮肤，确保皮肤能够正确识别和加载
            // 这对于Alex模型的正确识别特别重要
            try {
                if (ModConfig.isTestMode()) {
                    PlayerDollAddon.LOGGER.debug("[PlayerDollCommand] 预加载玩家皮肤...");
                }
                boolean isAlexModel = PlayerSkinUtil.isAlexModel(playerUUID, playerName);
                String modelType = isAlexModel ? "Alex(细手臂)" : "Steve(粗手臂)";
                PlayerDollAddon.LOGGER.info("[PlayerDollCommand] 玩家模型类型检测 - UUID: {}, 名称: {}, 模型类型: {}", 
                        playerUUID, playerName, modelType);
                
                // 获取皮肤位置，触发皮肤加载
                var skinLocation = PlayerSkinUtil.getSkinLocation(playerUUID, playerName);
                PlayerDollAddon.LOGGER.info("[PlayerDollCommand] 玩家皮肤加载完成 - 纹理位置: {}", skinLocation);
            } catch (Exception skinException) {
                PlayerDollAddon.LOGGER.warn("[PlayerDollCommand] 预加载皮肤时出错（继续创建物品）: {}", skinException.getMessage());
                // 即使皮肤加载失败，也继续创建物品，因为渲染时会再次尝试加载
            }
            
            // 使用新的原版方式创建玩家玩偶物品
            ItemStack dollItem = new ItemStack(com.lanye.dolladdon.init.ModItems.PLAYER_DOLL.get());
            
            // 保存玩家信息到物品 Data Component
            net.minecraft.nbt.CompoundTag playerData = new net.minecraft.nbt.CompoundTag();
            playerData.putUUID("player_uuid", playerUUID);
            playerData.putString("player_name", playerName);
            dollItem.set(com.lanye.dolladdon.init.ModDataComponents.PLAYER_DATA.get(), playerData);
            
            if (ModConfig.isTestMode()) {
                PlayerDollAddon.LOGGER.debug("[PlayerDollCommand] 玩家玩偶物品创建完成，添加到背包");
            }
            
            // 添加到执行者的背包
            if (executor.getInventory().add(dollItem)) {
                source.sendSuccess(() -> Component.translatable("command.player_doll_addon.success", 
                        playerName), true);
                if (ModConfig.isTestMode()) {
                    PlayerDollAddon.LOGGER.info("[PlayerDollCommand] 玩家玩偶成功添加到背包");
                }
                return 1;
            } else {
                // 如果背包满了，掉落在地上
                executor.drop(dollItem, false);
                source.sendSuccess(() -> Component.translatable("command.player_doll_addon.success_dropped", 
                        playerName), true);
                if (ModConfig.isTestMode()) {
                    PlayerDollAddon.LOGGER.info("[PlayerDollCommand] 背包已满，玩家玩偶掉落在地上");
                }
                return 1;
            }
        } catch (Exception e) {
            PlayerDollAddon.LOGGER.error("[PlayerDollCommand] 创建玩家玩偶失败", e);
            source.sendFailure(Component.translatable("command.player_doll_addon.error", e.getMessage()));
            return 0;
        }
    }
}

