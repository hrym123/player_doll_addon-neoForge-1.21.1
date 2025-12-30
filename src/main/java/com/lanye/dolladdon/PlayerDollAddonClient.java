package com.lanye.dolladdon;

import com.lanye.dolladdon.impl.render.AlexDollRenderer;
import com.lanye.dolladdon.impl.render.AlexDollItemRenderer;
import com.lanye.dolladdon.impl.render.SteveDollRenderer;
import com.lanye.dolladdon.impl.render.SteveDollItemRenderer;
import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.function.Consumer;

public class PlayerDollAddonClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        // 注册实体渲染器
        registerEntityRenderers();
        
        // 注册物品渲染器
        registerItemRenderers();
        
        // 注册资源重载监听器
        registerResourceReloadListener();
        
        // 注册客户端连接事件
        registerClientConnectionEvents();
    }
    
    /**
     * 注册实体渲染器
     */
    private void registerEntityRenderers() {
        // 注册史蒂夫玩偶实体渲染器（固定模型）
        EntityRendererRegistry.register(ModEntities.STEVE_DOLL, SteveDollRenderer::new);
        // 注册艾利克斯玩偶实体渲染器（固定模型）
        EntityRendererRegistry.register(ModEntities.ALEX_DOLL, AlexDollRenderer::new);
    }
    
    /**
     * 注册物品渲染器
     */
    private void registerItemRenderers() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // 注册史蒂夫玩偶物品渲染器
        BuiltinItemRendererRegistry.INSTANCE.register(
            ModItems.STEVE_DOLL,
            new SteveDollItemRenderer(client, null)
        );
        
        // 注册艾利克斯玩偶物品渲染器
        BuiltinItemRendererRegistry.INSTANCE.register(
            ModItems.ALEX_DOLL,
            new AlexDollItemRenderer(client, null)
        );
    }
    
    /**
     * 注册资源重载监听器
     * 当执行 F3+T 重新加载资源时会触发此监听器
     */
    private void registerResourceReloadListener() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(
            new SimpleSynchronousResourceReloadListener() {
                @Override
                public Identifier getFabricId() {
                    return new Identifier(PlayerDollAddon.MODID, "pose_action_reload");
                }
                
                @Override
                public void reload(ResourceManager resourceManager) {
                    try {
                        // 加载姿态和动作资源
                        com.lanye.dolladdon.util.PoseActionManager.loadResources(resourceManager);
                    } catch (Exception e) {
                        PlayerDollAddon.LOGGER.error("资源重载过程中发生异常", e);
                    }
                }
            }
        );
    }
    
    
    /**
     * 注册客户端连接事件
     * 在客户端登录后加载姿态和动作资源
     */
    private void registerClientConnectionEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // 延迟加载，确保资源管理器已完全初始化
            net.minecraft.util.Util.getIoWorkerExecutor().execute(() -> {
                try {
                    Thread.sleep(100); // 等待资源管理器完全初始化
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                
                net.minecraft.util.Util.getIoWorkerExecutor().execute(() -> {
                    MinecraftClient mcClient = MinecraftClient.getInstance();
                    if (mcClient != null && mcClient.getResourceManager() != null) {
                        com.lanye.dolladdon.util.PoseActionManager.loadResources(client.getResourceManager());
                    }
                });
            });
        });
    }
    
}

