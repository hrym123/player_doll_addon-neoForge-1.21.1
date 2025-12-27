package com.lanye.dolladdon.item;

import com.lanye.dolladdon.client.render.PlayerDollItemRenderer;
import com.lanye.dolladdon.entity.PlayerDollEntity;
import com.lanye.dolladdon.init.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * 玩家玩偶物品
 * 用于放置玩家玩偶实体
 */
public class PlayerDollItem extends Item {
    
    public PlayerDollItem() {
        super(new Item.Properties());
    }
    
    @Override
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private PlayerDollItemRenderer renderer = null;
            
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                Minecraft minecraft = Minecraft.getInstance();
                if (renderer == null) {
                    renderer = new PlayerDollItemRenderer(
                            minecraft.getBlockEntityRenderDispatcher(),
                            minecraft.getEntityModels()
                    );
                }
                return renderer;
            }
        });
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
        
        // 计算生成位置
        BlockPos spawnPos = clickedPos.relative(clickedFace);
        Vec3 spawnLocation = Vec3.atBottomCenterOf(spawnPos);
        
        // 创建玩家玩偶实体
        PlayerDollEntity dollEntity = new PlayerDollEntity(level, spawnLocation.x, spawnLocation.y, spawnLocation.z);
        
        // 从物品 Data Component 获取玩家信息，如果没有则使用当前玩家
        var playerData = stack.get(com.lanye.dolladdon.init.ModDataComponents.PLAYER_DATA.get());
        if (playerData != null && playerData.contains("player_uuid")) {
            var uuid = playerData.getUUID("player_uuid");
            var name = playerData.contains("player_name") ? playerData.getString("player_name") : "Unknown";
            dollEntity.setPlayer(uuid, name);
        } else {
            dollEntity.setPlayer(player); // 使用当前玩家
        }
        
        dollEntity.setYRot(player.getYRot() - 180); // 设置朝向
        
        // 检查是否可以生成
        if (!level.noCollision(dollEntity, dollEntity.getBoundingBox())) {
            return InteractionResult.FAIL;
        }
        
        // 生成实体
        if (!level.isClientSide) {
            level.addFreshEntity(dollEntity);
            level.playSound(null, dollEntity.getX(), dollEntity.getY(), dollEntity.getZ(),
                    SoundEvents.ARMOR_STAND_PLACE, SoundSource.PLAYERS, 0.75F, 0.8F);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, dollEntity.position());
        }
        
        // 消耗物品（创造模式不消耗）
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

