package com.lanye.dolladdon.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * 玩家玩偶渲染辅助类
 * 提供通用的模型变换方法
 */
public class PlayerDollRenderHelper {
    
    /**
     * 根据显示上下文调整玩家模型的位置、缩放和旋转
     * 注意：玩家模型的原点在脚部（Y=0），模型高度约1.8，中心在Y=0.9处
     * 
     * @param poseStack 变换矩阵栈
     * @param transformType 显示上下文类型
     */
    public static void applyPlayerModelTransform(PoseStack poseStack, ItemDisplayContext transformType) {
        if (transformType == ItemDisplayContext.GUI) {
            // GUI 中：居中显示
            // 参考参考项目：先缩放，再移动到中心
            poseStack.scale(0.8F, 0.8F, 0.8F);  // 先缩放
            poseStack.translate(0.5, 0.5, 0.5);  // 移动到物品槽中心
            // 玩家模型原点在脚部（Y=0），向上移动1使模型居中
            poseStack.translate(0.0, 1.0, 0.0);
            // 逆时针旋转135度（Y轴逆时针为负值）
            poseStack.mulPose(Axis.YP.rotationDegrees(-135.0F));
        } else if (transformType == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || 
                   transformType == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            // 第一人称：调整位置和大小
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            // 玩家模型原点在脚部，向上移动使模型居中
            // 缩放0.5后，模型高度0.9，中心在0.45，需要向上移动1.1使中心对齐（在缩放后的坐标系中）
            poseStack.translate(0.0, 1.1, 0.0);
            // 前后反转（Z轴反转）
            poseStack.scale(1.0F, 1.0F, -1.0F);
            // 相对镜头向前移动0.5（Z轴正方向）
            poseStack.translate(0.0, 0.0, 0.5);
            // 顺时针旋转15度（Y轴顺时针为正值）
            poseStack.mulPose(Axis.YP.rotationDegrees(15.0F));
            // 向右移动0.5（X轴正方向，在翻转模型前）
            poseStack.translate(0.5, 0.0, 0.0);
        } else if (transformType == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || 
                   transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            // 第三人称：调整位置和大小
            poseStack.translate(0.5, 1.0, 0.5);
            poseStack.scale(0.375F, 0.375F, 0.375F);
            // 玩家模型原点在脚部，向上移动使模型居中
            // 缩放0.375后，模型高度0.675，中心在0.3375，需要向上移动0.675使中心对齐（在缩放后的坐标系中）
            poseStack.translate(0.0, 0.675, 0.0);
            // 向下移动0.5（Y轴负方向）
            poseStack.translate(0.0, -0.5, 0.0);
            // 再向下移动0.05（Y轴负方向）
            poseStack.translate(0.0, -0.05, 0.0);
            // 前后反转（Z轴反转）
            poseStack.scale(1.0F, 1.0F, -1.0F);
        } else {
            // 其他情况（地面、框架等）
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            // 玩家模型原点在脚部，向上移动使模型居中
            poseStack.translate(0.0, 0.9, 0.0);
        }
        
        // 翻转模型（玩家模型需要翻转才能正确显示）
        // 原因：Minecraft的玩家模型在渲染时，默认朝向与物品渲染的坐标系不匹配
        // 实体渲染器（SteveDollRenderer）也使用 scale(-1.0F, -1.0F, 1.0F) 来翻转模型
        // 这是Minecraft渲染系统的约定，翻转后模型才能正确显示（正面朝向玩家）
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        
        // 在GUI模式下，翻转后向右移动0.2（X轴正方向）
        if (transformType == ItemDisplayContext.GUI) {
            poseStack.translate(0.2, 0.0, 0.0);
        }
    }
}

