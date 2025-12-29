package com.lanye.dolladdon;

import com.lanye.dolladdon.dynamic.DynamicDollEntity;
import com.lanye.dolladdon.dynamic.DynamicDollItem;
import com.lanye.dolladdon.dynamic.render.DynamicDollRenderer;
import com.lanye.dolladdon.dynamic.render.DynamicDollItemRenderer;
import com.lanye.dolladdon.impl.render.AlexDollRenderer;
import com.lanye.dolladdon.impl.render.AlexDollItemRenderer;
import com.lanye.dolladdon.impl.render.SteveDollRenderer;
import com.lanye.dolladdon.impl.render.SteveDollItemRenderer;
import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import com.lanye.dolladdon.util.DynamicDollLoader;
import com.lanye.dolladdon.util.DynamicResourcePack;
import com.lanye.dolladdon.util.PoseActionManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Path;

public class PlayerDollAddonClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        // 注册实体渲染器
        registerEntityRenderers();
        
        // 注册物品渲染器
        registerItemRenderers();
        
        // 注册资源包
        registerResourcePack();
        
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
        
        // 注册动态玩偶实体渲染器
        // 需要先扫描目录获取信息
        var dollInfos = DynamicDollLoader.scanDirectory(PlayerDollAddon.PNG_DIR);
        for (var dollInfo : dollInfos) {
            var entityType = ModEntities.DYNAMIC_DOLLS.get(dollInfo.getFileName());
            if (entityType != null) {
                EntityRendererRegistry.register(
                    entityType,
                    context -> new DynamicDollRenderer(
                        context,
                        dollInfo.getTextureLocation(),
                        dollInfo.isAlexModel()
                    )
                );
            }
        }
    }
    
    /**
     * 注册物品渲染器
     */
    private void registerItemRenderers() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        // 注册史蒂夫玩偶物品渲染器
        BuiltinItemRendererRegistry.INSTANCE.register(
            ModItems.STEVE_DOLL,
            new SteveDollItemRenderer(
                client,
                client.getEntityModelLoader()
            )
        );
        
        // 注册艾利克斯玩偶物品渲染器
        BuiltinItemRendererRegistry.INSTANCE.register(
            ModItems.ALEX_DOLL,
            new AlexDollItemRenderer(
                client,
                client.getEntityModelLoader()
            )
        );
        
        // 注册动态玩偶物品渲染器
        for (var entry : ModItems.DYNAMIC_DOLLS.entrySet()) {
            var item = entry.getValue();
            if (item instanceof DynamicDollItem dynamicItem) {
                BuiltinItemRendererRegistry.INSTANCE.register(
                    item,
                    new DynamicDollItemRenderer(
                        client,
                        client.getEntityModelLoader(),
                        dynamicItem.getTextureLocation(),
                        dynamicItem.isAlexModel()
                    )
                );
            }
        }
    }
    
    /**
     * 注册自定义资源包以加载动态资源
     */
    private void registerResourcePack() {
        try {
            // 获取游戏目录
            Path gameDir = FabricLoader.getInstance().getGameDir();
            
            // 创建动态资源包
            DynamicResourcePack resourcePack = new DynamicResourcePack(gameDir);
            
            // 使用 Fabric 的 ResourceManagerHelper 注册资源包
            ResourceManagerHelper.registerBuiltinResourcePack(
                new Identifier(PlayerDollAddon.MODID, "dynamic_doll_resources"),
                Text.literal("Dynamic Doll Resources"),
                ResourcePackActivationType.NORMAL
            );
            
            // 注意：Fabric 的资源包注册方式与 NeoForge 不同
            // 这里需要手动将资源包添加到资源包管理器
            // 由于 Fabric 的限制，我们可能需要使用其他方式
            // 暂时先保留这个结构，后续可能需要调整
        } catch (Exception e) {
            PlayerDollAddon.LOGGER.error("注册动态资源包失败", e);
            e.printStackTrace();
        }
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
                        PoseActionManager.loadResources(resourceManager);
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
            net.minecraft.Util.backgroundExecutor().execute(() -> {
                try {
                    Thread.sleep(100); // 等待资源管理器完全初始化
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                
                net.minecraft.util.Util.getIoWorkerExecutor().execute(() -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null && client.getResourceManager() != null) {
                        PoseActionManager.loadResources(client.getResourceManager());
                    }
                });
            });
        });
    }
}

