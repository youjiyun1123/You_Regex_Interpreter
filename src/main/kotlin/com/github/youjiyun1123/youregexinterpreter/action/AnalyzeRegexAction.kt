package com.github.youjiyun1123.youregexinterpreter.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.github.youjiyun1123.youregexinterpreter.core.parser.RegexParserFacade
import com.github.youjiyun1123.youregexinterpreter.core.interpreter.NaturalLanguageInterpreter
import com.github.youjiyun1123.youregexinterpreter.engine.JavaRegexEngine

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
        
        // 解析正则表达式
        val parseResult = RegexParserFacade.parse(selectedText)
        
        if (parseResult.isSuccess && parseResult.syntaxTree != null) {
            // 生成解释
            val interpreter = NaturalLanguageInterpreter()
            val explanation = interpreter.interpret(parseResult.syntaxTree)
            
            // 显示在工具窗口
            showExplanation(project, explanation, selectedText)
        } else {
            // 显示错误
            val errors = parseResult.errors.joinToString("\n") { it.message }
            showError(project, errors, selectedText)
        }
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
