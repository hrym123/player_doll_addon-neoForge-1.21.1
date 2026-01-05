package com.lanye.dolladdon;

import com.lanye.dolladdon.impl.render.AlexDollRenderer;
import com.lanye.dolladdon.impl.render.CustomTextureDollItemRenderer;
import com.lanye.dolladdon.impl.render.CustomTextureDollRenderer;
import com.lanye.dolladdon.impl.render.SteveDollRenderer;
import com.lanye.dolladdon.impl.render.StandardDollItemRenderer;
import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import com.lanye.dolladdon.util.logging.LogModuleConfig;
import com.lanye.dolladdon.util.logging.ModuleLogger;
import com.lanye.dolladdon.util.resource.ExternalTextureLoader;
import com.lanye.dolladdon.util.resource.PngTextureScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.Map;

public class PlayerDollAddonClient implements ClientModInitializer {
    // 日志模块名称
    private static final String LOG_MODULE = "3d_skin_layers";

    /**
     * 检测3D皮肤层mod是否已加载
     * @deprecated 请使用 {@link com.lanye.dolladdon.util.skinlayers3d.SkinLayersDetector#IS_3D_SKIN_LAYERS_LOADED} 代替
     */
    @Deprecated
    public static final boolean IS_3D_SKIN_LAYERS_LOADED = 
            com.lanye.dolladdon.util.skinlayers3d.SkinLayersDetector.IS_3D_SKIN_LAYERS_LOADED;
    
    @Override
    public void onInitializeClient() {
        // 配置日志级别：只保留[3D皮肤层]的DEBUG/INFO日志，其他模块只输出WARN及以上
        // 使用ModuleLogger统一管理日志级别
        com.lanye.dolladdon.util.logging.ModuleLogger.configureLogLevelsForDebugModules(
                "player_doll.3d_skin_layers",
                "com.lanye.dolladdon.util.skinlayers3d.Doll3DSkinUtil",
                "com.lanye.dolladdon.util.skinlayers3d.Doll3DSkinData"
        );
        
        // 输出3D皮肤层mod检测结果
        ModuleLogger.info(LOG_MODULE, "========== 3D皮肤层兼容性检测 ==========");
        if (com.lanye.dolladdon.util.skinlayers3d.SkinLayersDetector.IS_3D_SKIN_LAYERS_LOADED) {
            ModuleLogger.info(LOG_MODULE, "✓ 检测到3D皮肤层mod（skinlayers3d）");
            ModuleLogger.info(LOG_MODULE, "正在初始化API...");
            // 尝试初始化API以验证是否可用
            boolean apiAvailable = com.lanye.dolladdon.util.skinlayers3d.Doll3DSkinUtil.isAvailable();
            if (apiAvailable) {
                ModuleLogger.info(LOG_MODULE, "✓ API初始化成功，将启用3D皮肤渲染支持");
            } else {
                ModuleLogger.warn(LOG_MODULE, "✗ API初始化失败，将使用默认2D渲染");
            }
        } else {
            ModuleLogger.info(LOG_MODULE, "未检测到3D皮肤层mod，使用默认2D渲染");
        }
        ModuleLogger.info(LOG_MODULE, "========================================");
        // 注册动态资源包
        registerDynamicResourcePack();
        
        // 注册实体渲染器
        registerEntityRenderers();
        
        // 注册物品渲染器
        registerItemRenderers();
        
        // 注册资源重载监听器
        registerResourceReloadListener();
        
        // 注册客户端连接事件
        registerClientConnectionEvents();
        
        // 注册按键处理
        registerKeyBindings();
    }
    
    /**
     * 注册动态资源包
     * 注意：在 Fabric 1.20.1 中，我们使用纹理加载器直接加载外部 PNG 文件
     */
    private void registerDynamicResourcePack() {
        // 资源包将在资源重载时通过监听器加载
    }
    
    /**
     * 注册实体渲染器
     */
    private void registerEntityRenderers() {
        // 注册史蒂夫玩偶实体渲染器（固定模型）
        EntityRendererRegistry.register(ModEntities.STEVE_DOLL, SteveDollRenderer::new);
        
        // 注册艾利克斯玩偶实体渲染器（固定模型）
        EntityRendererRegistry.register(ModEntities.ALEX_DOLL, AlexDollRenderer::new);
        
        // 注册所有自定义纹理玩偶实体渲染器
        Map<String, net.minecraft.entity.EntityType<com.lanye.dolladdon.impl.entity.CustomTextureDollEntity>> customEntities = 
                ModEntities.getAllCustomTextureDollEntityTypes();
        
        // 获取所有自定义纹理信息，用于检测模型类型
        com.lanye.dolladdon.util.resource.PngTextureScanner.PngTextureInfo[] pngInfos = 
                com.lanye.dolladdon.util.resource.PngTextureScanner.scanPngFiles().toArray(
                        new com.lanye.dolladdon.util.resource.PngTextureScanner.PngTextureInfo[0]);
        
        for (Map.Entry<String, net.minecraft.entity.EntityType<com.lanye.dolladdon.impl.entity.CustomTextureDollEntity>> entry : customEntities.entrySet()) {
            try {
                String registryName = entry.getKey();
                
                // 查找对应的纹理信息
                com.lanye.dolladdon.util.resource.PngTextureScanner.PngTextureInfo pngInfo = null;
                for (com.lanye.dolladdon.util.resource.PngTextureScanner.PngTextureInfo info : pngInfos) {
                    if (info.getRegistryName().equals(registryName)) {
                        pngInfo = info;
                        break;
                    }
                }
                
                // 检测模型类型
                boolean isAlexModel = false;
                if (pngInfo != null) {
                    isAlexModel = CustomTextureDollRenderer.detectIsAlexModel(
                            registryName, pngInfo.getTextureIdentifier());
                }
                
                // 创建渲染器工厂，传入模型类型
                final boolean finalIsAlexModel = isAlexModel;
                EntityRendererRegistry.register(entry.getValue(), 
                        context -> new CustomTextureDollRenderer(context, finalIsAlexModel));
            } catch (Exception e) {
                ModuleLogger.error(LogModuleConfig.MODULE_RENDER, "[渲染器] ✗ 注册自定义纹理玩偶实体渲染器失败: {}", entry.getKey(), e);
            }
        }
    }
    
    /**
     * 注册物品渲染器
     */
    private void registerItemRenderers() {
        MinecraftClient client = MinecraftClient.getInstance();
        
        if (client == null) {
            ModuleLogger.error(LogModuleConfig.MODULE_RENDER, "[渲染器] ✗ MinecraftClient 未初始化，无法注册物品渲染器");
            return;
        }
        
        // 注册史蒂夫玩偶物品渲染器（使用标准渲染器，false = 粗手臂模型）
        try {
            BuiltinItemRendererRegistry.INSTANCE.register(
                ModItems.STEVE_DOLL,
                new StandardDollItemRenderer(client, null, false)
            );
        } catch (Exception e) {
            ModuleLogger.error(LogModuleConfig.MODULE_RENDER, "[渲染器] ✗ steve_doll 物品渲染器注册失败", e);
        }
        
        // 注册艾利克斯玩偶物品渲染器（使用标准渲染器，true = 细手臂模型）
        try {
            BuiltinItemRendererRegistry.INSTANCE.register(
                ModItems.ALEX_DOLL,
                new StandardDollItemRenderer(client, null, true)
            );
        } catch (Exception e) {
            ModuleLogger.error(LogModuleConfig.MODULE_RENDER, "[渲染器] ✗ alex_doll 物品渲染器注册失败", e);
        }
        
        // 注册所有自定义纹理玩偶物品渲染器
        Map<String, net.minecraft.item.Item> customItems = ModItems.getAllCustomTextureDollItems();
        
        for (Map.Entry<String, net.minecraft.item.Item> entry : customItems.entrySet()) {
            try {
                String registryName = entry.getKey();
                net.minecraft.item.Item item = entry.getValue();
                
                if (item == null) {
                    ModuleLogger.error(LogModuleConfig.MODULE_RENDER, "[渲染器] ✗ 物品为 null: {}", registryName);
                    continue;
                }
                
                // 获取纹理标识符
                Identifier textureId = new Identifier(PlayerDollAddon.MODID, "textures/entity/custom_doll/" + registryName);
                
                // 注册物品渲染器
                BuiltinItemRendererRegistry.INSTANCE.register(
                    item,
                    new CustomTextureDollItemRenderer(client, textureId)
                );
            } catch (Exception e) {
                ModuleLogger.error(LogModuleConfig.MODULE_RENDER, "[渲染器] ✗ 注册自定义纹理玩偶物品渲染器失败: {}", entry.getKey(), e);
            }
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
                        // 加载姿态和动作资源
                        com.lanye.dolladdon.util.pose.PoseActionManager.loadResources(resourceManager);
                        
                        // 加载外部纹理
                        ExternalTextureLoader.loadExternalTextures();
                        
                        // 将外部纹理注册到纹理管理器
                        net.minecraft.client.texture.TextureManager textureManager = 
                                MinecraftClient.getInstance().getTextureManager();
                        if (textureManager != null) {
                            Map<net.minecraft.util.Identifier, java.nio.file.Path> textures = 
                                    ExternalTextureLoader.getAllLoadedTextures();
                            for (net.minecraft.util.Identifier textureId : textures.keySet()) {
                                ExternalTextureLoader.loadTexture(textureId, textureManager);
                            }
                        }
                    } catch (Exception e) {
                        ModuleLogger.error(LogModuleConfig.MODULE_RESOURCE, "资源重载过程中发生异常", e);
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
                        com.lanye.dolladdon.util.pose.PoseActionManager.loadResources(client.getResourceManager());
                        
                        // 加载外部纹理
                        ExternalTextureLoader.loadExternalTextures();
                        
                        // 将外部纹理注册到纹理管理器
                        net.minecraft.client.texture.TextureManager textureManager = mcClient.getTextureManager();
                        if (textureManager != null) {
                            Map<net.minecraft.util.Identifier, java.nio.file.Path> textures = 
                                    ExternalTextureLoader.getAllLoadedTextures();
                            for (net.minecraft.util.Identifier textureId : textures.keySet()) {
                                ExternalTextureLoader.loadTexture(textureId, textureManager);
                            }
                        }
                    }
                });
            });
        });
    }
    
    /**
     * 注册按键绑定
     */
    private void registerKeyBindings() {
        com.lanye.dolladdon.client.DollActionKeyHandler.initialize();
        com.lanye.dolladdon.client.DollActionKeyHandler.registerTickEvent();
        
        // 初始化动作调试棒处理器
        com.lanye.dolladdon.client.ActionDebugStickHandler.initialize();
        
        // 初始化姿态调试棒处理器
        com.lanye.dolladdon.client.PoseDebugStickHandler.initialize();
    }
    
}

