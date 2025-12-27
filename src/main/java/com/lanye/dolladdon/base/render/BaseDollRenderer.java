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

/**
 * 玩偶实体渲染器基类
 * 提供所有玩偶实体渲染器的共同功能
 */
public abstract class BaseDollRenderer<T extends BaseDollEntity> extends EntityRenderer<T> {
    protected final PlayerModel<Player> playerModel;
    
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
        
        // 缩小模型大小，使高度为1.125（玩家模型高度约1.8，缩放比例 = 1.125 / 1.8 = 0.625）
        float modelScale = 0.5F; // 约 0.625F
        
        poseStack.translate(0.0, 0.75, 0.0);
        
        // 应用缩放和翻转
        poseStack.scale(-modelScale, -modelScale, modelScale);
        
        // 获取皮肤位置（由子类实现）
        ResourceLocation skinLocation = getSkinLocation(entity);
        
        // 设置模型姿态（站立姿态）
        float headRotX = 0.0F, headRotY = 0.0F, headRotZ = 0.0F;
        float hatRotX = 0.0F, hatRotY = 0.0F, hatRotZ = 0.0F;
        float bodyRotX = 0.0F, bodyRotY = 0.0F, bodyRotZ = 0.0F;
        float rightArmRotX = -0.6981317F, rightArmRotY = 0.0F, rightArmRotZ = 0.0F; // 约 -40 度
        float leftArmRotX = 0.6981317F, leftArmRotY = 0.0F, leftArmRotZ = 0.0F;   // 约 40 度
        float rightLegRotX = 0F, rightLegRotY = 0.0F, rightLegRotZ = 0.0F;
        float leftLegRotX = 0F, leftLegRotY = 0.0F, leftLegRotZ = 0.0F;
        
        playerModel.head.setRotation(headRotX, headRotY, headRotZ);
        playerModel.hat.setRotation(hatRotX, hatRotY, hatRotZ);
        playerModel.body.setRotation(bodyRotX, bodyRotY, bodyRotZ);
        playerModel.rightArm.setRotation(rightArmRotX, rightArmRotY, rightArmRotZ);
        playerModel.leftArm.setRotation(leftArmRotX, leftArmRotY, leftArmRotZ);
        playerModel.rightLeg.setRotation(rightLegRotX, rightLegRotY, rightLegRotZ);
        playerModel.leftLeg.setRotation(leftLegRotX, leftLegRotY, leftLegRotZ);
        
        // 同时设置外层部分的旋转，使它们跟随基础部分的动作
        setOverlayPartsRotation(bodyRotX, bodyRotY, bodyRotZ,
                               leftArmRotX, leftArmRotY, leftArmRotZ,
                               rightArmRotX, rightArmRotY, rightArmRotZ,
                               leftLegRotX, leftLegRotY, leftLegRotZ,
                               rightLegRotX, rightLegRotY, rightLegRotZ);
        
        // 获取渲染类型
        var cutoutRenderType = net.minecraft.client.renderer.RenderType.entityCutoutNoCull(skinLocation);
        var translucentRenderType = net.minecraft.client.renderer.RenderType.entityTranslucent(skinLocation);
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        
        // 第一步：渲染基础层（base layer）- 所有基础部分
        var baseVertexConsumer = bufferSource.getBuffer(cutoutRenderType);
        
        // 渲染基础身体部分（不包括外层）
        playerModel.head.render(poseStack, baseVertexConsumer, packedLight, overlay);
        playerModel.body.render(poseStack, baseVertexConsumer, packedLight, overlay);
        playerModel.rightArm.render(poseStack, baseVertexConsumer, packedLight, overlay);
        playerModel.leftArm.render(poseStack, baseVertexConsumer, packedLight, overlay);
        playerModel.rightLeg.render(poseStack, baseVertexConsumer, packedLight, overlay);
        playerModel.leftLeg.render(poseStack, baseVertexConsumer, packedLight, overlay);
        
        // 第二步：渲染外层（overlay layer）- 使用半透明渲染以正确显示多层皮肤
        var overlayVertexConsumer = bufferSource.getBuffer(translucentRenderType);
        
        // 渲染hat层（头发外层）- 如果皮肤有多层，这会显示头发的外层
        playerModel.hat.render(poseStack, overlayVertexConsumer, packedLight, overlay);
        
        // 渲染外层部分（overlay layer）- 用于多层皮肤
        // 在Minecraft中，PlayerModel支持多层皮肤渲染，外层部分包括：
        // - hat: 头发外层
        // - leftSleeve/rightSleeve: 袖子外层
        // - leftPants/rightPants: 腿外层
        // - jacket: 身体外层
        // 注意：这些字段可能不存在于所有版本的PlayerModel中，因此使用反射访问
        renderOverlayParts(poseStack, overlayVertexConsumer, packedLight, overlay);
        
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
            
            // jacket应该跟随body的旋转
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) jacket).setRotation(bodyRotX, bodyRotY, bodyRotZ);
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
     * @param poseStack 变换矩阵栈
     * @param overlayVertexConsumer 外层顶点消费者
     * @param packedLight 光照信息
     * @param overlay 覆盖纹理
     */
    private void renderOverlayParts(PoseStack poseStack, 
                                    com.mojang.blaze3d.vertex.VertexConsumer overlayVertexConsumer, 
                                    int packedLight, 
                                    int overlay) {
        try {
            // 使用反射访问PlayerModel的外层部分（如果存在）
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
            
            // 渲染夹克外层（身体外层）
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) jacket).render(poseStack, overlayVertexConsumer, packedLight, overlay);
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

