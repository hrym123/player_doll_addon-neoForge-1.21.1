package com.lanye.dolladdon.base.render;

import com.lanye.dolladdon.api.action.DollAction;
import com.lanye.dolladdon.api.pose.DollPose;
import com.lanye.dolladdon.api.pose.SimpleDollPose;
import com.lanye.dolladdon.info.BodyPartsTransformInfo;
import com.lanye.dolladdon.info.PartTransformInfo;
import com.lanye.dolladdon.info.RenderContextInfo;
import com.lanye.dolladdon.util.pose.PoseActionManager;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

/**
 * 玩偶物品渲染器基类
 * 提供所有玩偶物品渲染器的共同功能
 */
public abstract class BaseDollItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    protected PlayerEntityModel<PlayerEntity> playerModel;
    protected final MinecraftClient client;
    protected final boolean isAlexModel;
    
    protected BaseDollItemRenderer(MinecraftClient client, boolean isAlexModel) {
        this.client = client;
        this.isAlexModel = isAlexModel;
        // 模型延迟初始化，在第一次渲染时创建
    }
    
    /**
     * 获取或创建玩家模型（延迟初始化）
     */
    protected PlayerEntityModel<PlayerEntity> getPlayerModel() {
        if (playerModel == null) {
            EntityModelLoader modelLoader = client.getEntityModelLoader();
            if (modelLoader != null) {
                playerModel = new PlayerEntityModel<>(
                    modelLoader.getModelPart(isAlexModel ? 
                        net.minecraft.client.render.entity.model.EntityModelLayers.PLAYER_SLIM : 
                        net.minecraft.client.render.entity.model.EntityModelLayers.PLAYER), 
                    isAlexModel
                );
            } else {
                // 如果 modelLoader 仍然为 null，返回 null
                // 这种情况不应该发生，但为了安全起见
                return null;
            }
        }
        return playerModel;
    }
    
    /**
     * 获取皮肤资源位置
     * @return 皮肤资源位置
     */
    protected abstract Identifier getSkinLocation();
    
    @Override
    public void render(ItemStack stack, net.minecraft.client.render.model.json.ModelTransformationMode transformType, MatrixStack matrixStack,
                             VertexConsumerProvider vertexConsumerProvider, int light, int overlay) {
        // 延迟初始化模型
        PlayerEntityModel<PlayerEntity> playerModel = getPlayerModel();
        if (playerModel == null) {
            // 如果模型还未准备好，直接返回
            return;
        }
        
        matrixStack.push();
        
        // 根据显示上下文调整模型的位置、缩放和旋转
        applyPlayerModelTransform(matrixStack, transformType);
        
        // 获取皮肤位置（由子类实现）
        Identifier skinLocation = getSkinLocation();
        
        // 从NBT读取动作或姿态
        DollPose pose = getPoseFromNBT(stack);
        
        // 应用姿态的位置和大小
        float[] position = pose.getPosition();
        float[] scale = pose.getScale();
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
        
        playerModel.head.setAngles(headRotX, headRotY, headRotZ);
        playerModel.hat.setAngles(hatRotX, hatRotY, hatRotZ);
        // 注意：身体的旋转通过 MatrixStack 应用，不在这里设置，避免双重旋转
        playerModel.rightArm.setAngles(rightArmRotX, rightArmRotY, rightArmRotZ);
        playerModel.leftArm.setAngles(leftArmRotX, leftArmRotY, leftArmRotZ);
        playerModel.rightLeg.setAngles(rightLegRotX, rightLegRotY, rightLegRotZ);
        playerModel.leftLeg.setAngles(leftLegRotX, leftLegRotY, leftLegRotZ);
        
        // 同时设置外层部分的旋转，使它们跟随基础部分的动作
        // 注意：身体的旋转通过 MatrixStack 应用，所以 jacket 的旋转也设为0
        DollRenderHelper.setOverlayPartsRotation(playerModel, 0, 0, 0, // 身体旋转通过 MatrixStack 应用
                               leftArmRotX, leftArmRotY, leftArmRotZ,
                               rightArmRotX, rightArmRotY, rightArmRotZ,
                               leftLegRotX, leftLegRotY, leftLegRotZ,
                               rightLegRotX, rightLegRotY, rightLegRotZ);
        
        // 获取渲染类型
        var cutoutRenderType = net.minecraft.client.render.RenderLayer.getEntityCutoutNoCull(skinLocation);
        var translucentRenderType = net.minecraft.client.render.RenderLayer.getEntityTranslucent(skinLocation);
        int overlayValue = net.minecraft.client.render.OverlayTexture.DEFAULT_UV;
        
        // 第一步：渲染基础层（base layer）
        var baseVertexConsumer = vertexConsumerProvider.getBuffer(cutoutRenderType);
        RenderContextInfo baseContextInfo = new RenderContextInfo(light, overlayValue, baseVertexConsumer);
        BodyPartsTransformInfo baseTransforms = BodyPartsTransformInfo.of(
            headPosition, headScale,
            bodyPosition, bodyScale,
            leftArmPosition, leftArmScale,
            rightArmPosition, rightArmScale,
            leftLegPosition, leftLegScale,
            rightLegPosition, rightLegScale
        );
        
        // 如果有身体旋转，使用 MatrixStack 在身体旋转中心应用旋转，然后渲染身体、头部、手臂和腿部
        DollRenderHelper.applyBodyRotation(matrixStack, bodyRotX, bodyRotY, bodyRotZ, () -> {
            DollRenderHelper.renderBaseLayerParts(playerModel, matrixStack, baseContextInfo, baseTransforms);
        });
        
        // 第二步：渲染外层（overlay layer）- 使用半透明渲染以正确显示多层皮肤
        var overlayVertexConsumer = vertexConsumerProvider.getBuffer(translucentRenderType);
        RenderContextInfo overlayContextInfo = new RenderContextInfo(light, overlayValue, overlayVertexConsumer);
        
        // 如果有身体旋转，使用 MatrixStack 在身体旋转中心应用旋转，然后渲染所有外层部分
        DollRenderHelper.applyBodyRotation(matrixStack, bodyRotX, bodyRotY, bodyRotZ, () -> {
            renderPartWithTransform(matrixStack, playerModel.hat, overlayContextInfo, PartTransformInfo.of(hatPosition, hatCombinedScale));
            renderArmOverlayParts(playerModel, matrixStack, overlayContextInfo, PartTransformInfo.of(leftArmPosition, leftArmScale), PartTransformInfo.of(rightArmPosition, rightArmScale));
            setBodyOverlayRotation(playerModel, 0, 0, 0);
            renderBodyLegOverlayParts(playerModel, matrixStack, overlayContextInfo, PartTransformInfo.of(bodyPosition, bodyScale), PartTransformInfo.of(leftLegPosition, leftLegScale), PartTransformInfo.of(rightLegPosition, rightLegScale));
        });
        
        matrixStack.pop();
    }
    
    /**
     * 根据显示上下文调整玩家模型的位置、缩放和旋转
     * 注意：玩家模型的原点在脚部（Y=0），模型高度约1.8，中心在Y=0.9处
     * 但是为了与实体渲染器保持一致，模型被缩放到高度1.125（缩放比例约0.625）
     * 
     * @param poseStack 变换矩阵栈
     * @param transformType 显示上下文类型
     */
    protected void applyPlayerModelTransform(MatrixStack matrixStack, net.minecraft.client.render.model.json.ModelTransformationMode transformType) {
        // 模型缩放比例，与实体渲染器保持一致
        float modelScale = 1F;
        
        if (transformType == net.minecraft.client.render.model.json.ModelTransformationMode.GUI) {
            // GUI 中：居中显示
            matrixStack.translate(0.5, 0.75, 0.0);
            matrixStack.scale(0.5F * modelScale, 0.5F * modelScale, 0.5F * modelScale);
            matrixStack.multiply(new Quaternionf().rotateY((float) Math.toRadians(-155.0F)));
        } else if (transformType == net.minecraft.client.render.model.json.ModelTransformationMode.FIRST_PERSON_LEFT_HAND || 
                   transformType == net.minecraft.client.render.model.json.ModelTransformationMode.FIRST_PERSON_RIGHT_HAND) {
            matrixStack.translate(0.5, 1, 0.5);
            matrixStack.scale(0.5F * modelScale, 0.5F * modelScale, -0.5F * modelScale);
            // 旋转
            if (transformType == net.minecraft.client.render.model.json.ModelTransformationMode.FIRST_PERSON_LEFT_HAND) {
                // 第一人称左手
                matrixStack.multiply(new Quaternionf().rotateY((float) Math.toRadians(-45.0F)));
            } else{
                // 第一人称右手
                matrixStack.multiply(new Quaternionf().rotateY((float) Math.toRadians(15.0F)));
            }
        } else if (transformType == net.minecraft.client.render.model.json.ModelTransformationMode.THIRD_PERSON_LEFT_HAND || 
                   transformType == net.minecraft.client.render.model.json.ModelTransformationMode.THIRD_PERSON_RIGHT_HAND) {
            // 第三人称：调整位置和大小
            matrixStack.translate(0.5, 0.9, 0.5);
            // 缩放并前后反转（应用模型缩放）
            matrixStack.scale(0.25F * modelScale, 0.25F * modelScale, -0.25F * modelScale);
        } else {
            // 其他情况（地面、框架等）
            matrixStack.translate(0.5, 0.6, 0.5);
            matrixStack.scale(0.255F * modelScale, 0.25F * modelScale, 0.25F * modelScale);
        }
        
        // 翻转模型（玩家模型需要翻转才能正确显示）
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        
    }
    
    /**
     * 从物品堆的NBT标签中获取姿态
     * 支持从动作或姿态索引读取
     */
    private DollPose getPoseFromNBT(ItemStack stack) {
        // 从NBT标签读取
        net.minecraft.nbt.NbtCompound itemTag = stack.getNbt();
        if (itemTag == null || !itemTag.contains("EntityData")) {
            return getDefaultPose();
        }
        
        net.minecraft.nbt.NbtCompound entityTag = itemTag.getCompound("EntityData");
        
        // 优先检查是否有动作名称
        // 注意：对于物品渲染，动作应该显示第一帧（tick=0）的姿态
        // 因为动作是动态的，物品应该显示静态的姿态
        if (entityTag.contains("ActionName", 8)) { // 8 = TAG_STRING
            String actionName = entityTag.getString("ActionName");
            DollAction action = PoseActionManager.getAction(actionName);
            if (action != null) {
                // 物品渲染时，动作显示第一帧的姿态
                DollPose actionPose = action.getPoseAt(0);
                if (actionPose != null) {
                    return actionPose;
                }
            }
        }
        
        // 优先使用姿态名称（如果保存了）
        if (entityTag.contains("PoseName", 8)) { // 8 = TAG_STRING
            String poseName = entityTag.getString("PoseName");
            DollPose pose = PoseActionManager.getPose(poseName);
            if (pose != null) {
                return pose;
            }
        }
        
        // 如果没有姿态名称，尝试使用姿态索引（向后兼容）
        if (entityTag.contains("PoseIndex", 3)) { // 3 = TAG_INT
            int poseIndex = entityTag.getInt("PoseIndex");
            if (poseIndex >= 0) {
                java.util.List<String> poseNames = new java.util.ArrayList<>();
                java.util.Map<String, DollPose> allPoses = PoseActionManager.getAllPoses();
                poseNames.addAll(allPoses.keySet());
                poseNames.sort(String::compareTo);
                
                if (poseIndex < poseNames.size()) {
                    String poseName = poseNames.get(poseIndex);
                    DollPose pose = PoseActionManager.getPose(poseName);
                    if (pose != null) {
                        return pose;
                    }
                }
            }
        }
        
        // 默认返回standing姿态
        return getDefaultPose();
    }
    
    /**
     * 获取默认姿态（standing，如果不存在则使用createDefaultStandingPose）
     */
    private DollPose getDefaultPose() {
        DollPose standingPose = PoseActionManager.getPose("standing");
        if (standingPose != null) {
            return standingPose;
        }
        // 如果standing姿态不存在，回退到createDefaultStandingPose
        return SimpleDollPose.createDefaultStandingPose();
    }
    
    /**
     * 渲染单个部件，应用位置和缩放
     * @param matrixStack 变换矩阵栈
     * @param part 要渲染的部件
     * @param contextInfo 渲染上下文信息（包含光照、覆盖纹理、顶点消费者）
     * @param transformInfo 部件变换信息（包含位置和缩放）
     */
    private void renderPartWithTransform(MatrixStack matrixStack,
                                         net.minecraft.client.model.ModelPart part,
                                         RenderContextInfo contextInfo,
                                         PartTransformInfo transformInfo) {
        matrixStack.push();
        
        float[] position = transformInfo.getPositionInternal();
        float[] scale = transformInfo.getScaleInternal();
        
        // 应用位置偏移（Y轴取反，正数向上）
        if (position[0] != 0.0f || position[1] != 0.0f || position[2] != 0.0f) {
            matrixStack.translate(position[0], -position[1], position[2]);
        }
        
        // 应用缩放
        if (scale[0] != 1.0f || scale[1] != 1.0f || scale[2] != 1.0f) {
            matrixStack.scale(scale[0], scale[1], scale[2]);
        }
        
        // 渲染部件
        part.render(matrixStack, contextInfo.getVertexConsumer(), contextInfo.getLight(), contextInfo.getOverlay());
        
        matrixStack.pop();
    }
    
    /**
     * 渲染手臂外层部分（overlay layer）以支持多层皮肤
     * 
     * @param playerModel 玩家模型
     * @param matrixStack 变换矩阵栈
     * @param contextInfo 渲染上下文信息
     * @param leftArmTransform 左臂变换信息
     * @param rightArmTransform 右臂变换信息
     */
    private void renderArmOverlayParts(PlayerEntityModel<?> playerModel,
                                      MatrixStack matrixStack,
                                      RenderContextInfo contextInfo,
                                      PartTransformInfo leftArmTransform,
                                      PartTransformInfo rightArmTransform) {
        try {
            java.lang.reflect.Field leftSleeveField = PlayerEntityModel.class.getDeclaredField("leftSleeve");
            java.lang.reflect.Field rightSleeveField = PlayerEntityModel.class.getDeclaredField("rightSleeve");
            leftSleeveField.setAccessible(true);
            rightSleeveField.setAccessible(true);
            
            Object leftSleeve = leftSleeveField.get(playerModel);
            if (leftSleeve instanceof net.minecraft.client.model.ModelPart) {
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) leftSleeve, contextInfo, leftArmTransform);
            }
            
            Object rightSleeve = rightSleeveField.get(playerModel);
            if (rightSleeve instanceof net.minecraft.client.model.ModelPart) {
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) rightSleeve, contextInfo, rightArmTransform);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
    /**
     * 设置身体外层（jacket）的旋转
     * 用于在渲染时临时设置，因为身体的旋转通过 PoseStack 应用
     */
    private void setBodyOverlayRotation(PlayerEntityModel<?> playerModel, float bodyRotX, float bodyRotY, float bodyRotZ) {
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
     * 渲染身体和腿部外层部分（overlay layer）以支持多层皮肤
     * 
     * @param playerModel 玩家模型
     * @param matrixStack 变换矩阵栈
     * @param contextInfo 渲染上下文信息
     * @param bodyTransform 身体变换信息
     * @param leftLegTransform 左腿变换信息
     * @param rightLegTransform 右腿变换信息
     */
    private void renderBodyLegOverlayParts(PlayerEntityModel<?> playerModel,
                                          MatrixStack matrixStack,
                                          RenderContextInfo contextInfo,
                                          PartTransformInfo bodyTransform,
                                          PartTransformInfo leftLegTransform,
                                          PartTransformInfo rightLegTransform) {
        try {
            java.lang.reflect.Field leftPantsField = PlayerEntityModel.class.getDeclaredField("leftPants");
            java.lang.reflect.Field rightPantsField = PlayerEntityModel.class.getDeclaredField("rightPants");
            java.lang.reflect.Field jacketField = PlayerEntityModel.class.getDeclaredField("jacket");
            leftPantsField.setAccessible(true);
            rightPantsField.setAccessible(true);
            jacketField.setAccessible(true);
            
            Object jacket = jacketField.get(playerModel);
            if (jacket instanceof net.minecraft.client.model.ModelPart) {
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) jacket, contextInfo, bodyTransform);
            }
            
            Object leftPants = leftPantsField.get(playerModel);
            if (leftPants instanceof net.minecraft.client.model.ModelPart) {
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) leftPants, contextInfo, leftLegTransform);
            }
            
            Object rightPants = rightPantsField.get(playerModel);
            if (rightPants instanceof net.minecraft.client.model.ModelPart) {
                renderPartWithTransform(matrixStack, (net.minecraft.client.model.ModelPart) rightPants, contextInfo, rightLegTransform);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 如果模型不支持这些字段，则忽略
        }
    }
    
}

