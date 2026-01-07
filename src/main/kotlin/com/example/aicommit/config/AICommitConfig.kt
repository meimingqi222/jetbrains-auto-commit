package com.example.aicommit.config

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

/**
 * AI Commit 配置状态
 */
@State(
    name = "AICommitSettings",
    storages = [Storage("AICommitSettings.xml")]
)
@Service(Service.Level.PROJECT)
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

    companion object {
        private val CREDENTIAL_ATTRIBUTES = CredentialAttributes(
            "com.example.aicommit",
            AICommitConfig::class.java.name
        )

        const val DEFAULT_PROMPT = """Generate a commit message following Conventional Commits specification.

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
Return ONLY the commit message in {language}, no explanation."""
    }

    /**
     * 获取 API Key（从 PasswordSafe 中安全读取）
     */
    fun getApiKey(): String {
        return PasswordSafe.instance.getPassword(CREDENTIAL_ATTRIBUTES) ?: ""
    }

    /**
     * 保存 API Key（使用 PasswordSafe 安全存储）
     */
    fun setApiKey(apiKey: String) {
        PasswordSafe.instance.setPassword(CREDENTIAL_ATTRIBUTES, apiKey)
    }
}
