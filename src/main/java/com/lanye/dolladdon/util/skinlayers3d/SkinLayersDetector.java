package com.lanye.dolladdon.util.skinlayers3d;

import net.fabricmc.loader.api.FabricLoader;

/**
 * 3D皮肤层mod检测器
 * 用于检测3D皮肤层mod是否已加载
 */
public class SkinLayersDetector {
    /**
     * 检测3D皮肤层mod是否已加载
     */
    public static final boolean IS_3D_SKIN_LAYERS_LOADED = 
            FabricLoader.getInstance().isModLoaded("skinlayers3d");
    
    /**
     * 检查3D皮肤层mod是否已加载
     * @return 如果mod已加载返回true，否则返回false
     */
    public static boolean is3DSkinLayersLoaded() {
        return IS_3D_SKIN_LAYERS_LOADED;
    }
}
