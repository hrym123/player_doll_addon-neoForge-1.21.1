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
     * 转换规则：
     * - 单个下划线 _ → 双下划线 __（在文件名中）
     * - 空格 → 单下划线 _（在文件名中）
     * - 保留字母、数字
     * - 其他特殊字符替换为下划线 _
     * 确保文件名符合文件系统规范
     * 
     * @param playerName 原始玩家名
     * @return 清理后的玩家名（用于文件名）
     */
    public static String sanitizePlayerName(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return "Player";
        }
        
        StringBuilder result = new StringBuilder();
        for (char c : playerName.toCharArray()) {
            if (c == '_') {
                // 单个下划线 → 双下划线（在文件名中）
                result.append("__");
            } else if (c == ' ') {
                // 空格 → 单下划线（在文件名中）
                result.append('_');
            } else if (Character.isLetterOrDigit(c)) {
                // 保留字母、数字
                result.append(c);
            } else {
                // 其他特殊字符替换为下划线
                result.append('_');
            }
        }
        
        // 确保结果不为空
        if (result.length() == 0) {
            return "Player";
        }
        
        // 移除开头和结尾的下划线（如果存在，但需要小心处理，不要破坏双下划线）
        // 注意：开头和结尾的单个下划线可以移除，但双下划线的第一个下划线不应该移除
        String sanitized = result.toString();
        
        // 移除开头的单个下划线（但保留双下划线）
        while (sanitized.startsWith("_") && !sanitized.startsWith("__")) {
            sanitized = sanitized.substring(1);
        }
        
        // 移除结尾的单个下划线（但保留双下划线）
        while (sanitized.endsWith("_") && !sanitized.endsWith("__")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        
        // 如果清理后为空，使用默认名称
        if (sanitized.isEmpty()) {
            return "Player";
        }
        
        return sanitized;
    }
    
    /**
     * 从清理后的文件名恢复原始玩家名
     * 转换规则（反向）：
     * - 双下划线 __ → 单下划线 _（在玩家名中）
     * - 单下划线 _ → 空格（在玩家名中）
     * 
     * @param sanitizedName 清理后的文件名（不含前缀和后缀）
     * @return 恢复的玩家名
     */
    public static String restorePlayerName(String sanitizedName) {
        if (sanitizedName == null || sanitizedName.isEmpty()) {
            return "Player";
        }
        
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < sanitizedName.length()) {
            char c = sanitizedName.charAt(i);
            
            if (c == '_') {
                // 检查是否是双下划线
                if (i + 1 < sanitizedName.length() && sanitizedName.charAt(i + 1) == '_') {
                    // 双下划线 → 单下划线
                    result.append('_');
                    i += 2; // 跳过两个字符
                } else {
                    // 单下划线 → 空格
                    result.append(' ');
                    i++;
                }
            } else {
                result.append(c);
                i++;
            }
        }
        
        return result.toString();
    }
    
    /**
     * 从文件名中提取玩家名
     * 文件名格式：[S|A]<清理后的玩家名>.png（新格式）
     * 或 [S|A]<清理后的玩家名>_<UUID短版本>.png（旧格式，向后兼容）
     * 
     * 会进行反向转换：
     * - 双下划线 __ → 单下划线 _
     * - 单下划线 _ → 空格
     * 
     * @param fileName 文件名（可能包含路径）
     * @return 提取并恢复的玩家名，如果无法提取则返回null
     */
    public static String extractPlayerNameFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }
        
        // 移除路径，只保留文件名
        String name = fileName;
        int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < name.length() - 1) {
            name = name.substring(lastSlash + 1);
        }
        
        // 移除扩展名
        if (name.endsWith(".png")) {
            name = name.substring(0, name.length() - 4);
        }
        
        // 移除开头的模型类型标识符（S或A）
        if (name.length() > 0 && (name.charAt(0) == 'S' || name.charAt(0) == 'A')) {
            name = name.substring(1);
        }
        
        // 查找最后一个下划线的位置（可能用于旧格式的UUID分隔符）
        // 注意：需要小心处理，因为双下划线代表单下划线，不应该作为分隔符
        // 从后往前查找，找到第一个单独的下划线（前面不是下划线）
        int lastUnderscore = -1;
        for (int i = name.length() - 1; i > 0; i--) {
            if (name.charAt(i) == '_' && name.charAt(i - 1) != '_') {
                // 找到单独的下划线，检查后面是否是8位十六进制（UUID）
                String suffix = name.substring(i + 1);
                if (suffix.length() == 8 && suffix.matches("[0-9a-fA-F]{8}")) {
                    // 确认是旧格式的UUID，使用这个位置作为分隔符
                    lastUnderscore = i;
                    break;
                }
            }
        }
        
        String playerNamePart;
        if (lastUnderscore > 0) {
            // 旧格式：提取UUID之前的部分
            playerNamePart = name.substring(0, lastUnderscore);
        } else {
            // 新格式：直接使用整个名称（无UUID）
            playerNamePart = name;
        }
        
        if (playerNamePart.isEmpty()) {
            return null;
        }
        
        // 恢复原始玩家名（反向转换）
        return restorePlayerName(playerNamePart);
    }
    
    /**
     * 从文件名中提取模型类型
     * 文件名格式：[S|A]<玩家名>_<UUID短版本>.png
     * 
     * @param fileName 文件名（可能包含路径）
     * @return true表示Alex模型（细手臂），false表示Steve模型（粗手臂）
     */
    public static boolean extractModelTypeFromFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false; // 默认Steve模型
        }
        
        // 移除路径，只保留文件名
        String name = fileName;
        int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < name.length() - 1) {
            name = name.substring(lastSlash + 1);
        }
        
        // 移除扩展名
        if (name.endsWith(".png")) {
            name = name.substring(0, name.length() - 4);
        }
        
        // 检查开头的模型类型标识符
        if (name.length() > 0 && name.charAt(0) == 'A') {
            return true; // Alex模型（细手臂）
        }
        
        return false; // 默认Steve模型（粗手臂）
    }
}
