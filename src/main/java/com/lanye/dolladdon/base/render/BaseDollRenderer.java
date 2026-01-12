package com.lanye.dolladdon.base.render;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.compat.skinlayers3d.Doll3DSkinData;
import com.lanye.dolladdon.compat.skinlayers3d.Doll3DSkinUtil;
import com.lanye.dolladdon.compat.skinlayers3d.SkinLayersDetector;
import com.lanye.dolladdon.model.MeshRenderPartsInfo;
import com.lanye.dolladdon.model.PartTransformInfo;
import com.lanye.dolladdon.model.RenderContextInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 玩偶实体渲染器基类
 * 提供所有玩偶实体渲染器的共同功能
 */
public abstract class BaseDollRenderer<T extends BaseDollEntity> extends EntityRenderer<T> {
    // Logger removed - logging handled by Mixin
    protected final PlayerModel<Player> playerModel;
    
    // 3D皮肤层相关标志位，避免频繁输出日志
    private static boolean hasLoggedRenderCheck = false;
    private static boolean hasLogged3DRenderStart = false;
    private static boolean hasLoggedMeshCreation = false;
    private static boolean hasLoggedSkinCheck = false;
    private static boolean hasLoggedDistanceCheck = false;
    
    // 是否为细手臂模型（由子类设置）
    protected abstract boolean isThinArms();
    
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
            poseStack.translate(position[0], -position[1], position[2]);
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
        
        // 从姿态获取各部件的位置和缩放
        float[] headPosition = pose.getHeadPosition();
        float[] headScale = pose.getHeadScale();
        float[] hatPosition = pose.getHatPosition();
        float[] hatScale = pose.getHatScale();
        
        // hat 应该跟随 head 的缩放，所以使用 headScale 和 hatScale 的组合
        float[] hatCombinedScale = new float[]{
            headScale[0] * hatScale[0],
            headScale[1] * hatScale[1],
            headScale[2] * hatScale[2]
        };
        float[] bodyPosition = pose.getBodyPosition();
        float[] bodyScale = pose.getBodyScale();
        float[] rightArmPosition = pose.getRightArmPosition();
        float[] rightArmScale = pose.getRightArmScale();
        float[] leftArmPosition = pose.getLeftArmPosition();
        float[] leftArmScale = pose.getLeftArmScale();
        float[] rightLegPosition = pose.getRightLegPosition();
        float[] rightLegScale = pose.getRightLegScale();
        float[] leftLegPosition = pose.getLeftLegPosition();
        float[] leftLegScale = pose.getLeftLegScale();
        
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
            
            // 移回旋转中心
            poseStack.translate(0.0, -rotationCenterY, 0.0);
            // 在旋转后的坐标系中渲染身体、头部、手臂和腿部
            // 注意：头部和手臂的旋转值已经是相对于身体的，所以保持它们的旋转值
            playerModel.body.setRotation(0, 0, 0); // 确保身体不额外旋转（旋转已通过 PoseStack 应用）
            // 头部和手臂保持它们自己的相对旋转值（headRotX等已经在上面设置）
            renderPartWithTransform(poseStack, playerModel.body, baseVertexConsumer, packedLight, overlay, bodyPosition, bodyScale);
            renderPartWithTransform(poseStack, playerModel.head, baseVertexConsumer, packedLight, overlay, headPosition, headScale);
            renderPartWithTransform(poseStack, playerModel.rightArm, baseVertexConsumer, packedLight, overlay, rightArmPosition, rightArmScale);
            renderPartWithTransform(poseStack, playerModel.leftArm, baseVertexConsumer, packedLight, overlay, leftArmPosition, leftArmScale);
            renderPartWithTransform(poseStack, playerModel.rightLeg, baseVertexConsumer, packedLight, overlay, rightLegPosition, rightLegScale);
            renderPartWithTransform(poseStack, playerModel.leftLeg, baseVertexConsumer, packedLight, overlay, leftLegPosition, leftLegScale);
            poseStack.popPose();
        } else {
            // 没有身体旋转时，正常渲染
            playerModel.body.setRotation(0, 0, 0);
            renderPartWithTransform(poseStack, playerModel.body, baseVertexConsumer, packedLight, overlay, bodyPosition, bodyScale);
            renderPartWithTransform(poseStack, playerModel.head, baseVertexConsumer, packedLight, overlay, headPosition, headScale);
            renderPartWithTransform(poseStack, playerModel.rightArm, baseVertexConsumer, packedLight, overlay, rightArmPosition, rightArmScale);
            renderPartWithTransform(poseStack, playerModel.leftArm, baseVertexConsumer, packedLight, overlay, leftArmPosition, leftArmScale);
            renderPartWithTransform(poseStack, playerModel.rightLeg, baseVertexConsumer, packedLight, overlay, rightLegPosition, rightLegScale);
            renderPartWithTransform(poseStack, playerModel.leftLeg, baseVertexConsumer, packedLight, overlay, leftLegPosition, leftLegScale);
        }
        
        // 第二步：渲染外层（overlay layer）- 使用半透明渲染以正确显示多层皮肤
        var overlayVertexConsumer = bufferSource.getBuffer(translucentRenderType);
        
        // 检查是否使用3D皮肤层渲染
        boolean modLoaded = SkinLayersDetector.IS_3D_SKIN_LAYERS_LOADED;
        boolean apiAvailable = Doll3DSkinUtil.isAvailable();
        boolean inRange = shouldUse3DSkinLayers(entity);
        boolean use3DSkinLayers = modLoaded && apiAvailable && inRange;
        
        // 只在第一次渲染时记录检查结果，避免每帧都输出日志导致卡顿
        if (!hasLoggedRenderCheck) {
            // Debug logging handled by Mixin
            hasLoggedRenderCheck = true;
        }
        
        // 第三步：延迟渲染3D网格 - 在所有其他渲染完成后执行，确保不会被遮挡
        boolean willRender3DLast = false;
        
        if (use3DSkinLayers) {
            // 预加载3D数据，但暂时不渲染
            if (!hasLogged3DRenderStart) {
                // Debug logging handled by Mixin
                hasLogged3DRenderStart = true;
            }
            
            // 预加载3D皮肤数据，确保在需要时可用
            var preloadResult = Doll3DSkinUtil.setup3dLayers(skinLocation, isThinArms());
            if (preloadResult != null) {
                willRender3DLast = true;
                // Debug logging handled by Mixin
            } else {
                // Warning logging handled by Mixin
            }
        }
        
        // 如果有身体旋转，使用 PoseStack 在身体旋转中心应用旋转，然后渲染所有外层部分
        if (!use3DSkinLayers) {
            // 使用默认2D渲染
            if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
                poseStack.pushPose();
                
                // 移动到身体的旋转中心（身体和头连接处，Y坐标约为0.375）
                float rotationCenterY = 0.375f;
                poseStack.translate(0.0, rotationCenterY, 0.0);
                
                // 应用身体旋转（只在这里应用，不在 setRotation 中设置）
                poseStack.mulPose(Axis.XP.rotation(bodyRotX));
                poseStack.mulPose(Axis.YP.rotation(bodyRotY));
                poseStack.mulPose(Axis.ZP.rotation(bodyRotZ));
                
                // 移回旋转中心
                poseStack.translate(0.0, -rotationCenterY, 0.0);
                // 在旋转后的坐标系中 渲染所有外层部分
                // hat层（头发外层），使用 headScale 和 hatScale 的组合
                renderPartWithTransform(poseStack, playerModel.hat, overlayVertexConsumer, packedLight, overlay, hatPosition, hatCombinedScale);
                // 手臂外层（保持它们自己的旋转值）
                renderArmOverlayParts(poseStack, overlayVertexConsumer, packedLight, overlay, rightArmPosition, rightArmScale, leftArmPosition, leftArmScale);
                // 身体和腿部外层（jacket 的旋转设为0）
                setBodyOverlayRotation(0, 0, 0); // 确保身体外层不额外旋转
                renderBodyLegOverlayParts(poseStack, overlayVertexConsumer, packedLight, overlay, bodyPosition, bodyScale, rightLegPosition, rightLegScale, leftLegPosition, leftLegScale);
                poseStack.popPose();
            } else {
                // 没有身体旋转时，正常渲染
                renderPartWithTransform(poseStack, playerModel.hat, overlayVertexConsumer, packedLight, overlay, hatPosition, hatCombinedScale);
                renderArmOverlayParts(poseStack, overlayVertexConsumer, packedLight, overlay, rightArmPosition, rightArmScale, leftArmPosition, leftArmScale);
                setBodyOverlayRotation(0, 0, 0);
                renderBodyLegOverlayParts(poseStack, overlayVertexConsumer, packedLight, overlay, bodyPosition, bodyScale, rightLegPosition, rightLegScale, leftLegPosition, leftLegScale);
            }
        }
        
        poseStack.popPose();
        
        // 第四步：最后的3D网格渲染 - 在所有其他渲染完成后执行，确保不会被遮挡
        if (willRender3DLast) {
            // 只在第一次渲染时记录日志，避免每帧都输出导致卡顿
            if (!hasLoggedMeshCreation) {
                // Debug logging handled by Mixin
            }
            try {
                // 为3D网格渲染应用玩偶的基础变换
                poseStack.pushPose();
                applyBaseDollTransforms(poseStack, entity, partialTick);
                
                // 使用新的方法签名，封装参数为 info 类
                RenderContextInfo overlayContextInfo = new RenderContextInfo(packedLight, overlay, bufferSource, translucentRenderType);
                MeshRenderPartsInfo partsInfo = MeshRenderPartsInfo.of(
                    hatPosition, hatCombinedScale, hatRot,
                    rightArmPosition, rightArmScale, rightArmRot,
                    leftArmPosition, leftArmScale, leftArmRot,
                    bodyPosition, bodyScale, bodyRot,
                    rightLegPosition, rightLegScale, rightLegRot,
                    leftLegPosition, leftLegScale, leftLegRot
                );
                renderOverlayWith3DSkinLayers(poseStack, overlayContextInfo, skinLocation,
                    bodyRotX, bodyRotY, bodyRotZ, partsInfo);
                
                poseStack.popPose();
                // 只在第一次渲染时记录日志
                if (!hasLoggedMeshCreation) {
                    // Debug logging handled by Mixin"✅ 延迟3D网格渲染完成 - 这应该在最上层显示");
                    hasLoggedMeshCreation = true; // 标记已完成一次完整渲染
                }
            } catch (Exception e) {
                // Error logging handled by Mixin
            }
        }
        
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
    
    /**
     * 渲染单个部件，应用位置和缩放
     * @param poseStack 变换矩阵栈
     * @param part 要渲染的部件
     * @param vertexConsumer 顶点消费者
     * @param packedLight 光照信息
     * @param overlay 覆盖纹理
     * @param position 位置偏移 [x, y, z]
     * @param scale 缩放 [x, y, z]
     */
    private void renderPartWithTransform(PoseStack poseStack,
                                         net.minecraft.client.model.geom.ModelPart part,
                                         com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer,
                                         int packedLight,
                                         int overlay,
                                         float[] position,
                                         float[] scale) {
        poseStack.pushPose();
        
        // 应用位置偏移（Y轴取反，正数向上）
        if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
            poseStack.translate(position[0], -position[1], position[2]);
        }
        
        // 应用缩放
        if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
            poseStack.scale(scale[0], scale[1], scale[2]);
        }
        
        // 渲染部件
        part.render(poseStack, vertexConsumer, packedLight, overlay);
        
        poseStack.popPose();
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
     * @param rightArmPosition 右臂位置偏移
     * @param rightArmScale 右臂缩放
     * @param leftArmPosition 左臂位置偏移
     * @param leftArmScale 左臂缩放
     */
    private void renderArmOverlayParts(PoseStack poseStack, 
                                      com.mojang.blaze3d.vertex.VertexConsumer overlayVertexConsumer, 
                                      int packedLight, 
                                      int overlay,
                                      float[] rightArmPosition,
                                      float[] rightArmScale,
                                      float[] leftArmPosition,
                                      float[] leftArmScale) {
        try {
            // 使用反射访问PlayerModel的外层部分（如果存在）
            java.lang.reflect.Field leftSleeveField = PlayerModel.class.getDeclaredField("leftSleeve");
            java.lang.reflect.Field rightSleeveField = PlayerModel.class.getDeclaredField("rightSleeve");
            
            leftSleeveField.setAccessible(true);
            rightSleeveField.setAccessible(true);
            
            // 渲染左袖子外层
            Object leftSleeve = leftSleeveField.get(playerModel);
            if (leftSleeve instanceof net.minecraft.client.model.geom.ModelPart) {
                renderPartWithTransform(poseStack, (net.minecraft.client.model.geom.ModelPart) leftSleeve, overlayVertexConsumer, packedLight, overlay, leftArmPosition, leftArmScale);
            }
            
            // 渲染右袖子外层
            Object rightSleeve = rightSleeveField.get(playerModel);
            if (rightSleeve instanceof net.minecraft.client.model.geom.ModelPart) {
                renderPartWithTransform(poseStack, (net.minecraft.client.model.geom.ModelPart) rightSleeve, overlayVertexConsumer, packedLight, overlay, rightArmPosition, rightArmScale);
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
     * @param bodyPosition 身体位置偏移
     * @param bodyScale 身体缩放
     * @param rightLegPosition 右腿位置偏移
     * @param rightLegScale 右腿缩放
     * @param leftLegPosition 左腿位置偏移
     * @param leftLegScale 左腿缩放
     */
    private void renderBodyLegOverlayParts(PoseStack poseStack, 
                                          com.mojang.blaze3d.vertex.VertexConsumer overlayVertexConsumer, 
                                          int packedLight, 
                                          int overlay,
                                          float[] bodyPosition,
                                          float[] bodyScale,
                                          float[] rightLegPosition,
                                          float[] rightLegScale,
                                          float[] leftLegPosition,
                                          float[] leftLegScale) {
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
                renderPartWithTransform(poseStack, (net.minecraft.client.model.geom.ModelPart) jacket, overlayVertexConsumer, packedLight, overlay, bodyPosition, bodyScale);
            }
            
            // 渲染左腿外层
            Object leftPants = leftPantsField.get(playerModel);
            if (leftPants instanceof net.minecraft.client.model.geom.ModelPart) {
                renderPartWithTransform(poseStack, (net.minecraft.client.model.geom.ModelPart) leftPants, overlayVertexConsumer, packedLight, overlay, leftLegPosition, leftLegScale);
            }
            
            // 渲染右腿外层
            Object rightPants = rightPantsField.get(playerModel);
            if (rightPants instanceof net.minecraft.client.model.geom.ModelPart) {
                renderPartWithTransform(poseStack, (net.minecraft.client.model.geom.ModelPart) rightPants, overlayVertexConsumer, packedLight, overlay, rightLegPosition, rightLegScale);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    /**
     * 检查是否应该使用3D皮肤层渲染
     * 实现距离检测（12格LOD）
     */
    private boolean shouldUse3DSkinLayers(T entity) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gameRenderer == null ||
            client.gameRenderer.getMainCamera() == null) {
            // Debug logging handled by Mixin"客户端未初始化，无法使用3D渲染");
            return false;
        }

        // 获取皮肤路径进行兼容性检查
        ResourceLocation skinLocation = getSkinLocation(entity);
        if (skinLocation == null) {
            // Debug logging handled by Mixin"皮肤路径为空，无法使用3D渲染");
            return false;
        }

        // 不再预先检查皮肤路径，让3D渲染系统自己验证皮肤格式
        // 3D皮肤层mod会检查皮肤是否为有效的64x64格式，如果不是会自动回退到2D渲染
        // 只在第一次调用时记录日志，避免每帧都输出导致卡顿
        if (!hasLoggedSkinCheck) {
            // Info logging handled by Mixin
            hasLoggedSkinCheck = true;
        }

        // 计算距离：应该计算到玩家的距离，而不是到相机的距离
        // 3D皮肤层的LOD是基于到玩家的距离
        var player = client.player;
        if (player == null) {
            // 只在第一次调用时记录日志
            if (!hasLoggedDistanceCheck) {
                // Debug logging handled by Mixin"玩家对象为空，无法使用3D渲染");
                hasLoggedDistanceCheck = true;
            }
            return false;
        }

        var playerPos = player.position();
        var entityPos = entity.position();

        double distanceSq = entity.distanceToSqr(playerPos.x, playerPos.y, playerPos.z);
        double distance = Math.sqrt(distanceSq);
        boolean shouldUse = distanceSq <= 12.0 * 12.0;

        // 只在第一次调用时记录距离检测日志，避免每帧都输出导致卡顿
        if (!hasLoggedDistanceCheck) {
            // Debug logging handled by Mixin
            hasLoggedDistanceCheck = true;
        }

        // 12格以内且为标准Minecraft皮肤时使用3D渲染
        return shouldUse;
    }
    
    /**
     * 为3D网格渲染应用玩偶的基础变换
     * 确保3D网格和普通模型使用相同的坐标系统和缩放
     * 必须与主渲染方法中的变换顺序完全一致
     */
    private void applyBaseDollTransforms(PoseStack poseStack, T entity, float partialTick) {
        // 获取玩偶的pose数据
        var pose = entity.getCurrentPose();
        if (pose == null) {
            pose = com.lanye.dolladdon.api.pose.SimpleDollPose.createDefaultStandingPose();
        }

        // 第一步：应用实体旋转（与主渲染方法一致）
        float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(xRot));

        // 第二步：应用Y偏移和模型缩放（与主渲染方法一致）
        float modelScale = 0.5F;
        float[] scale = pose.getScale();
        float yOffset = 0.75f * scale[1];
        poseStack.translate(0.0, yOffset, 0.0);
        poseStack.scale(-modelScale, -modelScale, modelScale);

        // 第三步：应用姿态的位置和缩放（与主渲染方法一致）
        float[] position = pose.getPosition();
        if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
            poseStack.translate(position[0], -position[1], position[2]);
        }
        if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
            poseStack.scale(scale[0], scale[1], scale[2]);
        }

        // 注意：身体旋转在renderOverlayWith3DSkinLayers方法中处理，不在这里处理
    }
    
    /**
     * 使用3D皮肤层渲染外层（新方法签名，使用 MeshRenderPartsInfo）
     */
    private void renderOverlayWith3DSkinLayers(PoseStack matrixStack,
                                               RenderContextInfo contextInfo,
                                               ResourceLocation skinLocation,
                                               float bodyRotX, float bodyRotY, float bodyRotZ,
                                               MeshRenderPartsInfo partsInfo) {
        renderOverlayWith3DSkinLayersInternal(matrixStack, contextInfo, skinLocation, 
            bodyRotX, bodyRotY, bodyRotZ, partsInfo);
    }
    
    /**
     * 使用3D皮肤层渲染外层（内部实现）
     */
    private void renderOverlayWith3DSkinLayersInternal(PoseStack matrixStack,
                                                       RenderContextInfo contextInfo,
                                                       ResourceLocation skinLocation,
                                                       float bodyRotX, float bodyRotY, float bodyRotZ,
                                                       MeshRenderPartsInfo partsInfo) {
        if (!hasLogged3DRenderStart) {
            // Debug logging handled by Mixin
            hasLogged3DRenderStart = true;
        }

        // 获取或创建3D皮肤数据
        if (!hasLoggedMeshCreation) {
            // Debug logging handled by Mixin"获取3D皮肤数据...");
        }
        Doll3DSkinData skinData = Doll3DSkinUtil.setup3dLayers(skinLocation, isThinArms());
        if (skinData == null) {
            // Warning logging handled by Mixin
            return;
        }
        if (!skinData.hasValidData()) {
            // Warning logging handled by Mixin
            return;
        }
        
        if (!hasLoggedMeshCreation) {
            // Debug logging handled by Mixin"✓ 3D皮肤数据有效，开始渲染各个部位");
        }
        
        // 重要优化：批量初始化所有mesh（copyFrom和setVisible），避免每个部件都单独处理
        initializeAllMeshes(skinData);
        
        // 重要：在所有3D部件渲染前统一禁用深度测试，避免每个部件都处理
        try {
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.depthMask(false);
        } catch (Exception e) {
            if (!hasLoggedMeshCreation) {
                // Warning logging handled by Mixin
            }
        }
        
        // 确保纹理已绑定（只绑定一次，所有部件共享）
        if (skinLocation != null) {
            try {
                com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0, skinLocation);
            } catch (Exception texEx) {
                // 静默失败，纹理可能已经绑定
            }
        }
        
        try {
            // 处理身体旋转 - 使用辅助方法（它会自动处理有无旋转的情况）
            applyBodyRotationFor3D(matrixStack, bodyRotX, bodyRotY, bodyRotZ, () -> {
                // 批量渲染所有部位的3D网格
                renderAll3DMeshParts(matrixStack, skinData, contextInfo, partsInfo);
            });
        } finally {
            // 重要：在所有3D部件渲染后统一恢复深度测试
            try {
                com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
                com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
            } catch (Exception e) {
                if (!hasLoggedMeshCreation) {
                    // Warning logging handled by Mixin
                }
            }
        }
    }
    
    /**
     * 应用身体旋转（用于3D渲染）
     */
    private void applyBodyRotationFor3D(PoseStack matrixStack, float bodyRotX, float bodyRotY, float bodyRotZ, Runnable renderAction) {
        if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
            matrixStack.pushPose();
            
            // 移动到身体的旋转中心（身体和头连接处，Y坐标约为0.375）
            float rotationCenterY = 0.375f;
            matrixStack.translate(0.0, rotationCenterY, 0.0);
            
            // 应用身体旋转
            matrixStack.mulPose(Axis.XP.rotation(bodyRotX));
            matrixStack.mulPose(Axis.YP.rotation(bodyRotY));
            matrixStack.mulPose(Axis.ZP.rotation(bodyRotZ));
            
            // 移回旋转中心
            matrixStack.translate(0.0, -rotationCenterY, 0.0);
            
            renderAction.run();
            
            matrixStack.popPose();
        } else {
            renderAction.run();
        }
    }
    
    /**
     * 批量渲染所有部位的3D网格
     */
    private void renderAll3DMeshParts(PoseStack matrixStack,
                                     Doll3DSkinData skinData,
                                     RenderContextInfo contextInfo,
                                     MeshRenderPartsInfo partsInfo) {
        // 使用批量方法渲染各个部位
        render3DMeshPartFast(matrixStack, com.lanye.dolladdon.model.MeshRenderInfo.of(
            playerModel.hat, skinData.getHeadMesh(), "HEAD",
            contextInfo.getLight(), contextInfo.getOverlay(), contextInfo.getBufferSource(), contextInfo.getRenderType(),
            partsInfo.getHat().getPositionInternal(),
            partsInfo.getHat().getScaleInternal(),
            partsInfo.getHat().getRotationInternal()
        ));
        
        render3DMeshPartFast(matrixStack, com.lanye.dolladdon.model.MeshRenderInfo.of(
            playerModel.leftArm, skinData.getLeftArmMesh(),
            isThinArms() ? "LEFT_ARM_SLIM" : "LEFT_ARM",
            contextInfo.getLight(), contextInfo.getOverlay(), contextInfo.getBufferSource(), contextInfo.getRenderType(),
            partsInfo.getLeftArm().getPositionInternal(),
            partsInfo.getLeftArm().getScaleInternal(),
            partsInfo.getLeftArm().getRotationInternal()
        ));
        
        render3DMeshPartFast(matrixStack, com.lanye.dolladdon.model.MeshRenderInfo.of(
            playerModel.rightArm, skinData.getRightArmMesh(),
            isThinArms() ? "RIGHT_ARM_SLIM" : "RIGHT_ARM",
            contextInfo.getLight(), contextInfo.getOverlay(), contextInfo.getBufferSource(), contextInfo.getRenderType(),
            partsInfo.getRightArm().getPositionInternal(),
            partsInfo.getRightArm().getScaleInternal(),
            partsInfo.getRightArm().getRotationInternal()
        ));
        
        render3DMeshPartFast(matrixStack, com.lanye.dolladdon.model.MeshRenderInfo.of(
            playerModel.body, skinData.getTorsoMesh(), "BODY",
            contextInfo.getLight(), contextInfo.getOverlay(), contextInfo.getBufferSource(), contextInfo.getRenderType(),
            partsInfo.getBody().getPositionInternal(),
            partsInfo.getBody().getScaleInternal(),
            partsInfo.getBody().getRotationInternal()
        ));
        
        render3DMeshPartFast(matrixStack, com.lanye.dolladdon.model.MeshRenderInfo.of(
            playerModel.leftLeg, skinData.getLeftLegMesh(), "LEFT_LEG",
            contextInfo.getLight(), contextInfo.getOverlay(), contextInfo.getBufferSource(), contextInfo.getRenderType(),
            partsInfo.getLeftLeg().getPositionInternal(),
            partsInfo.getLeftLeg().getScaleInternal(),
            partsInfo.getLeftLeg().getRotationInternal()
        ));
        
        render3DMeshPartFast(matrixStack, com.lanye.dolladdon.model.MeshRenderInfo.of(
            playerModel.rightLeg, skinData.getRightLegMesh(), "RIGHT_LEG",
            contextInfo.getLight(), contextInfo.getOverlay(), contextInfo.getBufferSource(), contextInfo.getRenderType(),
            partsInfo.getRightLeg().getPositionInternal(),
            partsInfo.getRightLeg().getScaleInternal(),
            partsInfo.getRightLeg().getRotationInternal()
        ));
    }
    
    /**
     * 批量初始化所有mesh（copyFrom和setVisible），只执行一次
     */
    private void initializeAllMeshes(Doll3DSkinData skinData) {
        // 初始化render方法缓存（如果还没初始化）
        if (!renderMethodCacheInitialized && skinData.getHeadMesh() != null) {
            initializeRenderMethodCache(skinData.getHeadMesh());
        }
        
        // 批量初始化所有mesh
        initializeMesh(skinData.getHeadMesh(), playerModel.hat, "HEAD");
        initializeMesh(skinData.getLeftArmMesh(), playerModel.leftArm, isThinArms() ? "LEFT_ARM_SLIM" : "LEFT_ARM");
        initializeMesh(skinData.getRightArmMesh(), playerModel.rightArm, isThinArms() ? "RIGHT_ARM_SLIM" : "RIGHT_ARM");
        initializeMesh(skinData.getTorsoMesh(), playerModel.body, "BODY");
        initializeMesh(skinData.getLeftLegMesh(), playerModel.leftLeg, "LEFT_LEG");
        initializeMesh(skinData.getRightLegMesh(), playerModel.rightLeg, "RIGHT_LEG");
    }
    
    // 反射方法缓存
    private static java.lang.reflect.Method cachedCopyFromMethod;
    private static java.lang.reflect.Method cachedSetVisibleMethod;
    private static java.lang.reflect.Method cachedRenderMethod;
    private static boolean renderMethodCacheInitialized = false;
    
    /**
     * 初始化render方法缓存
     */
    private void initializeRenderMethodCache(Object mesh) {
        if (renderMethodCacheInitialized) {
            return;
        }
        
        try {
            Class<?> meshClass = mesh.getClass();
            cachedRenderMethod = meshClass.getMethod("render", 
                PoseStack.class, 
                com.mojang.blaze3d.vertex.VertexConsumer.class,
                int.class, int.class, float.class, float.class, float.class, float.class);
            cachedRenderMethod.setAccessible(true);
            renderMethodCacheInitialized = true;
        } catch (Exception e) {
            // Warning logging handled by Mixin
        }
    }
    
    /**
     * 初始化单个mesh（copyFrom和setVisible）
     */
    private void initializeMesh(Object mesh, net.minecraft.client.model.geom.ModelPart modelPart, String name) {
        if (mesh == null) {
            return;
        }

        try {
            // 使用缓存的方法，避免每帧都查找
            if (cachedCopyFromMethod == null) {
                cachedCopyFromMethod = mesh.getClass().getMethod("copyFrom", net.minecraft.client.model.geom.ModelPart.class);
            }
            cachedCopyFromMethod.invoke(mesh, modelPart);
            
            if (cachedSetVisibleMethod == null) {
                cachedSetVisibleMethod = mesh.getClass().getMethod("setVisible", boolean.class);
            }
            cachedSetVisibleMethod.invoke(mesh, true);
        } catch (Exception e) {
            if (!hasLoggedMeshCreation) {
                // Warning logging handled by Mixin
            }
        }
    }
    
    /**
     * 快速渲染3D网格部件（使用缓存的方法）
     */
    private void render3DMeshPartFast(PoseStack matrixStack, com.lanye.dolladdon.model.MeshRenderInfo meshInfo) {
        Object mesh = meshInfo.getMesh();
        if (mesh == null) {
            return;
        }
        
        try {
            // 应用变换
            matrixStack.pushPose();
            
            PartTransformInfo transform = meshInfo.getTransformInfo();
            float[] position = transform.getPositionInternal();
            float[] scale = transform.getScaleInternal();
            float[] rotation = transform.getRotationInternal();
            
            // 应用位置偏移
            if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
                matrixStack.translate(position[0], -position[1], position[2]);
            }
            
            // 应用缩放
            if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
                matrixStack.scale(scale[0], scale[1], scale[2]);
            }
            
            // 应用旋转（如果有）
            if (rotation != null && (rotation[0] != 0.0f || rotation[1] != 0.0f || rotation[2] != 0.0f)) {
                matrixStack.mulPose(Axis.XP.rotation(rotation[0]));
                matrixStack.mulPose(Axis.YP.rotation(rotation[1]));
                matrixStack.mulPose(Axis.ZP.rotation(rotation[2]));
            }
            
            // 使用缓存的方法调用render
            if (cachedRenderMethod == null) {
                initializeRenderMethodCache(mesh);
            }
            
            if (cachedRenderMethod != null) {
                RenderContextInfo contextInfo = meshInfo.getContextInfo();
                cachedRenderMethod.invoke(mesh, 
                    matrixStack,
                    contextInfo.getVertexConsumer(),
                    contextInfo.getLight(),
                    contextInfo.getOverlay(),
                    1.0f, 1.0f, 1.0f, 1.0f // r, g, b, a
                );
            }
            
            matrixStack.popPose();
        } catch (Exception e) {
            if (!hasLoggedMeshCreation) {
                // Warning logging handled by Mixin
            }
        }
    }
    
    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return getSkinLocation(entity);
    }
}

