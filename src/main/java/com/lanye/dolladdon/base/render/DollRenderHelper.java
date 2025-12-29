package com.lanye.dolladdon.base.render;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * 玩偶渲染辅助工具类
 * 提供渲染玩家模型的共同功能
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
     */
    public static void setOverlayPartsRotation(PlayerEntityModel<?> playerModel,
                                              float bodyRotX, float bodyRotY, float bodyRotZ,
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
            
            // jacket应该跟随body的旋转
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.ModelPart) {
                ((net.minecraft.client.model.ModelPart) jacket).setAngles(bodyRotX, bodyRotY, bodyRotZ);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    /**
     * 渲染外层部分（overlay layer）以支持多层皮肤
     * 这些外层部分需要使用半透明渲染类型来正确显示叠加层
     * 
     * 注意：外层部分已经通过setOverlayPartsRotation方法设置了与基础部分相同的旋转，
     * 所以它们现在应该能够跟随基础部分的动作。
     * 
     * @param playerModel 玩家模型
     * @param poseStack 变换矩阵栈
     * @param overlayVertexConsumer 外层顶点消费者
     * @param packedLight 光照信息
     * @param packedOverlay 覆盖纹理
     */
    public static void renderOverlayParts(PlayerEntityModel<?> playerModel,
                                         MatrixStack matrixStack,
                                         VertexConsumer overlayVertexConsumer,
                                         int light,
                                         int overlay) {
        try {
            // 使用反射访问PlayerEntityModel的外层部分（如果存在）
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
            
            // 渲染左袖子外层
            Object leftSleeve = leftSleeveField.get(playerModel);
            if (leftSleeve instanceof net.minecraft.client.model.ModelPart) {
                ((net.minecraft.client.model.ModelPart) leftSleeve).render(matrixStack, overlayVertexConsumer, light, overlay);
            }
            
            // 渲染右袖子外层
            Object rightSleeve = rightSleeveField.get(playerModel);
            if (rightSleeve instanceof net.minecraft.client.model.ModelPart) {
                ((net.minecraft.client.model.ModelPart) rightSleeve).render(matrixStack, overlayVertexConsumer, light, overlay);
            }
            
            // 渲染左腿外层
            Object leftPants = leftPantsField.get(playerModel);
            if (leftPants instanceof net.minecraft.client.model.ModelPart) {
                ((net.minecraft.client.model.ModelPart) leftPants).render(matrixStack, overlayVertexConsumer, light, overlay);
            }
            
            // 渲染右腿外层
            Object rightPants = rightPantsField.get(playerModel);
            if (rightPants instanceof net.minecraft.client.model.ModelPart) {
                ((net.minecraft.client.model.ModelPart) rightPants).render(matrixStack, overlayVertexConsumer, light, overlay);
            }
            
            // 渲染夹克外层（身体外层）
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.ModelPart) {
                ((net.minecraft.client.model.ModelPart) jacket).render(matrixStack, overlayVertexConsumer, light, overlay);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
}

