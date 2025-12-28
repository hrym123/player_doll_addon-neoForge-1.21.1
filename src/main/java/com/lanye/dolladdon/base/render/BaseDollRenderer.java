package com.lanye.dolladdon.base.render;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 玩偶实体渲染器基类
 * 提供所有玩偶实体渲染器的共同功能
 */
public abstract class BaseDollRenderer<T extends BaseDollEntity> extends EntityRenderer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDollRenderer.class);
    protected final PlayerModel<Player> playerModel;
    
    /**
     * 缓存计算结果，避免重复计算相同姿态的偏移量
     * Key: 姿态配置的字符串表示（旋转角度组合）
     * Value: 计算结果（totalYOffset, adjustY, upVector）
     */
    private static final Map<String, RotationOffsetCache> rotationOffsetCache = new ConcurrentHashMap<>();
    
    /**
     * 旋转偏移计算结果缓存
     */
    private static class RotationOffsetCache {
        final float totalYOffset;
        final float adjustY;
        final float[] upVector;
        
        RotationOffsetCache(float totalYOffset, float adjustY, float[] upVector) {
            this.totalYOffset = totalYOffset;
            this.adjustY = adjustY;
            this.upVector = upVector; // 数组会被复用，但计算结果相同，所以可以安全共享
        }
    }
    
    protected BaseDollRenderer(EntityRendererProvider.Context context, PlayerModel<Player> playerModel) {
        super(context);
        this.playerModel = playerModel;
    }
    
    /**
     * 获取皮肤资源位置
     * @param entity 实体
     * @return 皮肤资源位置
     */
    protected abstract ResourceLocation getSkinLocation(T entity);
    
    @Override
    public void render(T entity, float entityYaw, float partialTick, 
                      PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        
        // 应用旋转
        float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));
        
        float modelScale = 0.5F; 
        
        // 获取皮肤位置（由子类实现）
        ResourceLocation skinLocation = getSkinLocation(entity);
        
        // 从实体获取当前姿态
        com.lanye.dolladdon.api.pose.DollPose pose = entity.getCurrentPose();
        if (pose == null) {
            // 如果没有姿态，使用默认站立姿态
            pose = com.lanye.dolladdon.api.pose.SimpleDollPose.createDefaultStandingPose();
        }
        
        // 获取姿态的scale，用于计算Y偏移以保持模型底部对齐碰撞箱底部
        float[] scale = pose.getScale();
        // 玩家模型高度约为1.8，应用modelScale(=0.5)后高度为0.9
        // 应用scale[1]后，模型高度变为0.9 * scale[1]
        // 变换顺序：translate(yOffset) -> scale(modelScale) -> scale(scale[1])
        // 由于scale以当前位置为中心，最终模型中心在yOffset，模型底部在 yOffset - 0.45 * scale[1]
        // 为了保持模型底部对齐碰撞箱底部（y=0），需要：yOffset = 0.45 * scale[1]
        // 注意：这里0.45 = 1.8 * modelScale / 2 = 0.9 / 2
        float yOffset = 0.75f * scale[1];
        
        poseStack.translate(0.0, yOffset, 0.0);
        
        // 应用缩放和翻转
        poseStack.scale(-modelScale, -modelScale, modelScale);
        
        // 应用姿态的位置和大小
        float[] position = pose.getPosition();
        if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
            poseStack.translate(position[0], position[1], position[2]);
        }
        if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
            poseStack.scale(scale[0], scale[1], scale[2]);
        }
        
        // 从姿态获取旋转角度
        float[] headRot = pose.getHeadRotation();
        float[] hatRot = pose.getHatRotation();
        float[] bodyRot = pose.getBodyRotation();
        float[] rightArmRot = pose.getRightArmRotation();
        float[] leftArmRot = pose.getLeftArmRotation();
        float[] rightLegRot = pose.getRightLegRotation();
        float[] leftLegRot = pose.getLeftLegRotation();
        
        float headRotX = headRot[0], headRotY = headRot[1], headRotZ = headRot[2];
        float hatRotX = hatRot[0], hatRotY = hatRot[1], hatRotZ = hatRot[2];
        float bodyRotX = bodyRot[0], bodyRotY = bodyRot[1], bodyRotZ = bodyRot[2];
        float rightArmRotX = rightArmRot[0], rightArmRotY = rightArmRot[1], rightArmRotZ = rightArmRot[2];
        float leftArmRotX = leftArmRot[0], leftArmRotY = leftArmRot[1], leftArmRotZ = leftArmRot[2];
        float rightLegRotX = rightLegRot[0], rightLegRotY = rightLegRot[1], rightLegRotZ = rightLegRot[2];
        float leftLegRotX = leftLegRot[0], leftLegRotY = leftLegRot[1], leftLegRotZ = leftLegRot[2];
        
        // 注意：如果身体有旋转，头部、手臂、身体和腿部都在身体的旋转坐标系中渲染
        // 它们的旋转值都是相对于身体的，所以直接设置即可
        playerModel.head.setRotation(headRotX, headRotY, headRotZ);
        playerModel.hat.setRotation(hatRotX, hatRotY, hatRotZ);
        // 注意：身体的旋转通过 PoseStack 应用，不在这里设置，避免双重旋转
        playerModel.rightArm.setRotation(rightArmRotX, rightArmRotY, rightArmRotZ);
        playerModel.leftArm.setRotation(leftArmRotX, leftArmRotY, leftArmRotZ);
        playerModel.rightLeg.setRotation(rightLegRotX, rightLegRotY, rightLegRotZ);
        playerModel.leftLeg.setRotation(leftLegRotX, leftLegRotY, leftLegRotZ);
        
        // 同时设置外层部分的旋转，使它们跟随基础部分的动作
        // 注意：身体的旋转通过 PoseStack 应用，所以 jacket 的旋转也设为0
        setOverlayPartsRotation(0, 0, 0, // 身体旋转通过 PoseStack 应用
                               leftArmRotX, leftArmRotY, leftArmRotZ,
                               rightArmRotX, rightArmRotY, rightArmRotZ,
                               leftLegRotX, leftLegRotY, leftLegRotZ,
                               rightLegRotX, rightLegRotY, rightLegRotZ);
        
        // 获取渲染类型
        var cutoutRenderType = net.minecraft.client.renderer.RenderType.entityCutoutNoCull(skinLocation);
        var translucentRenderType = net.minecraft.client.renderer.RenderType.entityTranslucent(skinLocation);
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        
        // 如果有身体旋转，预先计算一次旋转后整个模型的最低点偏移（基础层和外层共享）
        // 使用缓存机制，每个姿态配置只计算一次，后续直接使用缓存结果
        float totalYOffset = 0.0f;
        float adjustY = 0.0f;
        float[] upVector = null;
        if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
            // 生成缓存key：基于所有旋转角度和缩放因子
            // 注意：将弧度转换为度数，因为JSON文件中配置的是度数，用于缓存key
            float bodyRotXDeg = (float) Math.toDegrees(bodyRotX);
            float bodyRotYDeg = (float) Math.toDegrees(bodyRotY);
            float bodyRotZDeg = (float) Math.toDegrees(bodyRotZ);
            float headRotXDeg = (float) Math.toDegrees(headRotX);
            float headRotYDeg = (float) Math.toDegrees(headRotY);
            float headRotZDeg = (float) Math.toDegrees(headRotZ);
            float rightLegRotXDeg = (float) Math.toDegrees(rightLegRotX);
            float rightLegRotYDeg = (float) Math.toDegrees(rightLegRotY);
            float rightLegRotZDeg = (float) Math.toDegrees(rightLegRotZ);
            float leftLegRotXDeg = (float) Math.toDegrees(leftLegRotX);
            float leftLegRotYDeg = (float) Math.toDegrees(leftLegRotY);
            float leftLegRotZDeg = (float) Math.toDegrees(leftLegRotZ);
            
            String cacheKey = String.format("%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f",
                bodyRotXDeg, bodyRotYDeg, bodyRotZDeg,
                headRotXDeg, headRotYDeg, headRotZDeg,
                rightLegRotXDeg, rightLegRotYDeg, rightLegRotZDeg,
                leftLegRotXDeg, leftLegRotYDeg, leftLegRotZDeg,
                scale[1]);
            
            // 检查缓存
            RotationOffsetCache cache = rotationOffsetCache.get(cacheKey);
            if (cache != null) {
                // 使用缓存结果（不输出日志，避免每帧都输出大量日志）
                totalYOffset = cache.totalYOffset;
                adjustY = cache.adjustY;
                upVector = cache.upVector;
            } else {
                // 缓存未命中，进行计算（只在首次计算时输出日志）
                LOGGER.debug("[BaseDollRenderer] 缓存未命中，开始计算（姿态配置: {}）", cacheKey);
                
                // 计算旋转后整个模型的最低点偏移
                // 1. 先计算身体旋转产生的整体偏移（包括身体、头部、手臂、腿部）
                // 2. 然后计算头部旋转产生的偏移（头部会跟随身体旋转）
                // 3. 最后计算腿部旋转产生的偏移（腿部会跟随身体旋转，取两条腿部的最大值）
                totalYOffset = calculateTotalRotationYOffset(
                    bodyRotXDeg, bodyRotYDeg, bodyRotZDeg,
                    headRotXDeg, headRotYDeg, headRotZDeg,
                    rightLegRotXDeg, rightLegRotYDeg, rightLegRotZDeg,
                    leftLegRotXDeg, leftLegRotYDeg, leftLegRotZDeg,
                    scale[1]
                );
                
                // 计算调整量
                // totalYOffset 是旋转后最低点相对于旋转中心的Y偏移（在已缩放的模型坐标系中）
                // 旋转中心在模型坐标系中的Y坐标是0.375（相对于模型中心）
                // 所以旋转后最低点在模型坐标系中的Y坐标 = 0.375 + totalYOffset
                float rotationCenterY = 0.375f;
                float lowestPointY = rotationCenterY + totalYOffset;
                // adjustY 表示需要调整的距离，使得最低点对齐到y=0
                // 如果最低点在 lowestPointY（相对于模型中心），要让最低点在y=0，需要将模型向下移动 lowestPointY
                // 在应用时使用 -upVector * adjustY，所以 adjustY 应该是正数（表示向下移动的距离）
                // 但之前的代码使用 -lowestPointY，说明应用方式可能不同，先恢复原来的逻辑
                adjustY = -lowestPointY;
                
                // 计算调整方向向量
                upVector = applyRotation(0.0f, 1.0f, 0.0f, bodyRotX, bodyRotY, bodyRotZ);
                
                // 创建新的数组副本用于缓存（避免数组被修改）
                float[] cachedUpVector = new float[]{upVector[0], upVector[1], upVector[2]};
                
                // 存入缓存
                rotationOffsetCache.put(cacheKey, new RotationOffsetCache(totalYOffset, adjustY, cachedUpVector));
                upVector = cachedUpVector; // 使用缓存的数组
                
                // 日志输出：计算流程（只输出一次，首次计算时）
                LOGGER.debug("[BaseDollRenderer] 身体旋转计算流程（已缓存）:");
                LOGGER.debug("  - 输入旋转角度: bodyRotX={}°, bodyRotY={}°, bodyRotZ={}°", bodyRotXDeg, bodyRotYDeg, bodyRotZDeg);
                LOGGER.debug("  - 旋转中心Y坐标: 0.375");
                LOGGER.debug("  - 计算得到的totalYOffset: {} (相对于旋转中心的Y偏移)", totalYOffset);
                LOGGER.debug("  - 旋转后最低点绝对位置: {} (旋转中心{} + totalYOffset{})", lowestPointY, rotationCenterY, totalYOffset);
                LOGGER.debug("  - 调整方向向量(upVector): [{}, {}, {}]", upVector[0], upVector[1], upVector[2]);
                LOGGER.debug("  - 调整量(adjustY): {} (负数表示向下调整，使最低点贴地)", adjustY);
            }
        }
        
        // 第一步：渲染基础层（base layer）
        var baseVertexConsumer = bufferSource.getBuffer(cutoutRenderType);
        
        // 如果有身体旋转，使用 PoseStack 在身体旋转中心应用旋转，然后渲染身体、头部、手臂和腿部
        if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
            poseStack.pushPose();
            
            // 移动到身体的旋转中心（身体和头连接处，Y坐标约为0.375）
            float rotationCenterY = 0.375f;
            poseStack.translate(0.0, rotationCenterY, 0.0);
            
            // 应用身体旋转（只在这里应用，不在 setRotation 中设置）
            poseStack.mulPose(Axis.XP.rotation(bodyRotX));
            poseStack.mulPose(Axis.YP.rotation(bodyRotY));
            poseStack.mulPose(Axis.ZP.rotation(bodyRotZ));
            
            // 移回，并在旋转后的坐标系中调整位置，使得旋转后的最低点贴地
            // 使用预先计算好的 adjustY 和 upVector（避免重复计算）
            // 在旋转后的坐标系中，我们需要：
            // 1. 先移回旋转中心（在世界坐标系中是-0.375的Y方向，但在旋转后的坐标系中需要应用旋转）
            // 2. 然后沿着 upVector 方向调整 adjustY 的距离
            // 注意：在旋转后的坐标系中，原来的Y方向已经改变了，所以需要计算旋转中心向量
            float[] rotationCenterVector = applyRotation(0.0f, -rotationCenterY, 0.0f, bodyRotX, bodyRotY, bodyRotZ);
            float translateX = rotationCenterVector[0] - upVector[0] * adjustY;
            float translateY = rotationCenterVector[1] - upVector[1] * adjustY;
            float translateZ = rotationCenterVector[2] - upVector[2] * adjustY;
            
            LOGGER.debug("  - 旋转中心向量(rotationCenterVector): [{}, {}, {}]", rotationCenterVector[0], rotationCenterVector[1], rotationCenterVector[2]);
            LOGGER.debug("  - 调整向量(-upVector * adjustY): [{}, {}, {}]", -upVector[0] * adjustY, -upVector[1] * adjustY, -upVector[2] * adjustY);
            LOGGER.debug("  - 最终平移向量: [{}, {}, {}]", translateX, translateY, translateZ);
            
            poseStack.translate(translateX, translateY, translateZ);
            // 在旋转后的坐标系中渲染身体、头部、手臂和腿部
            // 注意：头部和手臂的旋转值已经是相对于身体的，所以保持它们的旋转值
            playerModel.body.setRotation(0, 0, 0); // 确保身体不额外旋转（旋转已通过 PoseStack 应用）
            // 头部和手臂保持它们自己的相对旋转值（headRotX等已经在上面设置）
            playerModel.body.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.head.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.rightArm.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.leftArm.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.rightLeg.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.leftLeg.render(poseStack, baseVertexConsumer, packedLight, overlay);
            poseStack.popPose();
        } else {
            // 没有身体旋转时，正常渲染
            playerModel.body.setRotation(0, 0, 0);
            playerModel.body.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.head.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.rightArm.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.leftArm.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.rightLeg.render(poseStack, baseVertexConsumer, packedLight, overlay);
            playerModel.leftLeg.render(poseStack, baseVertexConsumer, packedLight, overlay);
        }
        
        // 第二步：渲染外层（overlay layer）- 使用半透明渲染以正确显示多层皮肤
        var overlayVertexConsumer = bufferSource.getBuffer(translucentRenderType);
        
        // 如果有身体旋转，使用 PoseStack 在身体旋转中心应用旋转，然后渲染所有外层部分
        if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
            poseStack.pushPose();
            // 使用预先计算好的 totalYOffset、adjustY 和 upVector（避免重复计算）
            // 移动到身体的旋转中心（身体和头连接处，Y坐标约为0.375）
            float rotationCenterY = 0.375f;
            poseStack.translate(0.0, rotationCenterY, 0.0);
            
            // 应用身体旋转（只在这里应用，不在 setRotation 中设置）
            poseStack.mulPose(Axis.XP.rotation(bodyRotX));
            poseStack.mulPose(Axis.YP.rotation(bodyRotY));
            poseStack.mulPose(Axis.ZP.rotation(bodyRotZ));
            
            // 移回，并在旋转后的坐标系中调整位置，使得旋转后的最低点贴地
            // 使用预先计算好的 adjustY 和 upVector（避免重复计算）
            // 在旋转后的坐标系中，我们需要：
            // 1. 先移回旋转中心（在世界坐标系中是-0.375的Y方向，但在旋转后的坐标系中需要应用旋转）
            // 2. 然后沿着 upVector 方向调整 adjustY 的距离
            // 注意：在旋转后的坐标系中，原来的Y方向已经改变了，所以需要计算旋转中心向量
            float[] rotationCenterVector = applyRotation(0.0f, -rotationCenterY, 0.0f, bodyRotX, bodyRotY, bodyRotZ);
            float translateX = rotationCenterVector[0] - upVector[0] * adjustY;
            float translateY = rotationCenterVector[1] - upVector[1] * adjustY;
            float translateZ = rotationCenterVector[2] - upVector[2] * adjustY;
            
            // 外层渲染使用相同的平移向量（日志已在基础层输出）
            poseStack.translate(translateX, translateY, translateZ);
            // 在旋转后的坐标系中 渲染所有外层部分
            // hat层（头发外层）
            playerModel.hat.render(poseStack, overlayVertexConsumer, packedLight, overlay);
            // 手臂外层（保持它们自己的旋转值）
            renderArmOverlayParts(poseStack, overlayVertexConsumer, packedLight, overlay);
            // 身体和腿部外层（jacket 的旋转设为0）
            setBodyOverlayRotation(0, 0, 0); // 确保身体外层不额外旋转
            renderBodyLegOverlayParts(poseStack, overlayVertexConsumer, packedLight, overlay);
            poseStack.popPose();
        } else {
            // 没有身体旋转时，正常渲染
            playerModel.hat.render(poseStack, overlayVertexConsumer, packedLight, overlay);
            renderArmOverlayParts(poseStack, overlayVertexConsumer, packedLight, overlay);
            setBodyOverlayRotation(0, 0, 0);
            renderBodyLegOverlayParts(poseStack, overlayVertexConsumer, packedLight, overlay);
        }
        
        poseStack.popPose();
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
    
    /**
     * 设置外层部分的旋转，使它们跟随基础部分的动作
     * 
     * @param bodyRotX 身体的X旋转
     * @param bodyRotY 身体的Y旋转
     * @param bodyRotZ 身体的Z旋转
     * @param leftArmRotX 左臂的X旋转
     * @param leftArmRotY 左臂的Y旋转
     * @param leftArmRotZ 左臂的Z旋转
     * @param rightArmRotX 右臂的X旋转
     * @param rightArmRotY 右臂的Y旋转
     * @param rightArmRotZ 右臂的Z旋转
     * @param leftLegRotX 左腿的X旋转
     * @param leftLegRotY 左腿的Y旋转
     * @param leftLegRotZ 左腿的Z旋转
     * @param rightLegRotX 右腿的X旋转
     * @param rightLegRotY 右腿的Y旋转
     * @param rightLegRotZ 右腿的Z旋转
     */
    private void setOverlayPartsRotation(float bodyRotX, float bodyRotY, float bodyRotZ,
                                        float leftArmRotX, float leftArmRotY, float leftArmRotZ,
                                        float rightArmRotX, float rightArmRotY, float rightArmRotZ,
                                        float leftLegRotX, float leftLegRotY, float leftLegRotZ,
                                        float rightLegRotX, float rightLegRotY, float rightLegRotZ) {
        try {
            // 使用反射访问PlayerModel的外层部分并设置旋转
            java.lang.reflect.Field leftSleeveField = PlayerModel.class.getDeclaredField("leftSleeve");
            java.lang.reflect.Field rightSleeveField = PlayerModel.class.getDeclaredField("rightSleeve");
            java.lang.reflect.Field leftPantsField = PlayerModel.class.getDeclaredField("leftPants");
            java.lang.reflect.Field rightPantsField = PlayerModel.class.getDeclaredField("rightPants");
            java.lang.reflect.Field jacketField = PlayerModel.class.getDeclaredField("jacket");
            
            leftSleeveField.setAccessible(true);
            rightSleeveField.setAccessible(true);
            leftPantsField.setAccessible(true);
            rightPantsField.setAccessible(true);
            jacketField.setAccessible(true);
            
            // leftSleeve应该跟随leftArm的旋转
            Object leftSleeve = leftSleeveField.get(playerModel);
            if (leftSleeve instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) leftSleeve).setRotation(leftArmRotX, leftArmRotY, leftArmRotZ);
            }
            
            // rightSleeve应该跟随rightArm的旋转
            Object rightSleeve = rightSleeveField.get(playerModel);
            if (rightSleeve instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) rightSleeve).setRotation(rightArmRotX, rightArmRotY, rightArmRotZ);
            }
            
            // leftPants应该跟随leftLeg的旋转
            Object leftPants = leftPantsField.get(playerModel);
            if (leftPants instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) leftPants).setRotation(leftLegRotX, leftLegRotY, leftLegRotZ);
            }
            
            // rightPants应该跟随rightLeg的旋转
            Object rightPants = rightPantsField.get(playerModel);
            if (rightPants instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) rightPants).setRotation(rightLegRotX, rightLegRotY, rightLegRotZ);
            }
            
            // jacket应该跟随body的旋转（但身体的旋转通过 PoseStack 应用，所以这里设为0）
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) jacket).setRotation(0, 0, 0);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    /**
     * 计算旋转后整个模型最低点在Y方向上的偏移
     * 
     * 参考 HeightCalculator 的实现，使用8顶点方法计算旋转后每个部件的最低点。
     * 对于每个部件（身体、头部、腿部），计算其8个顶点旋转后的Y坐标，找出最小值。
     * 
     * Minecraft玩家模型尺寸（参考 HeightCalculator）：
     * - 头部：0.5×0.5×0.5格（立方体）
     * - 身体：0.75×0.5×0.25格（高度×宽度×深度）
     * - 腿部：0.75×0.25×0.25格（高度×宽度×深度）
     * 
     * 旋转中心：Y=0.375（身体和头部连接处，相对于模型中心）
     * 
     * @param bodyRotX 身体绕X轴旋转角度（度数）
     * @param bodyRotY 身体绕Y轴旋转角度（度数）
     * @param bodyRotZ 身体绕Z轴旋转角度（度数）
     * @param headRotX 头部绕X轴旋转角度（度数，相对于身体）
     * @param headRotY 头部绕Y轴旋转角度（度数，相对于身体）
     * @param headRotZ 头部绕Z轴旋转角度（度数，相对于身体）
     * @param rightLegRotX 右腿绕X轴旋转角度（度数，相对于身体）
     * @param rightLegRotY 右腿绕Y轴旋转角度（度数，相对于身体）
     * @param rightLegRotZ 右腿绕Z轴旋转角度（度数，相对于身体）
     * @param leftLegRotX 左腿绕X轴旋转角度（度数，相对于身体）
     * @param leftLegRotY 左腿绕Y轴旋转角度（度数，相对于身体）
     * @param leftLegRotZ 左腿绕Z轴旋转角度（度数，相对于身体）
     * @param scaleY Y轴缩放因子
     * @return 旋转后整个模型最低点相对于旋转中心的Y偏移
     */
    private float calculateTotalRotationYOffset(
            float bodyRotX, float bodyRotY, float bodyRotZ,
            float headRotX, float headRotY, float headRotZ,
            float rightLegRotX, float rightLegRotY, float rightLegRotZ,
            float leftLegRotX, float leftLegRotY, float leftLegRotZ,
            float scaleY) {
        
        LOGGER.debug("[calculateTotalRotationYOffset] 开始计算旋转后最低点偏移 (bodyRot: X={}°, Y={}°, Z={}°)", bodyRotX, bodyRotY, bodyRotZ);
        
        // 将度数转换为弧度
        float bodyRotXRad = (float) Math.toRadians(bodyRotX);
        float bodyRotYRad = (float) Math.toRadians(bodyRotY);
        float bodyRotZRad = (float) Math.toRadians(bodyRotZ);
        float headRotXRad = (float) Math.toRadians(headRotX);
        float headRotYRad = (float) Math.toRadians(headRotY);
        float headRotZRad = (float) Math.toRadians(headRotZ);
        float rightLegRotXRad = (float) Math.toRadians(rightLegRotX);
        float rightLegRotYRad = (float) Math.toRadians(rightLegRotY);
        float rightLegRotZRad = (float) Math.toRadians(rightLegRotZ);
        float leftLegRotXRad = (float) Math.toRadians(leftLegRotX);
        float leftLegRotYRad = (float) Math.toRadians(leftLegRotY);
        float leftLegRotZRad = (float) Math.toRadians(leftLegRotZ);
        
        // 计算头部和腿部在世界坐标系中的累积旋转（身体旋转 + 相对旋转）
        // 使用简单的角度相加（对于大多数情况这是合理的近似）
        float headWorldRotX = bodyRotXRad + headRotXRad;
        float headWorldRotY = bodyRotYRad + headRotYRad;
        float headWorldRotZ = bodyRotZRad + headRotZRad;
        float rightLegWorldRotX = bodyRotXRad + rightLegRotXRad;
        float rightLegWorldRotY = bodyRotYRad + rightLegRotYRad;
        float rightLegWorldRotZ = bodyRotZRad + rightLegRotZRad;
        float leftLegWorldRotX = bodyRotXRad + leftLegRotXRad;
        float leftLegWorldRotY = bodyRotYRad + leftLegRotYRad;
        float leftLegWorldRotZ = bodyRotZRad + leftLegRotZRad;
        
        float minY = Float.MAX_VALUE;
        
        // 1. 计算身体的最低点
        // 身体尺寸：0.75×0.5×0.25格（高度×宽度×深度）
        // 身体顶部在旋转中心（Y=0），身体高度0.75，所以身体中心在Y=-0.375*scaleY
        float bodyHeight = 0.75f * scaleY;
        float bodyWidth = 0.5f;
        float bodyDepth = 0.25f;
        
        // 身体顶部在旋转中心，身体中心在旋转中心下方0.375*scaleY
        float bodyCenterY = -0.375f * scaleY;  // 相对于旋转中心，身体中心在Y=-0.375*scaleY
        float halfBodyHeight = bodyHeight / 2.0f;
        float halfBodyWidth = bodyWidth / 2.0f;
        float halfBodyDepth = bodyDepth / 2.0f;
        
        // 身体的8个顶点（相对于旋转中心）
        float[][] bodyVertices = {
            {-halfBodyWidth, bodyCenterY - halfBodyHeight, -halfBodyDepth},  // 0
            { halfBodyWidth, bodyCenterY - halfBodyHeight, -halfBodyDepth},  // 1
            { halfBodyWidth, bodyCenterY + halfBodyHeight, -halfBodyDepth},  // 2
            {-halfBodyWidth, bodyCenterY + halfBodyHeight, -halfBodyDepth},  // 3
            {-halfBodyWidth, bodyCenterY - halfBodyHeight,  halfBodyDepth},  // 4
            { halfBodyWidth, bodyCenterY - halfBodyHeight,  halfBodyDepth},  // 5
            { halfBodyWidth, bodyCenterY + halfBodyHeight,  halfBodyDepth},  // 6
            {-halfBodyWidth, bodyCenterY + halfBodyHeight,  halfBodyDepth}   // 7
        };
        
        float bodyMinY = Float.MAX_VALUE;
        for (float[] vertex : bodyVertices) {
            float y = applyRotation(vertex[0], vertex[1], vertex[2], bodyRotXRad, bodyRotYRad, bodyRotZRad)[1];
            bodyMinY = Math.min(bodyMinY, y);
        }
        
        // 2. 计算头部的最低点
        // 头部尺寸：0.5×0.5×0.5格（立方体）
        // 头部底部在旋转中心（Y=0），头部高度0.5，所以头部中心在Y=0.25*scaleY
        float headHeight = 0.5f * scaleY;
        float headWidth = 0.5f;
        float headDepth = 0.5f;
        
        // 头部底部在旋转中心，头部中心在旋转中心上方0.25*scaleY
        float headCenterY = 0.25f * scaleY;
        float halfHeadHeight = headHeight / 2.0f;
        float halfHeadWidth = headWidth / 2.0f;
        float halfHeadDepth = headDepth / 2.0f;
        
        // 头部的8个顶点（相对于旋转中心）
        float[][] headVertices = {
            {-halfHeadWidth, headCenterY - halfHeadHeight, -halfHeadDepth},  // 0
            { halfHeadWidth, headCenterY - halfHeadHeight, -halfHeadDepth},  // 1
            { halfHeadWidth, headCenterY + halfHeadHeight, -halfHeadDepth},  // 2
            {-halfHeadWidth, headCenterY + halfHeadHeight, -halfHeadDepth},  // 3
            {-halfHeadWidth, headCenterY - halfHeadHeight,  halfHeadDepth},  // 4
            { halfHeadWidth, headCenterY - halfHeadHeight,  halfHeadDepth},  // 5
            { halfHeadWidth, headCenterY + halfHeadHeight,  halfHeadDepth},  // 6
            {-halfHeadWidth, headCenterY + halfHeadHeight,  halfHeadDepth}   // 7
        };
        
        float headMinY = Float.MAX_VALUE;
        for (float[] vertex : headVertices) {
            float y = applyRotation(vertex[0], vertex[1], vertex[2], headWorldRotX, headWorldRotY, headWorldRotZ)[1];
            headMinY = Math.min(headMinY, y);
        }
        
        // 3. 计算腿部的最低点（取两个腿部的最小值，即最低点）
        // 腿部尺寸：0.75×0.25×0.25格（高度×宽度×深度）
        // 腿部顶部在身体底部（Y=-0.75*scaleY），腿部高度0.75，所以腿部中心在Y=-1.125*scaleY
        float legHeight = 0.75f * scaleY;
        float legWidth = 0.25f;
        float legDepth = 0.25f;
        
        // 腿部顶部在身体底部（Y=-0.75*scaleY），腿部中心在旋转中心下方1.125*scaleY
        float legCenterY = -1.125f * scaleY;
        float halfLegHeight = legHeight / 2.0f;
        float halfLegWidth = legWidth / 2.0f;
        float halfLegDepth = legDepth / 2.0f;
        
        // 腿部的8个顶点（相对于旋转中心）
        float[][] legVertices = {
            {-halfLegWidth, legCenterY - halfLegHeight, -halfLegDepth},  // 0
            { halfLegWidth, legCenterY - halfLegHeight, -halfLegDepth},  // 1
            { halfLegWidth, legCenterY + halfLegHeight, -halfLegDepth},  // 2
            {-halfLegWidth, legCenterY + halfLegHeight, -halfLegDepth},  // 3
            {-halfLegWidth, legCenterY - halfLegHeight,  halfLegDepth},  // 4
            { halfLegWidth, legCenterY - halfLegHeight,  halfLegDepth},  // 5
            { halfLegWidth, legCenterY + halfLegHeight,  halfLegDepth},  // 6
            {-halfLegWidth, legCenterY + halfLegHeight,  halfLegDepth}   // 7
        };
        
        // 右腿的最低点
        float rightLegMinY = Float.MAX_VALUE;
        for (float[] vertex : legVertices) {
            float y = applyRotation(vertex[0], vertex[1], vertex[2], rightLegWorldRotX, rightLegWorldRotY, rightLegWorldRotZ)[1];
            rightLegMinY = Math.min(rightLegMinY, y);
        }
        
        // 左腿的最低点
        float leftLegMinY = Float.MAX_VALUE;
        for (float[] vertex : legVertices) {
            float y = applyRotation(vertex[0], vertex[1], vertex[2], leftLegWorldRotX, leftLegWorldRotY, leftLegWorldRotZ)[1];
            leftLegMinY = Math.min(leftLegMinY, y);
        }
        
        // 取两个腿部的最小值（最低点）
        float legMinY = Math.min(rightLegMinY, leftLegMinY);
        
        // 取所有部分的最低点作为最终的最低点
        minY = Math.min(bodyMinY, Math.min(headMinY, legMinY));
        
        LOGGER.debug("[calculateTotalRotationYOffset] 计算完成，返回: {} (身体:{}, 头部:{}, 腿部:{})", minY, bodyMinY, headMinY, legMinY);
        
        // 返回旋转后最低点相对于旋转中心的Y坐标
        return minY;
    }
    
    /**
     * 应用旋转矩阵到点
     * 
     * 参考 HeightCalculator 的实现，使用标准的 Z-Y-X 欧拉角旋转顺序（Roll-Yaw-Pitch）
     * 旋转矩阵：R = Rz(roll) × Ry(yaw) × Rx(pitch)
     * 计算顺序：先绕X轴旋转pitch，再绕Y轴旋转yaw，最后绕Z轴旋转roll
     * 
     * @param x 点的X坐标
     * @param y 点的Y坐标
     * @param z 点的Z坐标
     * @param rotX 绕X轴旋转角度（弧度，pitch）
     * @param rotY 绕Y轴旋转角度（弧度，yaw）
     * @param rotZ 绕Z轴旋转角度（弧度，roll）
     * @return 旋转后的点坐标 [x, y, z]
     */
    private float[] applyRotation(float x, float y, float z, float rotX, float rotY, float rotZ) {
        double cp = Math.cos(rotX);  // cos(pitch)
        double sp = Math.sin(rotX);  // sin(pitch)
        double cy = Math.cos(rotY);  // cos(yaw)
        double sy = Math.sin(rotY);  // sin(yaw)
        double cr = Math.cos(rotZ);  // cos(roll)
        double sr = Math.sin(rotZ);  // sin(roll)
        
        // 旋转矩阵 R = Rz(roll) × Ry(yaw) × Rx(pitch)
        // 计算顺序：先X，再Y，最后Z
        
        // 先绕X轴旋转pitch
        double x1 = x;
        double y1 = y * cp - z * sp;
        double z1 = y * sp + z * cp;
        
        // 再绕Y轴旋转yaw
        double x2 = x1 * cy + z1 * sy;
        double y2 = y1;
        double z2 = -x1 * sy + z1 * cy;
        
        // 最后绕Z轴旋转roll
        double x3 = x2 * cr - y2 * sr;
        double y3 = x2 * sr + y2 * cr;  // 这是世界坐标系的Y（垂直方向）
        double z3 = z2;
        
        return new float[]{(float) x3, (float) y3, (float) z3};
    }
    
    /**
     * 应用逆旋转矩阵到点（将旋转后的坐标转换回世界坐标系）
     * 
     * 对于旋转矩阵 R = Rx * Ry * Rz，逆矩阵是 R^-1 = Rz^-1 * Ry^-1 * Rx^-1
     * 对于欧拉角旋转，逆旋转是取反角度并按相反顺序应用
     * 
     * @param x 点的X坐标（在旋转后的坐标系中）
     * @param y 点的Y坐标（在旋转后的坐标系中）
     * @param z 点的Z坐标（在旋转后的坐标系中）
     * @param rotX 绕X轴旋转角度（弧度）
     * @param rotY 绕Y轴旋转角度（弧度）
     * @param rotZ 绕Z轴旋转角度（弧度）
     * @return 转换回世界坐标系后的点坐标 [x, y, z]
     */
    private float[] applyInverseRotation(float x, float y, float z, float rotX, float rotY, float rotZ) {
        // 逆旋转：先应用逆X旋转，再应用逆Y旋转，最后应用逆Z旋转
        // 对于旋转矩阵，逆旋转是取反角度
        float cosX = Mth.cos(-rotX);
        float sinX = Mth.sin(-rotX);
        float cosY = Mth.cos(-rotY);
        float sinY = Mth.sin(-rotY);
        float cosZ = Mth.cos(-rotZ);
        float sinZ = Mth.sin(-rotZ);
        
        // 逆旋转顺序：先逆X，再逆Y，最后逆Z
        // 先逆X轴旋转
        float x1 = x;
        float y1 = y * cosX - z * sinX;
        float z1 = y * sinX + z * cosX;
        // 再逆Y轴旋转
        float x2 = x1 * cosY + z1 * sinY;
        float y2 = y1;
        float z2 = -x1 * sinY + z1 * cosY;
        // 最后逆Z轴旋转
        float x3 = x2 * cosZ - y2 * sinZ;
        float y3 = x2 * sinZ + y2 * cosZ;
        float z3 = z2;
        
        return new float[]{x3, y3, z3};
    }
    
    /**
     * 组合两个旋转（先应用第一个旋转，再应用第二个旋转）
     * 
     * 对于旋转的组合：R_result = R_first * R_second
     * 我们需要计算组合后的旋转在世界坐标系中的欧拉角
     * 
     * @param firstRotX 第一个旋转的X角度（弧度）
     * @param firstRotY 第一个旋转的Y角度（弧度）
     * @param firstRotZ 第一个旋转的Z角度（弧度）
     * @param secondRotX 第二个旋转的X角度（弧度，相对于第一个旋转后的坐标系）
     * @param secondRotY 第二个旋转的Y角度（弧度，相对于第一个旋转后的坐标系）
     * @param secondRotZ 第二个旋转的Z角度（弧度，相对于第一个旋转后的坐标系）
     * @return 组合后的旋转角度 [x, y, z]（世界坐标系）
     */
    private float[] combineRotations(float firstRotX, float firstRotY, float firstRotZ,
                                     float secondRotX, float secondRotY, float secondRotZ) {
        // 对于小角度旋转，可以近似地相加
        // 但对于大角度旋转，需要使用旋转矩阵的组合
        
        // 方法1：简单相加（适用于小角度）
        // return new float[]{firstRotX + secondRotX, firstRotY + secondRotY, firstRotZ + secondRotZ};
        
        // 方法2：使用旋转矩阵的组合（更准确）
        // 先应用第一个旋转，再应用第二个旋转
        // 对于点(0, 1, 0)，先应用第一个旋转，再应用第二个旋转，得到最终方向
        // 然后从最终方向提取欧拉角
        
        // 简化：对于旋转的组合，我们使用旋转矩阵的组合
        // R_result = R_first * R_second
        // 但提取欧拉角比较复杂，这里使用近似方法
        
        // 对于大多数情况，简单相加是合理的近似
        // 但如果需要更精确，可以使用旋转矩阵的组合然后提取欧拉角
        return new float[]{firstRotX + secondRotX, firstRotY + secondRotY, firstRotZ + secondRotZ};
    }
    
    
    /**
     * 设置身体外层（jacket）的旋转
     * 用于在渲染时临时设置，因为身体的旋转通过 PoseStack 应用
     */
    private void setBodyOverlayRotation(float bodyRotX, float bodyRotY, float bodyRotZ) {
        try {
            java.lang.reflect.Field jacketField = PlayerModel.class.getDeclaredField("jacket");
            jacketField.setAccessible(true);
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) jacket).setRotation(bodyRotX, bodyRotY, bodyRotZ);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    /**
     * 渲染手臂外层部分（overlay layer）以支持多层皮肤
     * 
     * @param poseStack 变换矩阵栈
     * @param overlayVertexConsumer 外层顶点消费者
     * @param packedLight 光照信息
     * @param overlay 覆盖纹理
     */
    private void renderArmOverlayParts(PoseStack poseStack, 
                                      com.mojang.blaze3d.vertex.VertexConsumer overlayVertexConsumer, 
                                      int packedLight, 
                                      int overlay) {
        try {
            // 使用反射访问PlayerModel的外层部分（如果存在）
            java.lang.reflect.Field leftSleeveField = PlayerModel.class.getDeclaredField("leftSleeve");
            java.lang.reflect.Field rightSleeveField = PlayerModel.class.getDeclaredField("rightSleeve");
            
            leftSleeveField.setAccessible(true);
            rightSleeveField.setAccessible(true);
            
            // 渲染左袖子外层
            Object leftSleeve = leftSleeveField.get(playerModel);
            if (leftSleeve instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) leftSleeve).render(poseStack, overlayVertexConsumer, packedLight, overlay);
            }
            
            // 渲染右袖子外层
            Object rightSleeve = rightSleeveField.get(playerModel);
            if (rightSleeve instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) rightSleeve).render(poseStack, overlayVertexConsumer, packedLight, overlay);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    /**
     * 渲染身体和腿部外层部分（overlay layer）以支持多层皮肤
     * 
     * @param poseStack 变换矩阵栈
     * @param overlayVertexConsumer 外层顶点消费者
     * @param packedLight 光照信息
     * @param overlay 覆盖纹理
     */
    private void renderBodyLegOverlayParts(PoseStack poseStack, 
                                          com.mojang.blaze3d.vertex.VertexConsumer overlayVertexConsumer, 
                                          int packedLight, 
                                          int overlay) {
        try {
            // 使用反射访问PlayerModel的外层部分（如果存在）
            java.lang.reflect.Field leftPantsField = PlayerModel.class.getDeclaredField("leftPants");
            java.lang.reflect.Field rightPantsField = PlayerModel.class.getDeclaredField("rightPants");
            java.lang.reflect.Field jacketField = PlayerModel.class.getDeclaredField("jacket");
            
            leftPantsField.setAccessible(true);
            rightPantsField.setAccessible(true);
            jacketField.setAccessible(true);
            
            // 渲染夹克外层（身体外层）
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) jacket).render(poseStack, overlayVertexConsumer, packedLight, overlay);
            }
            
            // 渲染左腿外层
            Object leftPants = leftPantsField.get(playerModel);
            if (leftPants instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) leftPants).render(poseStack, overlayVertexConsumer, packedLight, overlay);
            }
            
            // 渲染右腿外层
            Object rightPants = rightPantsField.get(playerModel);
            if (rightPants instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) rightPants).render(poseStack, overlayVertexConsumer, packedLight, overlay);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return getSkinLocation(entity);
    }
}

