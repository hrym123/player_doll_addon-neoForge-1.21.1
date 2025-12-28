package com.lanye.dolladdon.base.entity;

import com.lanye.dolladdon.api.action.DollAction;
import com.lanye.dolladdon.api.pose.DollPose;
import com.lanye.dolladdon.api.pose.SimpleDollPose;
import com.lanye.dolladdon.util.PoseActionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 玩偶实体基类
 * 提供所有玩偶实体的共同功能
 */
public abstract class BaseDollEntity extends Entity {
    private static final EntityDataAccessor<Byte> DATA_CLIENT_FLAGS = SynchedEntityData.defineId(BaseDollEntity.class, EntityDataSerializers.BYTE);
    // 同步姿态索引到客户端（使用Byte，支持0-255个姿态，足够使用）
    private static final EntityDataAccessor<Byte> DATA_POSE_INDEX = SynchedEntityData.defineId(BaseDollEntity.class, EntityDataSerializers.BYTE);
    
    // 姿态和动作相关字段
    private DollPose currentPose;
    private DollAction currentAction;
    private int actionTick = 0;
    
    // 当前姿态索引（用于循环切换）
    private int currentPoseIndex = -1;
    
    protected BaseDollEntity(EntityType<? extends BaseDollEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = false; // 有物理碰撞
        // 默认使用站立姿态
        this.currentPose = SimpleDollPose.createDefaultStandingPose();
        // 初始化时设置为255（默认姿态）
        if (!level.isClientSide) {
            this.entityData.set(DATA_POSE_INDEX, (byte) 255);
        }
    }
    
    protected BaseDollEntity(EntityType<? extends BaseDollEntity> entityType, Level level, double x, double y, double z) {
        this(entityType, level);
        this.setPos(x, y, z);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CLIENT_FLAGS, (byte) 0);
        builder.define(DATA_POSE_INDEX, (byte) 255); // 255 表示未设置（默认姿态）
    }
    
    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        restoreFromNBT(tag);
    }
    
    /**
     * 从NBT恢复实体数据（公共方法，供物品使用）
     * @param tag NBT标签
     */
    public void restoreFromNBT(net.minecraft.nbt.CompoundTag tag) {
        // 优先恢复动作（如果有）
        if (tag.contains("ActionName", net.minecraft.nbt.Tag.TAG_STRING)) {
            String actionName = tag.getString("ActionName");
            DollAction action = PoseActionManager.getAction(actionName);
            if (action != null) {
                setAction(action);
                // 动作已设置，不需要再设置姿态索引
                return;
            }
        }
        
        // 如果没有动作，恢复姿态索引
        if (tag.contains("PoseIndex")) {
            this.currentPoseIndex = tag.getInt("PoseIndex");
            // 加载时恢复姿态
            loadPoseByIndex();
            // 同步到客户端
            if (currentPoseIndex >= 0 && currentPoseIndex < 255) {
                this.entityData.set(DATA_POSE_INDEX, (byte) (currentPoseIndex & 0xFF));
            } else {
                this.entityData.set(DATA_POSE_INDEX, (byte) 255);
            }
        }
    }
    
    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        // 保存当前姿态索引
        tag.putInt("PoseIndex", this.currentPoseIndex);
        
        // 如果当前有动作，也保存动作名称
        if (currentAction != null) {
            tag.putString("ActionName", currentAction.getName());
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // 在客户端，根据同步的索引更新姿态
        if (this.level().isClientSide) {
            byte syncedIndex = this.entityData.get(DATA_POSE_INDEX);
            if (syncedIndex != 255) {
                int index = syncedIndex & 0xFF; // 转换为无符号整数
                if (index != currentPoseIndex) {
                    currentPoseIndex = index;
                    loadPoseByIndex();
                }
            } else if (currentPoseIndex != -1) {
                // 如果同步值为255，使用默认姿态
                currentPoseIndex = -1;
                currentPose = SimpleDollPose.createDefaultStandingPose();
            }
        }
        
        // 更新动作
        if (currentAction != null) {
            actionTick++;
            
            // 获取当前tick对应的姿态
            DollPose actionPose = currentAction.getPoseAt(actionTick);
            if (actionPose != null) {
                currentPose = actionPose;
            }
            
            // 如果动作不循环且播放完成，停止动作
            if (!currentAction.isLooping() && actionTick >= currentAction.getDuration()) {
                currentAction = null;
                actionTick = 0;
                // 恢复默认姿态
                currentPose = SimpleDollPose.createDefaultStandingPose();
            } else if (currentAction.isLooping()) {
                // 循环动作，重置tick
                if (actionTick >= currentAction.getDuration()) {
                    actionTick = 0;
                }
            }
        }
        
        // 应用重力
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }
        
        // 移动
        this.move(MoverType.SELF, this.getDeltaMovement());
        
        // 应用摩擦力
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.98, 0.98, 0.98));
        
        // 如果在地面上，停止垂直运动
        if (this.onGround()) {
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x * 0.7, 0.0, movement.z * 0.7);
        }
    }
    
    @Override
    public boolean isPickable() {
        return true;
    }
    
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
    
    @Override
    public boolean isPushable() {
        return true;
    }
    
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }
    
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            // 如果玩家潜行，则破坏实体并掉落物品
            if (player.isShiftKeyDown()) {
                return handleBreakAndDrop(player);
            } else {
                // 循环切换到下一个姿态
                cycleToNextPose(player);
                // 播放交互音效
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        SoundEvents.ARMOR_STAND_HIT, SoundSource.NEUTRAL, 0.5F, 1.0F);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }
    
    /**
     * 获取对应的物品堆（由子类实现）
     * @return 对应的物品堆
     */
    protected abstract ItemStack getDollItemStack();
    
    /**
     * 处理破坏并掉落物品
     */
    private InteractionResult handleBreakAndDrop(Player player) {
        // 创建物品堆
        ItemStack itemStack = getDollItemStack();
        
        // 保存NBT标签到物品
        net.minecraft.nbt.CompoundTag entityTag = new net.minecraft.nbt.CompoundTag();
        this.addAdditionalSaveData(entityTag);
        
        // 使用数据组件API保存NBT
        net.minecraft.nbt.Tag tag = itemStack.save(this.level().registryAccess());
        net.minecraft.nbt.CompoundTag itemTag;
        if (tag instanceof net.minecraft.nbt.CompoundTag) {
            itemTag = (net.minecraft.nbt.CompoundTag) tag;
        } else {
            itemTag = new net.minecraft.nbt.CompoundTag();
        }
        itemTag.put("EntityData", entityTag);
        // 从NBT重新加载ItemStack
        try {
            java.util.Optional<ItemStack> parsed = ItemStack.parse(this.level().registryAccess(), itemTag);
            if (parsed.isPresent()) {
                itemStack = parsed.get();
            }
        } catch (Exception e) {
            // 如果解析失败，使用原始物品堆
            com.lanye.dolladdon.PlayerDollAddon.LOGGER.warn("无法从NBT加载物品堆，使用原始物品堆", e);
        }
        
        // 掉落物品
        net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                this.level(),
                this.getX(),
                this.getY(),
                this.getZ(),
                itemStack
        );
        itemEntity.setDefaultPickUpDelay();
        this.level().addFreshEntity(itemEntity);
        
        // 播放破坏音效
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ARMOR_STAND_BREAK, SoundSource.NEUTRAL, 0.5F, 1.0F);
        
        // 移除实体
        this.remove(Entity.RemovalReason.DISCARDED);
        
        return InteractionResult.SUCCESS;
    }
    
    /**
     * 获取所有可用的姿态名称列表
     */
    private List<String> getAvailablePoseNames() {
        // 每次都重新获取，因为资源可能在运行时加载
        List<String> poseNames = new ArrayList<>();
        Map<String, DollPose> allPoses = PoseActionManager.getAllPoses();
        poseNames.addAll(allPoses.keySet());
        
        // 如果没有加载任何姿态，至少添加默认姿态
        if (poseNames.isEmpty()) {
            // 注册默认姿态
            PoseActionManager.registerPose("default", SimpleDollPose.createDefaultStandingPose());
            poseNames.add("default");
        }
        
        // 确保列表有序
        poseNames.sort(String::compareTo);
        
        return poseNames;
    }
    
    /**
     * 循环切换到下一个姿态
     */
    private void cycleToNextPose(Player player) {
        List<String> poseNames = getAvailablePoseNames();
        
        if (poseNames.isEmpty()) {
            if (player != null) {
                // 显示在动作栏（物品栏上方）
                player.displayClientMessage(Component.literal("没有可用的姿态"), true);
            }
            return;
        }
        
        // 停止当前动作
        stopAction();
        
        // 如果当前索引无效，重置为0
        if (currentPoseIndex < 0 || currentPoseIndex >= poseNames.size()) {
            currentPoseIndex = 0;
        } else {
            // 切换到下一个姿态
            currentPoseIndex++;
            if (currentPoseIndex >= poseNames.size()) {
                currentPoseIndex = 0; // 循环回到第一个
            }
        }
        
        // 同步姿态索引到客户端
        byte indexToSync = (byte) (currentPoseIndex & 0xFF);
        this.entityData.set(DATA_POSE_INDEX, indexToSync);
        
        // 加载新姿态
        String poseName = poseNames.get(currentPoseIndex);
        DollPose pose = PoseActionManager.getPose(poseName);
        
        if (pose != null) {
            setPose(pose);
            // 发送消息给玩家（优先使用中文名称，显示在动作栏）
            if (player != null) {
                String displayName = pose.getDisplayName();
                player.displayClientMessage(Component.literal("切换到姿态: " + displayName + " (" + (currentPoseIndex + 1) + "/" + poseNames.size() + ")"), true);
            }
        } else {
            // 如果找不到姿态，使用默认姿态
            setPose(SimpleDollPose.createDefaultStandingPose());
            // 设置为255表示使用默认姿态
            this.entityData.set(DATA_POSE_INDEX, (byte) 255);
            if (player != null) {
                player.displayClientMessage(Component.literal("切换到默认姿态"), true);
            }
        }
    }
    
    /**
     * 根据索引加载姿态（用于从NBT恢复和客户端同步）
     */
    private void loadPoseByIndex() {
        List<String> poseNames = getAvailablePoseNames();
        
        if (currentPoseIndex >= 0 && currentPoseIndex < poseNames.size()) {
            String poseName = poseNames.get(currentPoseIndex);
            DollPose pose = PoseActionManager.getPose(poseName);
            if (pose != null) {
                this.currentPose = pose;
            } else {
                // 如果找不到姿态，使用默认姿态
                this.currentPose = SimpleDollPose.createDefaultStandingPose();
            }
        } else {
            // 索引无效，使用默认姿态
            this.currentPose = SimpleDollPose.createDefaultStandingPose();
        }
    }
    
    /**
     * 获取当前姿态
     * @return 当前姿态
     */
    public DollPose getCurrentPose() {
        return currentPose;
    }
    
    /**
     * 设置姿态
     * @param pose 要设置的姿态
     */
    public void setPose(DollPose pose) {
        if (pose != null) {
            this.currentPose = pose;
            // 设置姿态时停止当前动作
            this.currentAction = null;
            this.actionTick = 0;
        }
    }
    
    /**
     * 获取当前动作
     * @return 当前动作，如果没有则返回null
     */
    public DollAction getCurrentAction() {
        return currentAction;
    }
    
    /**
     * 设置动作
     * @param action 要播放的动作
     */
    public void setAction(DollAction action) {
        this.currentAction = action;
        this.actionTick = 0;
    }
    
    /**
     * 停止当前动作
     */
    public void stopAction() {
        this.currentAction = null;
        this.actionTick = 0;
        // 恢复默认姿态
        this.currentPose = SimpleDollPose.createDefaultStandingPose();
    }
}

