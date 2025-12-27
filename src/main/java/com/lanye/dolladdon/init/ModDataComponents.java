package com.lanye.dolladdon.init;

import com.lanye.dolladdon.PlayerDollAddon;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, PlayerDollAddon.MODID);
    
    // 玩家信息数据组件
    public static final Supplier<DataComponentType<CompoundTag>> PLAYER_DATA = DATA_COMPONENTS.register(
            "player_data",
            () -> DataComponentType.<CompoundTag>builder()
                    .persistent(CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                    .build()
    );
}

