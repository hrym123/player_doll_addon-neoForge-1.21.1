package com.lanye.dolladdon.base.item;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

/**
 * 玩偶物品基类
 * 提供所有玩偶物品的共同功能
 */
public abstract class BaseDollItem extends Item {
    
    public BaseDollItem() {
        super(new Item.Settings());
    }
    
    /**
     * 创建玩偶实体
     * @param level 世界
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     * @return 玩偶实体
     */
    protected abstract BaseDollEntity createDollEntity(World world, double x, double y, double z);
    
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();
        BlockPos clickedPos = context.getBlockPos();
        Direction clickedFace = context.getSide();
        
        if (player == null) {
            return ActionResult.FAIL;
        }
        
        // 计算生成位置
        BlockPos spawnPos = clickedPos.offset(clickedFace);
        Vec3d spawnLocation = Vec3d.ofBottomCenter(spawnPos);
        
        // 创建玩偶实体（由子类实现）
        BaseDollEntity dollEntity = createDollEntity(world, spawnLocation.x, spawnLocation.y, spawnLocation.z);
        
        dollEntity.setYaw(player.getYaw() - 180); // 设置朝向
        
        // 如果物品有NBT标签，恢复实体的状态（包括姿态）
        // 从NBT标签读取
        net.minecraft.nbt.NbtCompound itemTag = stack.getNbt();
        if (itemTag != null && itemTag.contains("EntityData")) {
            net.minecraft.nbt.NbtCompound entityTag = itemTag.getCompound("EntityData");
            dollEntity.restoreFromNBT(entityTag);
        }
        
        // 检查是否可以生成
        if (!world.isSpaceEmpty(dollEntity, dollEntity.getBoundingBox())) {
            return ActionResult.FAIL;
        }
        
        // 生成实体
        if (!world.isClient) {
            // 记录生成前的信息
            com.lanye.dolladdon.util.ModuleLogger.info("entity", 
                "[实体生成] 准备生成实体: 位置=({}, {}, {}), 碰撞箱={}", 
                String.format("%.2f", dollEntity.getX()), 
                String.format("%.2f", dollEntity.getY()), 
                String.format("%.2f", dollEntity.getZ()),
                dollEntity.getBoundingBox());
            
            world.spawnEntity(dollEntity);
            
            // 记录生成后的信息
            com.lanye.dolladdon.util.ModuleLogger.info("entity", 
                "[实体生成] 实体已生成: ID={}, 位置=({}, {}, {}), 碰撞箱={}, 玩家位置=({}, {}, {}), 距离={}", 
                dollEntity.getId(),
                String.format("%.2f", dollEntity.getX()), 
                String.format("%.2f", dollEntity.getY()), 
                String.format("%.2f", dollEntity.getZ()),
                dollEntity.getBoundingBox(),
                String.format("%.2f", player.getX()),
                String.format("%.2f", player.getY()),
                String.format("%.2f", player.getZ()),
                String.format("%.2f", Math.sqrt(
                    Math.pow(player.getX() - dollEntity.getX(), 2) + 
                    Math.pow(player.getY() - dollEntity.getY(), 2) + 
                    Math.pow(player.getZ() - dollEntity.getZ(), 2)
                )));
            
            world.playSound(null, dollEntity.getX(), dollEntity.getY(), dollEntity.getZ(),
                    SoundEvents.ENTITY_ARMOR_STAND_PLACE, SoundCategory.PLAYERS, 0.75F, 0.8F);
            world.emitGameEvent(player, GameEvent.ENTITY_PLACE, dollEntity.getPos());
        }
        
        // 消耗物品（创造模式不消耗）
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        
        return world.isClient ? ActionResult.SUCCESS : ActionResult.CONSUME;
    }
}

