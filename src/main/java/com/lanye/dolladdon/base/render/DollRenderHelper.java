package com.lanye.dolladdon.base.render;

import com.lanye.dolladdon.info.OverlayPartsRotationInfo;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 玩偶渲染辅助工具类
 * 提供渲染玩家模型的共同功能
 * 
 * 注意：此类使用 PlayerEntityModelWrapper 来访问模型部件，避免直接使用反射
 */
public class DollRenderHelper {
    
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
}

