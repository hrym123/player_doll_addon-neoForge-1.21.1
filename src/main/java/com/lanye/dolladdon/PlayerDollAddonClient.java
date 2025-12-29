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
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.ResourcePackProvider;

import java.nio.file.Path;
import java.util.function.Consumer;

public class PlayerDollAddonClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        // 清空并重新加载动态资源
        PlayerDollAddon.LOGGER.info("[动态材质加载] Mod启动，清空并重新加载动态资源");
        DynamicResourcePackManager.clearDynamicResources();
        DynamicResourcePackManager.reloadDynamicResources();
        
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
        
        // 注册客户端生命周期事件（游戏关闭时清理）
        registerClientLifecycleEvents();
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
        PlayerDollAddon.LOGGER.info("[动态材质加载] 开始注册动态玩偶实体渲染器");
        var dollInfos = DynamicDollLoader.scanDirectory(PlayerDollAddon.PNG_DIR);
        int registeredCount = 0;
        for (var dollInfo : dollInfos) {
            var entityType = ModEntities.DYNAMIC_DOLLS.get(dollInfo.getFileName());
            if (entityType != null) {
                PlayerDollAddon.LOGGER.info("[动态材质加载] 注册动态玩偶实体渲染器: {} (材质: {}, 模型: {})", 
                    dollInfo.getFileName(), dollInfo.getTextureLocation(), dollInfo.isAlexModel() ? "Alex" : "Steve");
                EntityRendererRegistry.register(
                    entityType,
                    context -> new DynamicDollRenderer(
                        context,
                        dollInfo.getTextureLocation(),
                        dollInfo.isAlexModel()
                    )
                );
                registeredCount++;
            } else {
                PlayerDollAddon.LOGGER.warn("[动态材质加载] 未找到对应的实体类型，跳过渲染器注册: {}", dollInfo.getFileName());
            }
        }
        PlayerDollAddon.LOGGER.info("[动态材质加载] 动态玩偶实体渲染器注册完成，共注册 {} 个", registeredCount);
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
        
        // 注册动态玩偶物品渲染器
        PlayerDollAddon.LOGGER.info("[动态材质加载] 开始注册动态玩偶物品渲染器");
        int itemRendererCount = 0;
        for (var entry : ModItems.DYNAMIC_DOLLS.entrySet()) {
            var item = entry.getValue();
            if (item instanceof DynamicDollItem dynamicItem) {
                PlayerDollAddon.LOGGER.info("[动态材质加载] 注册动态玩偶物品渲染器: {} (材质: {}, 模型: {})", 
                    entry.getKey(), dynamicItem.getTextureLocation(), dynamicItem.isAlexModel() ? "Alex" : "Steve");
                BuiltinItemRendererRegistry.INSTANCE.register(
                    item,
                    new DynamicDollItemRenderer(
                        client,
                        null,
                        dynamicItem.getTextureLocation(),
                        dynamicItem.isAlexModel()
                    )
                );
                itemRendererCount++;
            }
        }
        PlayerDollAddon.LOGGER.info("[动态材质加载] 动态玩偶物品渲染器注册完成，共注册 {} 个", itemRendererCount);
    }
    
    /**
     * 注册自定义资源包以加载动态资源
     */
    private void registerResourcePack() {
        try {
            PlayerDollAddon.LOGGER.info("[动态材质加载] 开始注册动态资源包");
            
            // 注册内置资源包（用于加载占位符纹理）
            Identifier packId = new Identifier(PlayerDollAddon.MODID, "dynamic_doll_resources");
            PlayerDollAddon.LOGGER.debug("[动态材质加载] 资源包ID: {}", packId);
            
            ResourceManagerHelper.registerBuiltinResourcePack(
                packId,
                FabricLoader.getInstance().getModContainer(PlayerDollAddon.MODID).orElse(null),
                Text.literal("Dynamic Doll Resources"),
                ResourcePackActivationType.DEFAULT_ENABLED
            );
            
            PlayerDollAddon.LOGGER.info("[动态材质加载] 内置资源包注册成功: {} (默认启用)", packId);
            
            // 注册动态资源包提供者
            // 注意：ResourcePackProvider 需要在客户端启动后通过 ResourcePackManager 注册
            // 这里先注册事件监听器，等客户端启动后再注册
            ClientLifecycleEvents.CLIENT_STARTED.register((client) -> {
                try {
                    PlayerDollAddon.LOGGER.info("[动态材质加载] 客户端启动，注册动态资源包提供者");
                    ResourcePackManager resourcePackManager = client.getResourcePackManager();
                    if (resourcePackManager != null) {
                        // 尝试通过反射添加 ResourcePackProvider
                        try {
                            java.lang.reflect.Field providersField = ResourcePackManager.class.getDeclaredField("providers");
                            providersField.setAccessible(true);
                            @SuppressWarnings("unchecked")
                            java.util.List<ResourcePackProvider> providers = (java.util.List<ResourcePackProvider>) providersField.get(resourcePackManager);
                            if (providers != null) {
                                providers.add(new DynamicResourcePackProvider());
                                PlayerDollAddon.LOGGER.info("[动态材质加载] 动态资源包提供者注册成功（通过反射）");
                                
                                // 重新扫描资源包以加载动态资源包
                                resourcePackManager.scanPacks();
                                PlayerDollAddon.LOGGER.info("[动态材质加载] 资源包扫描完成，动态资源包应该已加载");
                            } else {
                                PlayerDollAddon.LOGGER.warn("[动态材质加载] providers 字段为 null");
                            }
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            PlayerDollAddon.LOGGER.warn("[动态材质加载] 无法通过反射添加 ResourcePackProvider: {}", e.getMessage());
                            // 如果反射失败，尝试其他方法
                            PlayerDollAddon.LOGGER.warn("[动态材质加载] 将使用备用方法：直接通过 ResourceManager 注入资源包");
                        }
                    } else {
                        PlayerDollAddon.LOGGER.warn("[动态材质加载] ResourcePackManager 为 null，无法注册动态资源包提供者");
                    }
                } catch (Exception e) {
                    PlayerDollAddon.LOGGER.error("[动态材质加载] 注册动态资源包提供者失败", e);
                }
            });
            
            PlayerDollAddon.LOGGER.info("[动态材质加载] 动态资源包注册完成");
        } catch (Exception e) {
            PlayerDollAddon.LOGGER.error("[动态材质加载] 注册动态资源包失败", e);
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
                        PlayerDollAddon.LOGGER.info("[动态材质加载] 资源重载开始");
                        
                        // 重新加载动态资源
                        DynamicResourcePackManager.onResourceManagerReload(resourceManager);
                        
                        // 加载姿态和动作资源
                        PoseActionManager.loadResources(resourceManager);
                        
                        PlayerDollAddon.LOGGER.info("[动态材质加载] 资源重载完成");
                    } catch (Exception e) {
                        PlayerDollAddon.LOGGER.error("[动态材质加载] 资源重载过程中发生异常", e);
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
                        PoseActionManager.loadResources(client.getResourceManager());
                    }
                });
            });
        });
    }
    
    /**
     * 注册客户端生命周期事件
     * 在游戏关闭时清理动态资源
     */
    private void registerClientLifecycleEvents() {
        // 在游戏关闭时清空动态资源
        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> {
            PlayerDollAddon.LOGGER.info("[动态材质加载] 游戏关闭，清空所有动态资源");
            try {
                DynamicResourcePackManager.clearDynamicResources();
                PlayerDollAddon.LOGGER.info("[动态材质加载] 动态资源清理完成");
            } catch (Exception e) {
                PlayerDollAddon.LOGGER.error("[动态材质加载] 清理动态资源时发生异常", e);
            }
        });
    }
}

