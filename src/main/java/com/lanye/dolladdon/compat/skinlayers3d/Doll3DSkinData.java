package com.lanye.dolladdon.compat.skinlayers3d;

import net.minecraft.util.Identifier;

/**
 * 玩偶3D皮肤数据
 * 存储从3D皮肤层mod创建的3D网格数据
 */
public class Doll3DSkinData {
    private Object headMesh;      // Mesh对象（通过反射获取，类型为dev.tr7zw.skinlayers.api.Mesh）
    private Object torsoMesh;     // Mesh对象
    private Object leftArmMesh;   // Mesh对象
    private Object rightArmMesh;  // Mesh对象
    private Object leftLegMesh;   // Mesh对象
    private Object rightLegMesh;  // Mesh对象
    
    private Identifier currentSkin;  // 当前皮肤标识符
    private boolean thinArms;        // 是否为细手臂模型
    
    /**
     * 检查是否有有效的3D网格数据
     */
    public boolean hasValidData() {
        return headMesh != null || torsoMesh != null || 
               leftArmMesh != null || rightArmMesh != null ||
               leftLegMesh != null || rightLegMesh != null;
    }
    
    /**
     * 清除所有网格数据
     */
    public void clearMeshes() {
        headMesh = null;
        torsoMesh = null;
        leftArmMesh = null;
        rightArmMesh = null;
        leftLegMesh = null;
        rightLegMesh = null;
    }
    
    // Getters and Setters
    public Object getHeadMesh() {
        return headMesh;
    }
    
    public void setHeadMesh(Object headMesh) {
        this.headMesh = headMesh;
    }
    
    public Object getTorsoMesh() {
        return torsoMesh;
    }
    
    public void setTorsoMesh(Object torsoMesh) {
        this.torsoMesh = torsoMesh;
    }
    
    public Object getLeftArmMesh() {
        return leftArmMesh;
    }
    
    public void setLeftArmMesh(Object leftArmMesh) {
        this.leftArmMesh = leftArmMesh;
    }
    
    public Object getRightArmMesh() {
        return rightArmMesh;
    }
    
    public void setRightArmMesh(Object rightArmMesh) {
        this.rightArmMesh = rightArmMesh;
    }
    
    public Object getLeftLegMesh() {
        return leftLegMesh;
    }
    
    public void setLeftLegMesh(Object leftLegMesh) {
        this.leftLegMesh = leftLegMesh;
    }
    
    public Object getRightLegMesh() {
        return rightLegMesh;
    }
    
    public void setRightLegMesh(Object rightLegMesh) {
        this.rightLegMesh = rightLegMesh;
    }
    
    public Identifier getCurrentSkin() {
        return currentSkin;
    }
    
    public void setCurrentSkin(Identifier currentSkin) {
        this.currentSkin = currentSkin;
    }
    
    public boolean isThinArms() {
        return thinArms;
    }
    
    public void setThinArms(boolean thinArms) {
        this.thinArms = thinArms;
    }
}
