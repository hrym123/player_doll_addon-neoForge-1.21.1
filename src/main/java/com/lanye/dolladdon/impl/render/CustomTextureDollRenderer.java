package com.lanye.dolladdon.impl.render;

import com.lanye.dolladdon.base.render.BaseDollRenderer;
import com.lanye.dolladdon.impl.entity.CustomTextureDollEntity;
import com.lanye.dolladdon.util.resource.ExternalTextureLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;

/**
 * 自定义纹理玩偶实体渲染器
 * 使用外部 PNG 文件作为纹理
 * 重构后：从实体NBT读取模型类型，不再在构造函数中持有模型类型信息
 */
public class CustomTextureDollRenderer extends BaseDollRenderer<CustomTextureDollEntity> {
    
    /**
     * 构造函数
     * @param context 渲染器上下文
     */
    public CustomTextureDollRenderer(EntityRendererProvider.Context context) {
        // 使用粗手臂模型作为默认，实际模型类型从NBT读取
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false));
    }
    
    @Override
    protected boolean isThinArms() {
        // 注意：这个方法在渲染时调用，但无法访问实体
        // 由于 playerModel 是 final 字段，无法动态切换模型
        // 这里返回默认值（粗手臂），实际模型类型通过重写 render 方法动态确定
        // 或者使用两个不同的渲染器（Alex和Steve），根据NBT选择
        // 当前实现：使用粗手臂模型作为默认，细手臂模型通过其他方式处理
        return false; // 默认粗手臂
    }
    
    /**
     * 从实体NBT读取模型类型
     */
    private boolean getIsAlexModelFromNBT(CustomTextureDollEntity entity) {
        var persistentData = entity.getPersistentData();
        if (persistentData.contains("IsAlexModel", net.minecraft.nbt.Tag.TAG_BYTE)) {
            return persistentData.getBoolean("IsAlexModel");
        }
        return false; // 默认粗手臂
    }
    
    /**
     * 检测皮肤是否为细手臂模型（Alex类型）
     * 根据文件名判断：
     * - 如果文件名以 "A" 开头，使用细手臂模型
     * - 如果文件名包含 "slim" 或 "alex"（不区分大小写），使用细手臂模型
     * - 否则使用粗手臂模型（Steve类型）
     * 
     * @param registryName 注册名称
     * @param textureId 纹理标识符
     * @return true 表示使用细手臂模型，false 表示使用粗手臂模型
     */
    public static boolean detectIsAlexModel(String registryName, ResourceLocation textureId) {
        // 检查注册名称（registryName）是否以 "a" 开头
        if (registryName != null && registryName.length() > 0) {
            char firstChar = registryName.charAt(0);
            if (firstChar == 'a' || firstChar == 'A') {
                return true;
            }
        }
        
        // 尝试从纹理路径获取文件名
        String texturePath = textureId.getPath();
        
        // 检查路径中是否包含 "slim" 或 "alex"
        String lowerPath = texturePath.toLowerCase();
        if (lowerPath.contains("slim") || lowerPath.contains("alex")) {
            return true;
        }
        
        // 尝试从文件路径获取原始文件名
        Path filePath = ExternalTextureLoader.getTexturePath(textureId);
        if (filePath != null) {
            String fileName = filePath.getFileName().toString();
            // 移除扩展名
            String nameWithoutExt = fileName;
            if (nameWithoutExt.toLowerCase().endsWith(".png")) {
                nameWithoutExt = nameWithoutExt.substring(0, nameWithoutExt.length() - 4);
            }
            
            // 检查文件名是否以 "A" 开头（表示Alex/细手臂）
            if (nameWithoutExt.length() > 0 && nameWithoutExt.charAt(0) == 'A') {
                return true;
            }
            
            // 检查文件名是否包含 "slim" 或 "alex"
            String lowerName = nameWithoutExt.toLowerCase();
            if (lowerName.contains("slim") || lowerName.contains("alex")) {
                return true;
            }
        }
        
        // 默认使用粗手臂模型
        return false;
    }
    
    @Override
    protected ResourceLocation getDefaultTexture(CustomTextureDollEntity entity) {
        // 注意：不要在这里调用 getSkinLocation()，因为 getSkinLocation() 会调用 getDefaultTexture()
        // 这会导致无限递归！
        // getSkinLocation() 已经在基类中处理了从NBT读取皮肤路径的逻辑
        
        // 如果 getSkinLocation() 返回 null（NBT中没有皮肤路径），这里返回默认纹理
        // 使用Steve默认皮肤作为回退
        return com.lanye.dolladdon.util.resource.PlayerSkinUtil.getSteveSkin();
    }
}
