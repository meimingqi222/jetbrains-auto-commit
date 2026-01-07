package com.example.aicommit.config

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * AI Commit 配置页面
 */
class AICommitConfigurable(
    private val project: Project
) : Configurable {

    private val config = project.getService(AICommitConfig::class.java)

    private lateinit var apiKeyField: JBPasswordField
    private lateinit var apiEndpointField: JBTextField
    private lateinit var modelField: JBTextField
    private lateinit var languageField: JBTextField
    private lateinit var maxTokensField: JBTextField
    private lateinit var temperatureField: JBTextField
    private lateinit var promptArea: JBTextArea

    private var mainPanel: JPanel? = null

    override fun getDisplayName(): String = "AI Commit Message"

    override fun createComponent(): JComponent? {
        apiKeyField = JBPasswordField()
        apiEndpointField = JBTextField()
        modelField = JBTextField()
        languageField = JBTextField()
        maxTokensField = JBTextField()
        temperatureField = JBTextField()
        promptArea = JBTextArea(10, 50)
        promptArea.lineWrap = true
        promptArea.wrapStyleWord = true

        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("API Key:", apiKeyField)
            .addComponent(JBLabel("<html><font size='2'>API Key 将被加密存储，建议使用 DeepSeek API Key</font></html>"))
            .addVerticalGap(10)
            .addSeparator(10)
            .addVerticalGap(10)
            .addLabeledComponent("API Endpoint:", apiEndpointField)
            .addComponent(JBLabel("<html><font size='2'>OpenAI 兼容的 API 基地址</font></html>"))
            .addVerticalGap(10)
            .addLabeledComponent("Model:", modelField)
            .addComponent(JBLabel("<html><font size='2'>模型名称，默认 deepseek-chat</font></html>"))
            .addVerticalGap(10)
            .addLabeledComponent("Language:", languageField)
            .addComponent(JBLabel("<html><font size='2'>语言代码: auto, zh-CN, en, ja, ko, es, fr, de, ru</font></html>"))
            .addVerticalGap(10)
            .addLabeledComponent("Max Tokens:", maxTokensField)
            .addComponent(JBLabel("<html><font size='2'>最大 token 数量，默认 500</font></html>"))
            .addVerticalGap(10)
            .addLabeledComponent("Temperature:", temperatureField)
            .addComponent(JBLabel("<html><font size='2'>温度参数 0-1，默认 0.3</font></html>"))
            .addVerticalGap(10)
            .addSeparator(10)
            .addVerticalGap(10)
            .addLabeledComponent("Prompt Template:", promptArea)
            .addComponent(JBLabel("<html><font size='2'>使用 {diff} 和 {language} 作为占位符</font></html>"))
            .addComponentFillVertically(JPanel(), 0)
            .panel

        reset()
        return mainPanel
    }

    override fun isModified(): Boolean {
        val currentApiKey = String(apiKeyField.password)
        val storedApiKey = config.getApiKey()

        return currentApiKey != storedApiKey ||
                apiEndpointField.text != config.state.apiEndpoint ||
                modelField.text != config.state.model ||
                languageField.text != config.state.language ||
                maxTokensField.text != config.state.maxTokens.toString() ||
                temperatureField.text != config.state.temperature.toString() ||
                promptArea.text != config.state.prompt
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        // 验证输入
        val apiKey = String(apiKeyField.password).trim()
        if (apiKey.isNotEmpty() && apiKey.length < 10) {
            throw ConfigurationException("API Key 格式不正确，请检查")
        }

        val apiEndpoint = apiEndpointField.text.trim()
        if (apiEndpoint.isEmpty()) {
            throw ConfigurationException("API Endpoint 不能为空")
        }

        val model = modelField.text.trim()
        if (model.isEmpty()) {
            throw ConfigurationException("Model 不能为空")
        }

        val language = languageField.text.trim()
        if (language.isEmpty()) {
            throw ConfigurationException("Language 不能为空")
        }

        val maxTokens = maxTokensField.text.trim().toIntOrNull()
        if (maxTokens == null || maxTokens <= 0) {
            throw ConfigurationException("Max Tokens 必须是正整数")
        }

        val temperature = temperatureField.text.trim().toDoubleOrNull()
        if (temperature == null || temperature < 0 || temperature > 1) {
            throw ConfigurationException("Temperature 必须在 0-1 之间")
        }

        val prompt = promptArea.text.trim()

        // 保存配置
        config.setApiKey(apiKey)
        config.state.apiEndpoint = apiEndpoint
        config.state.model = model
        config.state.language = language
        config.state.maxTokens = maxTokens
        config.state.temperature = temperature
        config.state.prompt = prompt
    }

    override fun reset() {
        apiKeyField.text = config.getApiKey()
        apiEndpointField.text = config.state.apiEndpoint
        modelField.text = config.state.model
        languageField.text = config.state.language
        maxTokensField.text = config.state.maxTokens.toString()
        temperatureField.text = config.state.temperature.toString()
        promptArea.text = config.state.prompt
    }
}
