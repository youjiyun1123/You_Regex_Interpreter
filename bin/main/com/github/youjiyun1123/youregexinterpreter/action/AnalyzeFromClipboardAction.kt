package com.github.youjiyun1123.youregexinterpreter.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

/**
 * 从剪贴板分析正则表达式的 Action
 */
class AnalyzeFromClipboardAction : AnAction() {
    
    override fun actionPerformed(event: AnActionEvent) {
        val project: Project = event.getRequiredData(CommonDataKeys.PROJECT)
        val editor: Editor? = event.getData(CommonDataKeys.EDITOR)
        
        if (editor == null) {
            return
        }
        
        // 获取剪贴板内容
        val clipboardContent = CopyPasteManager.getInstance().contents?.getTransferData(
            DataFlavor.stringFlavor
        ) as? String
        
        if (clipboardContent.isNullOrBlank()) {
            return
        }
        
        // 解析并分析
        val parseResult = com.github.youjiyun1123.youregexinterpreter.core.parser.RegexParserFacade.parse(clipboardContent)
        
        if (parseResult.isSuccess && parseResult.syntaxTree != null) {
            val interpreter = com.github.youjiyun1123.youregexinterpreter.core.interpreter.NaturalLanguageInterpreter()
            val explanation = interpreter.interpret(parseResult.syntaxTree)
            
            // 复制解释到剪贴板
            CopyPasteManager.getInstance().setContents(StringSelection(explanation))
        }
    }
}
