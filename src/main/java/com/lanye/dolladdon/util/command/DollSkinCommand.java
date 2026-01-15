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
                    Commands.argument("player", StringArgumentType.word())
                        .suggests(PLAYER_SUGGESTIONS) // Tab补全：显示在线玩家列表
                        .executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "player")))
                )
        );
        
        // 注册 /mydollskin 命令（专门用于获取当前玩家的皮肤）
        dispatcher.register(
            Commands.literal("mydollskin")
                .requires(source -> source.hasPermission(2)) // 需要OP权限
                .executes(ctx -> execute(ctx, null)) // 获取自己的皮肤
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
            
            // 生成文件名（包含UUID以避免重名）
            final String fileName = SkinFileNamingUtil.generateFileName(targetPlayerName, isAlexModel, targetPlayerUUID);
            
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
                source.sendSuccess(() -> Component.literal("§e[玩偶皮肤] 检测到已存在的皮肤文件，将覆盖: §7" + fileName), false);
            }
            
            // 异步下载并保存皮肤，避免阻塞服务器主线程
            source.sendSuccess(() -> Component.literal("§7[玩偶皮肤] 正在下载皮肤文件（异步执行，请稍候...）"), false);
            
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
        // 如果 fileName 已经是完整的 ResourceLocation 格式（包含 :），直接使用
        // 否则添加前缀
        String skinPath = fileName.contains(":") ? fileName : "player_doll:png/" + fileName;
        entityDataTag.putString("SkinPath", skinPath);
        entityDataTag.putBoolean("IsAlexModel", isAlexModel);
        // 注意：PlayerName 使用原始玩家名，不包含UUID（文件名中包含UUID用于避免重名，但显示名称不包含）
        entityDataTag.putString("PlayerName", playerName);
        entityDataTag.putString("PlayerUUID", playerUUID.toString());
        
        // 将EntityData放入customData
        customDataTag.put("EntityData", entityDataTag);
        
        // 设置到ItemStack的DataComponents
        // 使用applyComponents方法设置CUSTOM_DATA组件
        // 在NeoForge 1.21.1中，使用反射创建CustomData对象（使用of()静态方法）
        Object customData = createCustomData(customDataTag);
        if (customData == null) {
            com.lanye.dolladdon.util.logging.ModuleLogger.error(
                com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                "createDollItemWithNBT: 无法创建CustomData对象"
            );
            return dollItem; // 返回没有NBT的物品
        }
        
        try {
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
            
            // 验证 NBT 是否成功设置
            var verifyCustomData = dollItem.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (verifyCustomData == null) {
                com.lanye.dolladdon.util.logging.ModuleLogger.error(
                    com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                    "createDollItemWithNBT: applyComponents后验证失败 - customData为null"
                );
            } else {
                var verifyDataTag = verifyCustomData.copyTag();
                if (verifyDataTag == null || !verifyDataTag.contains("EntityData")) {
                    com.lanye.dolladdon.util.logging.ModuleLogger.error(
                        com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                        "createDollItemWithNBT: applyComponents后验证失败 - EntityData标签缺失, dataTag={}",
                        verifyDataTag
                    );
                } else {
                    com.lanye.dolladdon.util.logging.ModuleLogger.debug(
                        com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                        "createDollItemWithNBT: NBT设置成功 - SkinPath={}",
                        verifyDataTag.getCompound("EntityData").getString("SkinPath")
                    );
                }
            }
        } catch (Exception e) {
            // 如果applyComponents失败，尝试使用ItemStack的set方法（如果可用）
            com.lanye.dolladdon.util.logging.ModuleLogger.warn(
                com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                "createDollItemWithNBT: applyComponents失败，尝试备用方法 - 错误: {}",
                e.getMessage()
            );
            try {
                java.lang.reflect.Method setMethod = ItemStack.class.getMethod("set", 
                    net.minecraft.core.component.DataComponentType.class, Object.class);
                setMethod.invoke(dollItem, net.minecraft.core.component.DataComponents.CUSTOM_DATA, customData);
                
                // 验证备用方法是否成功
                var verifyCustomData = dollItem.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (verifyCustomData == null) {
                    com.lanye.dolladdon.util.logging.ModuleLogger.error(
                        com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                        "createDollItemWithNBT: 备用方法也失败 - customData为null"
                    );
                } else {
                    com.lanye.dolladdon.util.logging.ModuleLogger.debug(
                        com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                        "createDollItemWithNBT: 备用方法成功设置NBT"
                    );
                }
            } catch (Exception e2) {
                // 如果都失败，记录错误但不抛出异常（让游戏继续运行）
                com.lanye.dolladdon.util.logging.ModuleLogger.error(
                    com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                    "createDollItemWithNBT: 所有方法都失败 - 第一次错误: {}, 第二次错误: {}",
                    e.getMessage(), e2.getMessage(), e2
                );
            }
        }
        
        return dollItem;
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
            
            // 从文件名提取信息
            String playerName = SkinFileNamingUtil.extractPlayerNameFromFileName(fileName);
            boolean isAlexModel = SkinFileNamingUtil.extractModelTypeFromFileName(fileName);
            
            if (playerName == null || playerName.isEmpty()) {
                source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 无法从文件名提取玩家名称: " + fileName));
                return 0;
            }
            
            // 尝试从文件名提取UUID（如果存在）
            String uuidShort = extractUuidFromFileName(fileName);
            java.util.UUID playerUUID = null;
            if (uuidShort != null && uuidShort.length() == 8) {
                // 尝试从在线玩家中查找匹配的UUID
                for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                    String playerUuidShort = getUuidShort(player.getUUID());
                    if (playerUuidShort.equals(uuidShort)) {
                        playerUUID = player.getUUID();
                        playerName = player.getName().getString(); // 使用在线玩家的实际名称
                        break;
                    }
                }
            }
            
            // 如果找不到匹配的在线玩家，使用默认UUID或从文件名生成
            if (playerUUID == null) {
                // 使用一个基于玩家名的固定UUID（用于离线模式）
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
                // ResourceLocation 路径只允许 [a-z0-9/_-] 字符
                // URL 编码会引入 % 字符，也不被允许，所以直接使用哈希值
                boolean containsNonAscii = finalFileName.chars().anyMatch(ch -> ch > 127 || (ch < 32 && ch != 9 && ch != 10 && ch != 13));
                // 也检查是否包含不允许的字符（除了字母、数字、点、下划线、连字符）
                boolean containsInvalidChars = finalFileName.chars().anyMatch(ch -> {
                    return !((ch >= 'a' && ch <= 'z') || 
                            (ch >= 'A' && ch <= 'Z') || 
                            (ch >= '0' && ch <= '9') || 
                            ch == '.' || ch == '_' || ch == '-');
                });
                
                String safeFileName = null;
                if (containsNonAscii || containsInvalidChars) {
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
                    
                    // 如果生成了哈希文件名，需要将文件复制到哈希文件名，确保文件路径和NBT路径一致
                    Path safeFile = pngDir.resolve(safeFileName);
                    if (!java.nio.file.Files.exists(safeFile)) {
                        // 复制原始文件到哈希文件名
                        try {
                            java.nio.file.Files.copy(skinFile, safeFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            com.lanye.dolladdon.util.logging.ModuleLogger.debug(
                                com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                                "文件已复制到哈希文件名: {} -> {}",
                                skinFile.getFileName(), safeFileName
                            );
                        } catch (Exception e) {
                            com.lanye.dolladdon.util.logging.ModuleLogger.error(
                                com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                                "复制文件到哈希文件名失败: {} -> {}, 错误: {}",
                                skinFile.getFileName(), safeFileName, e.getMessage(), e
                            );
                            source.sendFailure(Component.literal("§c[玩偶皮肤] ✗ 无法创建哈希文件名副本: " + e.getMessage()));
                            return 0;
                        }
                    }
                    // 更新 skinFile 为哈希文件名路径，确保后续使用正确的文件路径
                    skinFile = safeFile;
                    
                    final String finalSafeFileName = safeFileName;
                    source.sendSuccess(() -> Component.literal("§e[玩偶皮肤] 提示: 文件名包含特殊字符，已使用哈希值: " + finalSafeFileName), false);
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
                    
                    // 如果生成了哈希文件名，需要将文件复制到哈希文件名
                    Path safeFile = pngDir.resolve(safeFileName);
                    if (!java.nio.file.Files.exists(safeFile)) {
                        try {
                            java.nio.file.Files.copy(skinFile, safeFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            com.lanye.dolladdon.util.logging.ModuleLogger.debug(
                                com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                                "文件已复制到哈希文件名（后备方案）: {} -> {}",
                                skinFile.getFileName(), safeFileName
                            );
                        } catch (Exception ex) {
                            com.lanye.dolladdon.util.logging.ModuleLogger.error(
                                com.lanye.dolladdon.util.logging.LogModuleConfig.MODULE_COMMAND,
                                "复制文件到哈希文件名失败（后备方案）: {} -> {}, 错误: {}",
                                skinFile.getFileName(), safeFileName, ex.getMessage(), ex
                            );
                        }
                    }
                    // 更新 skinFile 为哈希文件名路径
                    skinFile = safeFile;
                    
                    textureLocation = ResourceLocation.fromNamespaceAndPath(
                        "player_doll", 
                        texturePath
                    );
                    final String finalSafeFileName = safeFileName;
                    source.sendSuccess(() -> Component.literal("§e[玩偶皮肤] 提示: 使用哈希值作为纹理路径: " + finalSafeFileName), false);
                }
                
                // 注册纹理
                final ResourceLocation finalTextureLocation = textureLocation; // 创建 final 副本用于 lambda
                DynamicTextureManager.registerTexture(textureLocation, skinFile);
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
     * 创建CustomData对象
     * 使用反射调用CustomData.of()静态方法，支持多个可能的类路径
     * 
     * @param nbt NBT标签
     * @return CustomData对象，如果创建失败返回null
     */
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
