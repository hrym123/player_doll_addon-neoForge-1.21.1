package com.lanye.dolladdon.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置类
 * 管理模组的各种配置选项
 */
public class ModConfig {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    /**
     * 测试模式
     * 启用后会输出详细的调试日志，帮助分析问题
     */
    public static final ModConfigSpec.BooleanValue TEST_MODE;
    
    static {
        BUILDER.comment("玩家玩偶附属模组配置")
                .push("general");
        
        TEST_MODE = BUILDER
                .comment("测试模式：启用后会输出详细的调试日志，帮助分析皮肤加载和模型识别问题")
                .comment("Test Mode: Enable detailed debug logs to help analyze skin loading and model recognition issues")
                .define("testMode", false);
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
    
    /**
     * 检查是否启用了测试模式
     * @return 如果启用了测试模式返回 true，否则返回 false
     * 注意：如果配置尚未加载，将返回默认值 false
     */
    public static boolean isTestMode() {
        try {
            return TEST_MODE.get();
        } catch (IllegalStateException e) {
            // 配置尚未加载，返回默认值 false
            return false;
        }
    }
}

