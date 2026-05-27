package com.github.youjiyun1123.youregexinterpreter.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
/**
 * 分析选中正则表达式的 Action
 */
class AnalyzeRegexAction : AnAction() {
    
    override fun actionPerformed(event: AnActionEvent) {
        val project: Project = event.getRequiredData(CommonDataKeys.PROJECT)
        val editor: Editor? = event.getData(CommonDataKeys.EDITOR)
        
        if (editor == null) {
            return
        }
        
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText
        
        if (selectedText.isNullOrBlank()) {
            return
        }
        
        // Show tool window and fill the pattern from selection.
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("RegexToolWindow")
        toolWindow?.show()

        com.github.youjiyun1123.youregexinterpreter.ui.RegexToolWindowFactory.getInstance()?.setPattern(selectedText)
    }
    
    private fun showExplanation(project: Project, explanation: String, pattern: String) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("RegexToolWindow")
        toolWindow?.show()
        
        // 可以通过消息总线发送解释到工具窗口
    }
    
    private fun showError(project: Project, errors: String, pattern: String) {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("RegexToolWindow")
        toolWindow?.show()
    }
}
