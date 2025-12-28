# Player Doll Addon (玩家玩偶附属模组)

## 📖 简介

Player Doll Addon 是一个功能丰富的玩家玩偶模组，支持创建和放置自定义玩家玩偶实体。模组提供了 Steve 和 Alex 的默认玩偶，并支持从游戏目录动态加载自定义皮肤材质玩偶。此外，模组还提供了强大的姿态（Poses）和动作（Actions）系统，让您可以为玩偶设置各种静态姿势和动态动画。

## ✨ 功能特性

### 核心功能
- **默认玩偶**：提供 Steve（粗手臂）和 Alex（细手臂）的默认玩偶
- **动态加载**：从游戏目录自动扫描并注册自定义皮肤材质玩偶
- **右键放置**：使用玩偶物品右键方块可以在指定位置放置玩偶实体
- **姿态系统**：支持自定义静态姿势配置（如站立、坐下、趴下等）
- **动作系统**：支持创建动态动画序列（如挥手、跳舞等），支持循环播放和关键帧插值
- **模型类型**：支持粗手臂（Steve）和细手臂（Alex）两种模型类型

## 📋 安装要求

- **Minecraft 版本**: 1.21.1
- **NeoForge 版本**: 21.1.217 或更高版本
- **Java 版本**: 21 或更高版本

### 安装步骤

1. 确保已安装 NeoForge 21.1.217 或更高版本
2. 将模组文件（`.jar`）放入 `mods` 文件夹
3. 启动游戏，模组会自动创建必要的配置目录

---

## 🎮 玩家指南

### 基础使用

1. 进入创造模式，打开物品栏
2. 在"玩家玩偶"标签页中找到玩偶物品
3. 手持玩偶物品，右键点击方块即可在方块旁边放置对应的玩偶实体

### 添加自定义皮肤玩偶

模组支持从游戏目录自动加载自定义皮肤材质，无需修改代码即可添加新的玩偶。

#### 📁 目录位置

自定义皮肤文件应放在以下目录：
```
游戏根目录/
└── player_doll/
    └── png/              # 自定义皮肤材质目录（首次启动自动创建）
        ├── SMy_Character.png
        └── A123_ABC__qwe.png
```

#### 📝 文件命名规则

- 文件名必须以 `S` 或 `A` 开头，表示模型类型：
  - `S` = 粗手臂模型（Steve 模型）
  - `A` = 细手臂模型（Alex 模型）
- 第一个字符后的部分将作为玩偶的显示名称
- 名称处理规则：
  - 单个下划线 `_` 会被替换为空格
  - 双下划线 `__` 会被替换为单个下划线 `_`

#### 📊 命名示例

| 文件名 | 模型类型 | 显示名称 | 说明 |
|--------|---------|---------|------|
| `SMy_Character.png` | 粗手臂 | `My Character` | 单下划线变为空格 |
| `A123_ABC__qwe.png` | 细手臂 | `123 ABC_qwe` | 单下划线变空格，双下划线变单下划线 |
| `SHero_Doll.png` | 粗手臂 | `Hero Doll` | 单下划线变为空格 |
| `AVillager_Guard.png` | 细手臂 | `Villager Guard` | 单下划线变为空格 |

#### 🔄 使用流程

1. 将皮肤材质 PNG 文件放入 `player_doll/png/` 目录
2. 启动游戏或按 `F3+T` 重新加载资源包
3. 模组会自动扫描目录并注册所有符合规则的 PNG 文件
4. 在创造模式物品栏的"玩家玩偶"标签页中找到新注册的玩偶物品
5. 使用玩偶物品可以放置对应的玩偶实体

#### ⚠️ 注意事项

- 文件名首字符必须是 `S` 或 `A`，否则文件会被忽略
- 文件名长度至少为 2 个字符（首字符 + 至少 1 个字符）
- 只支持 PNG 格式的图片文件
- 皮肤材质文件应遵循 Minecraft 标准皮肤格式（64x64 或 64x32 像素）
- 如果文件名不符合规则，该文件会被跳过并记录警告日志
- 模组启动时会自动扫描目录

### 配置玩偶姿态和动作

模组支持通过 JSON 配置文件自定义玩偶的姿态和动作。配置文件位于 `player_doll/` 目录下。

#### 📂 配置文件目录结构

```
游戏根目录/
└── player_doll/
    ├── png/              # 自定义皮肤材质目录
    ├── poses/            # 姿态配置文件目录
    │   ├── standing.json
    │   ├── sitting.json
    │   └── ...
    ├── actions/          # 动作配置文件目录
    │   ├── wave.json
    │   ├── dance.json
    │   └── ...
    └── README.md         # 详细配置说明文档
```

**详细配置说明请查看 `player_doll/README.md` 文件**，其中包含：
- 姿态文件的格式和字段说明
- 动作文件的格式和关键帧配置
- 角度参考值和常用姿势示例
- 完整的使用教程和示例

#### 🔄 重新加载配置

修改配置文件后，按 `F3+T` 重新加载资源包即可应用更改，无需重启游戏。

### 配置选项

模组配置文件位于 `config/player_doll_addon-common.toml`：

- **testMode**（测试模式）：
  - 类型：布尔值
  - 默认值：`true`
  - 说明：启用后会输出详细的调试日志，帮助分析皮肤加载和模型识别问题

---

## 👨‍💻 开发者指南

### 技术信息

- **模组ID**: `player_doll_addon`
- **模组名称**: Player Doll Addon
- **Minecraft 版本**: 1.21.1
- **NeoForge 版本**: 21.1.217
- **Java 版本**: 21

### 项目结构

```
player_doll_addon/
├── src/main/java/com/lanye/dolladdon/
│   ├── api/              # API 接口
│   │   ├── action/       # 动作系统 API
│   │   └── pose/         # 姿态系统 API
│   ├── base/             # 基础类
│   │   ├── entity/       # 实体基类
│   │   ├── item/         # 物品基类
│   │   └── render/       # 渲染器基类
│   ├── dynamic/          # 动态加载系统
│   ├── impl/             # 默认实现（Steve/Alex）
│   ├── init/             # 初始化类
│   └── util/             # 工具类
│       ├── DynamicDollLoader.java      # 动态玩偶加载器
│       ├── PoseLoader.java             # 姿态加载器
│       ├── ActionLoader.java           # 动作加载器
│       ├── DynamicTextureManager.java  # 动态纹理管理器
│       └── ...
└── ...
```

### 核心系统

#### 动态加载系统

- **目录扫描**：模组启动时自动扫描 `player_doll/png/` 目录
- **实体注册**：自动解析文件名并动态注册实体类型和物品
- **资源管理**：使用自定义资源管理器处理外部纹理文件
- **模型生成**：动态生成物品模型和实体模型

#### 姿态系统

- **文件格式**：JSON 格式，存储在 `player_doll/poses/` 目录
- **加载机制**：启动时自动加载所有姿态配置文件
- **API 支持**：提供 `DollPose` 接口和 `SimpleDollPose` 实现

#### 动作系统

- **文件格式**：JSON 格式，存储在 `player_doll/actions/` 目录
- **关键帧插值**：自动在关键帧之间进行线性插值
- **循环支持**：支持循环播放和非循环播放
- **API 支持**：提供 `DollAction` 接口和 `SimpleDollAction` 实现

### API 使用示例

#### 创建自定义姿态

```java
DollPose customPose = new SimpleDollPose(
    "custom",
    new float[]{10, 0, 0},  // head
    new float[]{10, 0, 0},  // hat
    new float[]{0, 0, 0},   // body
    new float[]{-45, 0, 0}, // rightArm
    new float[]{-45, 0, 0}, // leftArm
    new float[]{0, 0, 0},   // rightLeg
    new float[]{0, 0, 0}    // leftLeg
);
```

#### 创建自定义动作

```java
List<ActionKeyframe> keyframes = new ArrayList<>();
keyframes.add(new ActionKeyframe(0, customPose));
keyframes.add(new ActionKeyframe(10, anotherPose));

DollAction customAction = new SimpleDollAction(
    "custom_action",
    true,  // looping
    keyframes
);
```

### 扩展开发

模组采用模块化设计，方便扩展：

1. **继承基础类**：继承 `BaseDollEntity` 和 `BaseDollItem` 创建自定义玩偶
2. **实现接口**：实现 `DollPose` 和 `DollAction` 接口创建自定义姿态和动作
3. **注册系统**：使用 NeoForge 的注册系统注册自定义实体和物品

---

## 📁 完整目录结构

```
游戏根目录/
├── mods/                              # 模组文件夹
│   └── player_doll_addon-*.jar
├── config/                            # 配置文件目录
│   └── player_doll_addon-common.toml # 模组配置
└── player_doll/                       # 玩偶资源目录（自动创建）
    ├── png/                           # 自定义皮肤材质目录
    │   ├── SMy_Character.png
    │   ├── A123_ABC__qwe.png
    │   └── ...
    ├── poses/                         # 姿态配置文件目录
    │   ├── standing.json
    │   ├── sitting.json
    │   ├── lying.json
    │   └── ...
    ├── actions/                       # 动作配置文件目录
    │   ├── wave.json
    │   ├── dance.json
    │   ├── sit.json
    │   └── ...
    └── README.md                      # 详细配置说明文档
```

---

## 🗺️ 开发路线图

### 已完成 ✅
- [x] 实现玩家模型渲染（替换方块显示）
- [x] 支持自定义玩家皮肤（通过动态加载）
- [x] 姿态系统（静态姿势配置）
- [x] 动作系统（动态动画序列）
- [x] 关键帧插值系统

### 计划中 🚧
- [ ] 支持查看其他玩家的玩偶
- [ ] 添加更多自定义选项（大小、颜色等）
- [ ] 支持子目录组织皮肤文件
- [ ] 支持热重载（无需重启游戏，按 F3+T 重新加载）
- [ ] GUI 编辑器（可视化编辑姿态和动作）
- [ ] 支持动画导出和导入
- [ ] 多玩家同步（服务器端支持）

---

## 🐛 常见问题

### Q: 自定义皮肤没有显示？
A: 请检查：
1. 文件名是否以 `S` 或 `A` 开头
2. 文件是否为有效的 PNG 格式
3. 文件是否放在 `player_doll/png/` 目录下
4. 是否按 `F3+T` 重新加载了资源包
5. 查看日志文件是否有错误信息

### Q: 如何修改玩偶的姿势？
A: 编辑 `player_doll/poses/` 目录下的 JSON 文件，或创建新的姿态文件。详细说明请查看 `player_doll/README.md`。

### Q: 如何创建自定义动作？
A: 在 `player_doll/actions/` 目录下创建 JSON 文件，定义关键帧序列。详细说明请查看 `player_doll/README.md`。

### Q: 模组崩溃怎么办？
A: 
1. 检查 Minecraft、NeoForge 和 Java 版本是否匹配
2. 查看 `logs/` 目录下的错误日志
3. 启用测试模式（`testMode = true`）查看详细日志
4. 检查配置文件格式是否正确

---

## 📄 许可证

All Rights Reserved

---

## 🔗 相关资源

- [NeoForge 官方文档](https://docs.neoforged.net/)
- [NeoForge Discord 社区](https://discord.neoforged.net/)
- [Minecraft Wiki](https://minecraft.wiki/)

---

## 💬 反馈与支持

如果您在使用过程中遇到问题或有建议，请：
1. 查看 `player_doll/README.md` 获取详细配置说明
2. 查看日志文件了解错误信息
3. 在项目仓库提交 Issue

---

*最后更新时间：2025-12-28*