package com.lanye.dolladdon.base.entity;

import com.lanye.dolladdon.api.action.DollAction;
import com.lanye.dolladdon.api.pose.DollPose;
import com.lanye.dolladdon.api.pose.SimpleDollPose;
import com.lanye.dolladdon.util.ModuleLogger;
import com.lanye.dolladdon.util.PoseActionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 玩偶实体基类
 * 提供所有玩偶实体的共同功能
 * 
 * <p>日志系统使用说明：</p>
 * <ul>
 *   <li>使用模块化日志系统，支持一键开关不同模块的日志</li>
 *   <li>可以通过 {@link com.lanye.dolladdon.util.ModuleLogger#setModuleEnabled(String, boolean)} 控制单个模块</li>
 *   <li>可以通过 {@link com.lanye.dolladdon.util.ModuleLogger#setGlobalEnabled(boolean)} 一键控制所有模块</li>
 *   <li>示例：ModuleLogger.setModuleEnabled("entity.pose", false) 禁用姿态相关日志</li>
 * </ul>
 */
public abstract class BaseDollEntity extends Entity {
    // 模块化日志：模块名称常量
    // 可以通过 ModuleLogger.setModuleEnabled(模块名, false) 来禁用特定模块的日志
    private static final String LOG_MODULE_ENTITY = "entity";           // 实体基础日志
    private static final String LOG_MODULE_POSE = "entity.pose";      // 姿态相关日志
    private static final String LOG_MODULE_ACTION = "entity.action";   // 动作相关日志
    private static final String LOG_MODULE_INTERACT = "entity.interact"; // 交互相关日志
    private static final String LOG_MODULE_NBT = "entity.nbt";          // NBT相关日志
    
    // 日志对象（通过ModuleLogger获取，支持模块化开关）
    // 注意：直接使用 LOGGER 时不会受模块开关控制，建议使用 ModuleLogger.debug/info/warn/error 方法
    private static final org.slf4j.Logger LOGGER = ModuleLogger.getLogger(LOG_MODULE_ENTITY);
    
    private static final TrackedData<Byte> DATA_CLIENT_FLAGS = DataTracker.registerData(BaseDollEntity.class, TrackedDataHandlerRegistry.BYTE);
    // 同步姿态索引到客户端（使用Byte，支持0-255个姿态，足够使用）
    private static final TrackedData<Byte> DATA_POSE_INDEX = DataTracker.registerData(BaseDollEntity.class, TrackedDataHandlerRegistry.BYTE);
    
    // 姿态和动作相关字段
    private DollPose currentPose;
    private DollAction currentAction;
    private int actionTick = 0;
    
    // 当前姿态索引（用于循环切换）
    private int currentPoseIndex = -1;
    
    protected BaseDollEntity(EntityType<? extends BaseDollEntity> entityType, World world) {
        super(entityType, world);
        this.noClip = false; // 有物理碰撞
        
        // 确保实体可以被交互（设置碰撞箱）
        // 注意：实体必须有碰撞箱才能被右键交互
        
        // 默认使用standing姿态
        DollPose standingPose = PoseActionManager.getPose("standing");
        this.currentPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
        // 初始化时设置为255（默认姿态）
        if (!world.isClient) {
            this.dataTracker.set(DATA_POSE_INDEX, (byte) 255);
        }
        // 初始化碰撞箱（重要：实体必须有碰撞箱才能被交互）
        updateBoundingBox();
        
        // 记录实体初始化信息（使用info级别，确保能看到）
        double avgSideLength = this.getBoundingBox().getAverageSideLength();
        ModuleLogger.info(LOG_MODULE_ENTITY, 
            "[实体初始化] 类型={}, 位置=({}, {}, {}), 碰撞箱平均边长={}, 碰撞箱={}, noClip={}, 世界={}", 
            entityType.getTranslationKey(), 
            String.format("%.2f", this.getX()), String.format("%.2f", this.getY()), String.format("%.2f", this.getZ()), 
            String.format("%.3f", avgSideLength), this.getBoundingBox(), this.noClip, world.isClient ? "客户端" : "服务端");
    }
    
    protected BaseDollEntity(EntityType<? extends BaseDollEntity> entityType, World world, double x, double y, double z) {
        this(entityType, world);
        this.setPosition(x, y, z);
    }
    
    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(DATA_CLIENT_FLAGS, (byte) 0);
        this.dataTracker.startTracking(DATA_POSE_INDEX, (byte) 255); // 255 表示未设置（默认姿态）
    }
    
    @Override
    protected void readCustomDataFromNbt(NbtCompound tag) {
        restoreFromNBT(tag);
    }
    
    /**
     * 从NBT恢复实体数据（公共方法，供物品使用）
     * @param tag NBT标签
     */
    public void restoreFromNBT(NbtCompound tag) {
        ModuleLogger.debug(LOG_MODULE_NBT, "从NBT恢复实体数据");
        
        // 优先恢复动作（如果有）
        if (tag.contains("ActionName", 8)) { // 8 = TAG_STRING
            String actionName = tag.getString("ActionName");
            DollAction action = PoseActionManager.getAction(actionName);
            if (action != null) {
                ModuleLogger.debug(LOG_MODULE_ACTION, "恢复动作: {}", actionName);
                setAction(action);
                // 动作已设置，不需要再设置姿态
                return;
            } else {
                ModuleLogger.warn(LOG_MODULE_ACTION, "找不到动作: {}", actionName);
            }
        }
        
        // 优先使用姿态名称恢复（如果保存了）
        if (tag.contains("PoseName", 8)) { // 8 = TAG_STRING
            String poseName = tag.getString("PoseName");
            DollPose pose = PoseActionManager.getPose(poseName);
            if (pose != null) {
                ModuleLogger.debug(LOG_MODULE_POSE, "从NBT恢复姿态: {}", poseName);
                setPose(pose);
                // 更新currentPoseIndex（如果可能）
                List<String> poseNames = getAvailablePoseNames();
                int index = poseNames.indexOf(poseName);
                if (index >= 0) {
                    // 索引0表示standing姿态
                    this.currentPoseIndex = index;
                    // 同步到客户端（索引0使用255表示默认状态，不保存到NBT）
                    if (currentPoseIndex == 0) {
                        this.dataTracker.set(DATA_POSE_INDEX, (byte) 255);
                    } else if (currentPoseIndex < 255) {
                        this.dataTracker.set(DATA_POSE_INDEX, (byte) (currentPoseIndex & 0xFF));
                    } else {
                        this.dataTracker.set(DATA_POSE_INDEX, (byte) 255);
                    }
                }
                return;
            } else {
                ModuleLogger.warn(LOG_MODULE_POSE, "从NBT恢复姿态失败，找不到姿态: {}", poseName);
            }
        }
        
        // 如果没有姿态名称，尝试使用姿态索引（向后兼容）
        if (tag.contains("PoseIndex")) {
            int savedIndex = tag.getInt("PoseIndex");
            // 索引0表示standing姿态
            this.currentPoseIndex = savedIndex;
            // 加载时恢复姿态
            loadPoseByIndex();
            // 同步到客户端（索引0使用255表示默认状态，不保存到NBT）
            if (currentPoseIndex == 0) {
                this.dataTracker.set(DATA_POSE_INDEX, (byte) 255);
            } else if (currentPoseIndex < 255) {
                this.dataTracker.set(DATA_POSE_INDEX, (byte) (currentPoseIndex & 0xFF));
            } else {
                this.dataTracker.set(DATA_POSE_INDEX, (byte) 255);
            }
        } else {
            // NBT中没有姿态信息，使用默认standing姿态（索引0）
            this.currentPoseIndex = 0;
            this.dataTracker.set(DATA_POSE_INDEX, (byte) 255);
            DollPose standingPose = PoseActionManager.getPose("standing");
            this.currentPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
        }
    }
    
    @Override
    protected void writeCustomDataToNbt(NbtCompound tag) {
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
        
        // 定期记录实体状态（每 100 tick，约 5 秒一次）
        if (this.age % 100 == 0 && !this.getWorld().isClient) {
            double avgSideLength = this.getBoundingBox().getAverageSideLength();
            ModuleLogger.debug(LOG_MODULE_ENTITY, 
                "[实体状态] 年龄={}, 位置=({}, {}, {}), 碰撞箱平均边长={}, 碰撞箱={}, noClip={}, 是否在地面={}, 速度=({}, {}, {})", 
                this.age, 
                String.format("%.2f", this.getX()), String.format("%.2f", this.getY()), String.format("%.2f", this.getZ()), 
                String.format("%.3f", avgSideLength), this.getBoundingBox(), this.noClip, this.isOnGround(),
                String.format("%.2f", this.getVelocity().x), String.format("%.2f", this.getVelocity().y), String.format("%.2f", this.getVelocity().z));
        }
        
        // 确保碰撞箱始终存在（重要：实体必须有碰撞箱才能被交互）
        double avgSideLength = this.getBoundingBox().getAverageSideLength();
        if (avgSideLength < 0.01) {
            ModuleLogger.warn(LOG_MODULE_ENTITY, "[碰撞箱警告] 检测到碰撞箱异常小 (平均边长={})，重新设置碰撞箱。位置=({}, {}, {})", 
                String.format("%.3f", avgSideLength), 
                String.format("%.2f", this.getX()), String.format("%.2f", this.getY()), String.format("%.2f", this.getZ()));
            updateBoundingBox();
        }
        
        // 在客户端，根据同步的索引更新姿态
        if (this.getWorld().isClient) {
            byte syncedIndex = this.dataTracker.get(DATA_POSE_INDEX);
            if (syncedIndex != 255) {
                int index = syncedIndex & 0xFF; // 转换为无符号整数
                if (index != currentPoseIndex) {
                    currentPoseIndex = index;
                    loadPoseByIndex();
                    // 姿态改变时更新碰撞箱
                    updateBoundingBox();
                }
            } else if (currentPoseIndex != 0) {
                // 如果同步值为255，使用standing姿态（索引0）
                currentPoseIndex = 0;
                DollPose standingPose = PoseActionManager.getPose("standing");
                currentPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
                // 姿态改变时更新碰撞箱
                updateBoundingBox();
            }
        }
        
        // 更新动作
        if (currentAction != null) {
            actionTick++;
            
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
            
            // 如果动作不循环且播放完成，停止动作
            if (!currentAction.isLooping() && actionTick >= currentAction.getDuration()) {
                currentAction = null;
                actionTick = 0;
                // 恢复standing姿态
                DollPose standingPose = PoseActionManager.getPose("standing");
                currentPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
                // 姿态改变时更新碰撞箱
                updateBoundingBox();
            } else if (currentAction.isLooping()) {
                // 循环动作，重置tick
                if (actionTick >= currentAction.getDuration()) {
                    actionTick = 0;
                }
            }
        }
        
        // 应用重力
        if (!this.hasNoGravity()) {
            this.setVelocity(this.getVelocity().add(0.0, -0.04, 0.0));
        }
        
        // 移动（使用重写的move方法，会自动恢复碰撞箱）
        this.move(MovementType.SELF, this.getVelocity());
        
        // 应用摩擦力
        this.setVelocity(this.getVelocity().multiply(0.98, 0.98, 0.98));
        
        // 如果在地面上，停止垂直运动
        if (this.isOnGround()) {
            Vec3d movement = this.getVelocity();
            this.setVelocity(movement.x * 0.7, 0.0, movement.z * 0.7);
        }
    }
    
    /**
     * 重写getDimensions方法，返回固定尺寸
     * 注意：使用固定尺寸确保碰撞箱稳定，实际碰撞箱通过updateBoundingBox()动态调整
     */
    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        // 返回固定尺寸（与DollEntityFactory中的sized(0.6f, 1f)保持一致）
        // 实际碰撞箱会通过updateBoundingBox()根据姿态的scale动态调整
        return EntityDimensions.fixed(0.6f, 1.0f);
    }
    
    /**
     * 重写move方法，在移动后自动恢复自定义碰撞箱
     * move()方法会根据EntityType的尺寸重置碰撞箱，所以需要在移动后立即恢复
     * 注意：如果getDimensions()正常工作，这个方法可能不再需要
     */
    @Override
    public void move(MovementType movementType, Vec3d movement) {
        super.move(movementType, movement);
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
        
        // 确保碰撞箱有最小尺寸，以便可以被交互（至少0.3，确保足够大）
        baseWidth = Math.max(baseWidth, 0.3);
        baseHeight = Math.max(baseHeight, 0.3);
        
        // 计算碰撞箱的半宽
        double halfWidth = baseWidth / 2.0;
        
        // 获取实体位置
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        
        // 创建并设置碰撞箱
        // 碰撞箱以实体底部中心为基准，向上延伸
        // 注意：position偏移主要用于渲染，不影响碰撞箱位置
        Box newBoundingBox = new Box(
            x - halfWidth, y, z - halfWidth,
            x + halfWidth, y + baseHeight, z + halfWidth
        );
        
        this.setBoundingBox(newBoundingBox);
        
        // 验证碰撞箱是否有效
        double avgSideLength = newBoundingBox.getAverageSideLength();
        if (avgSideLength < 0.1) {
            ModuleLogger.warn(LOG_MODULE_ENTITY, 
                "[碰撞箱警告] 碰撞箱异常小 (平均边长={})，可能导致无法交互！位置=({}, {}, {}), 尺寸=宽{}高{}", 
                String.format("%.3f", avgSideLength), 
                String.format("%.2f", x), String.format("%.2f", y), String.format("%.2f", z), 
                String.format("%.2f", baseWidth), String.format("%.2f", baseHeight));
        }
        
        // 调试日志：记录碰撞箱更新（仅在debug级别）
        ModuleLogger.debug(LOG_MODULE_ENTITY, 
            "[碰撞箱更新] 位置=({}, {}, {}), 尺寸=宽{}高{}, 平均边长={}, 碰撞箱={}", 
            String.format("%.2f", x), String.format("%.2f", y), String.format("%.2f", z), 
            String.format("%.2f", baseWidth), String.format("%.2f", baseHeight), 
            String.format("%.3f", avgSideLength), newBoundingBox);
    }
    
    // 注意：在 Minecraft 1.20.1 中，isPickable() 和 canBeCollidedWith() 方法可能不存在
    // 如果这些方法不存在，Minecraft 会使用默认行为
    // 实体可以通过实现 interact() 方法来支持交互
    
    /**
     * 检查实体是否可碰撞
     * 注意：在 Minecraft 1.20.1 中，isCollidable() 方法可能不存在于 Entity 类中
     * 如果编译错误，请移除 @Override 注解或删除此方法
     */
    // @Override  // 如果编译错误，请注释掉这行
    public boolean isCollidable() {
        boolean result = true;
        ModuleLogger.debug(LOG_MODULE_ENTITY, "[isCollidable] 返回: {}", result);
        return result;
    }
    
    @Override
    public boolean isPushable() {
        // 返回 false 防止玩家推动实体
        return false;
    }
    
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return false;
    }
    
    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        // 详细记录交互尝试（无论客户端还是服务端）
        boolean isSneaking = player.isSneaking();
        String playerName = player.getName().getString();
        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        double entityX = this.getX();
        double entityY = this.getY();
        double entityZ = this.getZ();
        double distance = Math.sqrt(
            Math.pow(playerX - entityX, 2) + 
            Math.pow(playerY - entityY, 2) + 
            Math.pow(playerZ - entityZ, 2)
        );
        double avgSideLength = this.getBoundingBox().getAverageSideLength();
        
        ModuleLogger.info(LOG_MODULE_INTERACT, 
            "[交互] 玩家右键交互触发！玩家={}, 潜行={}, 客户端={}, 手={}, 距离={}, 实体位置=({}, {}, {}), 玩家位置=({}, {}, {}), 碰撞箱平均边长={}, 当前姿态={}", 
            playerName, isSneaking, this.getWorld().isClient, hand, String.format("%.2f", distance),
            String.format("%.2f", entityX), String.format("%.2f", entityY), String.format("%.2f", entityZ), 
            String.format("%.2f", playerX), String.format("%.2f", playerY), String.format("%.2f", playerZ), 
            String.format("%.3f", avgSideLength),
            getCurrentPose() != null ? getCurrentPose().getName() : "null");
        
        // 在客户端，返回 SUCCESS 表示交互被接受，让服务端处理
        if (this.getWorld().isClient) {
            ModuleLogger.debug(LOG_MODULE_INTERACT, "[交互-客户端] 返回 SUCCESS，等待服务端处理");
            return ActionResult.SUCCESS;
        }
        
        // 服务端处理逻辑
        ModuleLogger.info(LOG_MODULE_INTERACT, "[交互-服务端] 开始处理交互逻辑");
        
        // 如果玩家潜行，则破坏实体并掉落物品
        if (isSneaking) {
            ModuleLogger.info(LOG_MODULE_INTERACT, "[交互-服务端] 玩家 {} 潜行右键，破坏实体", playerName);
            return handleBreakAndDrop(player);
        } else {
            // 循环切换到下一个姿态
            ModuleLogger.info(LOG_MODULE_INTERACT, "[交互-服务端] 玩家 {} 右键切换姿态 (当前索引: {})", 
                playerName, currentPoseIndex);
            cycleToNextPose(player);
            
            // 播放交互音效
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_ARMOR_STAND_HIT, SoundCategory.NEUTRAL, 0.5F, 1.0F);
            
            ModuleLogger.info(LOG_MODULE_INTERACT, "[交互-服务端] 姿态切换完成，返回 CONSUME");
            return ActionResult.CONSUME;
        }
    }
    
    /**
     * 获取对应的物品堆（由子类实现）
     * @return 对应的物品堆
     */
    protected abstract ItemStack getDollItemStack();
    
    /**
     * 处理破坏并掉落物品
     */
    private ActionResult handleBreakAndDrop(PlayerEntity player) {
        // 创建物品堆
        ItemStack itemStack = getDollItemStack();
        
        // 保存NBT标签到物品
        NbtCompound entityTag = new NbtCompound();
        this.writeCustomDataToNbt(entityTag);
        
        // 只有当entityTag不为空时才保存NBT，否则清除EntityData标签（允许物品叠加）
        if (!entityTag.isEmpty()) {
            // 获取或创建物品的NBT标签
            NbtCompound itemTag = itemStack.getOrCreateNbt();
            itemTag.put("EntityData", entityTag);
            itemStack.setNbt(itemTag);
        } else {
            // entityTag为空，需要清除EntityData标签以确保物品可以叠加
            NbtCompound itemTag = itemStack.getNbt();
            if (itemTag != null && itemTag.contains("EntityData")) {
                // 移除EntityData标签
                itemTag.remove("EntityData");
                
                // 如果移除后tag为空，完全移除NBT标签
                if (itemTag.isEmpty()) {
                    itemStack.setNbt(null);
                } else {
                    itemStack.setNbt(itemTag);
                }
            }
        }
        
        // 掉落物品
        net.minecraft.entity.ItemEntity itemEntity = new net.minecraft.entity.ItemEntity(
                this.getWorld(),
                this.getX(),
                this.getY(),
                this.getZ(),
                itemStack
        );
        itemEntity.setPickupDelay(40);
        this.getWorld().spawnEntity(itemEntity);
        
        // 播放破坏音效
        this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                SoundEvents.ENTITY_ARMOR_STAND_BREAK, SoundCategory.NEUTRAL, 0.5F, 1.0F);
        
        // 移除实体
        this.remove(Entity.RemovalReason.DISCARDED);
        
        return ActionResult.SUCCESS;
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
     * 正常循环所有姿态，包括standing姿态
     */
    private void cycleToNextPose(PlayerEntity player) {
        List<String> poseNames = getAvailablePoseNames();
        
        if (poseNames.isEmpty()) {
            ModuleLogger.warn(LOG_MODULE_POSE, "右键切换姿态: 没有可用的姿态");
            if (player != null) {
                // 显示在动作栏（物品栏上方）
                player.sendMessage(Text.literal("没有可用的姿态"), true);
            }
            return;
        }
        
        // 停止当前动作
        stopAction();
        
        int oldIndex = currentPoseIndex;
        String oldPoseName = oldIndex >= 0 && oldIndex < poseNames.size() ? poseNames.get(oldIndex) : "默认";
        DollPose oldPose = getCurrentPose();
        String oldDisplayName = oldPose != null && oldPose.getDisplayName() != null ? oldPose.getDisplayName() : oldPoseName;
        
        // 如果当前索引无效（-1表示默认standing状态），先设置为0（standing）
        if (currentPoseIndex < 0) {
            currentPoseIndex = 0;
            ModuleLogger.debug(LOG_MODULE_POSE, "右键切换姿态: 从默认状态(-1)切换到索引0");
        } else if (currentPoseIndex >= poseNames.size()) {
            // 索引超出范围，重置为0（standing姿态）
            ModuleLogger.warn(LOG_MODULE_POSE, "右键切换姿态: 索引超出范围 ({} >= {})，重置为0", 
                currentPoseIndex, poseNames.size());
            currentPoseIndex = 0;
        } else {
            // 切换到下一个姿态
            currentPoseIndex++;
            if (currentPoseIndex >= poseNames.size()) {
                // 循环回到第一个（standing）
                ModuleLogger.debug(LOG_MODULE_POSE, "右键切换姿态: 循环回到第一个姿态 (索引 {} -> 0)", oldIndex);
                currentPoseIndex = 0;
            }
        }
        
        // 加载新姿态
        String poseName = poseNames.get(currentPoseIndex);
        DollPose pose = PoseActionManager.getPose(poseName);
        
        if (pose != null) {
            setPose(pose);
            
            // 同步姿态索引到客户端
            // 如果索引为0（standing），使用255表示默认状态（不保存到NBT）
            if (currentPoseIndex == 0) {
                this.dataTracker.set(DATA_POSE_INDEX, (byte) 255);
            } else {
                byte indexToSync = (byte) (currentPoseIndex & 0xFF);
                this.dataTracker.set(DATA_POSE_INDEX, indexToSync);
            }
            
            String newDisplayName = pose.getDisplayName() != null ? pose.getDisplayName() : poseName;
            
            // 记录详细日志
            ModuleLogger.info(LOG_MODULE_POSE, "右键切换姿态成功: {} -> {} (索引: {} -> {}, 显示名称: {} -> {})", 
                oldPoseName, poseName, oldIndex, currentPoseIndex, oldDisplayName, newDisplayName);
            ModuleLogger.debug(LOG_MODULE_POSE, "姿态切换详情: 总姿态数={}, 当前进度={}/{}", 
                poseNames.size(), currentPoseIndex + 1, poseNames.size());
            
            // 发送消息给玩家（优先使用中文名称，显示在动作栏）
            if (player != null) {
                player.sendMessage(Text.literal("切换到姿态: " + newDisplayName + " (" + (currentPoseIndex + 1) + "/" + poseNames.size() + ")"), true);
            }
        } else {
            // 如果找不到姿态，使用standing姿态
            ModuleLogger.warn(LOG_MODULE_POSE, "右键切换姿态失败: 找不到姿态 '{}'，使用默认standing姿态", poseName);
            DollPose standingPose = PoseActionManager.getPose("standing");
            if (standingPose != null) {
                setPose(standingPose);
            } else {
                setPose(SimpleDollPose.createDefaultStandingPose());
            }
            // 设置为255表示使用默认姿态
            this.dataTracker.set(DATA_POSE_INDEX, (byte) 255);
            currentPoseIndex = 0; // 设置为0而不是-1，以便下次能正常循环
            if (player != null) {
                player.sendMessage(Text.literal("切换到standing姿态"), true);
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
                // 如果找不到姿态，使用standing姿态
                DollPose standingPose = PoseActionManager.getPose("standing");
                this.currentPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
            }
        } else {
            // 索引无效，使用standing姿态
            DollPose standingPose = PoseActionManager.getPose("standing");
            this.currentPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
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
            String oldPoseName = this.currentPose != null ? this.currentPose.getName() : "null";
            this.currentPose = pose;
            // 设置姿态时停止当前动作
            this.currentAction = null;
            this.actionTick = 0;
            // 姿态改变时更新碰撞箱
            updateBoundingBox();
            ModuleLogger.debug(LOG_MODULE_POSE, "设置姿态: {} -> {}", oldPoseName, pose.getName());
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
        String oldActionName = this.currentAction != null ? this.currentAction.getName() : "null";
        this.currentAction = action;
        this.actionTick = 0;
        ModuleLogger.debug(LOG_MODULE_ACTION, "设置动作: {} -> {}", oldActionName, 
            action != null ? action.getName() : "null");
    }
    
    /**
     * 停止当前动作
     */
    public void stopAction() {
        this.currentAction = null;
        this.actionTick = 0;
        // 恢复standing姿态
        DollPose standingPose = PoseActionManager.getPose("standing");
        this.currentPose = standingPose != null ? standingPose : SimpleDollPose.createDefaultStandingPose();
    }
}

