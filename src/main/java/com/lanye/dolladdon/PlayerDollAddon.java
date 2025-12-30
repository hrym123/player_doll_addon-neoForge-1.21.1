package com.lanye.dolladdon;

import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
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
            })
            .build();

    @Override
    public void onInitialize() {
        // 初始化默认文件（从资源包复制到文件系统）
        initializeDefaultFiles();
        
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
    
}

