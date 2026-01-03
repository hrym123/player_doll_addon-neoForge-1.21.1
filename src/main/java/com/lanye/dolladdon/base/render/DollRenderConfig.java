package com.lanye.dolladdon.base.render;

/**
 * 玩偶渲染配置类
 * 用于配置默认状态下模型的大小尺寸和旋转
 * 
 * <p><b>修改外层皮肤旋转轴的方法：</b></p>
 * <pre>{@code
 * // 方法1：获取配置并修改
 * DollRenderConfig config = DollRenderConfig.createFromDefault();
 * DollRenderConfig.OverlayRotationConfig rotationConfig = config.getOverlayRotationConfig();
 * 
 * // 修改旋转顺序（例如：改为XYZ顺序）
 * rotationConfig.setRotationOrder(DollRenderConfig.RotationOrder.XYZ);
 * 
 * // 启用使用pivot点作为旋转中心
 * rotationConfig.setUsePivotAsRotationCenter(true);
 * 
 * // 设置旋转中心偏移 [X, Y, Z]
 * rotationConfig.setRotationCenterOffset(new float[]{0.1f, 0.0f, 0.0f});
 * 
 * // 方法2：创建新的旋转配置
 * DollRenderConfig.OverlayRotationConfig newConfig = new DollRenderConfig.OverlayRotationConfig(
 *     DollRenderConfig.RotationOrder.XYZ,  // 旋转顺序
 *     true,                                  // 使用pivot点
 *     new float[]{0.1f, 0.0f, 0.0f}         // 旋转中心偏移
 * );
 * config.setOverlayRotationConfig(newConfig);
 * }</pre>
 */
public class DollRenderConfig {
    
    /**
     * 模型基础缩放因子
     * 玩家模型高度约为1.8，应用此缩放后高度为0.9
     * 默认值：0.5F
     */
    private float modelScale = 0.5F;
    
    /**
     * Y偏移系数
     * 用于计算Y偏移以保持模型底部对齐碰撞箱底部
     * 计算公式：yOffset = yOffsetMultiplier * scale[1]
     * 默认值：0.75f
     * 
     * 说明：
     * - 玩家模型高度约为1.8，应用modelScale(=0.5)后高度为0.9
     * - 应用scale[1]后，模型高度变为0.9 * scale[1]
     * - 变换顺序：translate(yOffset) -> scale(modelScale) -> scale(scale[1])
     * - 由于scale以当前位置为中心，最终模型中心在yOffset，模型底部在 yOffset - 0.45 * scale[1]
     * - 为了保持模型底部对齐碰撞箱底部（y=0），理论上需要：yOffset = 0.45 * scale[1]
     * - 注意：这里0.45 = 1.8 * modelScale / 2 = 0.9 / 2
     * - 实际使用的系数为0.75f，是因为调整完毕后，只有向上偏移0.75才能正常显示，否则模型会显示在点击位置的方块下面
     */
    private float yOffsetMultiplier = 0.75f;
    
    /**
     * 玩家模型原始高度（单位：格）
     * 默认值：1.8F
     */
    private float playerModelHeight = 1.8F;
    
    /**
     * 实体Y轴旋转偏移（度）
     * 在应用实体Y旋转时，会先加上此偏移值
     * 默认值：180.0F（使模型面向正确方向）
     */
    private float entityYawOffset = 180.0F;
    
    /**
     * 身体旋转中心Y坐标
     * 用于在应用身体旋转时确定旋转中心点
     * 默认值：0.375f（身体和头连接处）
     */
    private float bodyRotationCenterY = 0.375f;
    
    /**
     * 3D皮肤层大小倍数
     * 用于放大3D皮肤层以形成更好的3D效果
     * 默认值：1.20f（放大20%）
     */
    private float skin3DSizeMultiplier = 1.20f;
    
    /**
     * 3D皮肤层偏移距离
     * 用于手动应用3D偏移，使外层皮肤稍微向外偏移以形成3D效果
     * 默认值：0.01f
     */
    private float skin3DOffsetDistance = 0.01f;
    
    // ========== 各部件默认配置 ==========
    
    /**
     * 头部默认配置（旋转、位置、缩放）
     * 默认旋转：[0, 0, 0]（度）
     * 默认位置：[0, 0, 0]
     * 默认缩放：[1, 1, 1]
     */
    private PartConfig headConfig = new PartConfig(
        new float[]{0.0f, 0.0f, 0.0f},  // rotation
        new float[]{0.0f, 0.0f, 0.0f},  // position
        new float[]{1.0f, 1.0f, 1.0f}   // scale
    );
    
    /**
     * 帽子默认配置（旋转、位置、缩放）
     * 默认旋转：[0, 0, 0]（度）
     * 默认位置：[0, 0, 0]
     * 默认缩放：[1, 1, 1]
     */
    private PartConfig hatConfig = new PartConfig(
        new float[]{0.0f, 0.0f, 0.0f},  // rotation
        new float[]{0.0f, 0.0f, 0.0f},  // position
        new float[]{1.0f, 1.0f, 1.0f}   // scale
    );
    
    /**
     * 身体默认配置（旋转、位置、缩放）
     * 默认旋转：[0, 0, 0]（度）
     * 默认位置：[0, 0, 0]
     * 默认缩放：[1, 1, 1]
     */
    private PartConfig bodyConfig = new PartConfig(
        new float[]{0.0f, 0.0f, 0.0f},  // rotation
        new float[]{0.0f, 0.0f, 0.0f},  // position
        new float[]{1.0f, 1.0f, 1.0f}   // scale
    );
    
    /**
     * 右臂默认配置（旋转、位置、缩放）
     * 默认旋转：[-40, 0, 0]（度，约-0.6981317弧度）
     * 默认位置：[0, 0, 0]
     * 默认缩放：[1, 1, 1]
     */
    private PartConfig rightArmConfig = new PartConfig(
        new float[]{-40.0f, 0.0f, 0.0f},  // rotation (约-0.6981317弧度)
        new float[]{0.0f, 0.0f, 0.0f},    // position
        new float[]{1.0f, 1.0f, 1.0f}     // scale
    );
    
    /**
     * 左臂默认配置（旋转、位置、缩放）
     * 默认旋转：[40, 0, 0]（度，约0.6981317弧度）
     * 默认位置：[0, 0, 0]
     * 默认缩放：[1, 1, 1]
     */
    private PartConfig leftArmConfig = new PartConfig(
        new float[]{40.0f, 0.0f, 0.0f},   // rotation (约0.6981317弧度)
        new float[]{0.0f, 0.0f, 0.0f},    // position
        new float[]{1.0f, 1.0f, 1.0f}     // scale
    );
    
    /**
     * 右腿默认配置（旋转、位置、缩放）
     * 默认旋转：[0, 0, 0]（度）
     * 默认位置：[0, 0, 0]
     * 默认缩放：[1, 1, 1]
     */
    private PartConfig rightLegConfig = new PartConfig(
        new float[]{0.0f, 0.0f, 0.0f},  // rotation
        new float[]{0.0f, 0.0f, 0.0f},  // position
        new float[]{1.0f, 1.0f, 1.0f}   // scale
    );
    
    /**
     * 左腿默认配置（旋转、位置、缩放）
     * 默认旋转：[0, 0, 0]（度）
     * 默认位置：[0, 0, 0]
     * 默认缩放：[1, 1, 1]
     */
    private PartConfig leftLegConfig = new PartConfig(
        new float[]{0.0f, 0.0f, 0.0f},  // rotation
        new float[]{0.0f, 0.0f, 0.0f},  // position
        new float[]{1.0f, 1.0f, 1.0f}   // scale
    );
    
    /**
     * 旋转顺序枚举
     * 用于定义欧拉角的旋转顺序
     * 
     * <p><b>旋转顺序说明：</b></p>
     * <ul>
     *   <li><b>ZYX</b>：先绕Z轴，再绕Y轴，最后绕X轴（Minecraft默认）</li>
     *   <li><b>XYZ</b>：先绕X轴，再绕Y轴，最后绕Z轴</li>
     *   <li><b>YXZ</b>：先绕Y轴，再绕X轴，最后绕Z轴</li>
     *   <li><b>XZY</b>：先绕X轴，再绕Z轴，最后绕Y轴</li>
     *   <li><b>YZX</b>：先绕Y轴，再绕Z轴，最后绕X轴</li>
     *   <li><b>ZXY</b>：先绕Z轴，再绕X轴，最后绕Y轴</li>
     * </ul>
     */
    public enum RotationOrder {
        /** Z-Y-X顺序（默认，Minecraft常用） */
        ZYX,
        /** X-Y-Z顺序 */
        XYZ,
        /** Y-X-Z顺序 */
        YXZ,
        /** X-Z-Y顺序 */
        XZY,
        /** Y-Z-X顺序 */
        YZX,
        /** Z-X-Y顺序 */
        ZXY
    }
    
    /**
     * 外层皮肤旋转轴配置
     * 用于配置3D皮肤层渲染时的旋转轴设置
     * 
     * <p><b>配置项说明：</b></p>
     * <ul>
     *   <li><b>rotationOrder</b>：旋转顺序，控制X、Y、Z轴旋转的应用顺序</li>
     *   <li><b>usePivotAsRotationCenter</b>：是否使用ModelPart的pivot点作为旋转中心
     *       <ul>
     *         <li>true：以pivot点（部件连接点）为旋转中心，适合需要围绕连接点旋转的情况</li>
     *         <li>false：以部件当前位置为旋转中心（默认）</li>
     *       </ul>
     *   </li>
     *   <li><b>rotationCenterOffset</b>：自定义旋转中心偏移 [X, Y, Z]
     *       <ul>
     *         <li>可以微调旋转中心的位置</li>
     *         <li>如果usePivotAsRotationCenter为true，偏移会叠加到pivot点上</li>
     *       </ul>
     *   </li>
     * </ul>
     * 
     * <p><b>注意：</b>腿部部件会自动使用pivot点作为旋转中心，即使此配置为false</p>
     */
    public static class OverlayRotationConfig {
        /**
         * 旋转顺序
         * 默认值：ZYX（与Minecraft默认一致）
         */
        private RotationOrder rotationOrder = RotationOrder.ZYX;
        
        /**
         * 是否使用ModelPart的pivot点作为旋转中心
         * 如果为true，会在旋转前移动到pivot点，旋转后再移回
         * 默认值：false（使用部件当前位置作为旋转中心）
         * 
         * <p>使用pivot点的效果：</p>
         * <ul>
         *   <li>部件会围绕其连接点（pivot点）旋转，而不是围绕部件中心旋转</li>
         *   <li>这对于手臂、腿部等需要围绕连接点旋转的部件很有用</li>
         *   <li>例如：手臂会围绕肩膀连接点旋转，而不是手臂中心</li>
         * </ul>
         */
        private boolean usePivotAsRotationCenter = false;
        
        /**
         * 自定义旋转中心偏移 [X, Y, Z]
         * 相对于部件位置的旋转中心偏移
         * 默认值：[0, 0, 0]
         * 
         * <p>使用示例：</p>
         * <ul>
         *   <li>[0.1f, 0.0f, 0.0f]：将旋转中心向右偏移0.1格</li>
         *   <li>[0.0f, 0.2f, 0.0f]：将旋转中心向上偏移0.2格</li>
         *   <li>[0.0f, 0.0f, -0.1f]：将旋转中心向前偏移0.1格</li>
         * </ul>
         */
        private float[] rotationCenterOffset = new float[]{0.0f, 0.0f, 0.0f};
        
        public OverlayRotationConfig() {
        }
        
        public OverlayRotationConfig(RotationOrder rotationOrder, boolean usePivotAsRotationCenter, float[] rotationCenterOffset) {
            this.rotationOrder = rotationOrder != null ? rotationOrder : RotationOrder.ZYX;
            this.usePivotAsRotationCenter = usePivotAsRotationCenter;
            this.rotationCenterOffset = rotationCenterOffset != null ? rotationCenterOffset.clone() : new float[]{0.0f, 0.0f, 0.0f};
        }
        
        public RotationOrder getRotationOrder() {
            return rotationOrder;
        }
        
        public boolean isUsePivotAsRotationCenter() {
            return usePivotAsRotationCenter;
        }
        
        public float[] getRotationCenterOffset() {
            return rotationCenterOffset.clone();
        }
        
        /**
         * 设置旋转顺序
         * @param rotationOrder 旋转顺序
         * @return 当前配置对象（支持链式调用）
         */
        public OverlayRotationConfig setRotationOrder(RotationOrder rotationOrder) {
            this.rotationOrder = rotationOrder != null ? rotationOrder : RotationOrder.ZYX;
            return this;
        }
        
        /**
         * 设置是否使用pivot点作为旋转中心
         * @param usePivotAsRotationCenter 是否使用pivot点
         * @return 当前配置对象（支持链式调用）
         */
        public OverlayRotationConfig setUsePivotAsRotationCenter(boolean usePivotAsRotationCenter) {
            this.usePivotAsRotationCenter = usePivotAsRotationCenter;
            return this;
        }
        
        /**
         * 设置旋转中心偏移
         * @param rotationCenterOffset 旋转中心偏移 [X, Y, Z]
         * @return 当前配置对象（支持链式调用）
         */
        public OverlayRotationConfig setRotationCenterOffset(float[] rotationCenterOffset) {
            this.rotationCenterOffset = rotationCenterOffset != null ? rotationCenterOffset.clone() : new float[]{0.0f, 0.0f, 0.0f};
            return this;
        }
        
        public OverlayRotationConfig copy() {
            return new OverlayRotationConfig(rotationOrder, usePivotAsRotationCenter, rotationCenterOffset);
        }
        
        @Override
        public String toString() {
            return String.format(
                "OverlayRotationConfig{rotationOrder=%s, usePivot=%s, centerOffset=[%.3f, %.3f, %.3f]}",
                rotationOrder, usePivotAsRotationCenter,
                rotationCenterOffset[0], rotationCenterOffset[1], rotationCenterOffset[2]
            );
        }
    }
    
    /**
     * 外层皮肤旋转轴配置
     * 用于配置3D皮肤层渲染时的旋转轴设置
     * 默认值：ZYX旋转顺序，不使用pivot点，无旋转中心偏移
     */
    private OverlayRotationConfig overlayRotationConfig = new OverlayRotationConfig();
    
    /**
     * 部件配置类
     * 用于存储单个部件的旋转、位置和缩放信息
     */
    public static class PartConfig {
        private final float[] rotation;  // [X, Y, Z] 旋转角度（度）
        private final float[] position;  // [X, Y, Z] 位置偏移
        private final float[] scale;     // [X, Y, Z] 缩放因子
        
        public PartConfig(float[] rotation, float[] position, float[] scale) {
            this.rotation = rotation != null ? rotation.clone() : new float[]{0.0f, 0.0f, 0.0f};
            this.position = position != null ? position.clone() : new float[]{0.0f, 0.0f, 0.0f};
            this.scale = scale != null ? scale.clone() : new float[]{1.0f, 1.0f, 1.0f};
        }
        
        public float[] getRotation() {
            return rotation.clone();
        }
        
        public float[] getPosition() {
            return position.clone();
        }
        
        public float[] getScale() {
            return scale.clone();
        }
        
        public PartConfig copy() {
            return new PartConfig(rotation, position, scale);
        }
        
        @Override
        public String toString() {
            return String.format(
                "PartConfig{rotation=[%.2f, %.2f, %.2f], position=[%.3f, %.3f, %.3f], scale=[%.2f, %.2f, %.2f]}",
                rotation[0], rotation[1], rotation[2],
                position[0], position[1], position[2],
                scale[0], scale[1], scale[2]
            );
        }
    }
    
    /**
     * 默认配置实例（单例模式）
     */
    private static final DollRenderConfig DEFAULT = new DollRenderConfig();
    
    /**
     * 获取默认配置实例
     * @return 默认配置实例
     */
    public static DollRenderConfig getDefault() {
        return DEFAULT;
    }
    
    /**
     * 创建新的配置实例（使用默认值）
     * @return 新的配置实例
     */
    public static DollRenderConfig create() {
        return new DollRenderConfig();
    }
    
    /**
     * 创建新的配置实例（复制自默认配置）
     * @return 新的配置实例（包含默认值）
     */
    public static DollRenderConfig createFromDefault() {
        return DEFAULT.copy();
    }
    
    // Getter 方法
    
    public float getModelScale() {
        return modelScale;
    }
    
    public float getYOffsetMultiplier() {
        return yOffsetMultiplier;
    }
    
    public float getPlayerModelHeight() {
        return playerModelHeight;
    }
    
    public float getEntityYawOffset() {
        return entityYawOffset;
    }
    
    public float getBodyRotationCenterY() {
        return bodyRotationCenterY;
    }
    
    public float getSkin3DSizeMultiplier() {
        return skin3DSizeMultiplier;
    }
    
    public float getSkin3DOffsetDistance() {
        return skin3DOffsetDistance;
    }
    
    public OverlayRotationConfig getOverlayRotationConfig() {
        return overlayRotationConfig;
    }
    
    // ========== 各部件配置 Getter 方法 ==========
    
    public PartConfig getHeadConfig() {
        return headConfig;
    }
    
    public PartConfig getHatConfig() {
        return hatConfig;
    }
    
    public PartConfig getBodyConfig() {
        return bodyConfig;
    }
    
    public PartConfig getRightArmConfig() {
        return rightArmConfig;
    }
    
    public PartConfig getLeftArmConfig() {
        return leftArmConfig;
    }
    
    public PartConfig getRightLegConfig() {
        return rightLegConfig;
    }
    
    public PartConfig getLeftLegConfig() {
        return leftLegConfig;
    }
    
    // Setter 方法
    
    public DollRenderConfig setModelScale(float modelScale) {
        this.modelScale = modelScale;
        return this; // 支持链式调用
    }
    
    public DollRenderConfig setYOffsetMultiplier(float yOffsetMultiplier) {
        this.yOffsetMultiplier = yOffsetMultiplier;
        return this;
    }
    
    public DollRenderConfig setPlayerModelHeight(float playerModelHeight) {
        this.playerModelHeight = playerModelHeight;
        return this;
    }
    
    public DollRenderConfig setEntityYawOffset(float entityYawOffset) {
        this.entityYawOffset = entityYawOffset;
        return this;
    }
    
    public DollRenderConfig setBodyRotationCenterY(float bodyRotationCenterY) {
        this.bodyRotationCenterY = bodyRotationCenterY;
        return this;
    }
    
    public DollRenderConfig setSkin3DSizeMultiplier(float skin3DSizeMultiplier) {
        this.skin3DSizeMultiplier = skin3DSizeMultiplier;
        return this;
    }
    
    public DollRenderConfig setSkin3DOffsetDistance(float skin3DOffsetDistance) {
        this.skin3DOffsetDistance = skin3DOffsetDistance;
        return this;
    }
    
    /**
     * 设置外层皮肤旋转轴配置
     * @param overlayRotationConfig 旋转轴配置对象
     * @return 当前配置对象（支持链式调用）
     */
    public DollRenderConfig setOverlayRotationConfig(OverlayRotationConfig overlayRotationConfig) {
        this.overlayRotationConfig = overlayRotationConfig != null ? overlayRotationConfig.copy() : new OverlayRotationConfig();
        return this;
    }
    
    // ========== 各部件配置 Setter 方法 ==========
    
    public DollRenderConfig setHeadConfig(PartConfig headConfig) {
        this.headConfig = headConfig != null ? headConfig.copy() : new PartConfig(null, null, null);
        return this;
    }
    
    public DollRenderConfig setHeadConfig(float[] rotation, float[] position, float[] scale) {
        this.headConfig = new PartConfig(rotation, position, scale);
        return this;
    }
    
    public DollRenderConfig setHatConfig(PartConfig hatConfig) {
        this.hatConfig = hatConfig != null ? hatConfig.copy() : new PartConfig(null, null, null);
        return this;
    }
    
    public DollRenderConfig setHatConfig(float[] rotation, float[] position, float[] scale) {
        this.hatConfig = new PartConfig(rotation, position, scale);
        return this;
    }
    
    public DollRenderConfig setBodyConfig(PartConfig bodyConfig) {
        this.bodyConfig = bodyConfig != null ? bodyConfig.copy() : new PartConfig(null, null, null);
        return this;
    }
    
    public DollRenderConfig setBodyConfig(float[] rotation, float[] position, float[] scale) {
        this.bodyConfig = new PartConfig(rotation, position, scale);
        return this;
    }
    
    public DollRenderConfig setRightArmConfig(PartConfig rightArmConfig) {
        this.rightArmConfig = rightArmConfig != null ? rightArmConfig.copy() : new PartConfig(null, null, null);
        return this;
    }
    
    public DollRenderConfig setRightArmConfig(float[] rotation, float[] position, float[] scale) {
        this.rightArmConfig = new PartConfig(rotation, position, scale);
        return this;
    }
    
    public DollRenderConfig setLeftArmConfig(PartConfig leftArmConfig) {
        this.leftArmConfig = leftArmConfig != null ? leftArmConfig.copy() : new PartConfig(null, null, null);
        return this;
    }
    
    public DollRenderConfig setLeftArmConfig(float[] rotation, float[] position, float[] scale) {
        this.leftArmConfig = new PartConfig(rotation, position, scale);
        return this;
    }
    
    public DollRenderConfig setRightLegConfig(PartConfig rightLegConfig) {
        this.rightLegConfig = rightLegConfig != null ? rightLegConfig.copy() : new PartConfig(null, null, null);
        return this;
    }
    
    public DollRenderConfig setRightLegConfig(float[] rotation, float[] position, float[] scale) {
        this.rightLegConfig = new PartConfig(rotation, position, scale);
        return this;
    }
    
    public DollRenderConfig setLeftLegConfig(PartConfig leftLegConfig) {
        this.leftLegConfig = leftLegConfig != null ? leftLegConfig.copy() : new PartConfig(null, null, null);
        return this;
    }
    
    public DollRenderConfig setLeftLegConfig(float[] rotation, float[] position, float[] scale) {
        this.leftLegConfig = new PartConfig(rotation, position, scale);
        return this;
    }
    
    /**
     * 计算Y偏移值
     * @param scaleY 姿态的Y轴缩放值
     * @return 计算得到的Y偏移值
     */
    public float calculateYOffset(float scaleY) {
        return yOffsetMultiplier * scaleY;
    }
    
    /**
     * 计算应用缩放后的模型高度
     * @param scaleY 姿态的Y轴缩放值
     * @return 应用缩放后的模型高度
     */
    public float calculateScaledHeight(float scaleY) {
        return playerModelHeight * modelScale * scaleY;
    }
    
    /**
     * 计算模型中心到模型底部的距离
     * @param scaleY 姿态的Y轴缩放值
     * @return 模型中心到模型底部的距离
     */
    public float calculateModelCenterToBottom(float scaleY) {
        return calculateScaledHeight(scaleY) / 2.0f;
    }
    
    /**
     * 复制当前配置
     * @return 新的配置实例，包含当前配置的所有值
     */
    public DollRenderConfig copy() {
        DollRenderConfig copy = new DollRenderConfig();
        copy.modelScale = this.modelScale;
        copy.yOffsetMultiplier = this.yOffsetMultiplier;
        copy.playerModelHeight = this.playerModelHeight;
        copy.entityYawOffset = this.entityYawOffset;
        copy.bodyRotationCenterY = this.bodyRotationCenterY;
        copy.skin3DSizeMultiplier = this.skin3DSizeMultiplier;
        copy.skin3DOffsetDistance = this.skin3DOffsetDistance;
        copy.overlayRotationConfig = this.overlayRotationConfig.copy();
        
        // 复制各部件配置
        copy.headConfig = this.headConfig.copy();
        copy.hatConfig = this.hatConfig.copy();
        copy.bodyConfig = this.bodyConfig.copy();
        copy.rightArmConfig = this.rightArmConfig.copy();
        copy.leftArmConfig = this.leftArmConfig.copy();
        copy.rightLegConfig = this.rightLegConfig.copy();
        copy.leftLegConfig = this.leftLegConfig.copy();
        
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format(
            "DollRenderConfig{" +
            "modelScale=%.2f, yOffsetMultiplier=%.2f, playerModelHeight=%.2f, " +
            "entityYawOffset=%.2f, bodyRotationCenterY=%.3f, skin3DSizeMultiplier=%.2f, skin3DOffsetDistance=%.3f, " +
            "overlayRotation=%s, " +
            "head=%s, hat=%s, body=%s, rightArm=%s, leftArm=%s, rightLeg=%s, leftLeg=%s}",
            modelScale, yOffsetMultiplier, playerModelHeight, entityYawOffset,
            bodyRotationCenterY, skin3DSizeMultiplier, skin3DOffsetDistance,
            overlayRotationConfig,
            headConfig, hatConfig, bodyConfig, rightArmConfig, leftArmConfig, rightLegConfig, leftLegConfig
        );
    }
}
