package com.example.aicommit.core

import com.intellij.openapi.util.SystemInfo

/**
 * 语言映射：语言代码 -> 语言描述（用英文，AI 更容易理解）
 */
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

    /**
     * 获取语言描述
     * @param configLang 配置的语言代码，"auto" 表示自动检测
     * @param systemLocale 系统语言代码
     * @return 语言描述（英文）
     */
    fun getLanguage(configLang: String, systemLocale: String): String {
        return if (configLang != "auto") {
            LANGUAGE_MAP[configLang] ?: "English"
        } else {
            LANGUAGE_MAP[systemLocale] ?: "English"
        }
    }

    /**
     * 获取系统语言代码
     * @return 系统语言代码
     */
    fun getSystemLocale(): String {
        return System.getProperty("user.language", "en").let { lang ->
            System.getProperty("user.country", "")?.let { country ->
                if (country.isNotEmpty()) "$lang-$country" else lang
            } ?: lang
        }
    }
}
