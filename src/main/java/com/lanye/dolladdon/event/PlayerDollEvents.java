package com.lanye.dolladdon.event;

import com.github.ysbbbbbb.kaleidoscopedoll.entity.DollEntity;
import com.lanye.dolladdon.PlayerDollAddon;
import com.lanye.dolladdon.util.PlayerDollUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.UUID;

/**
 * 玩家玩偶事件处理器
 * 用于在实体生成时恢复玩家信息
 */
public class PlayerDollEvents {
    
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        
        // 只处理玩偶实体
        if (!(entity instanceof DollEntity dollEntity)) {
            return;
        }
        
        // 如果实体已经有玩家信息，跳过
        if (PlayerDollUtil.getPlayerUUID(dollEntity) != null) {
            return;
        }
        
        // 尝试从实体的 NBT 中恢复玩家信息
        // 当实体从物品恢复时，玩家信息可能保存在 NBT 中
        CompoundTag entityData = dollEntity.getPersistentData();
        if (entityData.contains("player_data")) {
            // 玩家信息已经在 PersistentData 中，不需要恢复
            return;
        }
        
        // 如果实体是从物品生成的，尝试从物品获取玩家信息
        // 注意：这里我们无法直接访问生成实体的物品，所以需要在其他地方处理
        // 或者使用 Mixin 扩展 DollEntity 的加载逻辑
    }
}

