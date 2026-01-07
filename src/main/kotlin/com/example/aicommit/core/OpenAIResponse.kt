package com.example.aicommit.core

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * OpenAI API 响应格式（Jackson 兼容）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OpenAIResponse @JsonCreator constructor(
    @JsonProperty("id") val id: String,
    @JsonProperty("object") val objectType: String,
    @JsonProperty("created") val created: Long,
    @JsonProperty("model") val model: String,
    @JsonProperty("choices") val choices: List<Choice>,
    @JsonProperty("usage") val usage: Usage?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Choice @JsonCreator constructor(
    @JsonProperty("index") val index: Int,
    @JsonProperty("finish_reason") val finishReason: String,
    @JsonProperty("message") val message: Message
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Message @JsonCreator constructor(
    @JsonProperty("role") val role: String,
    @JsonProperty("content") val content: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Usage @JsonCreator constructor(
    @JsonProperty("prompt_tokens") val promptTokens: Int,
    @JsonProperty("completion_tokens") val completionTokens: Int,
    @JsonProperty("total_tokens") val totalTokens: Int
)
