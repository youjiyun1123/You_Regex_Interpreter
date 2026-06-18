package com.github.youjiyun1123.youregexinterpreter.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.github.youjiyun1123.youregexinterpreter.ui.RegexToolWindowFactory

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
        
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("RegexToolWindow")
        toolWindow?.show()

        RegexToolWindowFactory.getInstance()?.setPattern(selectedText)
    }
}
