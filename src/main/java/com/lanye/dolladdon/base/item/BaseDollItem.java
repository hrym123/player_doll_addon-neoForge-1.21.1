package com.lanye.dolladdon.base.item;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
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

/**
 * 玩偶物品基类
 * 提供所有玩偶物品的共同功能
 */
public abstract class BaseDollItem extends Item {
    
    public BaseDollItem() {
        super(new Item.Properties());
    }
    
    /**
     * 创建玩偶实体
     * @param level 世界
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @return 玩偶实体
     */
    protected abstract BaseDollEntity createDollEntity(Level level, double x, double y, double z);
    
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
        
        // 创建玩偶实体（由子类实现）
        BaseDollEntity dollEntity = createDollEntity(level, spawnLocation.x, spawnLocation.y, spawnLocation.z);
        
        dollEntity.setYRot(player.getYRot() - 180); // 设置朝向
        
        // 如果物品有NBT标签，恢复实体的状态（包括姿态）
        // 从custom_data组件读取NBT
        com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[放置物品] 开始从物品NBT恢复实体状态，物品: {}", stack.getItem());
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var dataTag = customData.copyTag();
            com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[放置物品] 读取到custom_data内容: {}", dataTag);
            if (dataTag != null && dataTag.contains("EntityData")) {
                net.minecraft.nbt.CompoundTag entityTag = dataTag.getCompound("EntityData");
                com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[放置物品] 读取到EntityData内容: {}", entityTag);
                dollEntity.restoreFromNBT(entityTag);
                com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[放置物品] ✅ 实体状态已恢复");
            } else {
                com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[放置物品] ⚠️ 未找到EntityData标签");
            }
        } else {
            com.lanye.dolladdon.PlayerDollAddon.LOGGER.debug("[放置物品] ⚠️ custom_data组件为空，不恢复实体状态");
        }
        
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

