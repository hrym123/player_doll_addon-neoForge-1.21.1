package com.lanye.dolladdon.base.render;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 玩偶实体渲染器基类
 * 提供所有玩偶实体渲染器的共同功能
 */
public abstract class BaseDollRenderer<T extends BaseDollEntity> extends EntityRenderer<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDollRenderer.class);
    protected final PlayerEntityModel<PlayerEntity> playerModel;
    
    protected BaseDollRenderer(EntityRendererFactory.Context context, PlayerEntityModel<PlayerEntity> playerModel) {
        super(context);
        this.playerModel = playerModel;
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
}

