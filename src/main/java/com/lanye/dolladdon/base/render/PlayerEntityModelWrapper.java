package com.lanye.dolladdon.base.render;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.player.PlayerEntity;

import java.lang.reflect.Field;

/**
 * PlayerEntityModel 封装类
 * 提供便捷的方法来访问 PlayerEntityModel 的各个部件，包括私有字段
 */
public class PlayerEntityModelWrapper {
    private final PlayerEntityModel<PlayerEntity> model;
    private final boolean thinArms;
    
    // 缓存的反射字段（避免每次访问都查找）
    private static Field leftSleeveField = null;
    private static Field rightSleeveField = null;
    private static Field leftPantsField = null;
    private static Field rightPantsField = null;
    private static Field jacketField = null;
    private static Field slimField = null;
    
    // 字段初始化标志
    private static boolean fieldsInitialized = false;
    
    /**
     * 初始化反射字段（只执行一次）
     */
    private static void initializeFields() {
        if (fieldsInitialized) {
            return;
        }
        
        try {
            leftSleeveField = PlayerEntityModel.class.getDeclaredField("leftSleeve");
            leftSleeveField.setAccessible(true);
            
            rightSleeveField = PlayerEntityModel.class.getDeclaredField("rightSleeve");
            rightSleeveField.setAccessible(true);
            
            leftPantsField = PlayerEntityModel.class.getDeclaredField("leftPants");
            leftPantsField.setAccessible(true);
            
            rightPantsField = PlayerEntityModel.class.getDeclaredField("rightPants");
            rightPantsField.setAccessible(true);
            
            jacketField = PlayerEntityModel.class.getDeclaredField("jacket");
            jacketField.setAccessible(true);
            
            slimField = PlayerEntityModel.class.getDeclaredField("slim");
            slimField.setAccessible(true);
            
            fieldsInitialized = true;
        } catch (NoSuchFieldException e) {
            // 字段不存在，忽略
        }
    }
    
    /**
     * 构造函数
     * @param model PlayerEntityModel 实例
     */
    public PlayerEntityModelWrapper(PlayerEntityModel<PlayerEntity> model) {
        this.model = model;
        initializeFields();
        this.thinArms = isThinArms();
    }
    
    /**
     * 获取原始模型
     * @return PlayerEntityModel 实例
     */
    public PlayerEntityModel<PlayerEntity> getModel() {
        return model;
    }
    
    /**
     * 是否为细手臂模型
     * @return true 如果是细手臂模型（Alex），false 如果是粗手臂模型（Steve）
     */
    public boolean isThinArms() {
        if (slimField == null) {
            return false;
        }
        try {
            return slimField.getBoolean(model);
        } catch (IllegalAccessException e) {
            return false;
        }
    }
    
    /**
     * 获取是否为细手臂模型（缓存值）
     * @return true 如果是细手臂模型
     */
    public boolean hasThinArms() {
        return thinArms;
    }
    
    // ========== 基础部件访问 ==========
    
    public ModelPart getHead() {
        return model.head;
    }
    
    public ModelPart getHat() {
        return model.hat;
    }
    
    public ModelPart getBody() {
        return model.body;
    }
    
    public ModelPart getRightArm() {
        return model.rightArm;
    }
    
    public ModelPart getLeftArm() {
        return model.leftArm;
    }
    
    public ModelPart getRightLeg() {
        return model.rightLeg;
    }
    
    public ModelPart getLeftLeg() {
        return model.leftLeg;
    }
    
    // ========== 外层部件访问（通过反射） ==========
    
    /**
     * 获取左袖子（外层）
     * @return ModelPart，如果不存在则返回 null
     */
    public ModelPart getLeftSleeve() {
        if (leftSleeveField == null) {
            return null;
        }
        try {
            Object sleeve = leftSleeveField.get(model);
            if (sleeve instanceof ModelPart) {
                return (ModelPart) sleeve;
            }
        } catch (IllegalAccessException e) {
            // 忽略
        }
        return null;
    }
    
    /**
     * 获取右袖子（外层）
     * @return ModelPart，如果不存在则返回 null
     */
    public ModelPart getRightSleeve() {
        if (rightSleeveField == null) {
            return null;
        }
        try {
            Object sleeve = rightSleeveField.get(model);
            if (sleeve instanceof ModelPart) {
                return (ModelPart) sleeve;
            }
        } catch (IllegalAccessException e) {
            // 忽略
        }
        return null;
    }
    
    /**
     * 获取左腿外层
     * @return ModelPart，如果不存在则返回 null
     */
    public ModelPart getLeftPants() {
        if (leftPantsField == null) {
            return null;
        }
        try {
            Object pants = leftPantsField.get(model);
            if (pants instanceof ModelPart) {
                return (ModelPart) pants;
            }
        } catch (IllegalAccessException e) {
            // 忽略
        }
        return null;
    }
    
    /**
     * 获取右腿外层
     * @return ModelPart，如果不存在则返回 null
     */
    public ModelPart getRightPants() {
        if (rightPantsField == null) {
            return null;
        }
        try {
            Object pants = rightPantsField.get(model);
            if (pants instanceof ModelPart) {
                return (ModelPart) pants;
            }
        } catch (IllegalAccessException e) {
            // 忽略
        }
        return null;
    }
    
    /**
     * 获取夹克（身体外层）
     * @return ModelPart，如果不存在则返回 null
     */
    public ModelPart getJacket() {
        if (jacketField == null) {
            return null;
        }
        try {
            Object jacket = jacketField.get(model);
            if (jacket instanceof ModelPart) {
                return (ModelPart) jacket;
            }
        } catch (IllegalAccessException e) {
            // 忽略
        }
        return null;
    }
    
    // ========== 便捷方法 ==========
    
    /**
     * 设置所有基础部件的旋转
     * @param headRot 头部旋转 [X, Y, Z]（度）
     * @param hatRot 帽子旋转 [X, Y, Z]（度）
     * @param bodyRot 身体旋转 [X, Y, Z]（度）
     * @param rightArmRot 右臂旋转 [X, Y, Z]（度）
     * @param leftArmRot 左臂旋转 [X, Y, Z]（度）
     * @param rightLegRot 右腿旋转 [X, Y, Z]（度）
     * @param leftLegRot 左腿旋转 [X, Y, Z]（度）
     */
    public void setAllPartsRotation(float[] headRot, float[] hatRot, float[] bodyRot,
                                   float[] rightArmRot, float[] leftArmRot,
                                   float[] rightLegRot, float[] leftLegRot) {
        model.head.setAngles(headRot[0], headRot[1], headRot[2]);
        model.hat.setAngles(hatRot[0], hatRot[1], hatRot[2]);
        model.body.setAngles(bodyRot[0], bodyRot[1], bodyRot[2]);
        model.rightArm.setAngles(rightArmRot[0], rightArmRot[1], rightArmRot[2]);
        model.leftArm.setAngles(leftArmRot[0], leftArmRot[1], leftArmRot[2]);
        model.rightLeg.setAngles(rightLegRot[0], rightLegRot[1], rightLegRot[2]);
        model.leftLeg.setAngles(leftLegRot[0], leftLegRot[1], leftLegRot[2]);
    }
    
    /**
     * 设置所有外层部件的旋转，使它们跟随基础部件
     * @param bodyRot 身体旋转 [X, Y, Z]（度）
     * @param leftArmRot 左臂旋转 [X, Y, Z]（度）
     * @param rightArmRot 右臂旋转 [X, Y, Z]（度）
     * @param leftLegRot 左腿旋转 [X, Y, Z]（度）
     * @param rightLegRot 右腿旋转 [X, Y, Z]（度）
     */
    public void setOverlayPartsRotation(float[] bodyRot,
                                       float[] leftArmRot, float[] rightArmRot,
                                       float[] leftLegRot, float[] rightLegRot) {
        ModelPart leftSleeve = getLeftSleeve();
        if (leftSleeve != null) {
            leftSleeve.setAngles(leftArmRot[0], leftArmRot[1], leftArmRot[2]);
        }
        
        ModelPart rightSleeve = getRightSleeve();
        if (rightSleeve != null) {
            rightSleeve.setAngles(rightArmRot[0], rightArmRot[1], rightArmRot[2]);
        }
        
        ModelPart leftPants = getLeftPants();
        if (leftPants != null) {
            leftPants.setAngles(leftLegRot[0], leftLegRot[1], leftLegRot[2]);
        }
        
        ModelPart rightPants = getRightPants();
        if (rightPants != null) {
            rightPants.setAngles(rightLegRot[0], rightLegRot[1], rightLegRot[2]);
        }
        
        ModelPart jacket = getJacket();
        if (jacket != null) {
            // 身体的旋转通过 MatrixStack 应用，所以这里设为0
            jacket.setAngles(0, 0, 0);
        }
    }
    
    /**
     * 设置身体外层（jacket）的旋转
     * @param bodyRotX X轴旋转（度）
     * @param bodyRotY Y轴旋转（度）
     * @param bodyRotZ Z轴旋转（度）
     */
    public void setBodyOverlayRotation(float bodyRotX, float bodyRotY, float bodyRotZ) {
        ModelPart jacket = getJacket();
        if (jacket != null) {
            jacket.setAngles(bodyRotX, bodyRotY, bodyRotZ);
        }
    }
}
