package com.lanye.dolladdon;

import com.lanye.dolladdon.dynamic.DynamicDollEntity;
import com.lanye.dolladdon.dynamic.render.DynamicDollRenderer;
import com.lanye.dolladdon.impl.render.AlexDollRenderer;
import com.lanye.dolladdon.impl.render.SteveDollRenderer;
import com.lanye.dolladdon.init.ModEntities;
import com.lanye.dolladdon.init.ModItems;
import com.lanye.dolladdon.util.DynamicDollLoader;
import com.lanye.dolladdon.util.DynamicResourcePack;
import com.lanye.dolladdon.util.PoseActionManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.minecraft.client.Minecraft;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

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
        
        // 注册动态玩偶实体渲染器
        // 需要先扫描目录获取信息
        var dollInfos = DynamicDollLoader.scanDirectory(PlayerDollAddon.DOLL_TEXTURES_DIR);
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
                PlayerDollAddon.LOGGER.info("已注册动态玩偶渲染器: {}", dollInfo.getFileName());
            }
        }
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
                        PlayerDollAddon.LOGGER.debug("动态资源包 openPrimary 被调用");
                        return resourcePack;
                    }
                    
                    @Override
                    public net.minecraft.server.packs.PackResources openFull(PackLocationInfo locationInfo, Pack.Metadata metadata) {
                        PlayerDollAddon.LOGGER.debug("动态资源包 openFull 被调用");
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
                    PlayerDollAddon.LOGGER.info("动态资源包已添加到资源管理器，纹理数量: {}", com.lanye.dolladdon.util.DynamicTextureManager.TEXTURE_PATHS.size());
                    PlayerDollAddon.LOGGER.warn("【资源包注册】动态资源包 packId: {}, 位置: TOP, required: true", packLocationInfo.id());
                });
                
                PlayerDollAddon.LOGGER.info("已注册动态资源包，当前注册的纹理数量: {}", com.lanye.dolladdon.util.DynamicTextureManager.TEXTURE_PATHS.size());
            } catch (Exception e) {
                PlayerDollAddon.LOGGER.error("注册动态资源包失败", e);
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 在客户端登录后加载姿态和动作资源
     */
    @SubscribeEvent
    public static void onClientPlayerLoggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        // 延迟加载，确保资源管理器已完全初始化
        net.minecraft.Util.backgroundExecutor().execute(() -> {
            try {
                Thread.sleep(100); // 等待资源管理器完全初始化
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            net.minecraft.Util.ioPool().execute(() -> {
                if (Minecraft.getInstance().getResourceManager() != null) {
                    PoseActionManager.loadResources(Minecraft.getInstance().getResourceManager());
                }
            });
        });
    }
}

