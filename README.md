# Player Doll Addon (玩家玩偶附属模组)

## 简介

这是 KaleidoscopeDoll 模组的附属模组，可以获取当前玩家的皮肤并创建对应的玩偶。

## 功能特性

- 使用"玩家玩偶创建器"物品可以创建包含当前玩家皮肤的玩偶
- 右键使用：直接创建玩偶物品并放入背包
- 右键方块：在指定位置放置玩家玩偶实体
- 玩偶会保存玩家的UUID和名称信息

## 安装要求

- Minecraft 1.21.1
- NeoForge 21.1.217 或更高版本
- **必须安装 KaleidoscopeDoll 模组**（版本 1.2.3 或更高）

## 使用方法

1. 获得"玩家玩偶创建器"物品
2. 右键使用：
   - 直接右键：创建玩偶物品并放入背包
   - 右键方块：在方块旁边放置玩家玩偶实体

## 开发说明

### 构建项目

```bash
# Windows
gradlew build

# Linux/Mac
./gradlew build
```

### 运行客户端

```bash
# Windows
gradlew runClient

# Linux/Mac
./gradlew runClient
```

### 依赖配置

在 `build.gradle` 中配置 KaleidoscopeDoll 依赖：

```gradle
dependencies {
    // 方式1：如果 KaleidoscopeDoll 已发布到 Maven
    // implementation "com.github.ysbbbbbb:kaleidoscopedoll:1.2.3-neoforge+mc1.21.1"
    
    // 方式2：使用本地文件（将 jar 放在 libs 目录下）
    implementation files("libs/kaleidoscopedoll-1.2.3-neoforge+mc1.21.1.jar")
}
```

## 技术信息

- **模组ID**: `player_doll_addon`
- **模组名称**: Player Doll Addon
- **Minecraft版本**: 1.21.1
- **NeoForge版本**: 21.1.217
- **Java版本**: 21
- **依赖模组**: KaleidoscopeDoll (1.2.3+)

## 未来计划

- [ ] 实现玩家模型渲染（替换方块显示）
- [ ] 支持自定义玩家皮肤
- [ ] 支持查看其他玩家的玩偶
- [ ] 添加更多自定义选项

## 许可证

All Rights Reserved

## 相关资源

- [KaleidoscopeDoll 模组](https://github.com/ysbbbbbb/KaleidoscopeDoll)
- [NeoForge 文档](https://docs.neoforged.net/)
- [NeoForge Discord](https://discord.neoforged.net/)

