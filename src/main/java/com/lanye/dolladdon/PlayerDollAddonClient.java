package com.lanye.dolladdon;

import com.lanye.dolladdon.client.ActionDebugStickHandler;
import com.lanye.dolladdon.client.PoseDebugStickHandler;
import com.lanye.dolladdon.compat.skinlayers3d.Doll3DSkinUtil;
import com.lanye.dolladdon.compat.skinlayers3d.SkinLayersDetector;
import com.lanye.dolladdon.dynamic.render.DynamicDollRenderer;
import com.lanye.dolladdon.impl.entity.CustomTextureDollEntity;
import com.lanye.dolladdon.impl.item.ActionDebugStick;
import com.lanye.dolladdon.impl.item.PoseDebugStick;
import com.lanye.dolladdon.impl.render.AlexDollRenderer;
import com.lanye.dolladdon.impl.render.CustomTextureDollRenderer;
import com.lanye.dolladdon.impl.render.SteveDollRenderer;
import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.util.neoForge.DynamicDollLoader;
import com.lanye.dolladdon.util.neoForge.DynamicResourcePack;
import com.lanye.dolladdon.util.pose.PoseActionManager;
import com.lanye.dolladdon.util.resource.ExternalTextureLoader;
import com.lanye.dolladdon.util.resource.PngTextureScanner;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

@EventBusSubscriber(modid = PlayerDollAddon.MODID, value = Dist.CLIENT)
public class PlayerDollAddonClient {
    
    /**
     * 客户端初始化方法
     * 在客户端启动时调用
     */
    public static void init() {
        // 初始化3D皮肤层检测
        initialize3DSkinLayers();
        // 初始化调试棒处理器
        ActionDebugStickHandler.initialize();
        PoseDebugStickHandler.initialize();
    }
    
    /**
     * 初始化3D皮肤层检测（静态方法）
     */
    private static void initialize3DSkinLayers() {
        // Logging handled by Mixin
        if (SkinLayersDetector.IS_3D_SKIN_LAYERS_LOADED) {
            // Try to initialize API to verify availability
            boolean apiAvailable = Doll3DSkinUtil.isAvailable();
        }
    }
    
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // 注册史蒂夫玩偶实体渲染器（固定模型）
        event.registerEntityRenderer(ModEntities.STEVE_DOLL.get(), SteveDollRenderer::new);
        // 注册艾利克斯玩偶实体渲染器（固定模型）
        event.registerEntityRenderer(ModEntities.ALEX_DOLL.get(), AlexDollRenderer::new);
        
        // 注册动态玩偶实体渲染器
        // 需要先扫描目录获取信息
        var dollInfos = DynamicDollLoader.scanDirectory(PlayerDollAddon.PNG_DIR);
        for (var dollInfo : dollInfos) {
            var entityHolder = ModEntities.DYNAMIC_DOLLS.get(dollInfo.getFileName());
            if (entityHolder != null) {
                event.registerEntityRenderer(
                    entityHolder.get(),
                    context -> new DynamicDollRenderer(
                        context,
                        dollInfo.getTextureLocation(),
                        dollInfo.isAlexModel()
                    )
                );
            }
        }
        
        // 注意：不再注册 CustomTextureDoll 渲染器，因为已经不再注册 CustomTextureDoll 实体
        // 所有玩偶都通过 DynamicDoll 方式注册和渲染
        /*
        // 注册所有自定义纹理玩偶实体渲染器
        java.util.Map<String, net.minecraft.world.entity.EntityType<CustomTextureDollEntity>> customEntities = 
                ModEntities.getAllCustomTextureDollEntityTypes();
        
        // 获取所有自定义纹理信息，用于检测模型类型
        java.util.List<PngTextureScanner.PngTextureInfo> pngInfos = PngTextureScanner.scanPngFiles();
        
        for (java.util.Map.Entry<String, net.minecraft.world.entity.EntityType<CustomTextureDollEntity>> entry : customEntities.entrySet()) {
            try {
                String registryName = entry.getKey();
                
                // 查找对应的纹理信息
                PngTextureScanner.PngTextureInfo pngInfo = null;
                for (PngTextureScanner.PngTextureInfo info : pngInfos) {
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
                event.registerEntityRenderer(entry.getValue(), 
                        context -> new CustomTextureDollRenderer(context, finalIsAlexModel));
            } catch (Exception e) {
                // Error logging handled by Mixin
            }
        }
        */
    }
    
    /**
     * 注册自定义资源包以加载动态资源
     */
    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            try {
                // 获取游戏目录
                Path gameDir;
                try {
                    Class<?> fmlPathsClass = Class.forName("net.neoforged.fml.loading.FMLPaths");
                    java.lang.reflect.Method gameDirMethod = fmlPathsClass.getMethod("getGamePath");
                    gameDir = (Path) gameDirMethod.invoke(null);
                } catch (Exception e) {
                    gameDir = Paths.get(".").toAbsolutePath().normalize();
                }
                
                // 创建动态资源包
                DynamicResourcePack resourcePack = new DynamicResourcePack(gameDir);
                
                // 注册资源包
                PackLocationInfo packLocationInfo = resourcePack.location();
                MutableComponent packName = Component.literal("Dynamic Doll Resources");
                Pack.ResourcesSupplier resourcesSupplier = new Pack.ResourcesSupplier() {
                    @Override
                    public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo locationInfo) {
                        return resourcePack;
                    }
                    
                    @Override
                    public net.minecraft.server.packs.PackResources openFull(PackLocationInfo locationInfo, Pack.Metadata metadata) {
                        return resourcePack;
                    }
                };
                Pack.Metadata metadata = new Pack.Metadata(packName, PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), Collections.emptyList());
                // required=true 确保资源包被自动启用
                // Pack.Position.TOP 尝试将资源包放在最前面（但实际加载顺序可能仍然在最后）
                PackSelectionConfig selectionConfig = new PackSelectionConfig(true, Pack.Position.TOP, false);
                
                // 尝试在更早的时机添加资源包（在 AddPackFindersEvent 中尽早添加）
                event.addRepositorySource((packConsumer) -> {
                    Pack pack = new Pack(
                        packLocationInfo,
                        resourcesSupplier,
                        metadata,
                        selectionConfig
                    );
                    // 先接受包，确保它被添加到列表的最前面
                    packConsumer.accept(pack);
                });
            } catch (Exception e) {
                // Error logging handled by Mixin
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 注册资源重载监听器
     * 当执行 F3+T 重新加载资源时会触发此监听器
     */
    @SubscribeEvent
    public static void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> {
            try {
                // 加载姿态和动作资源
                PoseActionManager.loadResources(resourceManager);
                
                // 加载外部纹理
                ExternalTextureLoader.loadExternalTextures();
                
                // 将外部纹理注册到纹理管理器
                net.minecraft.client.renderer.texture.TextureManager textureManager = 
                        Minecraft.getInstance().getTextureManager();
                if (textureManager != null) {
                    java.util.Map<ResourceLocation, java.nio.file.Path> textures = 
                            ExternalTextureLoader.getAllLoadedTextures();
                    for (ResourceLocation textureId : textures.keySet()) {
                        ExternalTextureLoader.loadTexture(textureId, textureManager);
                    }
                }
            } catch (Exception e) {
                // Error logging handled by Mixin
            }
        });
    }
    
    /**
     * 在客户端登录后加载姿态和动作资源
     */
    @SubscribeEvent
    public static void onClientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // 恢复调试棒数据（从物品栏中的 ItemStack NBT）
        if (event.getPlayer() != null) {
            com.lanye.dolladdon.impl.item.ActionDebugStick.restoreFromInventory(event.getPlayer());
            com.lanye.dolladdon.impl.item.PoseDebugStick.restoreFromInventory(event.getPlayer());
        }
        
        // 延迟加载，确保资源管理器已完全初始化
        net.minecraft.Util.backgroundExecutor().execute(() -> {
            try {
                Thread.sleep(100); // 等待资源管理器完全初始化
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            
            net.minecraft.Util.ioPool().execute(() -> {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft != null && minecraft.getResourceManager() != null) {
                    PoseActionManager.loadResources(minecraft.getResourceManager());
                    
                    // 加载外部纹理
                    ExternalTextureLoader.loadExternalTextures();
                    
                    // 将外部纹理注册到纹理管理器
                    net.minecraft.client.renderer.texture.TextureManager textureManager = minecraft.getTextureManager();
                    if (textureManager != null) {
                        java.util.Map<ResourceLocation, java.nio.file.Path> textures = 
                                ExternalTextureLoader.getAllLoadedTextures();
                        for (ResourceLocation textureId : textures.keySet()) {
                            ExternalTextureLoader.loadTexture(textureId, textureManager);
                        }
                    }
                }
            });
        });
    }
    
    /**
     * 处理鼠标滚轮事件，用于调试棒的滚轮切换功能
     */
    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        
        // 如果当前有打开的屏幕（GUI），不处理（让默认行为执行）
        if (client.screen != null) {
            return;
        }
        
        // 检查玩家是否在潜行
        if (!client.player.isShiftKeyDown()) {
            return;
        }
        
        // 检查玩家是否手持动作调试棒或姿态调试棒
        net.minecraft.world.item.ItemStack mainHandStack = client.player.getMainHandItem();
        net.minecraft.world.item.ItemStack offHandStack = client.player.getOffhandItem();
        
        net.minecraft.world.item.ItemStack heldStack = null;
        boolean isActionDebugStick = false;
        boolean isPoseDebugStick = false;
        
        if (mainHandStack.getItem() instanceof ActionDebugStick) {
            heldStack = mainHandStack;
            isActionDebugStick = true;
        } else if (offHandStack.getItem() instanceof ActionDebugStick) {
            heldStack = offHandStack;
            isActionDebugStick = true;
        } else if (mainHandStack.getItem() instanceof PoseDebugStick) {
            heldStack = mainHandStack;
            isPoseDebugStick = true;
        } else if (offHandStack.getItem() instanceof PoseDebugStick) {
            heldStack = offHandStack;
            isPoseDebugStick = true;
        }
        
        if (heldStack == null) {
            return;
        }
        
        // 处理滚轮事件
        // 在 NeoForge 1.21.1 中，MouseScrollingEvent 使用 getScrollDeltaY() 获取垂直滚动量
        double scrollDelta = event.getScrollDeltaY();
        if (scrollDelta != 0) {
            boolean forward = scrollDelta > 0;
            if (isActionDebugStick) {
                ActionDebugStickHandler.switchToNextAction(client, heldStack, forward);
                event.setCanceled(true);
            } else if (isPoseDebugStick) {
                PoseDebugStickHandler.switchToNextPose(client, heldStack, forward);
                event.setCanceled(true);
            }
        }
    }
}

