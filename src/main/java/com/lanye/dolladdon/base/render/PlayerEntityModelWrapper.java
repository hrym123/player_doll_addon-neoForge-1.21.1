package com.lanye.dolladdon.base.render;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;

/**
 * PlayerModel 封装类
 * 提供便捷的方法来访问 PlayerModel 的各个部件，包括私有字段
 */
public class PlayerEntityModelWrapper {
    private final PlayerModel<Player> model;
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
            leftSleeveField = PlayerModel.class.getDeclaredField("leftSleeve");
            leftSleeveField.setAccessible(true);
            
            rightSleeveField = PlayerModel.class.getDeclaredField("rightSleeve");
            rightSleeveField.setAccessible(true);
            
            leftPantsField = PlayerModel.class.getDeclaredField("leftPants");
            leftPantsField.setAccessible(true);
            
            rightPantsField = PlayerModel.class.getDeclaredField("rightPants");
            rightPantsField.setAccessible(true);
            
            jacketField = PlayerModel.class.getDeclaredField("jacket");
            jacketField.setAccessible(true);
            
            slimField = PlayerModel.class.getDeclaredField("slim");
            slimField.setAccessible(true);
            
            fieldsInitialized = true;
        } catch (NoSuchFieldException e) {
            // 字段不存在，忽略
        }
    }
    
    /**
     * 构造函数
     * @param model PlayerModel 实例
     */
    public PlayerEntityModelWrapper(PlayerModel<Player> model) {
        this.model = model;
        initializeFields();
        this.thinArms = isThinArms();
    }
    
    /**
     * 获取原始模型
     * @return PlayerModel 实例
     */
    public PlayerModel<Player> getModel() {
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
     * 注意：NeoForge使用setRotation方法，参数是弧度
     */
    public void setAllPartsRotation(float[] headRot, float[] hatRot, float[] bodyRot,
                                   float[] rightArmRot, float[] leftArmRot,
                                   float[] rightLegRot, float[] leftLegRot) {
        // 将度转换为弧度
        model.head.setRotation((float) Math.toRadians(headRot[0]), (float) Math.toRadians(headRot[1]), (float) Math.toRadians(headRot[2]));
        model.hat.setRotation((float) Math.toRadians(hatRot[0]), (float) Math.toRadians(hatRot[1]), (float) Math.toRadians(hatRot[2]));
        model.body.setRotation((float) Math.toRadians(bodyRot[0]), (float) Math.toRadians(bodyRot[1]), (float) Math.toRadians(bodyRot[2]));
        model.rightArm.setRotation((float) Math.toRadians(rightArmRot[0]), (float) Math.toRadians(rightArmRot[1]), (float) Math.toRadians(rightArmRot[2]));
        model.leftArm.setRotation((float) Math.toRadians(leftArmRot[0]), (float) Math.toRadians(leftArmRot[1]), (float) Math.toRadians(leftArmRot[2]));
        model.rightLeg.setRotation((float) Math.toRadians(rightLegRot[0]), (float) Math.toRadians(rightLegRot[1]), (float) Math.toRadians(rightLegRot[2]));
        model.leftLeg.setRotation((float) Math.toRadians(leftLegRot[0]), (float) Math.toRadians(leftLegRot[1]), (float) Math.toRadians(leftLegRot[2]));
    }
    
    /**
     * 设置所有外层部件的旋转，使它们跟随基础部件
     * 注意：NeoForge使用setRotation方法，参数是弧度
     */
    public void setOverlayPartsRotation(float[] bodyRot,
                                       float[] leftArmRot, float[] rightArmRot,
                                       float[] leftLegRot, float[] rightLegRot) {
        ModelPart leftSleeve = getLeftSleeve();
        if (leftSleeve != null) {
            leftSleeve.setRotation((float) Math.toRadians(leftArmRot[0]), (float) Math.toRadians(leftArmRot[1]), (float) Math.toRadians(leftArmRot[2]));
        }
        
        ModelPart rightSleeve = getRightSleeve();
        if (rightSleeve != null) {
            rightSleeve.setRotation((float) Math.toRadians(rightArmRot[0]), (float) Math.toRadians(rightArmRot[1]), (float) Math.toRadians(rightArmRot[2]));
        }
        
        ModelPart leftPants = getLeftPants();
        if (leftPants != null) {
            leftPants.setRotation((float) Math.toRadians(leftLegRot[0]), (float) Math.toRadians(leftLegRot[1]), (float) Math.toRadians(leftLegRot[2]));
        }
        
        ModelPart rightPants = getRightPants();
        if (rightPants != null) {
            rightPants.setRotation((float) Math.toRadians(rightLegRot[0]), (float) Math.toRadians(rightLegRot[1]), (float) Math.toRadians(rightLegRot[2]));
        }
        
        ModelPart jacket = getJacket();
        if (jacket != null) {
            // 身体的旋转通过 PoseStack 应用，所以这里设为0
            jacket.setRotation(0, 0, 0);
        }
    }
    
    /**
     * 设置身体外层（jacket）的旋转
     * 注意：NeoForge使用setRotation方法，参数是弧度
     */
    public void setBodyOverlayRotation(float bodyRotX, float bodyRotY, float bodyRotZ) {
        ModelPart jacket = getJacket();
        if (jacket != null) {
            jacket.setRotation((float) Math.toRadians(bodyRotX), (float) Math.toRadians(bodyRotY), (float) Math.toRadians(bodyRotZ));
        }
    }
}
