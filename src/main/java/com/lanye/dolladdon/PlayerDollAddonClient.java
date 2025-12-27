package com.lanye.dolladdon;

import com.lanye.dolladdon.client.render.AlexDollRenderer;
import com.lanye.dolladdon.client.render.SteveDollRenderer;
import com.lanye.dolladdon.init.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = PlayerDollAddon.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = PlayerDollAddon.MODID, value = Dist.CLIENT)
public class PlayerDollAddonClient {
    
    public PlayerDollAddonClient(ModContainer container) {
    }
    
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 注册史蒂夫玩偶实体渲染器（固定模型）
        event.registerEntityRenderer(ModEntities.STEVE_DOLL.get(), SteveDollRenderer::new);
        // 注册艾利克斯玩偶实体渲染器（固定模型）
        event.registerEntityRenderer(ModEntities.ALEX_DOLL.get(), AlexDollRenderer::new);
    }
}

