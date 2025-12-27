package com.lanye.dolladdon.entity;

import com.lanye.dolladdon.init.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 艾利克斯玩偶实体
 * 固定使用Alex模型（细手臂）和Alex默认皮肤
 */
public class AlexDollEntity extends Entity {
    private static final EntityDataAccessor<Byte> DATA_CLIENT_FLAGS = SynchedEntityData.defineId(AlexDollEntity.class, EntityDataSerializers.BYTE);
    
    public AlexDollEntity(EntityType<? extends AlexDollEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = false; // 有物理碰撞
    }
    
    public AlexDollEntity(Level level, double x, double y, double z) {
        this(ModEntities.ALEX_DOLL.get(), level);
        this.setPos(x, y, z);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CLIENT_FLAGS, (byte) 0);
    }
    
    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        // 固定模型，不需要保存额外数据
    }
    
    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        // 固定模型，不需要保存额外数据
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

