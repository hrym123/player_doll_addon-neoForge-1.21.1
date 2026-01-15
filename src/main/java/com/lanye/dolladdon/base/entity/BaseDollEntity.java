package com.lanye.dolladdon.base.entity;

import com.lanye.dolladdon.api.action.DollAction;
import com.lanye.dolladdon.api.pose.DollPose;
import com.lanye.dolladdon.api.pose.SimpleDollPose;
import com.lanye.dolladdon.util.pose.PoseActionManager;
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
import net.minecraft.world.phys.AABB;
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
    // 同步动作名称到客户端（空字符串表示没有动作）
    private static final EntityDataAccessor<String> DATA_ACTION_NAME = SynchedEntityData.defineId(BaseDollEntity.class, EntityDataSerializers.STRING);
    // 同步动作tick到客户端（用于计算当前姿态）
    private static final EntityDataAccessor<Integer> DATA_ACTION_TICK = SynchedEntityData.defineId(BaseDollEntity.class, EntityDataSerializers.INT);
    
    // 姿态和动作相关字段
    private DollPose currentPose; // 当前显示的姿态（动作优先级更高）
    private DollPose savedPose; // 保存的姿态（动作播放前的姿态，用于动作完成后恢复）
    private DollAction currentAction;
    private int actionTick = 0;
    
    // 当前姿态索引（用于循环切换）
    private int currentPoseIndex = -1;
    
    // 皮肤相关字段（用于NBT动态切换皮肤）
    private String skinPath; // 皮肤纹理路径（ResourceLocation字符串）
    private boolean isAlexModel; // 是否为Alex模型（细手臂）
    private String playerName; // 玩家名称（用于显示）
    
    protected BaseDollEntity(EntityType<? extends BaseDollEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = false; // 有物理碰撞
        // 默认使用standing姿态
        DollPose standingPose = PoseActionManager.getPose("standing");
        DollPose defaultPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
        this.currentPose = defaultPose;
        this.savedPose = defaultPose; // 初始化时保存的姿态也是默认姿态
        // 初始化时设置为255（默认姿态）
        if (!level.isClientSide) {
            this.entityData.set(DATA_POSE_INDEX, (byte) 255);
            this.entityData.set(DATA_ACTION_NAME, "");
            this.entityData.set(DATA_ACTION_TICK, 0);
        }
        // 初始化碰撞箱
        updateBoundingBox();
    }
    
    protected BaseDollEntity(EntityType<? extends BaseDollEntity> entityType, Level level, double x, double y, double z) {
        this(entityType, level);
        this.setPos(x, y, z);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CLIENT_FLAGS, (byte) 0);
        builder.define(DATA_POSE_INDEX, (byte) 255); // 255 表示未设置（默认姿态）
        builder.define(DATA_ACTION_NAME, ""); // 空字符串表示没有动作
        builder.define(DATA_ACTION_TICK, 0); // 动作tick从0开始
    }
    
    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        restoreFromNBT(tag);
        // restoreFromNBT 会调用 setSkinFromNBT，从而设置 persistentData
        // 确保从世界文件加载时，persistentData 也被正确设置
    }
    
    /**
     * 从NBT恢复实体数据（公共方法，供物品使用）
     * @param tag NBT标签
     */
    public void restoreFromNBT(net.minecraft.nbt.CompoundTag tag) {
        // 优先恢复皮肤信息（如果有）
        if (tag.contains("SkinPath", net.minecraft.nbt.Tag.TAG_STRING)) {
            setSkinFromNBT(tag);
        }
        
        // 优先恢复动作（如果有）
        if (tag.contains("ActionName", net.minecraft.nbt.Tag.TAG_STRING)) {
            String actionName = tag.getString("ActionName");
            DollAction action = PoseActionManager.getAction(actionName);
            if (action != null) {
                setAction(action);
                // 动作已设置，不需要再设置姿态
                return;
            }
        }
        
        // 优先使用姿态名称恢复（如果保存了）
        if (tag.contains("PoseName", net.minecraft.nbt.Tag.TAG_STRING)) {
            String poseName = tag.getString("PoseName");
            DollPose pose = PoseActionManager.getPose(poseName);
            if (pose != null) {
                setPose(pose);
                // 更新currentPoseIndex（如果可能）
                List<String> poseNames = getAvailablePoseNames();
                int index = poseNames.indexOf(poseName);
                if (index >= 0) {
                    // 如果索引是0（standing），设置为-1表示默认状态
                    this.currentPoseIndex = (index == 0) ? -1 : index;
                    // 同步到客户端
                    if (currentPoseIndex >= 0 && currentPoseIndex < 255) {
                        this.entityData.set(DATA_POSE_INDEX, (byte) (currentPoseIndex & 0xFF));
                    } else {
                        this.entityData.set(DATA_POSE_INDEX, (byte) 255);
                    }
                }
                return;
            }
        }
        
        // 如果没有姿态名称，尝试使用姿态索引（向后兼容）
        if (tag.contains("PoseIndex")) {
            int savedIndex = tag.getInt("PoseIndex");
            // 如果索引是0（standing），设置为-1表示默认状态
            this.currentPoseIndex = (savedIndex == 0) ? -1 : savedIndex;
            // 加载时恢复姿态
            loadPoseByIndex();
            // 同步到客户端
            if (currentPoseIndex >= 0 && currentPoseIndex < 255) {
                this.entityData.set(DATA_POSE_INDEX, (byte) (currentPoseIndex & 0xFF));
            } else {
                this.entityData.set(DATA_POSE_INDEX, (byte) 255);
            }
        } else {
            // NBT中没有姿态信息，使用默认standing姿态
            this.currentPoseIndex = -1;
            this.entityData.set(DATA_POSE_INDEX, (byte) 255);
            DollPose standingPose = PoseActionManager.getPose("standing");
            DollPose defaultPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
            this.currentPose = defaultPose;
            this.savedPose = defaultPose; // 同时初始化保存的姿态
        }
        
        // 从NBT加载皮肤信息（在readAdditionalSaveData中处理，因为这是从世界文件加载）
        if (tag.contains("SkinPath", net.minecraft.nbt.Tag.TAG_STRING)) {
            setSkinFromNBT(tag);
        }
    }
    
    /**
     * 从物品NBT设置皮肤信息
     * 在创建实体时从物品NBT读取并设置皮肤路径
     * 
     * @param nbt NBT标签（包含SkinPath、IsAlexModel、PlayerName等）
     */
    public void setSkinFromNBT(net.minecraft.nbt.CompoundTag nbt) {
        if (nbt.contains("SkinPath", net.minecraft.nbt.Tag.TAG_STRING)) {
            this.skinPath = nbt.getString("SkinPath");
            // 安全读取 IsAlexModel，如果不存在则默认为 false
            if (nbt.contains("IsAlexModel", net.minecraft.nbt.Tag.TAG_BYTE)) {
                this.isAlexModel = nbt.getBoolean("IsAlexModel");
            } else {
                this.isAlexModel = false; // 默认粗手臂
            }
            if (nbt.contains("PlayerName", net.minecraft.nbt.Tag.TAG_STRING)) {
                this.playerName = nbt.getString("PlayerName");
            }
            
            // 同步到persistentData（用于客户端渲染器访问）
            // persistentData 是客户端可访问的，通过NBT机制自动同步
            this.getPersistentData().putString("SkinPath", this.skinPath);
            this.getPersistentData().putBoolean("IsAlexModel", this.isAlexModel);
            if (this.playerName != null) {
                this.getPersistentData().putString("PlayerName", this.playerName);
            }
        }
    }
    
    /**
     * 获取皮肤路径（用于渲染器）
     * @return 皮肤路径，如果未设置返回null
     */
    public String getSkinPath() {
        return this.skinPath;
    }
    
    /**
     * 是否为Alex模型（用于渲染器）
     * @return 是否为Alex模型
     */
    public boolean isAlexModel() {
        return this.isAlexModel;
    }
    
    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        // 保存皮肤信息（如果有）
        if (this.skinPath != null) {
            tag.putString("SkinPath", this.skinPath);
            tag.putBoolean("IsAlexModel", this.isAlexModel);
            if (this.playerName != null) {
                tag.putString("PlayerName", this.playerName);
            }
        }
        
        // 如果当前有动作，保存动作名称
        if (currentAction != null) {
            tag.putString("ActionName", currentAction.getName());
        }
        
        // 保存当前姿态名称（而不是索引）
        // standing姿态不保存到NBT，当NBT为空时默认使用standing姿态
        if (currentPose != null) {
            String poseName = currentPose.getName();
            // 只有当姿态不是standing时才保存
            if (poseName != null && !poseName.isEmpty() && !poseName.equals("standing")) {
                tag.putString("PoseName", poseName);
            }
        }
        
        // 为了向后兼容，也保存姿态索引
        // standing姿态（索引-1或0）不保存，当NBT为空时默认使用standing姿态
        if (this.currentPoseIndex > 0) {
            tag.putInt("PoseIndex", this.currentPoseIndex);
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // 注意：物理模拟（重力、移动、摩擦力）由原版Entity.tick()处理
        
        // 服务器端：更新动作tick（如果有动作）
        // 这是唯一必须使用tick的功能：动作需要逐帧播放，每tick递增actionTick
        if (!this.level().isClientSide && this.currentAction != null) {
            updateServerAction();
        }
        
        // 客户端：不需要在tick中更新姿态
        // 姿态更新通过onSyncedDataUpdated()在数据同步时触发，更高效
    }
    
    /**
     * 当实体数据同步到客户端时调用
     * 用于在数据变化时更新姿态
     * 这是客户端更新姿态的主要方式，不需要每tick检查
     */
    @Override
    public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        
        // 客户端：当姿态或动作数据同步时，立即更新姿态
        // 服务器端每tick都会同步DATA_ACTION_TICK，所以动作播放时这里会被每tick调用
        // 姿态变化时DATA_POSE_INDEX会变化，也会触发这里
        if (this.level().isClientSide) {
            if (key == DATA_POSE_INDEX || key == DATA_ACTION_NAME || key == DATA_ACTION_TICK) {
                updateClientPose();
            }
        }
    }
    
    /**
     * 重写getDimensions方法，根据当前姿态的scale动态返回尺寸
     * 这样Minecraft会自动使用这个尺寸来计算碰撞箱，无需手动设置
     */
    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        // 基础尺寸（与DollEntityFactory中的sized(0.6f, 1f)保持一致）
        float baseWidth = 0.6f;
        float baseHeight = 1.0f;
        
        // 获取当前姿态的scale
        DollPose currentPose = getCurrentPose();
        if (currentPose != null) {
            float[] scale = currentPose.getScale();
            // 应用scale到尺寸
            double widthScale = Math.max(Math.abs(scale[0]), Math.abs(scale[2]));
            double heightScale = Math.abs(scale[1]);
            
            baseWidth *= widthScale;
            baseHeight *= heightScale;
        }
        
        // 返回动态计算的尺寸，Minecraft会自动使用这个尺寸来计算碰撞箱
        return net.minecraft.world.entity.EntityDimensions.scalable(baseWidth, baseHeight);
    }
    
    /**
     * 重写move方法，在移动后自动恢复自定义碰撞箱
     * move()方法会根据EntityType的尺寸重置碰撞箱，所以需要在移动后立即恢复
     * 注意：如果getDimensions()正常工作，这个方法可能不再需要
     */
    @Override
    public void move(MoverType moverType, Vec3 movement) {
        super.move(moverType, movement);
        // 父类的move()方法会根据EntityType的尺寸重置碰撞箱，所以需要立即恢复自定义碰撞箱
        // 如果getDimensions()正常工作，这行代码可能不再需要
        updateBoundingBox();
    }
    
    /**
     * 根据当前姿态的scale动态更新碰撞箱大小
     * 基础尺寸：宽0.6，高1.0（与DollEntityFactory中的sized保持一致）
     * 注意：如果getDimensions()正常工作，这个方法可能不再需要
     */
    private void updateBoundingBox() {
        // 基础碰撞箱尺寸（与DollEntityFactory中的sized(0.6f, 1f)保持一致）
        double baseWidth = 0.6;
        double baseHeight = 1.0;
        
        // 获取当前姿态的scale
        DollPose pose = getCurrentPose();
        if (pose != null) {
            float[] scale = pose.getScale();
            // 应用scale到碰撞箱尺寸
            // 使用scale的最大值来确保碰撞箱足够大
            // 这里使用scale的Y值作为高度缩放，X和Z的最大值作为宽度缩放
            double widthScale = Math.max(Math.abs(scale[0]), Math.abs(scale[2]));
            double heightScale = Math.abs(scale[1]);
            
            baseWidth *= widthScale;
            baseHeight *= heightScale;
        }
        
        // 计算碰撞箱的半宽
        double halfWidth = baseWidth / 2.0;
        
        // 获取实体位置
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        
        // 创建并设置碰撞箱
        // 碰撞箱以实体底部中心为基准，向上延伸
        // 注意：position偏移主要用于渲染，不影响碰撞箱位置
        AABB newBoundingBox = new AABB(
            x - halfWidth, y, z - halfWidth,
            x + halfWidth, y + baseHeight, z + halfWidth
        );
        
        this.setBoundingBox(newBoundingBox);
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
        boolean isSneaking = player.isShiftKeyDown();
        
        if (!this.level().isClientSide) {
            // 如果玩家潜行，则破坏实体并掉落物品
            if (isSneaking) {
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
        
        // 只有当entityTag不为空时才保存custom_data，否则清除EntityData标签（允许物品叠加）
        var existingData = itemStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (!entityTag.isEmpty()) {
            // 使用数据组件API保存NBT到custom_data组件
            net.minecraft.nbt.CompoundTag customDataTag = new net.minecraft.nbt.CompoundTag();
            customDataTag.put("EntityData", entityTag);
            
            // 设置custom_data组件（合并现有的custom_data，如果有的话）
            net.minecraft.nbt.CompoundTag finalData;
            if (existingData != null) {
                var existingTag = existingData.copyTag();
                if (existingTag != null) {
                    finalData = existingTag;
                    finalData.put("EntityData", entityTag);
                } else {
                    finalData = customDataTag;
                }
            } else {
                finalData = customDataTag;
            }
            
            // 直接使用 DataComponents.CUSTOM_DATA API 设置 custom_data 组件
            // 在 Minecraft 1.21.1 中，需要使用 CustomData 对象包装 CompoundTag
            boolean saved = false;
            try {
                // 尝试使用 CustomData.of() 创建（尝试不同的包路径）
                String[] possiblePaths = {
                    "net.minecraft.core.component.types.CustomData",
                    "net.minecraft.core.component.CustomData",
                    "net.minecraft.world.item.component.CustomData"
                };
                
                Object customDataComponent = null;
                for (String className : possiblePaths) {
                    try {
                        Class<?> customDataClass = Class.forName(className);
                        java.lang.reflect.Method ofMethod = customDataClass.getMethod("of", net.minecraft.nbt.CompoundTag.class);
                        customDataComponent = ofMethod.invoke(null, finalData);
                        break;
                    } catch (ClassNotFoundException e2) {
                        // 继续尝试下一个路径
                        continue;
                    } catch (Exception e3) {
                        // 继续尝试下一个路径
                    }
                }
                
                if (customDataComponent != null) {
                    // 使用反射调用 set 方法，因为类型可能不匹配
                    try {
                        java.lang.reflect.Method setMethod = ItemStack.class.getMethod("set", 
                            net.minecraft.core.component.DataComponentType.class, Object.class);
                        setMethod.invoke(itemStack, net.minecraft.core.component.DataComponents.CUSTOM_DATA, customDataComponent);
                        saved = true;
                    } catch (Exception e) {
                        // Error logging handled by Mixin
                    }
                } else {
                    // 如果所有反射方法都失败，无法创建CustomData对象
                    // Error logging handled by Mixin
                }
            } catch (Exception e) {
                // Error logging handled by Mixin
            }
            
            if (!saved) {
                // Error logging handled by Mixin
            }
        } else {
            // entityTag为空，需要清除EntityData标签以确保物品可以叠加
            if (existingData != null) {
                var existingTag = existingData.copyTag();
                if (existingTag != null && existingTag.contains("EntityData")) {
                    // 移除EntityData标签
                    existingTag.remove("EntityData");
                    
                    // 如果移除后tag为空，完全移除custom_data组件
                    if (existingTag.isEmpty()) {
                        try {
                            java.lang.reflect.Method removeMethod = ItemStack.class.getMethod("remove", 
                                net.minecraft.core.component.DataComponentType.class);
                            removeMethod.invoke(itemStack, net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                        } catch (Exception e) {
                            // Error logging handled by Mixin
                        }
                    } else {
                        // 还有其他的标签，保留custom_data但移除EntityData
                        try {
                            String[] possiblePaths = {
                                "net.minecraft.core.component.types.CustomData",
                                "net.minecraft.core.component.CustomData",
                                "net.minecraft.world.item.component.CustomData"
                            };
                            
                            Object customDataComponent = null;
                            for (String className : possiblePaths) {
                                try {
                                    Class<?> customDataClass = Class.forName(className);
                                    java.lang.reflect.Method ofMethod = customDataClass.getMethod("of", net.minecraft.nbt.CompoundTag.class);
                                    customDataComponent = ofMethod.invoke(null, existingTag);
                                    break;
                                } catch (ClassNotFoundException e2) {
                                    continue;
                                } catch (Exception e3) {
                                    // 继续尝试下一个路径
                                }
                            }
                            
                            if (customDataComponent != null) {
                                java.lang.reflect.Method setMethod = ItemStack.class.getMethod("set", 
                                    net.minecraft.core.component.DataComponentType.class, Object.class);
                                setMethod.invoke(itemStack, net.minecraft.core.component.DataComponents.CUSTOM_DATA, customDataComponent);
                            }
                        } catch (Exception e) {
                            // Error logging handled by Mixin
                        }
                    }
                }
            }
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
     * standing姿态始终在列表的第一个位置
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
        
        // 确保列表有序（字母顺序）
        poseNames.sort(String::compareTo);
        
        // 确保standing始终在第一个位置
        if (poseNames.contains("standing")) {
            poseNames.remove("standing");
            poseNames.add(0, "standing");
        }
        
        return poseNames;
    }
    
    /**
     * 循环切换到下一个姿态
     * 当处于默认状态（standing）时，第一次切换会跳过standing，直接切换到下一个姿态
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
        
        // 如果当前索引无效（-1表示默认standing状态），需要特殊处理
        if (currentPoseIndex < 0) {
            // 处于默认状态，跳过第一个（standing），直接跳到第二个
            if (poseNames.size() > 1) {
                currentPoseIndex = 1; // 跳过索引0（standing），直接到索引1
            } else {
                // 如果只有一个姿态（standing），则保持在默认状态
                currentPoseIndex = -1;
                // 设置为255表示使用默认姿态
                this.entityData.set(DATA_POSE_INDEX, (byte) 255);
                if (player != null) {
                    player.displayClientMessage(Component.literal("只有standing姿态可用"), true);
                }
                return;
            }
        } else if (currentPoseIndex >= poseNames.size()) {
            // 索引超出范围，重置为-1（standing姿态）
            currentPoseIndex = -1;
        } else {
            // 切换到下一个姿态
            currentPoseIndex++;
            if (currentPoseIndex >= poseNames.size()) {
                // 循环回到第一个（standing），设置为-1表示默认状态，不保存到NBT
                currentPoseIndex = -1;
            }
        }
        
        // 如果循环回到standing姿态（索引为-1），设置为255表示默认姿态
        if (currentPoseIndex < 0) {
            this.entityData.set(DATA_POSE_INDEX, (byte) 255);
            DollPose standingPose = PoseActionManager.getPose("standing");
            if (standingPose != null) {
                setPose(standingPose);
            } else {
                setPose(SimpleDollPose.createDefaultStandingPose());
            }
            if (player != null) {
                DollPose pose = getCurrentPose();
                String displayName = pose != null ? pose.getDisplayName() : "站立";
                player.displayClientMessage(Component.literal("切换到姿态: " + displayName), true);
            }
        } else {
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
                // 如果找不到姿态，使用standing姿态
                DollPose standingPose = PoseActionManager.getPose("standing");
                if (standingPose != null) {
                    setPose(standingPose);
                } else {
                    setPose(SimpleDollPose.createDefaultStandingPose());
                }
                // 设置为255表示使用默认姿态
                this.entityData.set(DATA_POSE_INDEX, (byte) 255);
                currentPoseIndex = -1;
                if (player != null) {
                    player.displayClientMessage(Component.literal("切换到standing姿态"), true);
                }
            }
        }
    }
    
    /**
     * 根据姿态更新姿态索引并同步到客户端
     * @param pose 要更新的姿态
     */
    private void updatePoseIndexFromPose(DollPose pose) {
        if (pose == null) {
            return;
        }
        
        // 重要：始终从PoseActionManager获取姿态对象，确保使用的是注册的姿态对象
        // 这样可以避免使用内联定义的姿态对象，确保姿态名称和索引的一致性
        String poseName = pose.getName();
        DollPose registeredPose = PoseActionManager.getPose(poseName);
        
        // 如果找不到注册的姿态，使用传入的pose（可能是默认姿态）
        DollPose poseToUse = registeredPose != null ? registeredPose : pose;
        String finalPoseName = poseToUse.getName();
        
        List<String> poseNames = getAvailablePoseNames();
        int poseIndex = poseNames.indexOf(finalPoseName);
        
        if (poseIndex >= 0) {
            this.currentPoseIndex = poseIndex;
            // 更新currentPose为注册的姿态对象，确保一致性
            this.currentPose = poseToUse;
            // 同步姿态索引到客户端（仅在服务端设置）
            if (!this.level().isClientSide) {
                if (currentPoseIndex == 0) {
                    // standing姿态使用255表示默认姿态
                    this.entityData.set(DATA_POSE_INDEX, (byte) 255);
                } else if (currentPoseIndex < 255) {
                    this.entityData.set(DATA_POSE_INDEX, (byte) (currentPoseIndex & 0xFF));
                } else {
                    this.entityData.set(DATA_POSE_INDEX, (byte) 255);
                }
            }
        } else {
            // 如果找不到对应的索引，但poseName是"standing"，使用255
            if ("standing".equals(finalPoseName)) {
                this.currentPoseIndex = -1;
                DollPose standingPose = PoseActionManager.getPose("standing");
                this.currentPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
                if (!this.level().isClientSide) {
                    this.entityData.set(DATA_POSE_INDEX, (byte) 255);
                }
            } else {
                // 对于其他找不到索引的姿态，回退到standing姿态
                // 这不应该发生，因为savedPose应该始终是一个在姿态列表中的姿态
                this.currentPoseIndex = -1;
                DollPose standingPose = PoseActionManager.getPose("standing");
                this.currentPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
                if (!this.level().isClientSide) {
                    this.entityData.set(DATA_POSE_INDEX, (byte) 255);
                }
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
                this.savedPose = pose; // 同时更新保存的姿态
            } else {
                // 如果找不到姿态，使用standing姿态
                DollPose standingPose = PoseActionManager.getPose("standing");
                DollPose defaultPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
                this.currentPose = defaultPose;
                this.savedPose = defaultPose;
            }
        } else {
            // 索引无效，使用standing姿态
            DollPose standingPose = PoseActionManager.getPose("standing");
            DollPose defaultPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
            this.currentPose = defaultPose;
            this.savedPose = defaultPose;
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
            // 设置姿态时先停止当前动作（确保同步到客户端）
            if (this.currentAction != null) {
                // 只清空动作状态，不恢复standing姿态（因为即将设置新姿态）
                this.currentAction = null;
                this.actionTick = 0;
                // 同步到客户端：清空动作名称和tick
                if (!this.level().isClientSide) {
                    this.entityData.set(DATA_ACTION_NAME, "");
                    this.entityData.set(DATA_ACTION_TICK, 0);
                }
            }
            
            // 重要：始终从PoseActionManager获取注册的姿态对象，确保一致性
            // 这样可以避免使用内联定义的姿态对象，确保姿态名称和索引的一致性
            String poseName = pose.getName();
            DollPose registeredPose = PoseActionManager.getPose(poseName);
            DollPose poseToUse = registeredPose != null ? registeredPose : pose;
            
            // 更新currentPose（动作结束后恢复姿态时需要立即更新）
            this.currentPose = poseToUse;
            // 始终更新savedPose（用于动作完成后恢复），使用注册的姿态对象
            this.savedPose = poseToUse;

            // 更新姿态索引并同步到客户端
            updatePoseIndexFromPose(poseToUse);

            // 姿态改变时更新碰撞箱
            updateBoundingBox();
        } else {
            // 如果设置为null，使用默认standing姿态
            DollPose standingPose = PoseActionManager.getPose("standing");
            DollPose defaultPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
            if (this.currentAction == null) {
                this.currentPose = defaultPose;
                // 更新姿态索引并同步到客户端
                updatePoseIndexFromPose(defaultPose);
            }
            this.savedPose = defaultPose;
            updateBoundingBox();
        }
    }
    
    /**
     * 根据姿态名称设置姿态并更新索引
     * @param poseName 姿态名称
     * @return 是否设置成功
     */
    public boolean setPoseByName(String poseName) {
        if (poseName == null || poseName.isEmpty()) {
            return false;
        }
        
        DollPose pose = PoseActionManager.getPose(poseName);
        if (pose == null) {
            // Warning logging handled by Mixin
            return false;
        }
        
        // 先更新姿态索引并同步到客户端（在清空动作之前）
        // 这确保客户端在动作被清空时能立即应用正确的姿态
        List<String> poseNames = getAvailablePoseNames();
        int poseIndex = poseNames.indexOf(poseName);
        
        if (poseIndex >= 0) {
            this.currentPoseIndex = poseIndex;
            // 同步姿态索引到客户端（仅在服务端设置）
            if (!this.level().isClientSide()) {
                if (currentPoseIndex == 0) {
                    this.entityData.set(DATA_POSE_INDEX, (byte) 255);
                } else if (currentPoseIndex < 255) {
                    this.entityData.set(DATA_POSE_INDEX, (byte) (currentPoseIndex & 0xFF));
                } else {
                    this.entityData.set(DATA_POSE_INDEX, (byte) 255);
                }
            }
        } else {
            // Warning logging handled by Mixin
            return false;
        }
        
        // 然后设置姿态（这会清空动作）
        setPose(pose);
        
        // Debug logging handled by Mixin
        return true;
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
        if (action != null && this.currentAction == null) {
            // 开始播放新动作时，保存当前姿态（用于动作完成后恢复）
            // 重要：必须保存当前的currentPose，而不是savedPose，确保恢复的是动作开始前的真实姿态
            DollPose poseToSave = this.currentPose != null ? this.currentPose : 
                (PoseActionManager.getPose("standing") != null ? 
                    PoseActionManager.getPose("standing") : SimpleDollPose.createDefaultStandingPose());
            
            // 确保savedPose是一个在姿态列表中的有效姿态，这样恢复时才能正确同步到客户端
            // 重要：必须从PoseActionManager获取姿态对象，而不是直接保存poseToSave
            // 因为poseToSave可能是内联定义的姿态对象，虽然名称相同，但对象不同
            List<String> poseNames = getAvailablePoseNames();
            String poseName = poseToSave.getName();
            
            // 始终从PoseActionManager获取姿态对象，确保使用的是注册的姿态对象
            DollPose foundPose = PoseActionManager.getPose(poseName);
            if (foundPose != null && poseNames.contains(foundPose.getName())) {
                // 从PoseActionManager获取的姿态对象，确保是注册的姿态
                this.savedPose = foundPose;
            } else {
                // 如果找不到，使用standing姿态作为后备
                DollPose standingPose = PoseActionManager.getPose("standing");
                this.savedPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
            }
        }
        
        this.currentAction = action;
        this.actionTick = 0;
        
        // 同步动作名称和tick到客户端
        if (!this.level().isClientSide) {
            if (action != null) {
                this.entityData.set(DATA_ACTION_NAME, action.getName());
                this.entityData.set(DATA_ACTION_TICK, 0);
            } else {
                this.entityData.set(DATA_ACTION_NAME, "");
                this.entityData.set(DATA_ACTION_TICK, 0);
            }
        }
    }
    
    /**
     * 更新客户端姿态（仅在客户端调用）
     * 现在由事件处理器调用，不再在tick中调用
     */
    public void updateClientPose() {
        // 优先处理动作（如果有）
        String syncedActionName = this.entityData.get(DATA_ACTION_NAME);
        if (syncedActionName != null && !syncedActionName.isEmpty()) {
            int syncedActionTick = this.entityData.get(DATA_ACTION_TICK);
            DollAction action = PoseActionManager.getAction(syncedActionName);
            if (action != null) {
                // 根据动作和tick计算当前姿态
                DollPose actionPose = action.getPoseAt(syncedActionTick);
                if (actionPose != null) {
                    boolean poseChanged = currentPose != actionPose;
                    currentPose = actionPose;
                    // 如果姿态改变，更新碰撞箱
                    if (poseChanged) {
                        updateBoundingBox();
                    }
                }
            }
        } else {
            // 没有动作，根据同步的索引更新姿态
            byte syncedIndex = this.entityData.get(DATA_POSE_INDEX);
            if (syncedIndex != 255) {
                int index = syncedIndex & 0xFF; // 转换为无符号整数
                // 强制更新姿态，不检查currentPoseIndex是否改变
                // 因为动作结束后可能需要恢复到相同的姿态索引
                currentPoseIndex = index;
                loadPoseByIndex();
                // 姿态改变时更新碰撞箱
                updateBoundingBox();
            } else {
                // 如果同步值为255，使用standing姿态
                // 强制更新，不检查currentPoseIndex是否已经是-1
                // 因为动作结束后可能需要恢复到standing姿态
                currentPoseIndex = -1;
                DollPose standingPose = PoseActionManager.getPose("standing");
                DollPose defaultPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
                currentPose = defaultPose;
                savedPose = defaultPose; // 同时更新保存的姿态
                // 姿态改变时更新碰撞箱
                updateBoundingBox();
            }
        }
    }

    /**
     * 更新服务端动作状态（仅在服务端调用）
     * 现在由事件处理器调用，不再在tick中调用
     */
    public void updateServerAction() {
        actionTick++;

        // 同步 actionTick 到客户端
        this.entityData.set(DATA_ACTION_TICK, actionTick);

        // 获取当前tick对应的姿态
        DollPose actionPose = currentAction.getPoseAt(actionTick);
        if (actionPose != null) {
            // 检查姿态是否改变（包括scale的变化）
            boolean poseChanged = currentPose != actionPose;
            currentPose = actionPose;
            // 如果姿态改变，更新碰撞箱
            if (poseChanged) {
                updateBoundingBox();
            }
        }

        // 处理动作结束
        handleActionEnd();
    }

    /**
     * 处理动作结束逻辑（仅在服务端调用）
     */
    private void handleActionEnd() {
        if (currentAction == null) {
            return;
        }

        // 如果动作不循环且播放完成，停止动作
        // 使用 > 而不是 >=，确保当actionTick == duration时能先显示最后一个关键帧
        if (!currentAction.isLooping() && actionTick > currentAction.getDuration()) {
            // 保存动作模式，因为处理过程中会清空currentAction
            var actionMode = currentAction.getMode();

            // 根据动作模式决定恢复行为
            if (actionMode == com.lanye.dolladdon.api.action.ActionMode.ONCE) {
                // ONCE模式：恢复到动作播放前的姿态
                DollPose poseToRestore = savedPose != null ? savedPose : (PoseActionManager.getPose("standing") != null ?
                    PoseActionManager.getPose("standing") : SimpleDollPose.createDefaultStandingPose());

                // 直接设置服务端的当前姿态
                this.currentPose = poseToRestore;
                this.savedPose = poseToRestore;

                // 更新姿态索引并同步到客户端
                updatePoseIndexFromPose(poseToRestore);

            } else if (actionMode == com.lanye.dolladdon.api.action.ActionMode.HOLD) {
                // HOLD模式：保持最后一个关键帧的姿态
                DollPose poseToHold = currentPose;

                // 确保姿态是注册的姿态对象
                String poseName = poseToHold.getName();
                DollPose registeredPose = PoseActionManager.getPose(poseName);
                if (registeredPose != null) {
                    poseToHold = registeredPose;
                }

                // 更新姿态索引并同步到客户端
                updatePoseIndexFromPose(poseToHold);

            } else {
                // 默认行为：恢复到standing姿态
                DollPose standingPose = PoseActionManager.getPose("standing");
                currentPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
                // 更新姿态索引为standing（索引-1或255）
                currentPoseIndex = -1;
                this.entityData.set(DATA_POSE_INDEX, (byte) 255);
            }

            // 清空动作状态并同步到客户端
            currentAction = null;
            actionTick = 0;
            this.entityData.set(DATA_ACTION_NAME, "");
            this.entityData.set(DATA_ACTION_TICK, 0);

            // 更新碰撞箱
            updateBoundingBox();

        } else if (currentAction.isLooping()) {
            // 循环动作，重置tick
            if (actionTick >= currentAction.getDuration()) {
                actionTick = 0;
                // 同步重置后的 tick 到客户端
                this.entityData.set(DATA_ACTION_TICK, 0);
            }
        }
    }

    /**
     * 停止当前动作
     */
    public void stopAction() {
        // 恢复到保存的姿态
        this.currentPose = this.savedPose != null ? this.savedPose :
            (PoseActionManager.getPose("standing") != null ?
                PoseActionManager.getPose("standing") : SimpleDollPose.createDefaultStandingPose());

        this.currentAction = null;
        this.actionTick = 0;

        // 同步到客户端：清空动作名称和tick
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_ACTION_NAME, "");
            this.entityData.set(DATA_ACTION_TICK, 0);
        }

        // 姿态改变时更新碰撞箱
        updateBoundingBox();
    }
}

