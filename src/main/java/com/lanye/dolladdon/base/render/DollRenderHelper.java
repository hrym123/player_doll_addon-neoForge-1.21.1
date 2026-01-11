package com.lanye.dolladdon.base.render;

import com.lanye.dolladdon.info.BodyPartsTransformInfo;
import com.lanye.dolladdon.info.OverlayPartsRotationInfo;
import com.lanye.dolladdon.info.PartTransformInfo;
import com.lanye.dolladdon.info.RenderContextInfo;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

/**
 * 玩偶渲染辅助工具类
 * 提供渲染玩家模型的共同功能
 * 
 * 注意：此类使用 PlayerEntityModelWrapper 来访问模型部件，避免直接使用反射
 */
public class DollRenderHelper {
    /** 身体旋转中心Y坐标（身体和头连接处） */
    private static final float BODY_ROTATION_CENTER_Y = 0.375f;
    
    /**
     * 渲染玩家模型（包括基础层和外层）
     * 
     * @param playerModel 玩家模型
     * @param poseStack 变换矩阵栈
     * @param bufferSource 缓冲区源
     * @param skinLocation 皮肤资源位置
     * @param packedLight 光照信息
     * @param packedOverlay 覆盖纹理
     */
    public static void renderPlayerModel(PlayerEntityModel<?> playerModel,
                                        MatrixStack matrixStack,
                                        VertexConsumerProvider vertexConsumerProvider,
                                        Identifier skinLocation,
                                        int light,
                                        int overlay) {
        // 获取渲染类型
        RenderLayer cutoutRenderType = RenderLayer.getEntityCutoutNoCull(skinLocation);
        RenderLayer translucentRenderType = RenderLayer.getEntityTranslucent(skinLocation);
        
        // 第一步：渲染基础层（base layer）- 所有基础部分
        VertexConsumer baseVertexConsumer = vertexConsumerProvider.getBuffer(cutoutRenderType);
        
        // 渲染基础身体部分（不包括外层）
        playerModel.head.render(matrixStack, baseVertexConsumer, light, overlay);
        playerModel.body.render(matrixStack, baseVertexConsumer, light, overlay);
        playerModel.rightArm.render(matrixStack, baseVertexConsumer, light, overlay);
        playerModel.leftArm.render(matrixStack, baseVertexConsumer, light, overlay);
        playerModel.rightLeg.render(matrixStack, baseVertexConsumer, light, overlay);
        playerModel.leftLeg.render(matrixStack, baseVertexConsumer, light, overlay);
        
        // 第二步：渲染外层（overlay layer）- 使用半透明渲染以正确显示多层皮肤
        VertexConsumer overlayVertexConsumer = vertexConsumerProvider.getBuffer(translucentRenderType);
        
        // 渲染hat层（头发外层）
        playerModel.hat.render(matrixStack, overlayVertexConsumer, light, overlay);
        
        // 渲染外层部分（overlay layer）- 用于多层皮肤
        renderOverlayParts(playerModel, matrixStack, overlayVertexConsumer, light, overlay);
    }
    
    /**
     * 设置外层部分的旋转，使它们跟随基础部分的动作
     * 使用 PlayerEntityModelWrapper 来访问外层部件，避免直接使用反射
     * 
     * @param playerModel 玩家模型
     * @param rotationInfo 旋转信息对象，包含所有外层部件的旋转角度
     */
    @SuppressWarnings("unchecked")
    public static void setOverlayPartsRotation(PlayerEntityModel<?> playerModel,
                                              OverlayPartsRotationInfo rotationInfo) {
        // 使用模型封装类来设置外层部件旋转
        PlayerEntityModelWrapper wrapper = new PlayerEntityModelWrapper((PlayerEntityModel<PlayerEntity>) playerModel);
        wrapper.setOverlayPartsRotation(
            rotationInfo.getBodyRotation(),
            rotationInfo.getLeftArmRotation(),
            rotationInfo.getRightArmRotation(),
            rotationInfo.getLeftLegRotation(),
            rotationInfo.getRightLegRotation()
        );
    }
    
    /**
     * 设置外层部分的旋转，使它们跟随基础部分的动作（兼容旧API）
     * 使用 PlayerEntityModelWrapper 来访问外层部件，避免直接使用反射
     * 
     * @param playerModel 玩家模型
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
     * @deprecated 使用 {@link #setOverlayPartsRotation(PlayerEntityModel, OverlayPartsRotationInfo)} 代替
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public static void setOverlayPartsRotation(PlayerEntityModel<?> playerModel,
                                              float bodyRotX, float bodyRotY, float bodyRotZ,
                                              float leftArmRotX, float leftArmRotY, float leftArmRotZ,
                                              float rightArmRotX, float rightArmRotY, float rightArmRotZ,
                                              float leftLegRotX, float leftLegRotY, float leftLegRotZ,
                                              float rightLegRotX, float rightLegRotY, float rightLegRotZ) {
        // 创建旋转信息对象并调用新方法
        OverlayPartsRotationInfo rotationInfo = new OverlayPartsRotationInfo(
            bodyRotX, bodyRotY, bodyRotZ,
            leftArmRotX, leftArmRotY, leftArmRotZ,
            rightArmRotX, rightArmRotY, rightArmRotZ,
            leftLegRotX, leftLegRotY, leftLegRotZ,
            rightLegRotX, rightLegRotY, rightLegRotZ
        );
        setOverlayPartsRotation(playerModel, rotationInfo);
    }
    
    /**
     * 渲染外层部分（overlay layer）以支持多层皮肤
     * 这些外层部分需要使用半透明渲染类型来正确显示叠加层
     * 
     * 注意：外层部分已经通过setOverlayPartsRotation方法设置了与基础部分相同的旋转，
     * 所以它们现在应该能够跟随基础部分的动作。
     * 
     * @param playerModel 玩家模型
     * @param matrixStack 变换矩阵栈
     * @param overlayVertexConsumer 外层顶点消费者
     * @param light 光照信息
     * @param overlay 覆盖纹理
     */
    @SuppressWarnings("unchecked")
    public static void renderOverlayParts(PlayerEntityModel<?> playerModel,
                                         MatrixStack matrixStack,
                                         VertexConsumer overlayVertexConsumer,
                                         int light,
                                         int overlay) {
        // 使用模型封装类来访问外层部件
        PlayerEntityModelWrapper wrapper = new PlayerEntityModelWrapper((PlayerEntityModel<PlayerEntity>) playerModel);
        
        // 渲染左袖子外层
        ModelPart leftSleeve = wrapper.getLeftSleeve();
        if (leftSleeve != null) {
            leftSleeve.render(matrixStack, overlayVertexConsumer, light, overlay);
        }
        
        // 渲染右袖子外层
        ModelPart rightSleeve = wrapper.getRightSleeve();
        if (rightSleeve != null) {
            rightSleeve.render(matrixStack, overlayVertexConsumer, light, overlay);
        }
        
        // 渲染左腿外层
        ModelPart leftPants = wrapper.getLeftPants();
        if (leftPants != null) {
            leftPants.render(matrixStack, overlayVertexConsumer, light, overlay);
        }
        
        // 渲染右腿外层
        ModelPart rightPants = wrapper.getRightPants();
        if (rightPants != null) {
            rightPants.render(matrixStack, overlayVertexConsumer, light, overlay);
        }
        
        // 渲染夹克外层（身体外层）
        ModelPart jacket = wrapper.getJacket();
        if (jacket != null) {
            jacket.render(matrixStack, overlayVertexConsumer, light, overlay);
        }
    }
    
    /**
     * 应用身体旋转并在旋转后的坐标系中执行渲染回调
     * 
     * @param matrixStack 变换矩阵栈
     * @param bodyRotX 身体X旋转（度）
     * @param bodyRotY 身体Y旋转（度）
     * @param bodyRotZ 身体Z旋转（度）
     * @param renderCallback 渲染回调，在旋转后的坐标系中执行
     */
    public static void applyBodyRotation(MatrixStack matrixStack,
                                        float bodyRotX, float bodyRotY, float bodyRotZ,
                                        Runnable renderCallback) {
        if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
            matrixStack.push();
            
            // 移动到身体的旋转中心
            matrixStack.translate(0.0, BODY_ROTATION_CENTER_Y, 0.0);
            
            // 应用身体旋转
            matrixStack.multiply(new Quaternionf().rotateX(bodyRotX));
            matrixStack.multiply(new Quaternionf().rotateY(bodyRotY));
            matrixStack.multiply(new Quaternionf().rotateZ(bodyRotZ));
            
            // 移回旋转中心
            matrixStack.translate(0.0, -BODY_ROTATION_CENTER_Y, 0.0);
            
            // 在旋转后的坐标系中执行渲染
            renderCallback.run();
            
            matrixStack.pop();
        } else {
            // 没有身体旋转时，直接执行渲染
            renderCallback.run();
        }
    }
    
    /**
     * 渲染基础层部件（身体、头部、手臂、腿部）
     * 
     * @param playerModel 玩家模型
     * @param matrixStack 变换矩阵栈
     * @param contextInfo 渲染上下文信息
     * @param transforms 身体各部位变换信息
     */
    public static void renderBaseLayerParts(PlayerEntityModel<?> playerModel,
                                            MatrixStack matrixStack,
                                            RenderContextInfo contextInfo,
                                            BodyPartsTransformInfo transforms) {
        // 确保身体不额外旋转（如果旋转已通过 MatrixStack 应用）
        playerModel.body.setAngles(0, 0, 0);
        
        // 渲染各个部件
        renderPartWithTransform(matrixStack, playerModel.body, contextInfo, transforms.getBody());
        renderPartWithTransform(matrixStack, playerModel.head, contextInfo, transforms.getHead());
        renderPartWithTransform(matrixStack, playerModel.rightArm, contextInfo, transforms.getRightArm());
        renderPartWithTransform(matrixStack, playerModel.leftArm, contextInfo, transforms.getLeftArm());
        renderPartWithTransform(matrixStack, playerModel.rightLeg, contextInfo, transforms.getRightLeg());
        renderPartWithTransform(matrixStack, playerModel.leftLeg, contextInfo, transforms.getLeftLeg());
    }
    
    /**
     * 渲染单个部件，应用位置和缩放（内部辅助方法）
     * 
     * @param matrixStack 变换矩阵栈
     * @param part 要渲染的部件
     * @param contextInfo 渲染上下文信息
     * @param transformInfo 部件变换信息
     */
    private static void renderPartWithTransform(MatrixStack matrixStack,
                                                ModelPart part,
                                                RenderContextInfo contextInfo,
                                                PartTransformInfo transformInfo) {
        matrixStack.push();
        
        float[] position = transformInfo.getPositionInternal();
        float[] scale = transformInfo.getScaleInternal();
        
        // 应用位置偏移（Y轴取反，正数向上）
        if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
            matrixStack.translate(position[0], -position[1], position[2]);
        }
        
        // 应用缩放
        if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
            matrixStack.scale(scale[0], scale[1], scale[2]);
        }
        
        // 渲染部件
        part.render(matrixStack, contextInfo.getVertexConsumer(), contextInfo.getLight(), contextInfo.getOverlay());
        
        matrixStack.pop();
    }
}

