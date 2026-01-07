package com.example.aicommit.action

import com.example.aicommit.config.AICommitConfig
import com.example.aicommit.util.CommitMessageGenerator
import com.example.aicommit.util.DiffExtractor
import com.example.aicommit.util.AICommitException
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications.Bus
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.vcs.commit.AbstractCommitWorkflowHandler

/**
 * 手动触发生成 Commit 消息的 Action
 */
class GenerateCommitMessageAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val presentation = e.presentation

        if (project == null) {
            presentation.isEnabledAndVisible = false
            return
        }

        // 检查 commit 对话框是否打开（通过 COMMIT_WORKFLOW_HANDLER）
        val commitWorkflowHandler = e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER)
        
        // 检查是否有用户勾选的变更
        val hasSelectedChanges = hasUserSelectedChanges(e)
        
        // 只有 commit 对话框打开且有勾选变更时才启用
        presentation.isEnabled = commitWorkflowHandler != null && hasSelectedChanges
        presentation.isVisible = commitWorkflowHandler != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        
        // 获取 commit message 输入控件
        val commitMessageInput = getCommitMessageInput(e)
        if (commitMessageInput == null) {
            Messages.showWarningDialog(
                project,
                "无法找到 Commit Message 输入框。\n\n请打开 Commit 对话框。",
                "AI Commit Message"
            )
            return
        }
        
        val config = project.getService(AICommitConfig::class.java)

        // 获取用户勾选的变更
        val changes = getSelectedChanges(e)

        if (changes.isEmpty()) {
            Messages.showWarningDialog(
                project,
                "没有勾选要提交的文件。\n\n请在 Local Changes 中勾选要提交的文件。",
                "AI Commit Message"
            )
            return
        }

        // 检查 API Key
        if (config.getApiKey().isEmpty()) {
            val result = Messages.showYesNoDialog(
                project,
                "未配置 API Key，请先在设置中配置 API Key。\n\n是否现在打开设置？",
                "AI Commit Message",
                Messages.YES_BUTTON,
                Messages.NO_BUTTON,
                Messages.getQuestionIcon()
            )
            if (result == Messages.YES) {
                ApplicationManager.getApplication().invokeLater {
                    com.intellij.openapi.options.ShowSettingsUtil.getInstance().showSettingsDialog(
                        project,
                        "AI Commit Message"
                    )
                }
            }
            return
        }

        // 获取 diff 内容
        val diffContent = DiffExtractor.extractDiffContent(project, changes)

        if (diffContent.isBlank()) {
            Messages.showWarningDialog(
                project,
                "没有可提交的变更内容。",
                "AI Commit Message"
            )
            return
        }

        // 在后台任务中生成
        val generator = CommitMessageGenerator(config, project)
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "正在生成 Commit 消息...",
            true
        ) {
            private var commitMessage: String? = null
            private var errorMessage: String? = null

            override fun run(indicator: ProgressIndicator) {
                try {
                    commitMessage = generator.generate(diffContent)
                } catch (ex: Exception) {
                    errorMessage = ex.message ?: "未知错误"
                }
            }

            override fun onSuccess() {
                if (commitMessage != null) {
                    // 填充到 commit message 输入框
                    fillCommitMessage(commitMessageInput, commitMessage!!)

                    // 显示成功通知
                    showNotification(
                        project,
                        "成功",
                        "Commit 消息已生成！",
                        NotificationType.INFORMATION
                    )
                } else if (errorMessage != null) {
                    // 显示错误通知
                    showNotification(
                        project,
                        "失败",
                        "生成失败: $errorMessage",
                        NotificationType.ERROR
                    )
                }
            }
        })
    }

    /**
     * 获取 commit message 输入控件
     */
    private fun getCommitMessageInput(e: AnActionEvent): CommitMessage? {
        return e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) as? CommitMessage
    }

    /**
     * 检查是否有用户勾选的变更
     */
    private fun hasUserSelectedChanges(e: AnActionEvent): Boolean {
        val includedChanges = getIncludedChangesFromWorkflow(e)
        return includedChanges.isNotEmpty()
    }

    /**
     * 获取用户勾选的变更
     */
    private fun getSelectedChanges(e: AnActionEvent): List<Change> {
        return getIncludedChangesFromWorkflow(e)
    }

    /**
     * 从 CommitWorkflowHandler 获取用户勾选的变更
     */
    private fun getIncludedChangesFromWorkflow(e: AnActionEvent): List<Change> {
        val workflowHandler = e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER) as? AbstractCommitWorkflowHandler<*, *>
        return workflowHandler?.ui?.getIncludedChanges() ?: emptyList()
    }

    /**
     * 填充 commit message
     */
    private fun fillCommitMessage(commitMessageInput: CommitMessage, message: String) {
        ApplicationManager.getApplication().invokeLater {
            commitMessageInput.setCommitMessage(message)
            commitMessageInput.editorField.apply {
                requestFocus()
                caretModel.moveToOffset(message.length)
            }
        }
    }

    /**
     * 显示通知
     */
    private fun showNotification(
        project: Project,
        title: String,
        content: String,
        type: NotificationType
    ) {
        val notification = Notification(
            "AICommit",
            title,
            content,
            type
        )
        Bus.notify(notification, project)
    }
}
