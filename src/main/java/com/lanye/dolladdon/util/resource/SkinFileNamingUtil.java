package com.lanye.dolladdon.util.resource;

/**
 * 皮肤文件命名工具类
 * 根据玩家名和模型类型生成符合命名规则的文件名
 */
public class SkinFileNamingUtil {
    
    /**
     * 生成文件名
     * 格式：[S|A]<玩家名>_<UUID短版本>.png
     * - S = 粗手臂（Steve模型）
     * - A = 细手臂（Alex模型）
     * - UUID短版本：UUID的前8位（不含连字符），用于避免重名
     * 
     * @param playerName 玩家名称
     * @param isAlexModel 是否为Alex模型（细手臂）
     * @param playerUUID 玩家UUID（用于避免重名）
     * @return 生成的文件名（不含路径）
     */
    public static String generateFileName(String playerName, boolean isAlexModel, java.util.UUID playerUUID) {
        // 清理玩家名中的特殊字符
        String sanitizedName = sanitizePlayerName(playerName);
        
        // 获取UUID的短版本（前8位，不含连字符）
        String uuidShort = getUuidShort(playerUUID);
        
        // 生成文件名：模型类型标识符 + 清理后的玩家名 + _ + UUID短版本 + .png
        String prefix = isAlexModel ? "A" : "S";
        return prefix + sanitizedName + "_" + uuidShort + ".png";
    }
    
    /**
     * 生成文件名（向后兼容，不包含UUID）
     * 格式：[S|A]<玩家名>.png
     * 注意：此方法可能产生重名，建议使用包含UUID的版本
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
     * 获取UUID的短版本（前8位，不含连字符）
     * 用于文件名中避免重名
     * 
     * @param uuid 玩家UUID
     * @return UUID的短版本（8位十六进制字符串）
     */
    private static String getUuidShort(java.util.UUID uuid) {
        if (uuid == null) {
            return "00000000";
        }
        // 获取UUID的字符串表示，去掉连字符，取前8位
        String uuidString = uuid.toString().replace("-", "");
        return uuidString.substring(0, Math.min(8, uuidString.length()));
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
    
    /**
     * 从文件名中提取玩家名（忽略UUID部分）
     * 文件名格式：[S|A]<玩家名>_<UUID短版本>.png
     * 
     * @param fileName 文件名（可能包含路径）
     * @return 提取的玩家名，如果无法提取则返回null
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
        
        // 查找最后一个下划线的位置（玩家名和UUID之间的分隔符）
        int lastUnderscore = name.lastIndexOf('_');
        if (lastUnderscore > 0) {
            // 提取下划线之前的部分作为玩家名
            String playerName = name.substring(0, lastUnderscore);
            // 验证下划线之后的部分是否是UUID（8位十六进制）
            String uuidPart = name.substring(lastUnderscore + 1);
            if (uuidPart.length() == 8 && uuidPart.matches("[0-9a-fA-F]{8}")) {
                // 确认是UUID格式，返回玩家名
                return playerName.isEmpty() ? null : playerName;
            }
        }
        
        // 如果没有找到UUID部分，返回整个名称（向后兼容旧格式）
        return name.isEmpty() ? null : name;
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
