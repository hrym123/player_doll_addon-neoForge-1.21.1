package com.lanye.dolladdon.item;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.client.render.AlexDollItemRenderer;
import com.lanye.dolladdon.entity.AlexDollEntity;
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
 * 艾利克斯玩偶物品
 * 固定使用Alex模型（细手臂）和Alex默认皮肤
 */
public class AlexDollItem extends Item {
    
    public AlexDollItem() {
        super(new Item.Properties());
    }
    
    @Override
    @SuppressWarnings("removal")
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        PlayerDollAddon.LOGGER.info("[AlexDollItem] initializeClient 被调用");
        consumer.accept(new IClientItemExtensions() {
            private AlexDollItemRenderer renderer = null;
            
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                PlayerDollAddon.LOGGER.info("[AlexDollItem] getCustomRenderer 被调用");
                Minecraft minecraft = Minecraft.getInstance();
                if (renderer == null) {
                    PlayerDollAddon.LOGGER.info("[AlexDollItem] 创建新的 AlexDollItemRenderer");
                    renderer = new AlexDollItemRenderer(
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
        
        // 创建艾利克斯玩偶实体（固定模型，不需要设置UUID）
        AlexDollEntity dollEntity = new AlexDollEntity(level, spawnLocation.x, spawnLocation.y, spawnLocation.z);
        
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

