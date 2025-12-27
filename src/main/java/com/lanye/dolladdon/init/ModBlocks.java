package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.impl.block.CustomBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, PlayerDollAddon.MODID);
    
    // 自定义方块（使用模型文件，禁用遮蔽）
    public static final DeferredHolder<Block, Block> CUSTOM_BLOCK = BLOCKS.register("custom_block",
            () -> new CustomBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS)));
    
    // 方块物品
    public static final DeferredItem<BlockItem> CUSTOM_BLOCK_ITEM = ModItems.ITEMS.register("custom_block",
            () -> new BlockItem(CUSTOM_BLOCK.get(), new Item.Properties()));
}

