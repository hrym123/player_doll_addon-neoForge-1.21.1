package com.lanye.dolladdon.base.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 玩偶物品渲染器基类
 * 提供所有玩偶物品渲染器的共同功能
 */
public abstract class BaseDollItemRenderer extends BlockEntityWithoutLevelRenderer {
    protected final PlayerModel<net.minecraft.world.entity.player.Player> playerModel;
    
    protected BaseDollItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet, PlayerModel<net.minecraft.world.entity.player.Player> playerModel) {
        super(dispatcher, modelSet);
        this.playerModel = playerModel;
    }
    
    /**
     * 获取皮肤资源位置
     * @return 皮肤资源位置
     */
    protected abstract ResourceLocation getSkinLocation();
    
    /**
     * 获取日志标签（用于调试）
     * @return 日志标签
     */
    protected abstract String getLogTag();
    
    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        
        // 根据显示上下文调整模型的位置、缩放和旋转
        applyPlayerModelTransform(poseStack, transformType);
        
        // 获取皮肤位置（由子类实现）
        ResourceLocation skinLocation = getSkinLocation();
        
        // 设置模型姿态（站立姿态）
        playerModel.head.setRotation(0.0F, 0.0F, 0.0F);
        playerModel.hat.setRotation(0.0F, 0.0F, 0.0F);
        playerModel.body.setRotation(0.0F, 0.0F, 0.0F);
        playerModel.rightArm.setRotation(-0.6981317F, 0.0F, 0.0F);
        playerModel.leftArm.setRotation(0.6981317F, 0.0F, 0.0F);
        playerModel.rightLeg.setRotation(0.0F, 0.0F, 0.0F);
        playerModel.leftLeg.setRotation(0.0F, 0.0F, 0.0F);
        
        // 获取渲染类型
        var cutoutRenderType = net.minecraft.client.renderer.RenderType.entityCutoutNoCull(skinLocation);
        var translucentRenderType = net.minecraft.client.renderer.RenderType.entityTranslucent(skinLocation);
        
        // 第一步：渲染基础层（base layer）- 所有基础部分
        var baseVertexConsumer = bufferSource.getBuffer(cutoutRenderType);
        
        // 渲染基础身体部分（不包括外层）
        playerModel.head.render(poseStack, baseVertexConsumer, packedLight, packedOverlay);
        playerModel.body.render(poseStack, baseVertexConsumer, packedLight, packedOverlay);
        playerModel.rightArm.render(poseStack, baseVertexConsumer, packedLight, packedOverlay);
        playerModel.leftArm.render(poseStack, baseVertexConsumer, packedLight, packedOverlay);
        playerModel.rightLeg.render(poseStack, baseVertexConsumer, packedLight, packedOverlay);
        playerModel.leftLeg.render(poseStack, baseVertexConsumer, packedLight, packedOverlay);
        
        // 第二步：渲染外层（overlay layer）- 使用半透明渲染以正确显示多层皮肤
        var overlayVertexConsumer = bufferSource.getBuffer(translucentRenderType);
        
        // 渲染hat层（头发外层）
        playerModel.hat.render(poseStack, overlayVertexConsumer, packedLight, packedOverlay);
        
        // 渲染外层部分（overlay layer）- 用于多层皮肤
        renderOverlayParts(poseStack, overlayVertexConsumer, packedLight, packedOverlay);
        
        poseStack.popPose();
    }
    
    /**
     * 根据显示上下文调整玩家模型的位置、缩放和旋转
     * 注意：玩家模型的原点在脚部（Y=0），模型高度约1.8，中心在Y=0.9处
     * 但是为了与实体渲染器保持一致，模型被缩放到高度1.125（缩放比例约0.625）
     * 
     * @param poseStack 变换矩阵栈
     * @param transformType 显示上下文类型
     */
    protected void applyPlayerModelTransform(PoseStack poseStack, ItemDisplayContext transformType) {
        // 模型缩放比例，与实体渲染器保持一致（使模型高度为1.125）
        float modelScale = 1.125F / 1.8F; // 约 0.625F
        
        if (transformType == ItemDisplayContext.GUI) {
            // GUI 中：居中显示
            // 先应用基础缩放和模型缩放
            poseStack.scale(0.8F * modelScale, 0.8F * modelScale, 0.8F * modelScale);
            // 移动到物品槽中心并向上移动使模型居中
            // 原始模型高度1.8，缩放后为1.125，scale(0.8*0.625)后最终高度约为0.9
            // 所以Y位置需要相应调整：1.5 * (modelScale) ≈ 0.9375，但考虑到视觉居中效果，使用1.17
            poseStack.translate(0.641, 1.17, 0.359);
            // 逆时针旋转135度（Y轴逆时针为负值）
            poseStack.mulPose(Axis.YP.rotationDegrees(-135.0F));
        } else if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || 
                   transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            // 第一人称：左手和右手使用相同的初始变换
            // 原始模型高度1.8，缩放后为1.125，scale(0.5*0.625)后最终高度约为0.5625
            // 所以Y位置需要相应调整：1.25 * modelScale ≈ 0.78
            poseStack.translate(0.5, 0.78, 0.5);
            // 缩放并前后反转（应用模型缩放）
            poseStack.scale(0.5F * modelScale, 0.5F * modelScale, -0.5F * modelScale);
            // 旋转
            if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
                // 第一人称左手
                poseStack.mulPose(Axis.YP.rotationDegrees(-15.0F));
            } else{
                // 第一人称右手
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F));
            }
        } else if (transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || 
                   transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            // 第三人称：调整位置和大小
            // 原始模型高度1.8，缩放后为1.125，scale(0.375*0.625)后最终高度约为0.421875
            // 所以Y位置需要相应调整：1.075 * modelScale ≈ 0.67
            poseStack.translate(0.5, 0.67, 0.5);
            // 缩放并前后反转（应用模型缩放）
            poseStack.scale(0.375F * modelScale, 0.375F * modelScale, -0.375F * modelScale);
        } else {
            // 其他情况（地面、框架等）
            // 原始模型高度1.8，缩放后为1.125，scale(0.5*0.625)后最终高度约为0.5625
            // 所以Y位置需要相应调整：1.4 * modelScale ≈ 0.875
            poseStack.translate(0.5, 0.875, 0.5);
            poseStack.scale(0.5F * modelScale, 0.5F * modelScale, 0.5F * modelScale);
        }
        
        // 翻转模型（玩家模型需要翻转才能正确显示）
        // 原因：Minecraft的玩家模型在渲染时，默认朝向与物品渲染的坐标系不匹配
        // 实体渲染器（SteveDollRenderer）也使用 scale(-1.0F, -1.0F, 1.0F) 来翻转模型
        // 这是Minecraft渲染系统的约定，翻转后模型才能正确显示（正面朝向玩家）
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        
    }
    
    /**
     * 渲染外层部分（overlay layer）以支持多层皮肤
     * 这些外层部分需要使用半透明渲染类型来正确显示叠加层
     * 
     * @param poseStack 变换矩阵栈
     * @param overlayVertexConsumer 外层顶点消费者
     * @param packedLight 光照信息
     * @param packedOverlay 覆盖纹理
     */
    private void renderOverlayParts(PoseStack poseStack, 
                                    com.mojang.blaze3d.vertex.VertexConsumer overlayVertexConsumer, 
                                    int packedLight, 
                                    int packedOverlay) {
        try {
            // 使用反射访问PlayerModel的外层部分（如果存在）
            // 这些字段在Minecraft 1.21.1的PlayerModel中应该存在
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
                ((net.minecraft.client.model.geom.ModelPart) leftSleeve).render(poseStack, overlayVertexConsumer, packedLight, packedOverlay);
            }
            
            // 渲染右袖子外层
            Object rightSleeve = rightSleeveField.get(playerModel);
            if (rightSleeve instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) rightSleeve).render(poseStack, overlayVertexConsumer, packedLight, packedOverlay);
            }
            
            // 渲染左腿外层
            Object leftPants = leftPantsField.get(playerModel);
            if (leftPants instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) leftPants).render(poseStack, overlayVertexConsumer, packedLight, packedOverlay);
            }
            
            // 渲染右腿外层
            Object rightPants = rightPantsField.get(playerModel);
            if (rightPants instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) rightPants).render(poseStack, overlayVertexConsumer, packedLight, packedOverlay);
            }
            
            // 渲染夹克外层（身体外层）
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.geom.ModelPart) {
                ((net.minecraft.client.model.geom.ModelPart) jacket).render(poseStack, overlayVertexConsumer, packedLight, packedOverlay);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段（例如某些版本的PlayerModel可能没有这些外层部分），则忽略
            // 这是正常的，不影响基础渲染
        }
    }
}

