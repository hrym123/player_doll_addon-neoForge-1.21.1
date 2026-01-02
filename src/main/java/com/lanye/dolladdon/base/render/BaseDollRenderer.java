package com.lanye.dolladdon.base.render;

import com.lanye.dolladdon.PlayerDollAddonClient;
import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import com.lanye.dolladdon.util.skinlayers3d.Doll3DSkinData;
import com.lanye.dolladdon.util.skinlayers3d.Doll3DSkinUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Method;
/**
 * 玩偶实体渲染器基类
 * 提供所有玩偶实体渲染器的共同功能
 */
public abstract class BaseDollRenderer<T extends BaseDollEntity> extends EntityRenderer<T> {
    // 日志模块名称
    private static final String LOG_MODULE = "3d_skin_layers";

    // 日志控制标志（避免重复输出）
    private static boolean hasLogged3DRenderStart = false;
    private static boolean hasLoggedOffsetProviderStatus = false;
    private static boolean hasLoggedMeshCreation = false;
    private static boolean hasLoggedRenderCheck = false;
    private static boolean hasLogged2DRenderStart = false;
    private static boolean hasLogged2DRenderParts = false;
    private static boolean hasLoggedDistanceCheck = false;
    private static boolean hasLoggedSkinCheck = false;
    
    // 反射方法缓存（避免每帧都查找）
    private static Method cachedCopyFromMethod = null;
    private static Method cachedIsVisibleMethod = null;
    private static Method cachedSetVisibleMethod = null;
    private static Method cachedRenderMethod = null; // 缓存找到的render方法
    private static Class<?> cachedPoseStackClass = null;
    private static boolean renderMethodCacheInitialized = false;
    
    protected final PlayerEntityModel<PlayerEntity> playerModel;
    private final boolean thinArms;  // 是否为细手臂模型
    
    protected BaseDollRenderer(EntityRendererFactory.Context context, PlayerEntityModel<PlayerEntity> playerModel) {
        super(context);
        this.playerModel = playerModel;
        // 通过反射获取slim字段来判断是否为细手臂
        this.thinArms = isThinArmsModel(playerModel);
    }
    
    /**
     * 判断模型是否为细手臂模型
     */
    private boolean isThinArmsModel(PlayerEntityModel<PlayerEntity> model) {
        try {
            java.lang.reflect.Field slimField = PlayerEntityModel.class.getDeclaredField("slim");
            slimField.setAccessible(true);
            return slimField.getBoolean(model);
        } catch (Exception e) {
            // 如果无法访问字段，默认使用粗手臂
            return false;
        }
    }
    
    /**
     * 获取皮肤资源位置
     * @param entity 实体
     * @return 皮肤资源位置
     */
    protected abstract Identifier getSkinLocation(T entity);
    
    @Override
    public void render(T entity, float entityYaw, float partialTick, 
                      MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int light) {
        matrixStack.push();
        
        // 应用旋转
        float yRot = MathHelper.lerp(partialTick, entity.prevYaw, entity.getYaw());
        float xRot = MathHelper.lerp(partialTick, entity.prevPitch, entity.getPitch());
        
        matrixStack.multiply(new Quaternionf().rotateY((float) Math.toRadians(180.0F - yRot)));
        matrixStack.multiply(new Quaternionf().rotateX((float) Math.toRadians(xRot)));
        
        float modelScale = 0.5F; 
        
        // 获取皮肤位置（由子类实现）
        Identifier skinLocation = getSkinLocation(entity);
        
        // 从实体获取当前姿态
        com.lanye.dolladdon.api.pose.DollPose pose = entity.getCurrentPose();
        if (pose == null) {
            // 如果没有姿态，使用默认站立姿态
            pose = com.lanye.dolladdon.api.pose.SimpleDollPose.createDefaultStandingPose();
        }
        
        // 获取姿态的scale，用于计算Y偏移以保持模型底部对齐碰撞箱底部
        float[] scale = pose.getScale();
        // 玩家模型高度约为1.8，应用modelScale(=0.5)后高度为0.9
        // 应用scale[1]后，模型高度变为0.9 * scale[1]
        // 变换顺序：translate(yOffset) -> scale(modelScale) -> scale(scale[1])
        // 由于scale以当前位置为中心，最终模型中心在yOffset，模型底部在 yOffset - 0.45 * scale[1]
        // 为了保持模型底部对齐碰撞箱底部（y=0），需要：yOffset = 0.45 * scale[1]
        // 注意：这里0.45 = 1.8 * modelScale / 2 = 0.9 / 2
        float yOffset = 0.75f * scale[1];
        
        matrixStack.translate(0.0, yOffset, 0.0);
        
        // 应用缩放和翻转
        matrixStack.scale(-modelScale, -modelScale, modelScale);
        
        // 应用姿态的位置和大小
        float[] position = pose.getPosition();
        if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
            matrixStack.translate(position[0], -position[1], position[2]);
        }
        if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
            matrixStack.scale(scale[0], scale[1], scale[2]);
        }
        
        // 从姿态获取旋转角度
        float[] headRot = pose.getHeadRotation();
        float[] hatRot = pose.getHatRotation();
        float[] bodyRot = pose.getBodyRotation();
        float[] rightArmRot = pose.getRightArmRotation();
        float[] leftArmRot = pose.getLeftArmRotation();
        float[] rightLegRot = pose.getRightLegRotation();
        float[] leftLegRot = pose.getLeftLegRotation();
        
        // 从姿态获取各部件的位置和缩放
        float[] headPosition = pose.getHeadPosition();
        float[] headScale = pose.getHeadScale();
        float[] hatPosition = pose.getHatPosition();
        float[] hatScale = pose.getHatScale();
        
        // hat 应该跟随 head 的缩放，所以使用 headScale 和 hatScale 的组合
        float[] hatCombinedScale = new float[]{
            headScale[0] * hatScale[0],
            headScale[1] * hatScale[1],
            headScale[2] * hatScale[2]
        };
        float[] bodyPosition = pose.getBodyPosition();
        float[] bodyScale = pose.getBodyScale();
        float[] rightArmPosition = pose.getRightArmPosition();
        float[] rightArmScale = pose.getRightArmScale();
        float[] leftArmPosition = pose.getLeftArmPosition();
        float[] leftArmScale = pose.getLeftArmScale();
        float[] rightLegPosition = pose.getRightLegPosition();
        float[] rightLegScale = pose.getRightLegScale();
        float[] leftLegPosition = pose.getLeftLegPosition();
        float[] leftLegScale = pose.getLeftLegScale();
        
        float headRotX = headRot[0], headRotY = headRot[1], headRotZ = headRot[2];
        float hatRotX = hatRot[0], hatRotY = hatRot[1], hatRotZ = hatRot[2];
        float bodyRotX = bodyRot[0], bodyRotY = bodyRot[1], bodyRotZ = bodyRot[2];
        float rightArmRotX = rightArmRot[0], rightArmRotY = rightArmRot[1], rightArmRotZ = rightArmRot[2];
        float leftArmRotX = leftArmRot[0], leftArmRotY = leftArmRot[1], leftArmRotZ = leftArmRot[2];
        float rightLegRotX = rightLegRot[0], rightLegRotY = rightLegRot[1], rightLegRotZ = rightLegRot[2];
        float leftLegRotX = leftLegRot[0], leftLegRotY = leftLegRot[1], leftLegRotZ = leftLegRot[2];
        
        // 注意：如果身体有旋转，头部、手臂、身体和腿部都在身体的旋转坐标系中渲染
        // 它们的旋转值都是相对于身体的，所以直接设置即可
        playerModel.head.setAngles(headRotX, headRotY, headRotZ);
        playerModel.hat.setAngles(hatRotX, hatRotY, hatRotZ);
        // 注意：身体的旋转通过 MatrixStack 应用，不在这里设置，避免双重旋转
        playerModel.rightArm.setAngles(rightArmRotX, rightArmRotY, rightArmRotZ);
        playerModel.leftArm.setAngles(leftArmRotX, leftArmRotY, leftArmRotZ);
        playerModel.rightLeg.setAngles(rightLegRotX, rightLegRotY, rightLegRotZ);
        playerModel.leftLeg.setAngles(leftLegRotX, leftLegRotY, leftLegRotZ);
        
        // 同时设置外层部分的旋转，使它们跟随基础部分的动作
        // 注意：身体的旋转通过 PoseStack 应用，所以 jacket 的旋转也设为0
        setOverlayPartsRotation(0, 0, 0, // 身体旋转通过 PoseStack 应用
                               leftArmRotX, leftArmRotY, leftArmRotZ,
                               rightArmRotX, rightArmRotY, rightArmRotZ,
                               leftLegRotX, leftLegRotY, leftLegRotZ,
                               rightLegRotX, rightLegRotY, rightLegRotZ);
        
        // 获取渲染类型
        var cutoutRenderType = net.minecraft.client.render.RenderLayer.getEntityCutoutNoCull(skinLocation);
        var translucentRenderType = net.minecraft.client.render.RenderLayer.getEntityTranslucent(skinLocation);
        int overlay = net.minecraft.client.render.OverlayTexture.DEFAULT_UV;
        
        // 第一步：渲染基础层（base layer）
        var baseVertexConsumer = vertexConsumerProvider.getBuffer(cutoutRenderType);
        
        // 如果有身体旋转，使用 MatrixStack 在身体旋转中心应用旋转，然后渲染身体、头部、手臂和腿部
        if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
            matrixStack.push();
            
            // 移动到身体的旋转中心（身体和头连接处，Y坐标约为0.375）
            float rotationCenterY = 0.375f;
            matrixStack.translate(0.0, rotationCenterY, 0.0);
            
            // 应用身体旋转（只在这里应用，不在 setRotation 中设置）
            matrixStack.multiply(new Quaternionf().rotateX(bodyRotX));
            matrixStack.multiply(new Quaternionf().rotateY(bodyRotY));
            matrixStack.multiply(new Quaternionf().rotateZ(bodyRotZ));
            
            // 移回旋转中心
            matrixStack.translate(0.0, -rotationCenterY, 0.0);
            // 在旋转后的坐标系中渲染身体、头部、手臂和腿部
            // 注意：头部和手臂的旋转值已经是相对于身体的，所以保持它们的旋转值
            playerModel.body.setAngles(0, 0, 0); // 确保身体不额外旋转（旋转已通过 MatrixStack 应用）
            // 头部和手臂保持它们自己的相对旋转值（headRotX等已经在上面设置）
            renderPartWithTransform(matrixStack, playerModel.body, baseVertexConsumer, light, overlay, bodyPosition, bodyScale);
            renderPartWithTransform(matrixStack, playerModel.head, baseVertexConsumer, light, overlay, headPosition, headScale);
            renderPartWithTransform(matrixStack, playerModel.rightArm, baseVertexConsumer, light, overlay, rightArmPosition, rightArmScale);
            renderPartWithTransform(matrixStack, playerModel.leftArm, baseVertexConsumer, light, overlay, leftArmPosition, leftArmScale);
            renderPartWithTransform(matrixStack, playerModel.rightLeg, baseVertexConsumer, light, overlay, rightLegPosition, rightLegScale);
            renderPartWithTransform(matrixStack, playerModel.leftLeg, baseVertexConsumer, light, overlay, leftLegPosition, leftLegScale);
            matrixStack.pop();
        } else {
            // 没有身体旋转时，正常渲染
            playerModel.body.setAngles(0, 0, 0);
            renderPartWithTransform(matrixStack, playerModel.body, baseVertexConsumer, light, overlay, bodyPosition, bodyScale);
            renderPartWithTransform(matrixStack, playerModel.head, baseVertexConsumer, light, overlay, headPosition, headScale);
            renderPartWithTransform(matrixStack, playerModel.rightArm, baseVertexConsumer, light, overlay, rightArmPosition, rightArmScale);
            renderPartWithTransform(matrixStack, playerModel.leftArm, baseVertexConsumer, light, overlay, leftArmPosition, leftArmScale);
            renderPartWithTransform(matrixStack, playerModel.rightLeg, baseVertexConsumer, light, overlay, rightLegPosition, rightLegScale);
            renderPartWithTransform(matrixStack, playerModel.leftLeg, baseVertexConsumer, light, overlay, leftLegPosition, leftLegScale);
        }
        
        // 第二步：渲染外层（overlay layer）- 使用半透明渲染以正确显示多层皮肤
        var overlayVertexConsumer = vertexConsumerProvider.getBuffer(translucentRenderType);

        // 检查是否使用3D皮肤层渲染
        boolean modLoaded = PlayerDollAddonClient.IS_3D_SKIN_LAYERS_LOADED;
        boolean apiAvailable = Doll3DSkinUtil.isAvailable();
        boolean inRange = shouldUse3DSkinLayers(entity);
        boolean use3DSkinLayers = modLoaded && apiAvailable && inRange;

        // 只在第一次渲染时记录检查结果，避免每帧都输出日志导致卡顿
        if (!hasLoggedRenderCheck) {
        ModuleLogger.debug(LOG_MODULE, "渲染检查: modLoaded={}, apiAvailable={}, inRange={}, use3D={}",
                modLoaded, apiAvailable, inRange, use3DSkinLayers);
            hasLoggedRenderCheck = true;
        }

        // 第三步：延迟渲染3D网格 - 在所有其他渲染完成后执行，确保不会被遮挡
        boolean willRender3DLast = false;

        if (use3DSkinLayers) {
            // 预加载3D数据，但暂时不渲染
            if (!hasLogged3DRenderStart) {
                ModuleLogger.debug(LOG_MODULE, "🎨 准备3D皮肤层渲染，皮肤: {}", skinLocation);
                hasLogged3DRenderStart = true;
            }

            // 预加载3D皮肤数据，确保在需要时可用
            var preloadResult = Doll3DSkinUtil.setup3dLayers(skinLocation, thinArms);
            if (preloadResult != null) {
                willRender3DLast = true;
                ModuleLogger.debug(LOG_MODULE, "✓ 3D皮肤数据预加载成功，将在最后阶段渲染");
            } else {
                ModuleLogger.warn(LOG_MODULE, "✗ 3D皮肤数据预加载失败，降级到2D渲染");
            }
        } else {
            if (!modLoaded) {
                ModuleLogger.debug(LOG_MODULE, "mod未加载，使用2D渲染");
            } else if (!apiAvailable) {
                ModuleLogger.debug(LOG_MODULE, "API不可用，使用2D渲染");
            } else if (!inRange) {
                ModuleLogger.debug(LOG_MODULE, "距离过远，使用2D渲染");
            } else {
                ModuleLogger.debug(LOG_MODULE, "皮肤不兼容标准格式，使用2D渲染");
            }

            ModuleLogger.debug(LOG_MODULE, "开始2D外层渲染，皮肤: {}", skinLocation);
            // 使用默认2D渲染
            // 如果有身体旋转，使用 MatrixStack 在身体旋转中心应用旋转，然后渲染所有外层部分
            if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
                matrixStack.push();
                
                // 移动到身体的旋转中心（身体和头连接处，Y坐标约为0.375）
                float rotationCenterY = 0.375f;
                matrixStack.translate(0.0, rotationCenterY, 0.0);
                
                // 应用身体旋转（只在这里应用，不在 setRotation 中设置）
                matrixStack.multiply(new Quaternionf().rotateX(bodyRotX));
                matrixStack.multiply(new Quaternionf().rotateY(bodyRotY));
                matrixStack.multiply(new Quaternionf().rotateZ(bodyRotZ));
                
                // 移回旋转中心
                matrixStack.translate(0.0, -rotationCenterY, 0.0);
                // 在旋转后的坐标系中 渲染所有外层部分
                // hat层（头发外层），使用 headScale 和 hatScale 的组合
                ModuleLogger.debug(LOG_MODULE, "✓ 渲染帽子外层（旋转模式）");
                renderPartWithTransform(matrixStack, playerModel.hat, overlayVertexConsumer, light, overlay, hatPosition, hatCombinedScale);
                // 手臂外层（保持它们自己的旋转值）
                renderArmOverlayParts(matrixStack, overlayVertexConsumer, light, overlay, rightArmPosition, rightArmScale, leftArmPosition, leftArmScale);
                // 身体和腿部外层（jacket 的旋转设为0）
                setBodyOverlayRotation(0, 0, 0); // 确保身体外层不额外旋转
                renderBodyLegOverlayParts(matrixStack, overlayVertexConsumer, light, overlay, bodyPosition, bodyScale, rightLegPosition, rightLegScale, leftLegPosition, leftLegScale);
                matrixStack.pop();
            } else {
                // 没有身体旋转时，正常渲染
                ModuleLogger.debug(LOG_MODULE, "✓ 渲染帽子外层（正常模式）");
                renderPartWithTransform(matrixStack, playerModel.hat, overlayVertexConsumer, light, overlay, hatPosition, hatCombinedScale);
                renderArmOverlayParts(matrixStack, overlayVertexConsumer, light, overlay, rightArmPosition, rightArmScale, leftArmPosition, leftArmScale);
                setBodyOverlayRotation(0, 0, 0);
                renderBodyLegOverlayParts(matrixStack, overlayVertexConsumer, light, overlay, bodyPosition, bodyScale, rightLegPosition, rightLegScale, leftLegPosition, leftLegScale);
            }
        }

        matrixStack.pop();

        // 第四步：最后的3D网格渲染 - 在所有其他渲染完成后执行，确保不会被遮挡
        if (willRender3DLast) {
            // 只在第一次渲染时记录日志，避免每帧都输出导致卡顿
            if (!hasLoggedMeshCreation) {
            ModuleLogger.debug(LOG_MODULE, "🎨 执行延迟3D网格渲染 - 在所有渲染完成后");
            }
            try {
                // 为3D网格渲染应用玩偶的基础变换
                matrixStack.push();
                applyBaseDollTransforms(matrixStack, entity, partialTick);

                renderOverlayWith3DSkinLayers(matrixStack, overlayVertexConsumer, light, overlay,
                    skinLocation, bodyRotX, bodyRotY, bodyRotZ,
                    hatPosition, hatCombinedScale, hatRot,
                    rightArmPosition, rightArmScale, rightArmRot,
                    leftArmPosition, leftArmScale, leftArmRot,
                    bodyPosition, bodyScale, bodyRot,
                    rightLegPosition, rightLegScale, rightLegRot,
                    leftLegPosition, leftLegScale, leftLegRot);

                matrixStack.pop();
                // 只在第一次渲染时记录日志
                if (!hasLoggedMeshCreation) {
                ModuleLogger.debug(LOG_MODULE, "✅ 延迟3D网格渲染完成 - 这应该在最上层显示");
                    hasLoggedMeshCreation = true; // 标记已完成一次完整渲染
                }
            } catch (Exception e) {
                ModuleLogger.error(LOG_MODULE, "❌ 延迟3D网格渲染失败", e);
                ModuleLogger.error(LOG_MODULE, "  错误详情: {}", e.getMessage());
            }
        }

        super.render(entity, entityYaw, partialTick, matrixStack, vertexConsumerProvider, light);
    }
    
    /**
     * 为3D网格渲染应用玩偶的基础变换
     * 确保3D网格和普通模型使用相同的坐标系统和缩放
     * 必须与主渲染方法中的变换顺序完全一致
     */
    private void applyBaseDollTransforms(MatrixStack matrixStack, T entity, float partialTick) {
        // 获取玩偶的pose数据
        var pose = entity.getCurrentPose();
        if (pose == null) {
            pose = com.lanye.dolladdon.api.pose.SimpleDollPose.createDefaultStandingPose();
        }

        // 第一步：应用实体旋转（与主渲染方法第76-81行一致）
        float yRot = MathHelper.lerp(partialTick, entity.prevYaw, entity.getYaw());
        float xRot = MathHelper.lerp(partialTick, entity.prevPitch, entity.getPitch());
        matrixStack.multiply(new Quaternionf().rotateY((float) Math.toRadians(180.0F - yRot)));
        matrixStack.multiply(new Quaternionf().rotateX((float) Math.toRadians(xRot)));

        // 第二步：应用Y偏移和模型缩放（与主渲染方法第83-108行一致）
        float modelScale = 0.5F;
        float[] scale = pose.getScale();
        float yOffset = 0.75f * scale[1];
        matrixStack.translate(0.0, yOffset, 0.0);
        matrixStack.scale(-modelScale, -modelScale, modelScale);

        // 第三步：应用姿态的位置和缩放（与主渲染方法第111-117行一致）
        float[] position = pose.getPosition();
        if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
            matrixStack.translate(position[0], -position[1], position[2]);
        }
        if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
            matrixStack.scale(scale[0], scale[1], scale[2]);
        }

        // 注意：身体旋转在renderOverlayWith3DSkinLayers方法中处理，不在这里处理

        // 只在第一次应用变换时记录日志，避免每帧都输出导致卡顿
        if (!hasLoggedMeshCreation) {
            ModuleLogger.debug(LOG_MODULE, "✓ 已应用玩偶基础变换 - 实体旋转(Y:{:.1f}, X:{:.1f}), Y偏移:{:.3f}, 模型缩放:{:.1f}, 位置: [{}, {}, {}], 缩放: [{}, {}, {}]",
                    yRot, xRot, yOffset, modelScale,
                    position[0], position[1], position[2],
                    scale[0], scale[1], scale[2]);
        }
    }

    /**
     * 渲染单个部件，应用位置和缩放
     * @param poseStack 变换矩阵栈
     * @param part 要渲染的部件
     * @param vertexConsumer 顶点消费者
     * @param packedLight 光照信息
     * @param overlay 覆盖纹理
     * @param position 位置偏移 [x, y, z]
     * @param scale 缩放 [x, y, z]
     */
    private void renderPartWithTransform(MatrixStack matrixStack,
                                         net.minecraft.client.model.ModelPart part,
                                         net.minecraft.client.render.VertexConsumer vertexConsumer,
                                         int light,
                                         int overlay,
                                         float[] position,
                                         float[] scale) {
        matrixStack.push();
        
        // 应用位置偏移（Y轴取反，正数向上）
        if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
            matrixStack.translate(position[0], -position[1], position[2]);
        }
        
        // 应用缩放
        if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
            matrixStack.scale(scale[0], scale[1], scale[2]);
        }
        
        // 渲染部件
        part.render(matrixStack, vertexConsumer, light, overlay);
        
        matrixStack.pop();
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
            
            // jacket应该跟随body的旋转（但身体的旋转通过 MatrixStack 应用，所以这里设为0）
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.ModelPart) {
                ((net.minecraft.client.model.ModelPart) jacket).setAngles(0, 0, 0);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    /**
     * 设置身体外层（jacket）的旋转
     * 用于在渲染时临时设置，因为身体的旋转通过 PoseStack 应用
     */
    private void setBodyOverlayRotation(float bodyRotX, float bodyRotY, float bodyRotZ) {
        try {
            java.lang.reflect.Field jacketField = PlayerEntityModel.class.getDeclaredField("jacket");
            jacketField.setAccessible(true);
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.ModelPart) {
                ((net.minecraft.client.model.ModelPart) jacket).setAngles(bodyRotX, bodyRotY, bodyRotZ);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    /**
     * 渲染手臂外层部分（overlay layer）以支持多层皮肤
     * 
     * @param poseStack 变换矩阵栈
     * @param overlayVertexConsumer 外层顶点消费者
     * @param packedLight 光照信息
     * @param overlay 覆盖纹理
     * @param rightArmPosition 右臂位置偏移
     * @param rightArmScale 右臂缩放
     * @param leftArmPosition 左臂位置偏移
     * @param leftArmScale 左臂缩放
     */
    private void renderArmOverlayParts(MatrixStack matrixStack,
                                      net.minecraft.client.render.VertexConsumer overlayVertexConsumer,
                                      int light,
                                      int overlay,
                                      float[] rightArmPosition,
                                      float[] rightArmScale,
                                      float[] leftArmPosition,
                                      float[] leftArmScale) {
        ModuleLogger.debug(LOG_MODULE, "渲染手臂外层部件");
        try {
            // 使用反射访问PlayerEntityModel的外层部分（如果存在）
            java.lang.reflect.Field leftSleeveField = PlayerEntityModel.class.getDeclaredField("leftSleeve");
            java.lang.reflect.Field rightSleeveField = PlayerEntityModel.class.getDeclaredField("rightSleeve");
            
            leftSleeveField.setAccessible(true);
            rightSleeveField.setAccessible(true);
            
            // 渲染左袖子外层
            Object leftSleeve = leftSleeveField.get(playerModel);
            if (leftSleeve instanceof net.minecraft.client.model.ModelPart) {
                ModuleLogger.debug(LOG_MODULE, "✓ 渲染左袖子外层");
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) leftSleeve, overlayVertexConsumer, light, overlay, leftArmPosition, leftArmScale);
            } else {
                ModuleLogger.debug(LOG_MODULE, "✗ 左袖子外层不存在或类型不匹配");
            }
            
            // 渲染右袖子外层
            Object rightSleeve = rightSleeveField.get(playerModel);
            if (rightSleeve instanceof net.minecraft.client.model.ModelPart) {
                ModuleLogger.debug(LOG_MODULE, "✓ 渲染右袖子外层");
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) rightSleeve, overlayVertexConsumer, light, overlay, rightArmPosition, rightArmScale);
            } else {
                ModuleLogger.debug(LOG_MODULE, "✗ 右袖子外层不存在或类型不匹配");
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    /**
     * 渲染身体和腿部外层部分（overlay layer）以支持多层皮肤
     * 
     * @param poseStack 变换矩阵栈
     * @param overlayVertexConsumer 外层顶点消费者
     * @param packedLight 光照信息
     * @param overlay 覆盖纹理
     * @param bodyPosition 身体位置偏移
     * @param bodyScale 身体缩放
     * @param rightLegPosition 右腿位置偏移
     * @param rightLegScale 右腿缩放
     * @param leftLegPosition 左腿位置偏移
     * @param leftLegScale 左腿缩放
     */
    private void renderBodyLegOverlayParts(MatrixStack matrixStack,
                                          net.minecraft.client.render.VertexConsumer overlayVertexConsumer,
                                          int light,
                                          int overlay,
                                          float[] bodyPosition,
                                          float[] bodyScale,
                                          float[] rightLegPosition,
                                          float[] rightLegScale,
                                          float[] leftLegPosition,
                                          float[] leftLegScale) {
        ModuleLogger.debug(LOG_MODULE, "渲染身体和腿部外层部件");
        try {
            // 使用反射访问PlayerEntityModel的外层部分（如果存在）
            java.lang.reflect.Field leftPantsField = PlayerEntityModel.class.getDeclaredField("leftPants");
            java.lang.reflect.Field rightPantsField = PlayerEntityModel.class.getDeclaredField("rightPants");
            java.lang.reflect.Field jacketField = PlayerEntityModel.class.getDeclaredField("jacket");
            
            leftPantsField.setAccessible(true);
            rightPantsField.setAccessible(true);
            jacketField.setAccessible(true);
            
            // 渲染夹克外层（身体外层）
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.ModelPart) {
                ModuleLogger.debug(LOG_MODULE, "✓ 渲染夹克外层");
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) jacket, overlayVertexConsumer, light, overlay, bodyPosition, bodyScale);
            } else {
                ModuleLogger.debug(LOG_MODULE, "✗ 夹克外层不存在或类型不匹配");
            }
            
            // 渲染左腿外层
            Object leftPants = leftPantsField.get(playerModel);
            if (leftPants instanceof net.minecraft.client.model.ModelPart) {
                ModuleLogger.debug(LOG_MODULE, "✓ 渲染左腿外层");
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) leftPants, overlayVertexConsumer, light, overlay, leftLegPosition, leftLegScale);
            } else {
                ModuleLogger.debug(LOG_MODULE, "✗ 左腿外层不存在或类型不匹配");
            }

            // 渲染右腿外层
            Object rightPants = rightPantsField.get(playerModel);
            if (rightPants instanceof net.minecraft.client.model.ModelPart) {
                ModuleLogger.debug(LOG_MODULE, "✓ 渲染右腿外层");
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) rightPants, overlayVertexConsumer, light, overlay, rightLegPosition, rightLegScale);
            } else {
                ModuleLogger.debug(LOG_MODULE, "✗ 右腿外层不存在或类型不匹配");
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    @Override
    public Identifier getTexture(T entity) {
        return getSkinLocation(entity);
    }
    
    /**
     * 检查是否应该使用3D皮肤层渲染
     * 实现距离检测（12格LOD）
     */
    private boolean shouldUse3DSkinLayers(T entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.gameRenderer == null ||
            client.gameRenderer.getCamera() == null) {
            ModuleLogger.debug(LOG_MODULE, "客户端未初始化，无法使用3D渲染");
            return false;
        }

        // 获取皮肤路径进行兼容性检查
        Identifier skinLocation = getSkinLocation(entity);
        if (skinLocation == null) {
            ModuleLogger.debug(LOG_MODULE, "皮肤路径为空，无法使用3D渲染");
            return false;
        }

        // 不再预先检查皮肤路径，让3D渲染系统自己验证皮肤格式
        // 3D皮肤层mod会检查皮肤是否为有效的64x64格式，如果不是会自动回退到2D渲染
        // 只在第一次调用时记录日志，避免每帧都输出导致卡顿
        if (!hasLoggedSkinCheck) {
        ModuleLogger.info(LOG_MODULE, "🎯 第6次修复生效：皮肤路径 {}，移除路径检查，允许尝试3D渲染", skinLocation);
            hasLoggedSkinCheck = true;
        }

        // 计算距离：应该计算到玩家的距离，而不是到相机的距离
        // 3D皮肤层的LOD是基于到玩家的距离
        var player = client.player;
        if (player == null) {
            // 只在第一次调用时记录日志
            if (!hasLoggedDistanceCheck) {
            ModuleLogger.debug(LOG_MODULE, "玩家对象为空，无法使用3D渲染");
                hasLoggedDistanceCheck = true;
            }
            return false;
        }

        var playerPos = player.getPos();
        var entityPos = entity.getPos();

        double distanceSq = entity.squaredDistanceTo(playerPos.x, playerPos.y, playerPos.z);
        double distance = Math.sqrt(distanceSq);
        boolean shouldUse = distanceSq <= 12.0 * 12.0;

        // 只在第一次调用时记录距离检测日志，避免每帧都输出导致卡顿
        if (!hasLoggedDistanceCheck) {
        ModuleLogger.debug(LOG_MODULE, "距离检测: 实体位置({:.1f}, {:.1f}, {:.1f}), 玩家位置({:.1f}, {:.1f}, {:.1f}), 到玩家距离={:.2f}格, 阈值=144.0, 使用3D渲染={}",
                entityPos.x, entityPos.y, entityPos.z,
                playerPos.x, playerPos.y, playerPos.z,
                distance, shouldUse);
            hasLoggedDistanceCheck = true;
        }

        // 12格以内且为标准Minecraft皮肤时使用3D渲染
        return shouldUse;
    }
    
    /**
     * 使用3D皮肤层渲染外层
     */
    private void renderOverlayWith3DSkinLayers(MatrixStack matrixStack,
                                               net.minecraft.client.render.VertexConsumer vertexConsumer,
                                               int light, int overlay,
                                               Identifier skinLocation,
                                               float bodyRotX, float bodyRotY, float bodyRotZ,
                                               float[] hatPosition, float[] hatScale, float[] hatRot,
                                               float[] rightArmPosition, float[] rightArmScale, float[] rightArmRot,
                                               float[] leftArmPosition, float[] leftArmScale, float[] leftArmRot,
                                               float[] bodyPosition, float[] bodyScale, float[] bodyRot,
                                               float[] rightLegPosition, float[] rightLegScale, float[] rightLegRot,
                                               float[] leftLegPosition, float[] leftLegScale, float[] leftLegRot) {
        if (!hasLogged3DRenderStart) {
            ModuleLogger.debug(LOG_MODULE, "开始3D渲染，皮肤: {}, thinArms: {}", skinLocation, thinArms);
            hasLogged3DRenderStart = true;
        }

        // 使用存储的thinArms字段

        // 获取或创建3D皮肤数据
        if (!hasLoggedMeshCreation) {
            ModuleLogger.debug(LOG_MODULE, "获取3D皮肤数据...");
        }
        Doll3DSkinData skinData = Doll3DSkinUtil.setup3dLayers(skinLocation, thinArms);
        if (skinData == null) {
            ModuleLogger.warn(LOG_MODULE, "✗ 无法获取3D皮肤数据（返回null），回退到2D渲染");
            return;
        }
        if (!skinData.hasValidData()) {
            ModuleLogger.warn(LOG_MODULE, "✗ 3D皮肤数据无效，回退到2D渲染");
            return;
        }
        
        if (!hasLoggedMeshCreation) {
            ModuleLogger.debug(LOG_MODULE, "✓ 3D皮肤数据有效，开始渲染各个部位");
        }
        
        // 重要优化：批量初始化所有mesh（copyFrom和setVisible），避免每个部件都单独处理
        initializeAllMeshes(skinData);
        
        // 重要：在所有3D部件渲染前统一禁用深度测试，避免每个部件都处理
        try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
        } catch (Exception e) {
            if (!hasLoggedMeshCreation) {
                ModuleLogger.warn(LOG_MODULE, "⚠ 无法禁用深度测试: {}", e.getMessage());
            }
        }
        
        // 确保纹理已绑定（只绑定一次，所有部件共享）
        if (skinLocation != null) {
            try {
                RenderSystem.setShaderTexture(0, skinLocation);
            } catch (Exception texEx) {
                // 静默失败，纹理可能已经绑定
            }
        }
        
        // 初始化render方法缓存（只执行一次）
        if (!renderMethodCacheInitialized && skinData.getHeadMesh() != null) {
            initializeRenderMethodCache(skinData.getHeadMesh());
        }
        
        try {
        // 处理身体旋转
        if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
            matrixStack.push();
            
            float rotationCenterY = 0.375f;
            matrixStack.translate(0.0, rotationCenterY, 0.0);
            matrixStack.multiply(new Quaternionf().rotateX(bodyRotX));
            matrixStack.multiply(new Quaternionf().rotateY(bodyRotY));
            matrixStack.multiply(new Quaternionf().rotateZ(bodyRotZ));
            matrixStack.translate(0.0, -rotationCenterY, 0.0);
            
                // 渲染各个部位的3D网格（已批量初始化，这里只负责变换和渲染）
                render3DMeshPartFast(matrixStack, playerModel.hat, skinData.getHeadMesh(),
                    "HEAD", vertexConsumer, light, overlay, hatPosition, hatScale, hatRot);
            
                render3DMeshPartFast(matrixStack, playerModel.leftArm, skinData.getLeftArmMesh(),
                    thinArms ? "LEFT_ARM_SLIM" : "LEFT_ARM", vertexConsumer, light, overlay,
                    leftArmPosition, leftArmScale, leftArmRot);
            
                render3DMeshPartFast(matrixStack, playerModel.rightArm, skinData.getRightArmMesh(),
                    thinArms ? "RIGHT_ARM_SLIM" : "RIGHT_ARM", vertexConsumer, light, overlay,
                    rightArmPosition, rightArmScale, rightArmRot);
            
                render3DMeshPartFast(matrixStack, playerModel.body, skinData.getTorsoMesh(),
                    "BODY", vertexConsumer, light, overlay, bodyPosition, bodyScale, bodyRot);
            
                render3DMeshPartFast(matrixStack, playerModel.leftLeg, skinData.getLeftLegMesh(),
                    "LEFT_LEG", vertexConsumer, light, overlay, leftLegPosition, leftLegScale, leftLegRot);
            
                render3DMeshPartFast(matrixStack, playerModel.rightLeg, skinData.getRightLegMesh(),
                    "RIGHT_LEG", vertexConsumer, light, overlay, rightLegPosition, rightLegScale, rightLegRot);
            
            matrixStack.pop();
        } else {
            // 没有身体旋转时，正常渲染
                render3DMeshPartFast(matrixStack, playerModel.hat, skinData.getHeadMesh(),
                    "HEAD", vertexConsumer, light, overlay, hatPosition, hatScale, hatRot);
            
                render3DMeshPartFast(matrixStack, playerModel.leftArm, skinData.getLeftArmMesh(),
                    thinArms ? "LEFT_ARM_SLIM" : "LEFT_ARM", vertexConsumer, light, overlay,
                    leftArmPosition, leftArmScale, leftArmRot);
            
                render3DMeshPartFast(matrixStack, playerModel.rightArm, skinData.getRightArmMesh(),
                    thinArms ? "RIGHT_ARM_SLIM" : "RIGHT_ARM", vertexConsumer, light, overlay,
                    rightArmPosition, rightArmScale, rightArmRot);
            
                render3DMeshPartFast(matrixStack, playerModel.body, skinData.getTorsoMesh(),
                    "BODY", vertexConsumer, light, overlay, bodyPosition, bodyScale, bodyRot);
            
                render3DMeshPartFast(matrixStack, playerModel.leftLeg, skinData.getLeftLegMesh(),
                    "LEFT_LEG", vertexConsumer, light, overlay, leftLegPosition, leftLegScale, leftLegRot);
            
                render3DMeshPartFast(matrixStack, playerModel.rightLeg, skinData.getRightLegMesh(),
                    "RIGHT_LEG", vertexConsumer, light, overlay, rightLegPosition, rightLegScale, rightLegRot);
            }
        } finally {
            // 重要：在所有3D部件渲染后统一恢复深度测试
            try {
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
            } catch (Exception e) {
                if (!hasLoggedMeshCreation) {
                    ModuleLogger.warn(LOG_MODULE, "⚠ 无法恢复深度测试: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * 批量初始化所有mesh（copyFrom和setVisible），只执行一次
     */
    private void initializeAllMeshes(Doll3DSkinData skinData) {
        // 初始化render方法缓存（如果还没初始化）
        if (!renderMethodCacheInitialized && skinData.getHeadMesh() != null) {
            initializeRenderMethodCache(skinData.getHeadMesh());
        }
        
        // 批量初始化所有mesh
        initializeMesh(skinData.getHeadMesh(), playerModel.hat, "HEAD");
        initializeMesh(skinData.getLeftArmMesh(), playerModel.leftArm, thinArms ? "LEFT_ARM_SLIM" : "LEFT_ARM");
        initializeMesh(skinData.getRightArmMesh(), playerModel.rightArm, thinArms ? "RIGHT_ARM_SLIM" : "RIGHT_ARM");
        initializeMesh(skinData.getTorsoMesh(), playerModel.body, "BODY");
        initializeMesh(skinData.getLeftLegMesh(), playerModel.leftLeg, "LEFT_LEG");
        initializeMesh(skinData.getRightLegMesh(), playerModel.rightLeg, "RIGHT_LEG");
            }
    
    /**
     * 初始化单个mesh（copyFrom和setVisible）
     */
    private void initializeMesh(Object mesh, net.minecraft.client.model.ModelPart modelPart, String name) {
        if (mesh == null) {
            return;
        }

        try {
            // 使用缓存的方法，避免每帧都查找
            if (cachedCopyFromMethod == null) {
                cachedCopyFromMethod = mesh.getClass().getMethod("copyFrom", net.minecraft.client.model.ModelPart.class);
            }
            cachedCopyFromMethod.invoke(mesh, modelPart);
            
            if (cachedSetVisibleMethod == null) {
                cachedSetVisibleMethod = mesh.getClass().getMethod("setVisible", boolean.class);
            }
            cachedSetVisibleMethod.invoke(mesh, true);
        } catch (Exception e) {
            if (!hasLoggedMeshCreation) {
                ModuleLogger.warn(LOG_MODULE, "⚠ {} 初始化mesh失败: {}", name, e.getMessage());
            }
        }
    }
    
    /**
     * 手动应用3D偏移，使外层皮肤稍微向外偏移以形成3D效果
     * 当OffsetProvider不可用时使用此方法
     * 
     * 注意：此方法在copyFrom之后、render之前调用
     * render方法内部会调用translateAndRotate，先应用平移再应用旋转
     * 所以我们的偏移会在ModelPart的局部坐标系中，会随着ModelPart的旋转而旋转
     * 这样在站立、趴下、坐着等不同姿态下，偏移方向都会正确
     * 
     * @param matrixStack 变换矩阵栈
     * @param offsetProviderName 部位名称（如 "HEAD", "BODY" 等）
     * @param modelPart 模型部件（用于日志，实际偏移不依赖旋转角度）
     */
    private void applyManual3DOffset(MatrixStack matrixStack, String offsetProviderName, net.minecraft.client.model.ModelPart modelPart) {
        // 3D皮肤层的典型偏移距离：约0.01-0.02个单位（在Minecraft坐标系统中）
        // 经过模型缩放（0.5倍）后，实际偏移约为0.02-0.04个单位
        // 减小偏移距离，避免网格被渲染到视野外
        float offsetDistance = 0.01f; // 减小偏移，确保网格在正确位置
        
        // 根据部位应用不同的偏移方向
        // 偏移在ModelPart的局部坐标系中，会随着旋转而旋转
        switch (offsetProviderName) {
            case "HEAD":
                // 头部：向上和向前偏移
                matrixStack.translate(0.0, offsetDistance * 0.5, offsetDistance);
                break;
            case "BODY":
                // 身体：向前偏移
                matrixStack.translate(0.0, 0.0, offsetDistance);
                break;
            case "LEFT_ARM":
            case "LEFT_ARM_SLIM":
                // 左臂：向左和向前偏移
                matrixStack.translate(-offsetDistance * 0.3, 0.0, offsetDistance);
                break;
            case "RIGHT_ARM":
            case "RIGHT_ARM_SLIM":
                // 右臂：向右和向前偏移
                matrixStack.translate(offsetDistance * 0.3, 0.0, offsetDistance);
                break;
            case "LEFT_LEG":
                // 左腿：主要向前偏移，减少左右偏移以避免平行时分离过大
                // 偏移会在ModelPart的局部坐标系中，随着腿部旋转而旋转
                matrixStack.translate(-offsetDistance * 0.1, 0.0, offsetDistance);
                break;
            case "RIGHT_LEG":
                // 右腿：主要向前偏移，减少左右偏移以避免平行时分离过大
                // 偏移会在ModelPart的局部坐标系中，随着腿部旋转而旋转
                matrixStack.translate(offsetDistance * 0.1, 0.0, offsetDistance);
                break;
            default:
                // 默认：向前偏移
                matrixStack.translate(0.0, 0.0, offsetDistance);
                break;
        }
        
        // 记录偏移应用日志
        ModuleLogger.debug(LogModuleConfig.MODULE_RENDER_3D_OFFSET, 
            "应用3D偏移: 部位={}, 偏移距离={:.3f}, 偏移方向=局部坐标系", 
            offsetProviderName, offsetDistance);
    }
            
    /**
     * 快速渲染单个3D网格部件（已批量初始化，只负责变换和渲染）
     * 优化版本：移除了所有初始化逻辑，只保留变换和渲染
     */
    private void render3DMeshPartFast(MatrixStack matrixStack,
                                      net.minecraft.client.model.ModelPart modelPart,
                                      Object mesh,
                                      String offsetProviderName,
                                      net.minecraft.client.render.VertexConsumer vertexConsumer,
                                      int light, int overlay,
                                      float[] position, float[] scale,
                                      float[] rotation) {
        if (mesh == null) {
            return;
        }

        try {
            matrixStack.push();
                    
            // 应用位置偏移
            if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
                matrixStack.translate(position[0], -position[1], position[2]);
            }
                    
            // 更新mesh状态（因为ModelPart可能已经改变）
            try {
                if (cachedCopyFromMethod != null) {
                    cachedCopyFromMethod.invoke(mesh, modelPart);
                }
            } catch (Exception e) {
                // 静默失败
            }
            
            // 关键修复：清除mesh内部的旋转信息，避免双重应用旋转
            // 问题分析：
            // 1. mesh.copyFrom()会将ModelPart的旋转信息复制到mesh内部
            // 2. mesh.render()内部可能会使用mesh内部存储的旋转信息
            // 3. 即使我们在MatrixStack中应用了旋转，mesh.render()内部可能还会再次应用旋转
            // 解决方案：在copyFrom()之后，使用mesh.setRotation(0, 0, 0)清除mesh内部的旋转
            try {
                java.lang.reflect.Method setRotationMethod = mesh.getClass().getMethod("setRotation", float.class, float.class, float.class);
                setRotationMethod.invoke(mesh, 0.0f, 0.0f, 0.0f);
                ModuleLogger.debug(LogModuleConfig.MODULE_RENDER_3D_OFFSET, 
                    "部位={}: 已清除mesh内部旋转", offsetProviderName);
            } catch (Exception e) {
                ModuleLogger.debug(LogModuleConfig.MODULE_RENDER_3D_OFFSET, 
                    "部位={}: 无法清除mesh内部旋转 - {}", offsetProviderName, e.getMessage());
            }
            
            // 关键修复：在旋转之前应用偏移（在身体的坐标系中）
            // 问题分析：
            // 1. 问题描述："向前旋转 → 向后偏移"，说明偏移方向是反的
            // 2. 如果偏移在旋转之后应用，偏移会随着部件旋转而旋转，导致位置错误
            // 3. 如果偏移在旋转之前应用，偏移在身体的坐标系中，不会随着部件旋转而旋转
            // 解决方案：在旋转之前应用偏移（在身体的坐标系中）
            
            // 使用姿态文件的旋转值（rotation参数），转换为弧度
            float rotX = (float) Math.toRadians(rotation[0]);
            float rotY = (float) Math.toRadians(rotation[1]);
            float rotZ = (float) Math.toRadians(rotation[2]);
            
            ModuleLogger.debug(LogModuleConfig.MODULE_RENDER_3D_OFFSET, 
                "部位={}: 姿态旋转值 (X:{:.1f}°, Y:{:.1f}°, Z:{:.1f}°), 缩放=({:.2f}, {:.2f}, {:.2f})", 
                offsetProviderName, rotation[0], rotation[1], rotation[2], scale[0], scale[1], scale[2]);
            
            // 关键：在旋转之前应用偏移（在身体的坐标系中）
            applyManual3DOffset(matrixStack, offsetProviderName, modelPart);
            
            // 应用旋转（使用姿态文件的旋转值）
            if (rotX != 0.0F || rotY != 0.0F || rotZ != 0.0F) {
                matrixStack.multiply(new Quaternionf().rotationZYX(rotZ, rotY, rotX));
                ModuleLogger.debug(LogModuleConfig.MODULE_RENDER_3D_OFFSET, 
                    "部位={}: 应用旋转 (X:{:.1f}°, Y:{:.1f}°, Z:{:.1f}°)", 
                    offsetProviderName, rotation[0], rotation[1], rotation[2]);
            }
            
            // 应用基础缩放
            if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
                matrixStack.scale(scale[0], scale[1], scale[2]);
                ModuleLogger.debug(LogModuleConfig.MODULE_RENDER_3D_OFFSET, 
                    "部位={}: 应用基础缩放 ({:.2f}, {:.2f}, {:.2f})", 
                    offsetProviderName, scale[0], scale[1], scale[2]);
            }
            
            // 应用额外的缩放因子来放大3D皮肤层
            float sizeMultiplier = 1.20f; // 放大20%
            matrixStack.scale(sizeMultiplier, sizeMultiplier, sizeMultiplier);
            
            // 使用缓存的render方法直接渲染
            if (cachedRenderMethod != null) {
                try {
                    int paramCount = cachedRenderMethod.getParameterCount();
                    if (paramCount == 6) {
                        cachedRenderMethod.invoke(mesh, modelPart, matrixStack, vertexConsumer, light, overlay, 0xFFFFFFFF);
                    } else if (paramCount == 5) {
                        cachedRenderMethod.invoke(mesh, modelPart, matrixStack, vertexConsumer, light, overlay);
                    } else if (paramCount == 4) {
                        cachedRenderMethod.invoke(mesh, matrixStack, vertexConsumer, light, overlay);
                    }
                } catch (Exception e) {
                    // 静默失败，避免每帧都输出错误
                }
            }

            matrixStack.pop();
            
        } catch (Exception e) {
            // 静默失败，避免每帧都输出错误
                }
    }

    /**
     * 初始化render方法缓存（只执行一次）
     */
    private static void initializeRenderMethodCache(Object mesh) {
        if (renderMethodCacheInitialized) {
            return;
                }

        try {
            // 尝试多种render方法签名，找到第一个可用的
            // 方法1：3参数版本 (MatrixStack, VertexConsumer, int, int) - 最常用
                try {
                cachedRenderMethod = mesh.getClass().getMethod("render",
                            MatrixStack.class,
                            net.minecraft.client.render.VertexConsumer.class,
                        int.class, int.class);
                renderMethodCacheInitialized = true;
                ModuleLogger.debug(LOG_MODULE, "✓ 缓存render方法：3参数版本");
                return;
                } catch (NoSuchMethodException e1) {
                // 尝试PoseStack
                try {
                    cachedPoseStackClass = Class.forName("com.mojang.blaze3d.vertex.PoseStack");
                    cachedRenderMethod = mesh.getClass().getMethod("render",
                            cachedPoseStackClass,
                                net.minecraft.client.render.VertexConsumer.class,
                                int.class, int.class);
                    renderMethodCacheInitialized = true;
                    ModuleLogger.debug(LOG_MODULE, "✓ 缓存render方法：3参数版本（PoseStack）");
                    return;
                } catch (Exception e2) {
                    // 继续尝试其他方法
                }
            }
            
            // 方法2：5参数版本 (ModelPart, MatrixStack, VertexConsumer, int, int)
                        try {
                cachedRenderMethod = mesh.getClass().getMethod("render",
                        net.minecraft.client.model.ModelPart.class,
                                    MatrixStack.class,
                                    net.minecraft.client.render.VertexConsumer.class,
                                    int.class, int.class);
                renderMethodCacheInitialized = true;
                ModuleLogger.debug(LOG_MODULE, "✓ 缓存render方法：5参数版本");
                return;
                        } catch (NoSuchMethodException e3) {
                // 继续尝试
            }
            
            // 方法3：6参数版本 (ModelPart, MatrixStack, VertexConsumer, int, int, int)
            try {
                cachedRenderMethod = mesh.getClass().getMethod("render",
                        net.minecraft.client.model.ModelPart.class,
                        MatrixStack.class,
                        net.minecraft.client.render.VertexConsumer.class,
                        int.class, int.class, int.class);
                renderMethodCacheInitialized = true;
                ModuleLogger.debug(LOG_MODULE, "✓ 缓存render方法：6参数版本");
                return;
            } catch (NoSuchMethodException e4) {
                ModuleLogger.error(LOG_MODULE, "✗ 无法找到任何render方法");
                }
            } catch (Exception e) {
            ModuleLogger.error(LOG_MODULE, "✗ 初始化render方法缓存失败: {}", e.getMessage());
        }
        
        renderMethodCacheInitialized = true; // 标记为已初始化，避免重复尝试
    }

    /**
     * 3D渲染失败时的2D渲染降级方案
     */
    private void fallbackTo2DRender(MatrixStack matrixStack, net.minecraft.client.render.VertexConsumer vertexConsumer,
                                   int light, int overlay, float bodyRotX, float bodyRotY, float bodyRotZ,
                                   float[] hatPosition, float[] hatCombinedScale,
                                   float[] rightArmPosition, float[] rightArmScale, float[] leftArmPosition, float[] leftArmScale,
                                   float[] bodyPosition, float[] bodyScale,
                                   float[] rightLegPosition, float[] rightLegScale, float[] leftLegPosition, float[] leftLegScale) {
        ModuleLogger.info(LOG_MODULE, "🔄 3D渲染失败，执行2D渲染降级");

        try {
            // 使用与原始2D渲染完全相同的逻辑
            if (bodyRotX != 0 || bodyRotY != 0 || bodyRotZ != 0) {
                matrixStack.push();

                // 移动到身体的旋转中心（身体和头连接处，Y坐标约为0.375）
                float rotationCenterY = 0.375f;
                matrixStack.translate(0.0, rotationCenterY, 0.0);

                // 应用身体旋转
                matrixStack.multiply(new Quaternionf().rotateX(bodyRotX));
                matrixStack.multiply(new Quaternionf().rotateY(bodyRotY));
                matrixStack.multiply(new Quaternionf().rotateZ(bodyRotZ));

                // 移回旋转中心
                matrixStack.translate(0.0, -rotationCenterY, 0.0);

                // 在旋转后的坐标系中渲染所有外层部分
                // hat层（头发外层），使用 headScale 和 hatScale 的组合
                renderPartWithTransform(matrixStack, playerModel.hat, vertexConsumer, light, overlay, hatPosition, hatCombinedScale);
                // 手臂外层（保持它们自己的旋转值）
                renderArmOverlayParts(matrixStack, vertexConsumer, light, overlay, rightArmPosition, rightArmScale, leftArmPosition, leftArmScale);
                // 身体和腿部外层（jacket 的旋转设为0）
                setBodyOverlayRotation(0, 0, 0); // 确保身体外层不额外旋转
                renderBodyLegOverlayParts(matrixStack, vertexConsumer, light, overlay, bodyPosition, bodyScale, rightLegPosition, rightLegScale, leftLegPosition, leftLegScale);

                matrixStack.pop();
            } else {
                // 没有身体旋转时，正常渲染
                renderPartWithTransform(matrixStack, playerModel.hat, vertexConsumer, light, overlay, hatPosition, hatCombinedScale);
                renderArmOverlayParts(matrixStack, vertexConsumer, light, overlay, rightArmPosition, rightArmScale, leftArmPosition, leftArmScale);
                setBodyOverlayRotation(0, 0, 0);
                renderBodyLegOverlayParts(matrixStack, vertexConsumer, light, overlay, bodyPosition, bodyScale, rightLegPosition, rightLegScale, leftLegPosition, leftLegScale);
            }

            ModuleLogger.info(LOG_MODULE, "✅ 2D渲染降级成功完成");
        } catch (Exception fallbackEx) {
            ModuleLogger.error(LOG_MODULE, "❌ 2D渲染降级也失败", fallbackEx);
        }
    }
}

