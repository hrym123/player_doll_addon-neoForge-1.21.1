package com.lanye.dolladdon.base.item;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
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
        
        // 如果物品有NBT标签，恢复实体的状态（包括姿态和皮肤）
        // 从custom_data组件读取NBT
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var dataTag = customData.copyTag();
            if (dataTag != null && dataTag.contains("EntityData")) {
                net.minecraft.nbt.CompoundTag entityTag = dataTag.getCompound("EntityData");
                // restoreFromNBT会处理所有NBT数据，包括皮肤路径
                dollEntity.restoreFromNBT(entityTag);
            }
        }
        
        // 如果物品有 CUSTOM_NAME（玩家通过铁砧重命名），将其同步到 NBT 的 DisplayName
        // 这样实体破坏后，重命名的名称能够保留
        var customName = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (customName != null) {
            String renamedName = customName.getString();
            if (renamedName != null && !renamedName.isEmpty()) {
                // 保存到 persistentData，这样 addAdditionalSaveData 会将其保存到 NBT
                dollEntity.getPersistentData().putString("DisplayName", renamedName);
            }
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
    
    /**
     * 添加物品Tooltip，显示玩家信息（如果NBT中有）
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        // 显示玩家名称（如果NBT中有）
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (customData != null) {
            var dataTag = customData.copyTag();
            if (dataTag != null && dataTag.contains("EntityData")) {
                net.minecraft.nbt.CompoundTag entityTag = dataTag.getCompound("EntityData");
                
                if (entityTag.contains("PlayerName", net.minecraft.nbt.Tag.TAG_STRING)) {
                    String playerName = entityTag.getString("PlayerName");
                    tooltip.add(Component.literal("玩家: " + playerName).withStyle(ChatFormatting.GRAY));
                }
                
                // 显示模型类型
                if (entityTag.contains("IsAlexModel", net.minecraft.nbt.Tag.TAG_BYTE)) {
                    boolean isAlex = entityTag.getBoolean("IsAlexModel");
                    String modelType = isAlex ? "细手臂 (Alex)" : "粗手臂 (Steve)";
                    tooltip.add(Component.literal("模型: " + modelType).withStyle(ChatFormatting.GRAY));
                }
            }
        }
    }
}

