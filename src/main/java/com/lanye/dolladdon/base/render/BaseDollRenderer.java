package com.lanye.dolladdon.base.render;

import com.lanye.dolladdon.PlayerDollAddonClient;
import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import com.lanye.dolladdon.util.skinlayers3d.Doll3DSkinData;
import com.lanye.dolladdon.util.skinlayers3d.Doll3DSkinUtil;
import com.lanye.dolladdon.util.skinlayers3d.SkinLayersLogger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Method;
/**
 * 玩偶实体渲染器基类
 * 提供所有玩偶实体渲染器的共同功能
 */
public abstract class BaseDollRenderer<T extends BaseDollEntity> extends EntityRenderer<T> {
    protected final PlayerEntityModel<PlayerEntity> playerModel;
    private final boolean thinArms;  // 是否为细手臂模型
    
    protected BaseDollRenderer(EntityRendererFactory.Context context, PlayerEntityModel<PlayerEntity> playerModel) {
        super(context);
        this.playerModel = playerModel;
        // 通过反射获取slim字段来判断是否为细手臂
        this.thinArms = isThinArmsModel(playerModel);
    }
    
    /**
     * 判断模型是否为细手臂模型
     */
    private boolean isThinArmsModel(PlayerEntityModel<PlayerEntity> model) {
        try {
            java.lang.reflect.Field slimField = PlayerEntityModel.class.getDeclaredField("slim");
            slimField.setAccessible(true);
            return slimField.getBoolean(model);
        } catch (Exception e) {
            // 如果无法访问字段，默认使用粗手臂
            return false;
        }
    }
    
    /**
     * 获取皮肤资源位置
     * @param entity 实体
     * @return 皮肤资源位置
     */
    protected abstract Identifier getSkinLocation(T entity);
    
    @Override
    public void render(T entity, float entityYaw, float partialTick, 
                      MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {
        matrixStack.push();
        
        // 应用旋转
        float yRot = MathHelper.lerp(partialTick, entity.prevYaw, entity.getYaw());
        float xRot = MathHelper.lerp(partialTick, entity.prevPitch, entity.getPitch());
        
        matrixStack.multiply(new Quaternionf().rotateY((float) Math.toRadians(180.0F - yRot)));
        matrixStack.multiply(new Quaternionf().rotateX((float) Math.toRadians(xRot)));
        
        float modelScale = 0.5F; 
        
        // 获取皮肤位置（由子类实现）
        Identifier skinLocation = getSkinLocation(entity);
        
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
        
        matrixStack.translate(0.0, yOffset, 0.0);
        
        // 应用缩放和翻转
        matrixStack.scale(-modelScale, -modelScale, modelScale);
        
        // 应用姿态的位置和大小
        float[] position = pose.getPosition();
        if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
            matrixStack.translate(position[0], -position[1], position[2]);
        }
        if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
            matrixStack.scale(scale[0], scale[1], scale[2]);
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
        playerModel.head.setAngles(headRotX, headRotY, headRotZ);
        playerModel.hat.setAngles(hatRotX, hatRotY, hatRotZ);
        // 注意：身体的旋转通过 MatrixStack 应用，不在这里设置，避免双重旋转
        playerModel.rightArm.setAngles(rightArmRotX, rightArmRotY, rightArmRotZ);
        playerModel.leftArm.setAngles(leftArmRotX, leftArmRotY, leftArmRotZ);
        playerModel.rightLeg.setAngles(rightLegRotX, rightLegRotY, rightLegRotZ);
        playerModel.leftLeg.setAngles(leftLegRotX, leftLegRotY, leftLegRotZ);
        
        // 同时设置外层部分的旋转，使它们跟随基础部分的动作
        // 注意：身体的旋转通过 PoseStack 应用，所以 jacket 的旋转也设为0
        setOverlayPartsRotation(0, 0, 0, // 身体旋转通过 PoseStack 应用
                               leftArmRotX, leftArmRotY, leftArmRotZ,
                               rightArmRotX, rightArmRotY, rightArmRotZ,
                               leftLegRotX, leftLegRotY, leftLegRotZ,
                               rightLegRotX, rightLegRotY, rightLegRotZ);
        
        // 获取渲染类型
        var cutoutRenderType = net.minecraft.client.render.RenderLayer.getEntityCutoutNoCull(skinLocation);
        var translucentRenderType = net.minecraft.client.render.RenderLayer.getEntityTranslucent(skinLocation);
        int overlay = net.minecraft.client.render.OverlayTexture.DEFAULT_UV;
        
        // 第一步：渲染基础层（base layer）
        var baseVertexConsumer = vertexConsumerProvider.getBuffer(cutoutRenderType);
        
        // 如果有身体旋转，使用 MatrixStack 在身体旋转中心应用旋转，然后渲染身体、头部、手臂和腿部
        if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
            matrixStack.push();
            
            // 移动到身体的旋转中心（身体和头连接处，Y坐标约为0.375）
            float rotationCenterY = 0.375f;
            matrixStack.translate(0.0, rotationCenterY, 0.0);
            
            // 应用身体旋转（只在这里应用，不在 setRotation 中设置）
            matrixStack.multiply(new Quaternionf().rotateX(bodyRotX));
            matrixStack.multiply(new Quaternionf().rotateY(bodyRotY));
            matrixStack.multiply(new Quaternionf().rotateZ(bodyRotZ));
            
            // 移回旋转中心
            matrixStack.translate(0.0, -rotationCenterY, 0.0);
            // 在旋转后的坐标系中渲染身体、头部、手臂和腿部
            // 注意：头部和手臂的旋转值已经是相对于身体的，所以保持它们的旋转值
            playerModel.body.setAngles(0, 0, 0); // 确保身体不额外旋转（旋转已通过 MatrixStack 应用）
            // 头部和手臂保持它们自己的相对旋转值（headRotX等已经在上面设置）
            renderPartWithTransform(matrixStack, playerModel.body, baseVertexConsumer, light, overlay, bodyPosition, bodyScale);
            renderPartWithTransform(matrixStack, playerModel.head, baseVertexConsumer, light, overlay, headPosition, headScale);
            renderPartWithTransform(matrixStack, playerModel.rightArm, baseVertexConsumer, light, overlay, rightArmPosition, rightArmScale);
            renderPartWithTransform(matrixStack, playerModel.leftArm, baseVertexConsumer, light, overlay, leftArmPosition, leftArmScale);
            renderPartWithTransform(matrixStack, playerModel.rightLeg, baseVertexConsumer, light, overlay, rightLegPosition, rightLegScale);
            renderPartWithTransform(matrixStack, playerModel.leftLeg, baseVertexConsumer, light, overlay, leftLegPosition, leftLegScale);
            matrixStack.pop();
        } else {
            // 没有身体旋转时，正常渲染
            playerModel.body.setAngles(0, 0, 0);
            renderPartWithTransform(matrixStack, playerModel.body, baseVertexConsumer, light, overlay, bodyPosition, bodyScale);
            renderPartWithTransform(matrixStack, playerModel.head, baseVertexConsumer, light, overlay, headPosition, headScale);
            renderPartWithTransform(matrixStack, playerModel.rightArm, baseVertexConsumer, light, overlay, rightArmPosition, rightArmScale);
            renderPartWithTransform(matrixStack, playerModel.leftArm, baseVertexConsumer, light, overlay, leftArmPosition, leftArmScale);
            renderPartWithTransform(matrixStack, playerModel.rightLeg, baseVertexConsumer, light, overlay, rightLegPosition, rightLegScale);
            renderPartWithTransform(matrixStack, playerModel.leftLeg, baseVertexConsumer, light, overlay, leftLegPosition, leftLegScale);
        }
        
        // 第二步：渲染外层（overlay layer）- 使用半透明渲染以正确显示多层皮肤
        var overlayVertexConsumer = vertexConsumerProvider.getBuffer(translucentRenderType);
        
        // 检查是否使用3D皮肤层渲染
        boolean modLoaded = PlayerDollAddonClient.IS_3D_SKIN_LAYERS_LOADED;
        boolean apiAvailable = Doll3DSkinUtil.isAvailable();
        boolean inRange = shouldUse3DSkinLayers(entity);
        boolean use3DSkinLayers = modLoaded && apiAvailable && inRange;
        
        SkinLayersLogger.debug("渲染检查: modLoaded={}, apiAvailable={}, inRange={}, use3D={}", 
                modLoaded, apiAvailable, inRange, use3DSkinLayers);
        
        if (use3DSkinLayers) {
            // 使用3D皮肤层渲染
            SkinLayersLogger.debug("使用3D渲染，皮肤: {}", skinLocation);
            renderOverlayWith3DSkinLayers(matrixStack, overlayVertexConsumer, light, overlay,
                    skinLocation, bodyRotX, bodyRotY, bodyRotZ,
                    hatPosition, hatCombinedScale,
                    rightArmPosition, rightArmScale, leftArmPosition, leftArmScale,
                    bodyPosition, bodyScale,
                    rightLegPosition, rightLegScale, leftLegPosition, leftLegScale);
        } else {
            if (!modLoaded) {
                SkinLayersLogger.debug("mod未加载，使用2D渲染");
            } else if (!apiAvailable) {
                SkinLayersLogger.debug("API不可用，使用2D渲染");
            } else if (!inRange) {
                SkinLayersLogger.debug("距离过远，使用2D渲染");
            }
            // 使用默认2D渲染
            // 如果有身体旋转，使用 MatrixStack 在身体旋转中心应用旋转，然后渲染所有外层部分
            if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
                matrixStack.push();
                
                // 移动到身体的旋转中心（身体和头连接处，Y坐标约为0.375）
                float rotationCenterY = 0.375f;
                matrixStack.translate(0.0, rotationCenterY, 0.0);
                
                // 应用身体旋转（只在这里应用，不在 setRotation 中设置）
                matrixStack.multiply(new Quaternionf().rotateX(bodyRotX));
                matrixStack.multiply(new Quaternionf().rotateY(bodyRotY));
                matrixStack.multiply(new Quaternionf().rotateZ(bodyRotZ));
                
                // 移回旋转中心
                matrixStack.translate(0.0, -rotationCenterY, 0.0);
                // 在旋转后的坐标系中 渲染所有外层部分
                // hat层（头发外层），使用 headScale 和 hatScale 的组合
                renderPartWithTransform(matrixStack, playerModel.hat, overlayVertexConsumer, light, overlay, hatPosition, hatCombinedScale);
                // 手臂外层（保持它们自己的旋转值）
                renderArmOverlayParts(matrixStack, overlayVertexConsumer, light, overlay, rightArmPosition, rightArmScale, leftArmPosition, leftArmScale);
                // 身体和腿部外层（jacket 的旋转设为0）
                setBodyOverlayRotation(0, 0, 0); // 确保身体外层不额外旋转
                renderBodyLegOverlayParts(matrixStack, overlayVertexConsumer, light, overlay, bodyPosition, bodyScale, rightLegPosition, rightLegScale, leftLegPosition, leftLegScale);
                matrixStack.pop();
            } else {
                // 没有身体旋转时，正常渲染
                renderPartWithTransform(matrixStack, playerModel.hat, overlayVertexConsumer, light, overlay, hatPosition, hatCombinedScale);
                renderArmOverlayParts(matrixStack, overlayVertexConsumer, light, overlay, rightArmPosition, rightArmScale, leftArmPosition, leftArmScale);
                setBodyOverlayRotation(0, 0, 0);
                renderBodyLegOverlayParts(matrixStack, overlayVertexConsumer, light, overlay, bodyPosition, bodyScale, rightLegPosition, rightLegScale, leftLegPosition, leftLegScale);
            }
        }
        
        matrixStack.pop();
        
        super.render(entity, entityYaw, partialTick, matrixStack, vertexConsumerProvider, light);
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
    private void renderPartWithTransform(MatrixStack matrixStack,
                                         net.minecraft.client.model.ModelPart part,
                                         net.minecraft.client.render.VertexConsumer vertexConsumer,
                                         int light,
                                         int overlay,
                                         float[] position,
                                         float[] scale) {
        matrixStack.push();
        
        // 应用位置偏移（Y轴取反，正数向上）
        if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
            matrixStack.translate(position[0], -position[1], position[2]);
        }
        
        // 应用缩放
        if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
            matrixStack.scale(scale[0], scale[1], scale[2]);
        }
        
        // 渲染部件
        part.render(matrixStack, vertexConsumer, light, overlay);
        
        matrixStack.pop();
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
            // 使用反射访问PlayerEntityModel的外层部分并设置旋转
            java.lang.reflect.Field leftSleeveField = PlayerEntityModel.class.getDeclaredField("leftSleeve");
            java.lang.reflect.Field rightSleeveField = PlayerEntityModel.class.getDeclaredField("rightSleeve");
            java.lang.reflect.Field leftPantsField = PlayerEntityModel.class.getDeclaredField("leftPants");
            java.lang.reflect.Field rightPantsField = PlayerEntityModel.class.getDeclaredField("rightPants");
            java.lang.reflect.Field jacketField = PlayerEntityModel.class.getDeclaredField("jacket");
            
            leftSleeveField.setAccessible(true);
            rightSleeveField.setAccessible(true);
            leftPantsField.setAccessible(true);
            rightPantsField.setAccessible(true);
            jacketField.setAccessible(true);
            
            // leftSleeve应该跟随leftArm的旋转
            Object leftSleeve = leftSleeveField.get(playerModel);
            if (leftSleeve instanceof net.minecraft.client.model.ModelPart) {
                ((net.minecraft.client.model.ModelPart) leftSleeve).setAngles(leftArmRotX, leftArmRotY, leftArmRotZ);
            }
            
            // rightSleeve应该跟随rightArm的旋转
            Object rightSleeve = rightSleeveField.get(playerModel);
            if (rightSleeve instanceof net.minecraft.client.model.ModelPart) {
                ((net.minecraft.client.model.ModelPart) rightSleeve).setAngles(rightArmRotX, rightArmRotY, rightArmRotZ);
            }
            
            // leftPants应该跟随leftLeg的旋转
            Object leftPants = leftPantsField.get(playerModel);
            if (leftPants instanceof net.minecraft.client.model.ModelPart) {
                ((net.minecraft.client.model.ModelPart) leftPants).setAngles(leftLegRotX, leftLegRotY, leftLegRotZ);
            }
            
            // rightPants应该跟随rightLeg的旋转
            Object rightPants = rightPantsField.get(playerModel);
            if (rightPants instanceof net.minecraft.client.model.ModelPart) {
                ((net.minecraft.client.model.ModelPart) rightPants).setAngles(rightLegRotX, rightLegRotY, rightLegRotZ);
            }
            
            // jacket应该跟随body的旋转（但身体的旋转通过 MatrixStack 应用，所以这里设为0）
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.ModelPart) {
                ((net.minecraft.client.model.ModelPart) jacket).setAngles(0, 0, 0);
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
            java.lang.reflect.Field jacketField = PlayerEntityModel.class.getDeclaredField("jacket");
            jacketField.setAccessible(true);
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.ModelPart) {
                ((net.minecraft.client.model.ModelPart) jacket).setAngles(bodyRotX, bodyRotY, bodyRotZ);
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
    private void renderArmOverlayParts(MatrixStack matrixStack, 
                                      net.minecraft.client.render.VertexConsumer overlayVertexConsumer, 
                                      int light, 
                                      int overlay,
                                      float[] rightArmPosition,
                                      float[] rightArmScale,
                                      float[] leftArmPosition,
                                      float[] leftArmScale) {
        try {
            // 使用反射访问PlayerEntityModel的外层部分（如果存在）
            java.lang.reflect.Field leftSleeveField = PlayerEntityModel.class.getDeclaredField("leftSleeve");
            java.lang.reflect.Field rightSleeveField = PlayerEntityModel.class.getDeclaredField("rightSleeve");
            
            leftSleeveField.setAccessible(true);
            rightSleeveField.setAccessible(true);
            
            // 渲染左袖子外层
            Object leftSleeve = leftSleeveField.get(playerModel);
            if (leftSleeve instanceof net.minecraft.client.model.ModelPart) {
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) leftSleeve, overlayVertexConsumer, light, overlay, leftArmPosition, leftArmScale);
            }
            
            // 渲染右袖子外层
            Object rightSleeve = rightSleeveField.get(playerModel);
            if (rightSleeve instanceof net.minecraft.client.model.ModelPart) {
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) rightSleeve, overlayVertexConsumer, light, overlay, rightArmPosition, rightArmScale);
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
    private void renderBodyLegOverlayParts(MatrixStack matrixStack, 
                                          net.minecraft.client.render.VertexConsumer overlayVertexConsumer, 
                                          int light, 
                                          int overlay,
                                          float[] bodyPosition,
                                          float[] bodyScale,
                                          float[] rightLegPosition,
                                          float[] rightLegScale,
                                          float[] leftLegPosition,
                                          float[] leftLegScale) {
        try {
            // 使用反射访问PlayerEntityModel的外层部分（如果存在）
            java.lang.reflect.Field leftPantsField = PlayerEntityModel.class.getDeclaredField("leftPants");
            java.lang.reflect.Field rightPantsField = PlayerEntityModel.class.getDeclaredField("rightPants");
            java.lang.reflect.Field jacketField = PlayerEntityModel.class.getDeclaredField("jacket");
            
            leftPantsField.setAccessible(true);
            rightPantsField.setAccessible(true);
            jacketField.setAccessible(true);
            
            // 渲染夹克外层（身体外层）
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.ModelPart) {
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) jacket, overlayVertexConsumer, light, overlay, bodyPosition, bodyScale);
            }
            
            // 渲染左腿外层
            Object leftPants = leftPantsField.get(playerModel);
            if (leftPants instanceof net.minecraft.client.model.ModelPart) {
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) leftPants, overlayVertexConsumer, light, overlay, leftLegPosition, leftLegScale);
            }
            
            // 渲染右腿外层
            Object rightPants = rightPantsField.get(playerModel);
            if (rightPants instanceof net.minecraft.client.model.ModelPart) {
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) rightPants, overlayVertexConsumer, light, overlay, rightLegPosition, rightLegScale);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    @Override
    public Identifier getTexture(T entity) {
        return getSkinLocation(entity);
    }
    
    /**
     * 检查是否应该使用3D皮肤层渲染
     * 实现距离检测（12格LOD）
     */
    private boolean shouldUse3DSkinLayers(T entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.gameRenderer == null || 
            client.gameRenderer.getCamera() == null) {
            SkinLayersLogger.debug("客户端未初始化，无法使用3D渲染");
            return false;
        }
        
        // 计算距离（使用距离的平方避免开方运算）
        double distanceSq = entity.squaredDistanceTo(
            client.gameRenderer.getCamera().getPos().x,
            client.gameRenderer.getCamera().getPos().y,
            client.gameRenderer.getCamera().getPos().z
        );
        
        double distance = Math.sqrt(distanceSq);
        boolean shouldUse = distanceSq <= 12.0 * 12.0;
        
        SkinLayersLogger.debug("距离检测: 距离={}格, 使用3D渲染={}", 
                String.format("%.2f", distance), shouldUse);
        
        // 12格以内使用3D渲染
        return shouldUse;
    }
    
    /**
     * 使用3D皮肤层渲染外层
     */
    private void renderOverlayWith3DSkinLayers(MatrixStack matrixStack,
                                               net.minecraft.client.render.VertexConsumer vertexConsumer,
                                               int light, int overlay,
                                               Identifier skinLocation,
                                               float bodyRotX, float bodyRotY, float bodyRotZ,
                                               float[] hatPosition, float[] hatScale,
                                               float[] rightArmPosition, float[] rightArmScale,
                                               float[] leftArmPosition, float[] leftArmScale,
                                               float[] bodyPosition, float[] bodyScale,
                                               float[] rightLegPosition, float[] rightLegScale,
                                               float[] leftLegPosition, float[] leftLegScale) {
        SkinLayersLogger.debug("开始3D渲染，皮肤: {}, thinArms: {}", skinLocation, thinArms);
        
        // 使用存储的thinArms字段
        
        // 获取或创建3D皮肤数据
        SkinLayersLogger.debug("获取3D皮肤数据...");
        Doll3DSkinData skinData = Doll3DSkinUtil.setup3dLayers(skinLocation, thinArms);
        if (skinData == null) {
            SkinLayersLogger.warn("✗ 无法获取3D皮肤数据（返回null），回退到2D渲染");
            return;
        }
        if (!skinData.hasValidData()) {
            SkinLayersLogger.warn("✗ 3D皮肤数据无效，回退到2D渲染");
            return;
        }
        
        SkinLayersLogger.debug("✓ 3D皮肤数据有效，开始渲染各个部位");
        
        // 处理身体旋转
        if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
            matrixStack.push();
            
            float rotationCenterY = 0.375f;
            matrixStack.translate(0.0, rotationCenterY, 0.0);
            matrixStack.multiply(new Quaternionf().rotateX(bodyRotX));
            matrixStack.multiply(new Quaternionf().rotateY(bodyRotY));
            matrixStack.multiply(new Quaternionf().rotateZ(bodyRotZ));
            matrixStack.translate(0.0, -rotationCenterY, 0.0);
            
            // 渲染各个部位的3D网格
            render3DMeshPart(matrixStack, playerModel.hat, skinData.getHeadMesh(),
                    "HEAD", vertexConsumer, light, overlay, hatPosition, hatScale);
            
            render3DMeshPart(matrixStack, playerModel.leftArm, skinData.getLeftArmMesh(),
                    thinArms ? "LEFT_ARM_SLIM" : "LEFT_ARM", vertexConsumer, light, overlay,
                    leftArmPosition, leftArmScale);
            
            render3DMeshPart(matrixStack, playerModel.rightArm, skinData.getRightArmMesh(),
                    thinArms ? "RIGHT_ARM_SLIM" : "RIGHT_ARM", vertexConsumer, light, overlay,
                    rightArmPosition, rightArmScale);
            
            render3DMeshPart(matrixStack, playerModel.body, skinData.getTorsoMesh(),
                    "BODY", vertexConsumer, light, overlay, bodyPosition, bodyScale);
            
            render3DMeshPart(matrixStack, playerModel.leftLeg, skinData.getLeftLegMesh(),
                    "LEFT_LEG", vertexConsumer, light, overlay, leftLegPosition, leftLegScale);
            
            render3DMeshPart(matrixStack, playerModel.rightLeg, skinData.getRightLegMesh(),
                    "RIGHT_LEG", vertexConsumer, light, overlay, rightLegPosition, rightLegScale);
            
            matrixStack.pop();
        } else {
            // 没有身体旋转时，正常渲染
            render3DMeshPart(matrixStack, playerModel.hat, skinData.getHeadMesh(),
                    "HEAD", vertexConsumer, light, overlay, hatPosition, hatScale);
            
            render3DMeshPart(matrixStack, playerModel.leftArm, skinData.getLeftArmMesh(),
                    thinArms ? "LEFT_ARM_SLIM" : "LEFT_ARM", vertexConsumer, light, overlay,
                    leftArmPosition, leftArmScale);
            
            render3DMeshPart(matrixStack, playerModel.rightArm, skinData.getRightArmMesh(),
                    thinArms ? "RIGHT_ARM_SLIM" : "RIGHT_ARM", vertexConsumer, light, overlay,
                    rightArmPosition, rightArmScale);
            
            render3DMeshPart(matrixStack, playerModel.body, skinData.getTorsoMesh(),
                    "BODY", vertexConsumer, light, overlay, bodyPosition, bodyScale);
            
            render3DMeshPart(matrixStack, playerModel.leftLeg, skinData.getLeftLegMesh(),
                    "LEFT_LEG", vertexConsumer, light, overlay, leftLegPosition, leftLegScale);
            
            render3DMeshPart(matrixStack, playerModel.rightLeg, skinData.getRightLegMesh(),
                    "RIGHT_LEG", vertexConsumer, light, overlay, rightLegPosition, rightLegScale);
        }
    }
    
    /**
     * 渲染单个3D网格部件
     */
    private void render3DMeshPart(MatrixStack matrixStack,
                                  net.minecraft.client.model.ModelPart modelPart,
                                  Object mesh,
                                  String offsetProviderName,
                                  net.minecraft.client.render.VertexConsumer vertexConsumer,
                                  int light, int overlay,
                                  float[] position, float[] scale) {
        if (mesh == null) {
            SkinLayersLogger.debug("跳过渲染 {}（mesh为null）", offsetProviderName);
            return;
        }
        
        SkinLayersLogger.debug("渲染3D网格部件: {}", offsetProviderName);
        
        try {
            // 获取OffsetProvider
            Object offsetProvider = Doll3DSkinUtil.getOffsetProvider(offsetProviderName);
            if (offsetProvider == null) {
                SkinLayersLogger.warn("✗ 无法获取OffsetProvider: {}", offsetProviderName);
                return;
            }
            SkinLayersLogger.debug("✓ OffsetProvider获取成功: {}", offsetProviderName);
            
            matrixStack.push();
            
            // 应用位置偏移
            if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
                matrixStack.translate(position[0], -position[1], position[2]);
            }
            
            // 应用缩放
            if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
                matrixStack.scale(scale[0], scale[1], scale[2]);
            }
            
            // 应用ModelPart的变换（通过反射调用，因为方法在不同版本中可能不同）
            // 在1.20.1中，ModelPart可能没有translateAndRotate方法，我们尝试调用
            try {
                // 尝试调用translateAndRotate方法（如果存在）
                Method translateAndRotateMethod = modelPart.getClass().getMethod("translateAndRotate", MatrixStack.class);
                translateAndRotateMethod.invoke(modelPart, matrixStack);
            } catch (NoSuchMethodException e) {
                // 如果方法不存在，手动应用ModelPart的变换
                // 在1.20.1中，我们需要手动应用pivot和rotation
                try {
                    // 获取pivot位置
                    java.lang.reflect.Field pivotXField = modelPart.getClass().getDeclaredField("pivotX");
                    java.lang.reflect.Field pivotYField = modelPart.getClass().getDeclaredField("pivotY");
                    java.lang.reflect.Field pivotZField = modelPart.getClass().getDeclaredField("pivotZ");
                    pivotXField.setAccessible(true);
                    pivotYField.setAccessible(true);
                    pivotZField.setAccessible(true);
                    
                    float pivotX = pivotXField.getFloat(modelPart);
                    float pivotY = pivotYField.getFloat(modelPart);
                    float pivotZ = pivotZField.getFloat(modelPart);
                    
                    // 应用pivot变换
                    if (pivotX != 0 || pivotY != 0 || pivotZ != 0) {
                        matrixStack.translate(pivotX / 16.0f, pivotY / 16.0f, pivotZ / 16.0f);
                    }
                    
                    // 获取rotation（通过setAngles设置的）
                    java.lang.reflect.Field pitchField = modelPart.getClass().getDeclaredField("pitch");
                    java.lang.reflect.Field yawField = modelPart.getClass().getDeclaredField("yaw");
                    java.lang.reflect.Field rollField = modelPart.getClass().getDeclaredField("roll");
                    pitchField.setAccessible(true);
                    yawField.setAccessible(true);
                    rollField.setAccessible(true);
                    
                    float pitch = pitchField.getFloat(modelPart);
                    float yaw = yawField.getFloat(modelPart);
                    float roll = rollField.getFloat(modelPart);
                    
                    // 应用旋转
                    if (pitch != 0) matrixStack.multiply(new Quaternionf().rotateX(pitch));
                    if (yaw != 0) matrixStack.multiply(new Quaternionf().rotateY(yaw));
                    if (roll != 0) matrixStack.multiply(new Quaternionf().rotateZ(roll));
                    
                    // 移回pivot
                    if (pivotX != 0 || pivotY != 0 || pivotZ != 0) {
                        matrixStack.translate(-pivotX / 16.0f, -pivotY / 16.0f, -pivotZ / 16.0f);
                    }
                } catch (Exception ex) {
                    // 如果无法手动应用变换，就跳过
                    SkinLayersLogger.debug("无法手动应用ModelPart变换，跳过");
                }
            } catch (Exception e) {
                SkinLayersLogger.warn("应用ModelPart变换失败", e);
            }
            
            // 应用OffsetProvider的偏移
            try {
                Method applyOffsetMethod = offsetProvider.getClass().getMethod("applyOffset",
                        MatrixStack.class, mesh.getClass());
                applyOffsetMethod.invoke(offsetProvider, matrixStack, mesh);
            } catch (Exception e) {
                SkinLayersLogger.error("应用OffsetProvider失败: {}", offsetProviderName, e);
            }
            
            // 渲染3D网格
            try {
                SkinLayersLogger.debug("调用mesh.render()方法...");
                Method renderMethod = mesh.getClass().getMethod("render",
                        net.minecraft.client.model.ModelPart.class,
                        MatrixStack.class,
                        net.minecraft.client.render.VertexConsumer.class,
                        int.class, int.class, int.class);
                renderMethod.invoke(mesh, modelPart, matrixStack, vertexConsumer, light, overlay, 0xFFFFFFFF);
                SkinLayersLogger.debug("✓ {} 渲染完成", offsetProviderName);
            } catch (NoSuchMethodException e) {
                SkinLayersLogger.error("✗ render方法未找到: {}", e.getMessage());
            } catch (Exception e) {
                SkinLayersLogger.error("✗ 渲染3D网格失败", e);
            }
            
            matrixStack.pop();
            
        } catch (Exception e) {
            SkinLayersLogger.error("✗ 渲染3D网格部件失败: {}", offsetProviderName, e);
        }
    }
}

