package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, PlayerDollAddon.MODID);
}

