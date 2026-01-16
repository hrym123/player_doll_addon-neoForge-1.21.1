package com.lanye.dolladdon.impl.entity;

import com.lanye.dolladdon.base.entity.BaseDollEntity;
import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import com.lanye.dolladdon.util.factory.DollItemFactory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 自定义纹理玩偶实体
 * 使用外部 PNG 文件作为纹理
 * 重构后：从NBT读取皮肤路径，不再在构造函数中持有纹理信息
 */
public class CustomTextureDollEntity extends BaseDollEntity {
    
    public CustomTextureDollEntity(EntityType<? extends CustomTextureDollEntity> entityType, Level world) {
        super(entityType, world);
    }
    
    public CustomTextureDollEntity(EntityType<? extends CustomTextureDollEntity> entityType, Level world, double x, double y, double z) {
        super(entityType, world, x, y, z);
    }
    
    @Override
    protected ItemStack getDollItemStack() {
        // 使用工厂类统一创建物品，确保NBT结构标准化，从而可以叠加
        
        // 获取核心信息
        String skinPath = this.getSkinPath();
        if (skinPath == null) {
            // 如果没有皮肤路径，返回无NBT的物品
            return new ItemStack(ModItems.CUSTOM_TEXTURE_DOLL.get());
        }
        
        boolean isAlexModel = this.isAlexModel();
        
        // 从 persistentData 读取玩家信息
        // PlayerName 是必需字段，如果不存在，尝试从皮肤路径提取或使用默认值
        String playerName = null;
        if (this.getPersistentData().contains("PlayerName", net.minecraft.nbt.Tag.TAG_STRING)) {
            playerName = this.getPersistentData().getString("PlayerName");
        }
        
        // 如果 PlayerName 不存在，尝试从皮肤路径提取
        if (playerName == null || playerName.isEmpty()) {
            // 尝试从皮肤路径中提取玩家名（如果是 png/ 路径，文件名可能包含玩家名）
            if (skinPath != null && skinPath.startsWith("player_doll:png/")) {
                String fileName = skinPath.substring("player_doll:png/".length());
                // 移除扩展名
                if (fileName.endsWith(".png")) {
                    fileName = fileName.substring(0, fileName.length() - 4);
                }
                // 移除模型类型前缀（S或A）
                if (fileName.length() > 0 && (fileName.charAt(0) == 'S' || fileName.charAt(0) == 'A')) {
                    fileName = fileName.substring(1);
                }
                // 移除可能的UUID后缀（旧格式兼容）
                int lastUnderscore = fileName.lastIndexOf('_');
                if (lastUnderscore > 0) {
                    String suffix = fileName.substring(lastUnderscore + 1);
                    // 如果是8位十六进制（可能是UUID），移除它
                    if (suffix.length() == 8 && suffix.matches("[0-9a-fA-F]{8}")) {
                        fileName = fileName.substring(0, lastUnderscore);
                    }
                }
                if (!fileName.isEmpty()) {
                    playerName = fileName;
                }
            }
        }
        
        // 如果仍然没有 PlayerName，使用默认值
        if (playerName == null || playerName.isEmpty()) {
            playerName = "Unknown";
        }
        
        // 使用工厂类创建物品，确保NBT结构标准化
        // 注意：不再使用 DisplayName，直接使用 PlayerName 显示
        return DollItemFactory.createCustomTextureDoll(
            skinPath,
            isAlexModel,
            playerName
        );
    }
}
