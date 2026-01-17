# Modrinth 发布指南

本指南将帮助您将 Player Doll Addon 模组发布到 Modrinth 平台。

## 🚀 快速开始

如果您已经熟悉流程，可以按照以下步骤快速发布：

1. **创建 Modrinth 项目**（如果还没有）
   - 访问 https://modrinth.com/ 创建项目
   - 记录项目 ID（在项目 URL 中可以看到）

2. **获取 API Token**
   - 访问 https://modrinth.com/settings → API Tokens
   - 创建新令牌，权限选择 `CREATE_VERSION`
   - 复制令牌

3. **配置环境变量**
   ```powershell
   $env:MODRINTH_TOKEN = "your_token_here"
   ```

4. **修改 build.gradle**
   - 找到 `modrinth` 块
   - 将 `projectId` 改为您的项目 ID

5. **发布**
   ```powershell
   .\gradlew.bat build
   .\gradlew.bat modrinth
   ```

---

## 📋 前置准备

### 1. 创建 Modrinth 账户和项目

1. 访问 [Modrinth](https://modrinth.com/)
2. 注册/登录账户
3. 点击右上角头像 → **Create project**（创建项目）
4. 填写项目信息：
   - **Project ID/Slug**: `player-doll-addon`（或您想要的唯一标识符）
   - **Project Name**: `Player Doll Addon`
   - **Summary**: 简短描述
   - **Description**: 详细描述（支持 Markdown）
   - **Categories**: 选择相关分类（如 `Library`、`Utility` 等）
   - **Client Side**: 选择 `Required`、`Optional` 或 `Unsupported`
   - **Server Side**: 选择 `Required`、`Optional` 或 `Unsupported`
   - **License**: 选择许可证类型

### 2. 获取 API Token

1. 访问 [Modrinth 用户设置](https://modrinth.com/settings)
2. 进入 **API Tokens** 标签页
3. 点击 **Create Token**（创建令牌）
4. 设置令牌名称（如 `Gradle Publish`）
5. 选择权限范围：
   - ✅ `CREATE_VERSION`（必须，用于创建版本）
   - ✅ `PROJECT_WRITE`（可选，用于同步项目描述）
6. 点击 **Create** 创建令牌
7. **重要**: 复制并保存令牌，它只会显示一次！

### 3. 配置环境变量（推荐方式）

#### Windows PowerShell
```powershell
# 临时设置（当前会话有效）
$env:MODRINTH_TOKEN = "your_token_here"

# 永久设置（用户级别）
[System.Environment]::SetEnvironmentVariable("MODRINTH_TOKEN", "your_token_here", "User")
```

#### Windows CMD
```cmd
# 临时设置
set MODRINTH_TOKEN=your_token_here

# 永久设置（用户级别）
setx MODRINTH_TOKEN "your_token_here"
```

#### 或在 gradle.properties 中配置（不推荐，但可用）
在项目根目录的 `gradle.properties` 文件中添加：
```properties
modrinth_token=your_token_here
```

**注意**: 如果使用 `gradle.properties`，请确保将其添加到 `.gitignore` 中，避免泄露令牌。

## 🛠️ 配置 Gradle 插件

项目已配置 **Minotaur** 插件（Modrinth 官方 Gradle 插件），配置位于 `build.gradle` 中。

### 配置说明

在 `build.gradle` 中的 `modrinth` 块包含以下配置：

```groovy
modrinth {
    token = System.getenv("MODRINTH_TOKEN") ?: project.findProperty("modrinth_token")
    projectId = "your-project-id"  // 您的 Modrinth 项目 ID
    versionNumber = "${mod_version}"
    versionType = "release"  // 或 "beta"、"alpha"
    uploadFile = jar  // 上传构建的 JAR 文件
    gameVersions = ["1.21.1"]
    loaders = ["neo_forge"]
    changelog = "更新日志内容"
}
```

### 需要修改的配置项

1. **projectId**: 替换为您的 Modrinth 项目 ID（在项目设置页面可以看到）
2. **versionType**: 
   - `release`: 正式发布版本
   - `beta`: 测试版本
   - `alpha`: 开发版本
3. **changelog**: 更新日志内容（支持 Markdown）

## 📦 发布流程

### 步骤 1: 构建模组

确保项目已正确构建：

```bash
./gradlew build
```

或使用 PowerShell：
```powershell
.\gradlew.bat build
```

构建完成后，JAR 文件将位于 `build/libs/` 目录。

### 步骤 2: 准备更新日志

在发布前，准备本次版本的更新日志。可以：
- 直接在 `build.gradle` 的 `changelog` 字段中填写
- 或从文件读取（见下方高级配置）

### 步骤 3: 发布到 Modrinth

运行发布任务：

```bash
./gradlew modrinth
```

或使用 PowerShell：
```powershell
.\gradlew.bat modrinth
```

### 步骤 4: 验证发布

1. 访问您的 Modrinth 项目页面
2. 检查新版本是否已创建
3. 验证版本信息是否正确
4. 测试下载和安装

## 🔧 高级配置

### 从文件读取更新日志

如果更新日志较长，可以从文件读取：

```groovy
modrinth {
    // ... 其他配置
    changelog = file("CHANGELOG.md").text
}
```

### 同步项目描述

使用 `modrinthSyncBody` 任务同步项目描述：

```bash
./gradlew modrinthSyncBody
```

这会将 `README.md` 的内容同步到 Modrinth 项目描述。

### 配置依赖关系

如果您的模组依赖其他模组，可以在 `modrinth` 块中配置：

```groovy
modrinth {
    // ... 其他配置
    dependencies {
        required.project "fabric-api"  // 必需依赖
        optional.project "modmenu"     // 可选依赖
    }
}
```

### 多文件上传

如果需要上传多个文件（如主 JAR 和源代码 JAR）：

```groovy
modrinth {
    // ... 其他配置
    uploadFile = jar
    additionalFiles = [sourcesJar]  // 如果有 sourcesJar 任务
}
```

## ⚠️ 常见问题

### 1. 令牌未找到

**错误**: `MODRINTH_TOKEN environment variable is not set`

**解决方案**:
- 确保已设置环境变量 `MODRINTH_TOKEN`
- 或在 `gradle.properties` 中设置 `modrinth_token`
- 重启 IDE 或终端以确保环境变量生效

### 2. 项目 ID 错误

**错误**: `Project not found`

**解决方案**:
- 检查 `projectId` 是否正确
- 确保项目在 Modrinth 上已创建
- 检查项目 ID 是否与 Modrinth 项目页面 URL 中的 ID 一致

### 3. 版本已存在

**错误**: `Version already exists`

**解决方案**:
- 修改 `mod_version` 在 `gradle.properties` 中
- 或删除 Modrinth 上的旧版本

### 4. 游戏版本不支持

**错误**: `Game version not supported`

**解决方案**:
- 检查 `gameVersions` 中的版本是否正确
- 确保 Modrinth 支持该 Minecraft 版本
- NeoForge 版本格式应为 `1.21.1`，不是 `1.21.1-neoforge`

### 5. 加载器标识错误

**错误**: `Loader not supported`

**解决方案**:
- 确保 `loaders` 数组包含 `"neo_forge"`（注意下划线）
- 不要使用 `"neoforge"` 或 `"NeoForge"`

## 📝 发布检查清单

发布前请确认：

- [ ] Modrinth 项目已创建并配置完成
- [ ] API Token 已获取并配置
- [ ] `build.gradle` 中的 `projectId` 已正确设置
- [ ] `mod_version` 已更新为新的版本号
- [ ] 更新日志已准备
- [ ] 模组已成功构建（`./gradlew build`）
- [ ] 已测试模组在游戏中正常运行
- [ ] 已检查所有依赖关系
- [ ] 已阅读并遵守 Modrinth 的使用条款

## 🔗 相关链接

- [Modrinth 官网](https://modrinth.com/)
- [Minotaur 插件文档](https://github.com/modrinth/minotaur)
- [Modrinth API 文档](https://docs.modrinth.com/)
- [NeoForge 文档](https://docs.neoforged.net/)

## 📞 获取帮助

如果遇到问题：
1. 查看 [Modrinth 帮助中心](https://modrinth.com/help)
2. 访问 [Modrinth Discord](https://discord.gg/modrinth)
3. 查看 [Minotaur GitHub Issues](https://github.com/modrinth/minotaur/issues)

---

**祝您发布顺利！** 🎉
