# JetBrains Auto Commit 插件开发计划

## 项目概述

开发一个 JetBrains 平台的 IntelliJ IDEA 插件，实现 AI 自动生成 Git commit 消息功能。该插件将使用 Kotlin/Java 实现，功能与现有的 VSCode Auto Commit 插件完全对齐。

**目标：** 在 JetBrains IDE 中提供与 VSCode 插件一致的用户体验和功能特性。

---

## 一、功能特性

### 1.1 核心功能

- 🤖 自动分析暂存区的更改，生成合适的 commit 消息
- 🔧 支持自定义提示词模板
- 🌐 支持任何 OpenAI 兼容的 API 端点
- 💡 默认使用 DeepSeek API（经济实惠）
- 🎨 集成到 JetBrains Commit 界面，使用简单
- ⚙️ 灵活的配置选项（模型、温度、最大 token 数等）

### 1.2 配置选项

- **API Key**: OpenAI 兼容的 API 密钥（安全存储）
- **API Endpoint**: API 基地址 URL（默认：`https://api.deepseek.com`）
- **Model**: 模型名称（默认：`deepseek-chat`）
- **Language**: Commit 消息语言（支持多语言，默认自动检测）
- **Prompt**: 提示词模板（使用 `{diff}` 和 `{language}` 作为占位符）
- **Max Tokens**: 最大 token 数量（默认：500）
- **Temperature**: 温度参数 0-1（默认：0.3）

### 1.3 用户交互

- 在 Commit Tool Window 中点击按钮生成 commit 消息
- 支持快捷键触发（Ctrl+Shift+M）
- 生成后自动填充到 commit message 输入框
- 用户可编辑后手动提交
- 错误时友好提示，不阻塞正常 commit 流程

---

## 二、技术栈

| 维度 | 选择 |
|------|------|
| 语言 | Kotlin（推荐）/ Java |
| 构建工具 | Gradle (Kotlin DSL) |
| 运行时 | JVM |
| HTTP 客户端 | Ktor Client / OkHttp |
| 配置存储 | PersistentStateComponent |
| API Key 存储 | PasswordSafe |
| Git API | IntelliJ VCS/Git API |
| UI 框架 | IntelliJ UI DSL (Kotlin) |
| 平台版本 | IntelliJ Platform 2023.2+ |

---

## 三、开发步骤

### 阶段 1：插件基础搭建（0.5-1 天）

**目标：** 创建 IntelliJ Platform Plugin 项目骨架

**任务：**
1. 使用 IntelliJ Platform Plugin 模板创建项目
2. 配置 Gradle 构建脚本
3. 编写 `plugin.xml` 声明扩展点
4. 验证插件能在 IDEA 中加载

**plugin.xml 关键配置：**
```xml
<idea-plugin>
    <depends>com.intellij.modules.platform</depends>
    <depends>Git4Idea</depends>

    <!-- 扩展点：Commit 流程集成 -->
    <extensions defaultExtensionNs="com.intellij">
        <checkinHandlerFactory implementation="com.example.AICommitCheckinHandlerFactory"/>
    </extensions>

    <!-- 动作：手动触发 -->
    <actions>
        <action id="GenerateCommitMessage"
                class="com.example.GenerateCommitMessageAction"
                text="Generate Commit Message with AI"
                description="Generate commit message using AI">
            <add-to-group group-id="Vcs.MessageActionGroup" anchor="first"/>
            <keyboard-shortcut keymap="$default" first-keystroke="ctrl shift M"/>
        </action>
    </actions>

    <!-- 设置页 -->
    <extensions defaultExtensionNs="com.intellij">
        <projectConfigurable
            instance="com.example.AICommitConfigurable"
            id="AICommit"
            displayName="AI Commit Message"/>
    </extensions>
</idea-plugin>
```

**输出：**
- 可运行的插件骨架项目
- 基本的 plugin.xml 配置

---

### 阶段 2：Git/VCS 集成（1.5-3 天）

**目标：** 获取当前准备提交的变更并转换为 diff 结构

**任务：**
1. 学习 `ChangeListManager` API
2. 实现 `CheckinHandler` 获取 included changes
3. 使用 `DiffContentFactory` / `VcsDiffUtil` 提取 diff 内容
4. 将 JetBrains 的 `Change` 对象转换为 diff 文本
5. 处理多仓库、多模块工程场景
6. 处理多 change list 场景（只处理选中的变更）

**关键实现：**
```kotlin
class AICommitCheckinHandler : CheckinHandler {
    override fun beforeCheckin(
        session: CommitSession,
        editor: CommitSessionEditor
    ): ReturnResult {
        val project = session.project
        val includedChanges = session.includedChanges

        // 检查是否有变更
        if (includedChanges.isEmpty()) {
            return ReturnResult.COMMIT
        }

        // 获取 diff 内容
        val diffContent = extractDiffContent(project, includedChanges)

        // 调用生成器
        val commitMessage = generateCommitMessage(diffContent)

        // 填充 commit message
        editor.setCommitMessage(commitMessage)

        return ReturnResult.COMMIT
    }
}
```

**输出：**
- 可获取并转换 Git diff 的代码
- 多仓库、多 change list 处理逻辑

---

### 阶段 3：AI 调用与核心模块实现（1-2 天）

**目标：** 实现 OpenAI 兼容 API 调用逻辑

**任务：**
1. 选择 HTTP 客户端（Ktor 或 OkHttp）
2. 实现 OpenAI 兼容 API 调用逻辑
3. 实现提示词模板处理（替换 `{diff}` 和 `{language}` 占位符）
4. 实现错误处理和重试策略
5. 实现 token 限制和 diff 截断逻辑
6. 实现语言映射和检测逻辑

**核心实现：**
```kotlin
class CommitMessageGenerator(
    private val config: AICommitConfig,
    private val httpClient: HttpClient
) {
    suspend fun generate(diffContent: String): String {
        // 1. 限制 diff 长度
        val truncatedDiff = limitDiffLength(diffContent)

        // 2. 构造 prompt
        val language = getLanguage()
        val prompt = buildPrompt(truncatedDiff, language)

        // 3. 调用 API
        val response = callOpenAIAPI(prompt)

        return response.trim()
    }

    private suspend fun callOpenAIAPI(prompt: String): String {
        return httpClient.post("${config.apiEndpoint}/v1/chat/completions") {
            headers {
                append("Content-Type", "application/json")
                append("Authorization", "Bearer ${config.apiKey}")
            }
            setBody(
                buildJsonObject {
                    put("model", config.model)
                    put("messages", buildJsonArray {
                        add(buildJsonObject {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                    put("max_tokens", config.maxTokens)
                    put("temperature", config.temperature)
                }
            )
        }.body<OpenAIResponse>().choices[0].message.content
    }

    private fun buildPrompt(diff: String, language: String): String {
        return config.prompt
            .replace("{diff}", diff)
            .replace("{language}", language)
    }
}
```

**语言映射：**
```kotlin
object LanguageMapper {
    private val LANGUAGE_MAP = mapOf(
        "zh-CN" to "Simplified Chinese",
        "zh-cn" to "Simplified Chinese",
        "en" to "English",
        "ja" to "Japanese",
        "ko" to "Korean",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "ru" to "Russian"
    )

    fun getLanguage(configLang: String, systemLocale: String): String {
        if (configLang != "auto") {
            return LANGUAGE_MAP[configLang] ?: "English"
        }
        return LANGUAGE_MAP[systemLocale] ?: "English"
    }
}
```

**输出：**
- 完整的 `CommitMessageGenerator` 实现
- API 调用和错误处理逻辑
- 语言映射和检测逻辑

---

### 阶段 4：UI 集成与配置页（1-2 天）

**目标：** 实现用户界面和配置管理

**任务：**
1. 实现 `AICommitConfigurable` 设置页
2. 使用 `PersistentStateComponent` 存储配置
3. 使用 `PasswordSafe` 存储 API Key
4. 实现 `GenerateCommitMessageAction` 手动触发动作
5. 添加通知和错误提示
6. 优化用户体验（进度提示、可取消等）

**配置存储：**
```kotlin
@Service
@State(
    name = "AICommitSettings",
    storages = [Storage("AICommitSettings.xml")]
)
class AICommitConfig : PersistentStateComponent<AICommitConfig.State> {
    data class State(
        var apiEndpoint: String = "https://api.deepseek.com",
        var model: String = "deepseek-chat",
        var language: String = "auto",
        var maxTokens: Int = 500,
        var temperature: Double = 0.3,
        var prompt: String = DEFAULT_PROMPT
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun getApiKey(): String {
        return PasswordSafe.getInstance().getPassword(
            null,
            AICommitConfig::class.java,
            "API_KEY"
        ) ?: ""
    }

    fun setApiKey(apiKey: String) {
        PasswordSafe.getInstance().setPassword(
            null,
            AICommitConfig::class.java,
            "API_KEY",
            apiKey
        )
    }
}
```

**设置页面：**
```kotlin
class AICommitConfigurable : SearchableConfigurable {
    private lateinit var panel: AICommitConfigPanel
    private val config = service<AICommitConfig>()

    override fun getDisplayName(): String = "AI Commit Message"

    override fun createComponent(): JComponent {
        panel = AICommitConfigPanel()
        return panel.mainPanel
    }

    override fun isModified(): Boolean {
        return panel.apiEndpoint != config.apiEndpoint ||
               panel.model != config.model ||
               panel.language != config.language ||
               panel.maxTokens != config.maxTokens ||
               panel.temperature != config.temperature ||
               panel.prompt != config.prompt ||
               panel.apiKey != config.getApiKey()
    }

    override fun apply() {
        config.apiEndpoint = panel.apiEndpoint
        config.model = panel.model
        config.language = panel.language
        config.maxTokens = panel.maxTokens
        config.temperature = panel.temperature
        config.prompt = panel.prompt
        config.setApiKey(panel.apiKey)
    }

    override fun reset() {
        panel.resetFrom(config)
    }
}
```

**手动触发动作：**
```kotlin
class GenerateCommitMessageAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 在后台任务中执行
        object : Task.Backgroundable(project, "Generating Commit Message...", true) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val result = generateCommitMessage(project)
                    // 在 EDT 中更新 UI
                    ApplicationManager.getApplication().invokeLater {
                        fillCommitMessage(project, result)
                        Notifications.Bus.notify(
                            Notification("AICommit", "Success", "Commit message generated!", NotificationType.INFORMATION)
                        )
                    }
                } catch (e: Exception) {
                    ApplicationManager.getApplication().invokeLater {
                        Notifications.Bus.notify(
                            Notification("AICommit", "Error", e.message ?: "Unknown error", NotificationType.ERROR)
                        )
                    }
                }
            }
        }.queue()
    }
}
```

**输出：**
- 完整的设置页面
- API Key 安全存储
- 手动触发动作
- 通知和错误提示

---

### 阶段 5：性能与错误处理优化（1-2 天）

**目标：** 优化性能，完善错误处理

**任务：**
1. 在后台任务中执行生成逻辑（已实现）
2. 添加可取消能力
3. 对大 diff 做限制与截断
4. 错误时不阻塞 commit，只提示用户
5. 添加日志和调试开关

**性能优化：**
```kotlin
// 限制 diff 长度
private fun limitDiffLength(diff: String): String {
    val maxDiffLength = 8000
    return if (diff.length > maxDiffLength) {
        diff.substring(0, maxDiffLength) + "\n\n...(内容过长，已截断)"
    } else {
        diff
    }
}
```

**错误处理：**
```kotlin
class AICommitCheckinHandler : CheckinHandler {
    override fun beforeCheckin(
        session: CommitSession,
        editor: CommitSessionEditor
    ): ReturnResult {
        return try {
            val result = generateCommitMessage(session)
            editor.setCommitMessage(result)
            ReturnResult.COMMIT
        } catch (e: NetworkException) {
            // 网络错误，不阻塞 commit
            showWarning("AI generation failed due to network error. Please enter commit message manually.")
            ReturnResult.COMMIT
        } catch (e: APIException) {
            // API 错误，不阻塞 commit
            showWarning("AI generation failed: ${e.message}. Please enter commit message manually.")
            ReturnResult.COMMIT
        } catch (e: Exception) {
            // 其他错误，记录日志但不阻塞
            Logger.getInstance(AICommitCheckinHandler::class.java)
                .error("Failed to generate commit message", e)
            ReturnResult.COMMIT
        }
    }
}
```

**输出：**
- 性能优化完成
- 完善的错误处理
- 日志和调试支持

---

### 阶段 6：系统测试与迭代（1-3 天）

**目标：** 全面测试，修复问题

**测试场景：**
1. 单文件小变更：是否能生成合理的 commit message
2. 多文件、多类型变更：是否整合信息得当
3. 无暂存变更：是否有友好提示，不触发生成
4. 大 diff 场景：是否会卡顿或超时，截断策略是否生效
5. 多仓库工程：是否正确处理
6. 多 change list 场景：是否只处理选中的变更
7. 网络错误场景：是否有友好提示
8. API 失败场景：是否不阻塞 commit

**配置与安全测试：**
- 更改 API base URL / 模型名后是否立即生效
- API key 的保存/读取是否正常
- 重启 IDE 后配置保留
- 代理/SSL 场景下的网络行为

**输出：**
- 测试报告
- 修复的问题列表
- 性能测试结果

---

## 四、工作量评估

以一个熟悉 IntelliJ 平台的中高级 Kotlin 工程师为基准：

| 阶段 | 工作量 | 说明 |
|------|--------|------|
| 阶段 1：插件基础搭建 | 0.5-1 天 | Gradle + plugin.xml |
| 阶段 2：Git/VCS 集成 | 1.5-3 天 | 学习 IntelliJ VCS API |
| 阶段 3：核心模块实现 | 1-2 天 | HTTP 调用 + 业务逻辑 |
| 阶段 4：UI 集成与配置 | 1-2 天 | 设置页 + Action + 通知 |
| 阶段 5：性能与错误处理 | 1-2 天 | 优化 + 完善错误处理 |
| 阶段 6：系统测试 | 1-3 天 | 全面测试 + 修复 |
| **总计（熟手）** | **6-10 天** | |
| **总计（新手）** | **10-15 天** | 含学习时间 |

---

## 五、主要风险点与应对

### 5.1 IntelliJ VCS API 复杂度

**风险：**
- 如何准确获取"用户当前准备提交的变更集"
- 多仓库、多模块工程的路径与仓库映射
- 多 change list 场景处理

**应对：**
- 提前学习 `ChangeListManager`、`CheckinHandler` API
- 参考 JetBrains 官方示例和开源插件
- 充分测试多仓库、多 change list 场景

### 5.2 性能与体验

**风险：**
- 大仓库、大 diff 时的性能问题
- API 调用延迟影响用户提交节奏

**应对：**
- 在后台任务中执行（`Task.Backgroundable`）
- 提供可取消能力
- 对大 diff 做截断处理
- 建议用户提前点击按钮生成 message

### 5.3 API 调用稳定性与安全性

**风险：**
- API key 存储安全
- 代理、公司网络、证书问题

**应对：**
- 使用 `PasswordSafe` 存储 API Key
- 支持代理配置
- 添加详细的错误提示

### 5.4 多平台兼容

**风险：**
- 需要兼容多个 IDE 版本与产品（IDEA、WebStorm 等）
- 新版本 SDK API 的兼容性

**应对：**
- 控制最低 platformVersion
- 避免使用新版本特有 API
- 做好版本条件处理

---

## 六、验证与测试建议

### 6.1 功能测试清单

- [ ] 单文件小变更生成 commit message
- [ ] 多文件变更整合信息
- [ ] 无暂存变更时的友好提示
- [ ] 大 diff 场景下的截断策略
- [ ] 多仓库工程的正确处理
- [ ] 多 change list 场景的正确处理
- [ ] 网络错误时的友好提示
- [ ] API 失败时不阻塞 commit

### 6.2 配置与安全测试

- [ ] API base URL / 模型名更改后立即生效
- [ ] API key 的保存/读取正常
- [ ] 重启 IDE 后配置保留
- [ ] 代理/SSL 场景下的网络行为

### 6.3 性能与稳定性测试

- [ ] 大项目上多次生成，观察 IDE 响应
- [ ] 网络抖动或 API 失败时不崩溃
- [ ] 后台任务可正常取消
- [ ] 内存泄漏检查

---

## 七、下一步行动

### 立即开始：

1. **阶段 1：插件基础搭建**
   - 创建插件项目
   - 配置 build.gradle.kts
   - 编写 plugin.xml

2. **阶段 2：Git/VCS 集成**
   - 学习 IntelliJ VCS API
   - 实现变更获取逻辑

### 学习资源：

- [IntelliJ Platform SDK Documentation](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- [VCS API Examples](https://github.com/JetBrains/intellij-sdk-code-samples)

### 参考项目：

- VSCode 插件：`H:\Codes\vscode-auto-commit`
- JetBrains 插件位置：`H:\Codes\jetbrains-auto-commit`

---

## 八、附录

### A. VSCode 插件核心文件（参考）

- `src/extension.ts` - 扩展主入口
- `src/commitGenerator.ts` - Commit 消息生成逻辑
- `src/apiClient.ts` - API 调用客户端
- `package.json` - 扩展配置

### B. JetBrains 插件项目结构（建议）

```
jetbrains-auto-commit/
├── build.gradle.kts
├── gradle.properties
├── src/
│   └── main/
│       ├── resources/
│       │   ├── META-INF/
│       │   │   └── plugin.xml
│       │   └── messages/
│       │       └── AICommitBundle.properties
│       └── kotlin/
│           └── com/
│               └── example/
│                   ├── aicommit/
│                   │   ├── AICommitConfig.kt
│                   │   ├── AICommitConfigurable.kt
│                   │   ├── CommitMessageGenerator.kt
│                   │   ├── OpenAIResponse.kt
│                   │   └── LanguageMapper.kt
│                   ├── vcs/
│                   │   ├── AICommitCheckinHandler.kt
│                   │   └── AICommitCheckinHandlerFactory.kt
│                   ├── action/
│                   │   └── GenerateCommitMessageAction.kt
│                   └── util/
│                       ├── DiffExtractor.kt
│                       └── PromptBuilder.kt
└── docs/
    └── DEVELOPMENT_PLAN.md
```

### C. 关键 API 文档链接

- [CheckinHandler](https://plugins.jetbrains.com/docs/intellij/vcs.html#commit-process)
- [ChangeListManager](https://plugins.jetbrains.com/docs/intellij/vcs.html#working-with-changelists)
- [PersistentStateComponent](https://plugins.jetbrains.com/docs/intellij/persisting-state-of-components.html)
- [PasswordSafe](https://plugins.jetbrains.com/docs/intellij/persisting-sensitive-data.html)
- [Notifications](https://plugins.jetbrains.com/docs/intellij/notifications.html)
- [Task.Backgroundable](https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html#background-tasks)

### D. 默认提示词模板

```text
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

---

**文档版本：** 2.0
**创建日期：** 2026-01-07
**最后更新：** 2026-01-07
