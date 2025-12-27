package com.lanye.dolladdon.entity;

import com.lanye.dolladdon.init.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 玩家玩偶实体
 * 类似于盔甲架，但显示玩家模型
 */
public class PlayerDollEntity extends Entity {
    private static final EntityDataAccessor<Byte> DATA_CLIENT_FLAGS = SynchedEntityData.defineId(PlayerDollEntity.class, EntityDataSerializers.BYTE);
    
    private static final String TAG_PLAYER_UUID = "player_uuid";
    private static final String TAG_PLAYER_NAME = "player_name";
    
    @Nullable
    private UUID playerUUID;
    @Nullable
    private String playerName;
    
    public PlayerDollEntity(EntityType<? extends PlayerDollEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = false; // 有物理碰撞
    }
    
    public PlayerDollEntity(Level level, double x, double y, double z) {
        this(ModEntities.PLAYER_DOLL.get(), level);
        this.setPos(x, y, z);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CLIENT_FLAGS, (byte) 0);
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID(TAG_PLAYER_UUID)) {
            this.playerUUID = tag.getUUID(TAG_PLAYER_UUID);
        }
        if (tag.contains(TAG_PLAYER_NAME)) {
            this.playerName = tag.getString(TAG_PLAYER_NAME);
        }
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.playerUUID != null) {
            tag.putUUID(TAG_PLAYER_UUID, this.playerUUID);
        }
        if (this.playerName != null) {
            tag.putString(TAG_PLAYER_NAME, this.playerName);
        }
    }
    
    /**
     * 设置玩家信息
     */
    public void setPlayer(Player player) {
        this.playerUUID = player.getUUID();
        this.playerName = player.getName().getString();
    }
    
    /**
     * 设置玩家信息
     */
    public void setPlayer(UUID uuid, String name) {
        this.playerUUID = uuid;
        this.playerName = name;
    }
    
    /**
     * 获取玩家 UUID
     */
    @Nullable
    public UUID getPlayerUUID() {
        return this.playerUUID;
    }
    
    /**
     * 获取玩家名称
     */
    @Nullable
    public String getPlayerName() {
        return this.playerName;
    }
    
    @Override
    public void tick() {
        super.tick();
        
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
}

