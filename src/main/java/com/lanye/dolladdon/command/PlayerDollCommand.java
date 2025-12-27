package com.lanye.dolladdon.command;

import com.lanye.dolladdon.PlayerDollAddon;
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
    private static int getSelfDoll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        
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
            // 使用新的原版方式创建玩家玩偶物品
            ItemStack dollItem = new ItemStack(com.lanye.dolladdon.init.ModItems.PLAYER_DOLL.get());
            
            // 保存玩家信息到物品 Data Component
            net.minecraft.nbt.CompoundTag playerData = new net.minecraft.nbt.CompoundTag();
            playerData.putUUID("player_uuid", target.getUUID());
            playerData.putString("player_name", target.getName().getString());
            dollItem.set(com.lanye.dolladdon.init.ModDataComponents.PLAYER_DATA.get(), playerData);
            
            // 添加到执行者的背包
            if (executor.getInventory().add(dollItem)) {
                source.sendSuccess(() -> Component.translatable("command.player_doll_addon.success", 
                        target.getName().getString()), true);
                return 1;
            } else {
                // 如果背包满了，掉落在地上
                executor.drop(dollItem, false);
                source.sendSuccess(() -> Component.translatable("command.player_doll_addon.success_dropped", 
                        target.getName().getString()), true);
                return 1;
            }
        } catch (Exception e) {
            PlayerDollAddon.LOGGER.error("创建玩家玩偶失败", e);
            source.sendFailure(Component.translatable("command.player_doll_addon.error", e.getMessage()));
            return 0;
        }
    }
}

