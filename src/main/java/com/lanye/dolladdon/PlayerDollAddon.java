package com.lanye.dolladdon;

import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import com.lanye.dolladdon.util.DynamicDollLoader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class PlayerDollAddon implements ModInitializer {
    public static final String MODID = "player_doll_addon";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    
    // 玩偶图片目录路径（相对于游戏目录）
    public static final String PNG_DIR = "player_doll/png";
    // 姿态文件目录路径（相对于游戏目录）
    public static final String POSES_DIR = "player_doll/poses";
    // 动作文件目录路径（相对于游戏目录）
    public static final String ACTIONS_DIR = "player_doll/actions";
    
    // 创造模式物品栏
    public static final ItemGroup PLAYER_DOLL_TAB = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.STEVE_DOLL))
            .displayName(Text.translatable("itemGroup.player_doll_addon.player_doll_tab"))
            .entries((displayContext, entries) -> {
                // 添加史蒂夫玩偶物品（固定模型：粗手臂 + Steve默认皮肤）
                entries.add(new ItemStack(ModItems.STEVE_DOLL));
                
                // 添加艾利克斯玩偶物品（固定模型：细手臂 + Alex默认皮肤）
                entries.add(new ItemStack(ModItems.ALEX_DOLL));
                
                // 添加动态注册的玩偶物品
                for (var entry : ModItems.DYNAMIC_DOLLS.entrySet()) {
                    try {
                        ItemStack stack = new ItemStack(entry.getValue());
                        entries.add(stack);
                    } catch (Exception e) {
                        LOGGER.error("添加动态玩偶到物品栏失败: {}", entry.getKey(), e);
                    }
                }
            })
            .build();

    @Override
    public void onInitialize() {
        // 初始化默认文件（从资源包复制到文件系统）
        initializeDefaultFiles();
        // 先扫描目录并注册动态玩偶（必须在注册器注册之前）
        registerDynamicDolls();
        
        // 注册物品
        ModItems.register();
        // 注册实体
        ModEntities.register();
        // 注册创造模式物品栏
        Registry.register(Registries.ITEM_GROUP, 
                new Identifier(MODID, "player_doll_tab"), 
                PLAYER_DOLL_TAB);
    }
    
    /**
     * 初始化默认文件（生成到文件系统）
     */
    private void initializeDefaultFiles() {
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir();
            com.lanye.dolladdon.util.DefaultFileInitializer.initializeDefaultFiles(gameDir);
        } catch (Exception e) {
            LOGGER.error("初始化默认文件失败", e);
        }
    }
    
    /**
     * 注册动态玩偶（从文件加载）
     * 在构造函数中调用，确保在注册器注册之前完成
     */
    private void registerDynamicDolls() {
        // 先清理旧的动态模型文件（保留 alex_doll.json 和 steve_doll.json）
        com.lanye.dolladdon.util.DynamicModelGenerator.cleanupOldModelFiles();
        
        // 扫描目录
        var dollInfos = DynamicDollLoader.scanDirectory(PNG_DIR);
        
        // 批量生成所有动态玩偶的模型文件（所有动态玩偶都使用相同的模型内容）
        java.util.List<String> registryNames = new java.util.ArrayList<>();
        for (var dollInfo : dollInfos) {
            registryNames.add(dollInfo.getFileName());
        }
        com.lanye.dolladdon.util.DynamicModelGenerator.generateAllItemModels(registryNames);
        
        // 注册每个玩偶
        int successCount = 0;
        for (var dollInfo : dollInfos) {
            try {
                // 注册实体
                var entityType = ModEntities.registerDynamicDoll(dollInfo.getFileName());
                
                // 模型文件已在上面批量生成，这里不需要再生成
                
                // 注册物品（传递 EntityType）
                ModItems.registerDynamicDoll(
                    dollInfo.getFileName(),
                    entityType,
                    dollInfo.getTextureLocation(),
                    dollInfo.isAlexModel(),
                    dollInfo.getDisplayName()
                );
                
                successCount++;
            } catch (Exception e) {
                LOGGER.error("注册动态玩偶失败: {}", dollInfo.getFileName(), e);
                e.printStackTrace();
            }
        }
    }
    
}

