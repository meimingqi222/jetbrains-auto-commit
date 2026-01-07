package com.example.aicommit.util

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision

/**
 * 提取 diff 内容的工具类
 */
object DiffExtractor {
    /**
     * 从变更列表中提取 diff 内容
     */
    fun extractDiffContent(project: Project, changes: List<Change>): String {
        if (changes.isEmpty()) {
            return ""
        }

        val diffBuilder = StringBuilder()

        for ((index, change) in changes.withIndex()) {
            try {
                val filePath = change.virtualFile?.path ?: change.afterRevision?.file?.path ?: "Unknown"
                diffBuilder.appendLine("diff for: $filePath")

                // 获取 diff 内容
                val diffText = extractDiffText(change)
                diffBuilder.appendLine(diffText)
                diffBuilder.appendLine()

                if (index < changes.size - 1) {
                    diffBuilder.appendLine("---")
                }
            } catch (e: VcsException) {
                diffBuilder.appendLine("Error extracting diff: ${e.message}")
            } catch (e: Exception) {
                diffBuilder.appendLine("Error processing change: ${e.message}")
            }
        }

        return diffBuilder.toString()
    }

    /**
     * 提取单个变更的 diff 文本
     */
    private fun extractDiffText(change: Change): String {
        val beforeRevision: ContentRevision? = change.beforeRevision
        val afterRevision: ContentRevision? = change.afterRevision

        return when {
            // 新增文件
            beforeRevision == null && afterRevision != null -> {
                val content = afterRevision.content
                """
                +++ ${afterRevision.file.path}
                ${contentAsString(content)}
                """.trimIndent()
            }
            // 删除文件
            beforeRevision != null && afterRevision == null -> {
                val content = beforeRevision.content
                """
                --- ${beforeRevision.file.path}
                ${contentAsString(content)}
                """.trimIndent()
            }
            // 修改文件
            beforeRevision != null && afterRevision != null -> {
                val beforeContent = contentAsString(beforeRevision.content)
                val afterContent = contentAsString(afterRevision.content)

                """
                --- ${beforeRevision.file.path}
                +++ ${afterRevision.file.path}
                ${generateSimpleDiff(beforeContent, afterContent)}
                """.trimIndent()
            }
            else -> {
                "Unknown change type"
            }
        }
    }

    /**
     * 将内容转换为字符串
     */
    private fun contentAsString(content: String?): String {
        if (content == null) return ""
        return content
    }

    /**
     * 生成简单的 diff（简化版，实际应该使用更完整的 diff 算法）
     */
    private fun generateSimpleDiff(before: String, after: String): String {
        val beforeLines = before.lines()
        val afterLines = after.lines()

        val diffBuilder = StringBuilder()

        val maxLines = maxOf(beforeLines.size, afterLines.size)
        for (i in 0 until maxLines) {
            val beforeLine = if (i < beforeLines.size) beforeLines[i] else null
            val afterLine = if (i < afterLines.size) afterLines[i] else null

            when {
                beforeLine == afterLine -> {
                    // 相同的行，不显示或显示为空格
                    // diffBuilder.appendLine(" $beforeLine")
                }
                beforeLine != null && afterLine == null -> {
                    // 删除的行
                    diffBuilder.appendLine("-$beforeLine")
                }
                beforeLine == null && afterLine != null -> {
                    // 新增的行
                    diffBuilder.appendLine("+$afterLine")
                }
                else -> {
                    // 修改的行
                    diffBuilder.appendLine("-$beforeLine")
                    diffBuilder.appendLine("+$afterLine")
                }
            }
        }

        return diffBuilder.toString()
    }
}
