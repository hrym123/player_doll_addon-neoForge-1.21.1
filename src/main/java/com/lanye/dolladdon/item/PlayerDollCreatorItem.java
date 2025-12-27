package com.lanye.dolladdon.item;

import com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity;
import com.github.ysbbbbbb.kaleidoscopedoll.init.ModEntities;
import com.github.ysbbbbbb.kaleidoscopedoll.item.DollEntityItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 玩家玩偶创建器物品
 * 右键使用可以创建一个包含当前玩家皮肤的玩偶
 */
public class PlayerDollCreatorItem extends Item {
    
    private static final String TAG_PLAYER_UUID = "player_uuid";
    private static final String TAG_PLAYER_NAME = "player_name";
    
    public PlayerDollCreatorItem() {
        super(new Item.Properties()
                .stacksTo(1));
    }
    
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide) {
            // 创建包含玩家信息的玩偶物品
            ItemStack dollItem = createPlayerDollItem(player);
            
            // 如果玩家背包有空间，直接给予
            if (player.getInventory().add(dollItem)) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 1.0F);
            } else {
                // 否则掉落在地上
                player.drop(dollItem, false);
            }
            
            return InteractionResultHolder.success(stack);
        }
        
        return InteractionResultHolder.consume(stack);
    }
    
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        
        if (player == null) {
            return InteractionResult.FAIL;
        }
        
        // 检查位置是否可以放置实体
        BlockPos spawnPos = clickedPos.relative(clickedFace);
        Vec3 spawnLocation = Vec3.atBottomCenterOf(spawnPos);
        
        // 从物品获取或创建实体
        DollEntity dollEntity = com.github.ysbbbbbb.kaleidoscopedoll.item.DollEntityItem.getDollEntity(level, stack);
        
        // 重要：如果物品中有玩家信息，恢复到实体中
        UUID playerUUID = getPlayerUUID(stack);
        if (playerUUID != null) {
            // 恢复玩家信息到实体
            CompoundTag playerData = new CompoundTag();
            playerData.putUUID(TAG_PLAYER_UUID, playerUUID);
            String playerName = getPlayerName(stack);
            if (playerName != null) {
                playerData.putString(TAG_PLAYER_NAME, playerName);
            }
            dollEntity.getPersistentData().put("player_data", playerData);
            // 确保 BlockState 是空气，以便渲染玩家模型
            dollEntity.setDisplayBlockState(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        } else {
            // 如果没有玩家信息，创建新的玩家玩偶
            dollEntity = createPlayerDollEntity(level, player);
        }
        
        dollEntity.setPos(spawnLocation.x, spawnLocation.y, spawnLocation.z);
        dollEntity.setYRot(player.getYRot() - 180);
        
        // 生成实体到世界
        if (dollEntity.canSurvives()) {
            if (!level.isClientSide) {
                dollEntity.playSound(SoundEvents.WOOL_PLACE, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.ENTITY_PLACE, dollEntity.position());
                level.addFreshEntity(dollEntity);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        
        return InteractionResult.FAIL;
    }
    
    /**
     * 创建包含玩家信息的玩偶物品
     */
    public static ItemStack createPlayerDollItem(Player player) {
        // 使用 KaleidoscopeDoll 的 DollEntityItem
        ItemStack dollItem = new ItemStack(com.github.ysbbbbbb.kaleidoscopedoll.init.ModItems.DOLL_ENTITY_ITEM.get());
        
        // 创建玩偶实体
        DollEntity dollEntity = createPlayerDollEntity(player.level(), player);
        
        // 保存到物品
        DollEntityItem.saveDollEntity(dollItem, dollEntity);
        
        // 重要：将玩家信息保存到物品的 Data Component 中
        CompoundTag playerData = new CompoundTag();
        playerData.putUUID(TAG_PLAYER_UUID, player.getUUID());
        playerData.putString(TAG_PLAYER_NAME, player.getName().getString());
        dollItem.set(com.lanye.dolladdon.init.ModDataComponents.PLAYER_DATA.get(), playerData);
        
        return dollItem;
    }
    
    /**
     * 创建包含玩家信息的玩偶实体
     */
    public static DollEntity createPlayerDollEntity(Level level, Player player) {
        DollEntity dollEntity = new DollEntity(ModEntities.DOLL.get(), level);
        
        // 保存玩家信息到实体的 PersistentData 中
        // 注意：这不会自动保存到 NBT，需要手动处理
        CompoundTag playerData = new CompoundTag();
        playerData.putUUID(TAG_PLAYER_UUID, player.getUUID());
        playerData.putString(TAG_PLAYER_NAME, player.getName().getString());
        dollEntity.getPersistentData().put("player_data", playerData);
        
        // 重要：不要设置 BlockState，让渲染器根据玩家信息渲染玩家模型
        // 如果设置了 BlockState，会优先渲染方块模型而不是玩家模型
        
        return dollEntity;
    }
    
    /**
     * 从玩偶实体获取玩家UUID
     */
    @Nullable
    public static UUID getPlayerUUID(DollEntity dollEntity) {
        // 首先尝试从 PersistentData 获取
        CompoundTag playerData = dollEntity.getPersistentData().getCompound("player_data");
        if (playerData != null && playerData.hasUUID(TAG_PLAYER_UUID)) {
            return playerData.getUUID(TAG_PLAYER_UUID);
        }
        return null;
    }
    
    /**
     * 从物品获取玩家UUID
     */
    @Nullable
    public static UUID getPlayerUUID(ItemStack itemStack) {
        var playerData = itemStack.get(com.lanye.dolladdon.init.ModDataComponents.PLAYER_DATA.get());
        if (playerData != null && playerData.hasUUID(TAG_PLAYER_UUID)) {
            return playerData.getUUID(TAG_PLAYER_UUID);
        }
        return null;
    }
    
    /**
     * 从玩偶实体获取玩家名称
     */
    @Nullable
    public static String getPlayerName(DollEntity dollEntity) {
        CompoundTag playerData = dollEntity.getPersistentData().getCompound("player_data");
        if (playerData != null && playerData.contains(TAG_PLAYER_NAME)) {
            return playerData.getString(TAG_PLAYER_NAME);
        }
        return null;
    }
    
    /**
     * 从物品获取玩家名称
     */
    @Nullable
    public static String getPlayerName(ItemStack itemStack) {
        var playerData = itemStack.get(com.lanye.dolladdon.init.ModDataComponents.PLAYER_DATA.get());
        if (playerData != null && playerData.contains(TAG_PLAYER_NAME)) {
            return playerData.getString(TAG_PLAYER_NAME);
        }
        return null;
    }
    
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltips, TooltipFlag flag) {
        tooltips.add(Component.translatable("item.player_doll_addon.player_doll_creator.tooltip")
                .withStyle(ChatFormatting.GRAY));
        tooltips.add(Component.translatable("item.player_doll_addon.player_doll_creator.tooltip2")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}

