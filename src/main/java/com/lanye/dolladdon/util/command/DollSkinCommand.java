package com.lanye.dolladdon.util.command;

import com.lanye.dolladdon.PlayerDoll;
import com.lanye.dolladdon.init.ModItems;
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
            final ServerPlayer finalTargetPlayer = targetPlayer;
            
            // 检查文件是否已存在
            // 在读取时忽略UUID部分，只比较玩家名和模型类型
            // 如果存在相同玩家名和模型类型的文件，允许覆盖
            boolean fileExists = java.nio.file.Files.exists(targetPath);
            if (!fileExists) {
                // 检查是否存在相同玩家名和模型类型的文件（忽略UUID）
                String sanitizedPlayerName = SkinFileNamingUtil.sanitizePlayerName(targetPlayerName);
                String expectedNamePart = (isAlexModel ? "A" : "S") + sanitizedPlayerName;
                
                try {
                    // 使用已定义的 pngDir 变量（第119行）
                    if (java.nio.file.Files.exists(pngDir) && java.nio.file.Files.isDirectory(pngDir)) {
                        try (Stream<Path> paths = java.nio.file.Files.list(pngDir)) {
                            fileExists = paths
                                .filter(java.nio.file.Files::isRegularFile)
                                .filter(path -> path.toString().toLowerCase().endsWith(".png"))
                                .anyMatch(path -> {
                                    String existingFileName = path.getFileName().toString();
                                    // 从文件名提取玩家名（忽略UUID）
                                    String existingPlayerName = SkinFileNamingUtil.extractPlayerNameFromFileName(existingFileName);
                                    boolean existingIsAlex = SkinFileNamingUtil.extractModelTypeFromFileName(existingFileName);
                                    // 比较玩家名和模型类型（忽略UUID）
                                    return existingPlayerName != null 
                                        && existingPlayerName.equals(sanitizedPlayerName)
                                        && existingIsAlex == isAlexModel;
                                });
                        }
                    }
                } catch (Exception e) {
                    // 如果检查失败，继续使用原始文件存在检查
                }
            }
            
            if (fileExists) {
                // 文件已存在（或存在相同玩家名和模型类型的文件），允许覆盖
                source.sendSuccess(() -> Component.literal("文件已存在，将覆盖: " + fileName), false);
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
                    
                    // 注册纹理到DynamicTextureManager（确保纹理可以被访问）
                    try {
                        ResourceLocation textureLocation = ResourceLocation.fromNamespaceAndPath(
                            "player_doll", 
                            "png/" + fileName
                        );
                        DynamicTextureManager.registerTexture(textureLocation, targetPath);
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
                                source.sendSuccess(() -> Component.literal("玩偶物品已给予: " + targetPlayerName), true);
                            } else {
                                // 如果背包满了，尝试掉落物品
                                commandPlayer.drop(dollItem, false);
                                source.sendSuccess(() -> Component.literal("背包已满，玩偶物品已掉落在地"), true);
                            }
                        }
                    } catch (Exception e) {
                        // 如果给予物品失败，不影响主流程
                        source.sendSuccess(() -> Component.literal("注意：无法自动给予玩偶物品，请手动在创造模式物品栏中获取"), false);
                    }
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
    
    /**
     * 创建带NBT的玩偶物品
     * 根据模型类型选择Steve或Alex玩偶，并在NBT中保存皮肤路径
     * 
     * @param playerName 玩家名称
     * @param playerUUID 玩家UUID
     * @param fileName 皮肤文件名
     * @param isAlexModel 是否为Alex模型
     * @return 带NBT的玩偶物品
     */
    private static ItemStack createDollItemWithNBT(String playerName, java.util.UUID playerUUID, 
                                                   String fileName, boolean isAlexModel) {
        // 根据模型类型选择玩偶
        ItemStack dollItem = isAlexModel ? 
            new ItemStack(ModItems.ALEX_DOLL.get(), 1) : 
            new ItemStack(ModItems.STEVE_DOLL.get(), 1);
        
        // 设置NBT数据（使用DataComponents）
        // 在1.21.1中，NBT数据存储在DataComponents.CUSTOM_DATA中
        net.minecraft.nbt.CompoundTag customDataTag = new net.minecraft.nbt.CompoundTag();
        
        // 创建EntityData子标签（用于存储实体相关的NBT数据）
        net.minecraft.nbt.CompoundTag entityDataTag = new net.minecraft.nbt.CompoundTag();
        entityDataTag.putString("SkinPath", "player_doll:png/" + fileName);
        entityDataTag.putBoolean("IsAlexModel", isAlexModel);
        // 注意：PlayerName 使用原始玩家名，不包含UUID（文件名中包含UUID用于避免重名，但显示名称不包含）
        entityDataTag.putString("PlayerName", playerName);
        entityDataTag.putString("PlayerUUID", playerUUID.toString());
        
        // 将EntityData放入customData
        customDataTag.put("EntityData", entityDataTag);
        
        // 设置到ItemStack的DataComponents
        // 使用applyComponents方法设置CUSTOM_DATA组件
        // 在NeoForge 1.21.1中，使用反射创建CustomData对象
        try {
            // 使用反射获取CustomData类并创建实例
            Class<?> customDataClass = Class.forName("net.minecraft.core.component.CustomData");
            java.lang.reflect.Constructor<?> constructor = customDataClass.getConstructor(net.minecraft.nbt.CompoundTag.class);
            constructor.setAccessible(true);
            Object customData = constructor.newInstance(customDataTag);
            
            // 使用applyComponents设置组件
            // 使用反射调用builder的set方法，避免类型推断问题
            net.minecraft.core.component.DataComponentPatch.Builder builder = 
                net.minecraft.core.component.DataComponentPatch.builder();
            // 使用反射调用set方法，避免泛型类型检查
            java.lang.reflect.Method setMethod = builder.getClass().getDeclaredMethod("set", 
                net.minecraft.core.component.DataComponentType.class, Object.class);
            setMethod.setAccessible(true);
            setMethod.invoke(builder, 
                net.minecraft.core.component.DataComponents.CUSTOM_DATA, 
                customData);
            dollItem.applyComponents(builder.build());
        } catch (Exception e) {
            // 如果反射失败，尝试使用ItemStack的set方法（如果可用）
            try {
                java.lang.reflect.Method setMethod = ItemStack.class.getMethod("set", 
                    net.minecraft.core.component.DataComponentType.class, Object.class);
                // 需要先创建CustomData对象
                Class<?> customDataClass = Class.forName("net.minecraft.core.component.CustomData");
                java.lang.reflect.Constructor<?> constructor = customDataClass.getConstructor(net.minecraft.nbt.CompoundTag.class);
                constructor.setAccessible(true);
                Object customData = constructor.newInstance(customDataTag);
                setMethod.invoke(dollItem, net.minecraft.core.component.DataComponents.CUSTOM_DATA, customData);
            } catch (Exception e2) {
                // 如果都失败，记录错误但不抛出异常（让游戏继续运行）
                // Error logging handled by Mixin
            }
        }
        
        return dollItem;
    }
}
