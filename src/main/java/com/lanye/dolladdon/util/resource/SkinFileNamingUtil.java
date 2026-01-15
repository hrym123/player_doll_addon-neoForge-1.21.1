package com.lanye.dolladdon.util.resource;

/**
 * 皮肤文件命名工具类
 * 根据玩家名和模型类型生成符合命名规则的文件名
 */
public class SkinFileNamingUtil {
    
    /**
     * 生成文件名
     * 格式：[S|A]<玩家名>.png
     * - S = 粗手臂（Steve模型）
     * - A = 细手臂（Alex模型）
     * 
     * @param playerName 玩家名称
     * @param isAlexModel 是否为Alex模型（细手臂）
     * @return 生成的文件名（不含路径）
     */
    public static String generateFileName(String playerName, boolean isAlexModel) {
        // 清理玩家名中的特殊字符
        String sanitizedName = sanitizePlayerName(playerName);
        
        // 生成文件名：模型类型标识符 + 清理后的玩家名 + .png
        String prefix = isAlexModel ? "A" : "S";
        return prefix + sanitizedName + ".png";
    }
    
    /**
     * 清理玩家名中的特殊字符
     * 保留字母、数字、下划线
     * 其他特殊字符替换为下划线
     * 确保文件名符合文件系统规范
     * 
     * @param playerName 原始玩家名
     * @return 清理后的玩家名
     */
    public static String sanitizePlayerName(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return "Player";
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : playerName.toCharArray()) {
            // 保留字母、数字、下划线
            if (Character.isLetterOrDigit(c) || c == '_') {
                result.append(c);
            } else {
                // 其他字符替换为下划线
                result.append('_');
            }
        }
        
        // 确保结果不为空
        if (result.length() == 0) {
            return "Player";
        }
        
        // 移除开头和结尾的下划线（如果存在）
        String sanitized = result.toString();
        while (sanitized.startsWith("_")) {
            sanitized = sanitized.substring(1);
        }
        while (sanitized.endsWith("_")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        
        // 如果清理后为空，使用默认名称
        if (sanitized.isEmpty()) {
            return "Player";
        }
        
        return sanitized;
    }
}
