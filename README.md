# JetBrains Auto Commit 插件

使用 AI 自动生成 Git commit 消息的 IntelliJ IDEA 插件，支持 OpenAI 兼容的 API（如 DeepSeek）。

## 功能特性

- 🤖 手动生成 commit 消息，支持暂存区的变更分析
- 🔧 支持自定义提示词模板
- 🌐 支持任何 OpenAI 兼容的 API 端点
- 💡 默认使用 DeepSeek API（经济实惠）
- 🎨 集成到 JetBrains Git 界面，使用简单
- ⚙️ 灵活的配置选项（模型、温度、最大 token 数等）
- ⌨️ 快捷键支持（Ctrl+Shift+M）
- 📋 自动复制到剪贴板

## 安装

### 从源码构建

1. 克隆仓库：
   ```bash
   git clone https://github.com/meimingqi222/jetbrains-auto-commit.git
   cd jetbrains-auto-commit
   ```

2. 使用 Gradle Wrapper 构建插件（无需预装 Gradle）：
   ```bash
   .\gradlew.bat build
   ```

3. 在 IntelliJ IDEA 中安装：
   - 打开 `File` -> `Settings` -> `Plugins`
   - 点击齿轮图标，选择 `Install Plugin from Disk...`
   - 选择构建生成的插件文件（位于 `build/distributions/jetbrains-auto-commit-1.0.0.zip`）

### 直接安装

从 [Releases](https://github.com/meimingqi222/jetbrains-auto-commit/releases) 下载最新版本的插件包，然后按照上述步骤 3 安装。

## 配置

安装插件后，需要在设置中配置 API：

1. 打开 `File` -> `Settings` -> `Tools` -> `AI Commit Message`
2. 配置以下选项：
   - **API Key**: OpenAI 兼容的 API 密钥（建议使用 DeepSeek API Key）
   - **API Endpoint**: API 基地址 URL（默认：`https://api.deepseek.com`）
   - **Model**: 模型名称（默认：`deepseek-chat`）
   - **Language**: Commit 消息语言（支持 auto, zh-CN, en, ja, ko, es, fr, de, ru）
   - **Max Tokens**: 最大 token 数量（默认：500）
   - **Temperature**: 温度参数 0-1（默认：0.3）
   - **Prompt**: 提示词模板（使用 `{diff}` 和 `{language}` 作为占位符）

## 使用方法

### 生成 Commit 消息

1. 在 IntelliJ IDEA 中打开 Git 窗口（`Alt+0` 或点击 Git 工具窗口）
2. 暂存要提交的文件
3. 点击工具栏的 "Generate Commit Message with AI" 按钮，或使用快捷键 `Ctrl+Shift+M`
4. 插件会自动分析暂存区的变更并生成 commit 消息
5. 生成的消息会**自动填充到 commit message 输入框**，光标定位到末尾

### 与 VSCode 版本的对比

| 功能 | VSCode | JetBrains | 状态 |
|------|--------|-----------|------|
| 手动触发生成 | ✅ | ✅ | ✅ |
| API 配置 | ✅ | ✅ | ✅ |
| 自定义提示词 | ✅ | ✅ | ✅ |
| 多语言支持 | ✅ | ✅ | ✅ |
| 快捷键触发 | ✅ | ✅ | ✅ |
| 安全存储 API Key | ✅ | ✅ | ✅ |
| 后台任务 | ✅ | ✅ | ✅ |
| **自动填充消息框** | ✅ | ✅ | ✅ 完全对齐 |

**功能对齐**：JetBrains 版本已完全实现对齐，支持自动填充到 commit message 输入框！

## 配置选项

| 选项 | 说明 | 默认值 |
|------|------|--------|
| API Key | OpenAI 兼容的 API 密钥 | - |
| API Endpoint | API 基地址 URL | https://api.deepseek.com |
| Model | 模型名称 | deepseek-chat |
| Language | Commit 消息语言 | auto |
| Max Tokens | 最大 token 数量 | 500 |
| Temperature | 温度参数 | 0.3 |
| Prompt | 提示词模板 | 默认模板 |

## 提示词模板

默认提示词模板遵循 Conventional Commits 规范：

```
Generate a commit message following Conventional Commits specification.

IMPORTANT: You MUST write the commit message in {language}. All text including type, scope, and description must be in {language}.

Format: <type>(<scope>): <description>

[optional body with bullet points]

Types:
- feat: New feature
- fix: Bug fix
- docs: Documentation
- style: Code style
- refactor: Code refactoring
- perf: Performance
- test: Testing
- chore: Build/tooling

Rules:
1. scope is optional
2. description: concise, under 50 chars, no period
3. For multiple changes, add body with "- " prefix per line

Git diff:
```
{diff}
```

Output language: {language}
Return ONLY the commit message in {language}, no explanation.
```

## 支持的语言

- auto（自动检测）
- zh-CN（简体中文）
- en（英语）
- ja（日语）
- ko（韩语）
- es（西班牙语）
- fr（法语）
- de（德语）
- ru（俄语）

## 技术栈

- **语言**: Kotlin
- **构建工具**: Gradle (Kotlin DSL)
- **HTTP 客户端**: Ktor Client
- **平台**: IntelliJ Platform 2023.2+

## 开发

### 构建项目

```bash
./gradlew build
```

### 运行测试

```bash
./gradlew test
```

### 在开发环境中运行插件

```bash
./gradlew runIde
```

## 兼容性

- **IntelliJ IDEA**: 2023.2 - 2025.1
- **其他 JetBrains IDE**: PyCharm, WebStorm, CLion, GoLand 等（支持 Git 功能的产品）
- **JDK**: Java 17+
- **操作系统**: Windows, macOS, Linux

## 安全性

- API Key 使用 IntelliJ 的 PasswordSafe 加密存储
- 不会将任何代码或敏感信息发送到第三方（除了配置的 API 端点）
- 支持自定义 API 端点，可以使用私有部署的模型

## 许可证

MIT License

## 致谢

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [Ktor Client](https://ktor.io/docs/client.html)
- [DeepSeek](https://www.deepseek.com/)

## 相关项目

- [VSCode Auto Commit](https://github.com/meimingqi222/vscode-auto-commit) - VSCode 版本的插件

## 反馈与贡献

欢迎提交 Issue 和 Pull Request！
