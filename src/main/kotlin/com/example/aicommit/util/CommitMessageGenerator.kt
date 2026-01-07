package com.example.aicommit.util

import com.example.aicommit.core.LanguageMapper
import com.example.aicommit.config.AICommitConfig
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import java.io.IOException
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Commit 消息生成器（同步版本，避免类加载器冲突）
 */
class CommitMessageGenerator(
    private val config: AICommitConfig,
    private val project: com.intellij.openapi.project.Project
) {
    private val httpClient: OkHttpClient
    private val objectMapper = ObjectMapper()

    init {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    companion object {
        private const val MAX_DIFF_LENGTH = 8000
        private const val DEFAULT_PROMPT = """Generate a commit message following Conventional Commits specification.

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
     * 生成 commit 消息（同步方法）
     */
    fun generate(diffContent: String): String {
        val apiKey = config.getApiKey()
        if (apiKey.isEmpty()) {
            throw AICommitException("未配置 API Key，请在设置中配置")
        }

        // 限制 diff 长度
        val truncatedDiff = limitDiffLength(diffContent)

        // 构造 prompt
        val language = LanguageMapper.getLanguage(config.state.language, LanguageMapper.getSystemLocale())
        val prompt = buildPrompt(truncatedDiff, language)

        // 调用 API
        return callOpenAIAPI(prompt, apiKey)
    }

    /**
     * 限制 diff 长度
     */
    private fun limitDiffLength(diff: String): String {
        return if (diff.length > MAX_DIFF_LENGTH) {
            diff.substring(0, MAX_DIFF_LENGTH) + "\n\n...(内容过长，已截断)"
        } else {
            diff
        }
    }

    /**
     * 构造 prompt
     */
    private fun buildPrompt(diff: String, language: String): String {
        val promptTemplate = config.state.prompt.ifEmpty { DEFAULT_PROMPT }
        return promptTemplate
            .replace("{diff}", diff)
            .replace("{language}", language)
    }

    /**
     * 调用 OpenAI 兼容 API（同步方法）
     */
    private fun callOpenAIAPI(prompt: String, apiKey: String): String {
        val apiEndpoint = config.state.apiEndpoint.trimEnd('/')

        val requestBody = mapOf(
            "model" to config.state.model,
            "messages" to listOf(
                mapOf("role" to "user", "content" to prompt)
            ),
            "max_tokens" to config.state.maxTokens,
            "temperature" to config.state.temperature
        )

        val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        val requestBodyJson = objectMapper.writeValueAsString(requestBody)
        val requestBodyOkHttp = requestBodyJson.toRequestBody(jsonMediaType)

        val request = Request.Builder()
            .url("$apiEndpoint/v1/chat/completions")
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBodyOkHttp)
            .build()

        try {
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw AICommitException("API 错误 (${response.code}): ${response.body?.string()}")
            }

            val responseBody = response.body?.string() ?: throw AICommitException("API 返回空响应")
            
            val openAIResponse = objectMapper.readValue(responseBody, OpenAIResponse::class.java)

            if (openAIResponse.choices.isEmpty()) {
                throw AICommitException("API 返回了空响应")
            }

            return openAIResponse.choices[0].message.content.trim()
        } catch (e: IOException) {
            throw AICommitException("网络错误: ${e.message}")
        } catch (e: Exception) {
            if (e is AICommitException) {
                throw e
            }
            throw AICommitException("API 调用失败: ${e.message}")
        }
    }

    /**
     * 关闭 HTTP 客户端
     */
    fun close() {
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }
}

/**
 * AI Commit 异常
 */
class AICommitException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * OpenAI API 响应格式（Jackson 兼容）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAIResponse(
    @JsonProperty("id") val id: String,
    @JsonProperty("object") val objectType: String,
    @JsonProperty("created") val created: Long,
    @JsonProperty("model") val model: String,
    @JsonProperty("choices") val choices: List<Choice>,
    @JsonProperty("usage") val usage: Usage?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Choice(
    @JsonProperty("index") val index: Int,
    @JsonProperty("finish_reason") val finishReason: String,
    @JsonProperty("message") val message: Message
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Message(
    @JsonProperty("role") val role: String,
    @JsonProperty("content") val content: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Usage(
    @JsonProperty("prompt_tokens") val promptTokens: Int,
    @JsonProperty("completion_tokens") val completionTokens: Int,
    @JsonProperty("total_tokens") val totalTokens: Int
)
