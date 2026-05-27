package com.github.youjiyun1123.youregexinterpreter.ui

import com.github.youjiyun1123.youregexinterpreter.core.model.RegexFlag
import com.github.youjiyun1123.youregexinterpreter.core.parser.EnhancedParseError
import com.github.youjiyun1123.youregexinterpreter.core.parser.SyntaxErrorDetector
import com.github.youjiyun1123.youregexinterpreter.ui.viewmodel.RegexViewModel
import com.github.youjiyun1123.youregexinterpreter.ui.viewmodel.ViewModelListener
import com.github.youjiyun1123.youregexinterpreter.ui.viewmodel.ViewState
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Regex Tool Window - 主工具窗口 UI
 */
class RegexToolWindow(
    private val project: com.intellij.openapi.project.Project,
    private val toolWindow: com.intellij.openapi.wm.ToolWindow
) {
    private val viewModel = RegexViewModel()
    
    private val regexInput = JTextField(30)
    private val testInput = JTextArea(5, 30)
    private val explanationArea = JTextArea(3, 30)
    private val structureArea = JTextArea(5, 30)
    private val matchResultArea = JTextArea(10, 30)
    private val errorLabel = JLabel("")
    private val errorDetailArea = JTextArea(3, 30)
    
    private val checkCaseInsensitive = JCheckBox("i", false)
    private val checkMultiline = JCheckBox("m", false)
    private val checkDotAll = JCheckBox("s", false)
    private val checkUnicode = JCheckBox("u", false)
    private val checkDetailedExplanation = JCheckBox("详细解释", false)
    
    private val explanationScrollPane = JScrollPane()
    private val structureScrollPane = JScrollPane()
    
    fun getContent(): JComponent {
        val mainPanel = JPanel(BorderLayout(10, 10))
        mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        mainPanel.add(createRegexInputPanel(), BorderLayout.NORTH)
        mainPanel.add(createCenterPanel(), BorderLayout.CENTER)
        
        viewModel.addListener(StateListener())
        
        return mainPanel
    }
    
    private fun createRegexInputPanel(): JPanel {
        val panel = JPanel(BorderLayout(5, 5))
        
        val inputLabel = JLabel("正则表达式:")
        inputLabel.font = inputLabel.font.deriveFont(java.awt.Font.BOLD)
        panel.add(inputLabel, BorderLayout.NORTH)
        
        val inputRow = JPanel(BorderLayout(5, 5))
        inputRow.add(regexInput, BorderLayout.CENTER)
        
        val flagsPanel = JPanel()
        flagsPanel.add(JLabel("Flags:"))
        flagsPanel.add(checkCaseInsensitive)
        flagsPanel.add(checkMultiline)
        flagsPanel.add(checkDotAll)
        flagsPanel.add(checkUnicode)
        flagsPanel.add(Box.createHorizontalStrut(10))
        flagsPanel.add(checkDetailedExplanation)
        inputRow.add(flagsPanel, BorderLayout.EAST)
        
        panel.add(inputRow, BorderLayout.CENTER)
        
        // 错误提示区域
        val errorPanel = JPanel(BorderLayout())
        errorLabel.foreground = Color.RED
        errorLabel.font = errorLabel.font.deriveFont(java.awt.Font.BOLD)
        errorPanel.add(errorLabel, BorderLayout.NORTH)
        
        errorDetailArea.isEditable = false
        errorDetailArea.foreground = Color.RED.darker()
        errorDetailArea.background = Color(255, 240, 240)
        errorDetailArea.font = errorDetailArea.font.deriveFont(11f)
        errorPanel.add(JScrollPane(errorDetailArea), BorderLayout.CENTER)
        
        panel.add(errorPanel, BorderLayout.SOUTH)
        
        // 监听器
        regexInput.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) = onPatternChanged()
            override fun insertUpdate(e: DocumentEvent?) = onPatternChanged()
            override fun removeUpdate(e: DocumentEvent?) = onPatternChanged()
        })
        
        checkCaseInsensitive.addActionListener { onFlagsChanged() }
        checkMultiline.addActionListener { onFlagsChanged() }
        checkDotAll.addActionListener { onFlagsChanged() }
        checkUnicode.addActionListener { onFlagsChanged() }
        checkDetailedExplanation.addActionListener { 
            viewModel.toggleDetailedExplanation()
            viewModel.forceUpdate()
        }
        
        return panel
    }
    
    private fun createCenterPanel(): JPanel {
        val panel = JPanel(BorderLayout(10, 10))
        
        // 测试输入
        val testPanel = JPanel(BorderLayout(5, 5))
        testPanel.add(JLabel("测试字符串:"), BorderLayout.NORTH)
        testInput.lineWrap = true
        testPanel.add(JScrollPane(testInput), BorderLayout.CENTER)
        panel.add(testPanel, BorderLayout.NORTH)
        
        // 解释和结构
        val explanationPanel = JPanel(BorderLayout(5, 5))
        explanationPanel.add(JLabel("解释:"), BorderLayout.NORTH)
        
        explanationArea.isEditable = false
        explanationArea.background = Color(245, 245, 250)
        explanationScrollPane.viewport.view = explanationArea
        explanationPanel.add(explanationScrollPane, BorderLayout.CENTER)
        
        structureArea.isEditable = false
        structureArea.background = Color(250, 250, 255)
        structureArea.font = java.awt.Font("Monaco", java.awt.Font.PLAIN, 11)
        structureScrollPane.viewport.view = structureArea
        explanationPanel.add(structureScrollPane, BorderLayout.SOUTH)
        
        // 匹配结果
        val resultPanel = JPanel(BorderLayout(5, 5))
        resultPanel.add(JLabel("匹配结果:"), BorderLayout.NORTH)
        matchResultArea.isEditable = false
        matchResultArea.background = Color(240, 255, 240)
        resultPanel.add(JScrollPane(matchResultArea), BorderLayout.CENTER)
        
        // 分割面板
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        splitPane.topComponent = explanationPanel
        splitPane.bottomComponent = resultPanel
        splitPane.resizeWeight = 0.4
        
        panel.add(splitPane, BorderLayout.CENTER)
        
        // 监听器
        testInput.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) = onTestInputChanged()
            override fun insertUpdate(e: DocumentEvent?) = onTestInputChanged()
            override fun removeUpdate(e: DocumentEvent?) = onTestInputChanged()
        })
        
        return panel
    }
    
    private fun onPatternChanged() {
        viewModel.updatePattern(regexInput.text)
    }
    
    private fun onTestInputChanged() {
        viewModel.updateTestInput(testInput.text)
    }
    
    private fun onFlagsChanged() {
        val flags = mutableSetOf<RegexFlag>()
        if (checkCaseInsensitive.isSelected) flags.add(RegexFlag.CASE_INSENSITIVE)
        if (checkMultiline.isSelected) flags.add(RegexFlag.MULTILINE)
        if (checkDotAll.isSelected) flags.add(RegexFlag.DOTALL)
        if (checkUnicode.isSelected) flags.add(RegexFlag.UNICODE_CASE)
        viewModel.updateFlags(flags)
    }
    
    private inner class StateListener : ViewModelListener {
        override fun onStateChanged(state: ViewState) {
            when (state) {
                is ViewState.Empty -> {
                    explanationArea.text = ""
                    structureArea.text = ""
                    matchResultArea.text = ""
                    errorLabel.text = ""
                    errorDetailArea.text = ""
                }
                
                is ViewState.Error -> {
                    val firstError = state.errors.first()
                    errorLabel.text = "错误: ${firstError.message}"
                    
                    val errorDetails = state.errors.joinToString("\n") { error ->
                        SyntaxErrorDetector.formatError(error, regexInput.text)
                    }
                    errorDetailArea.text = errorDetails
                    
                    explanationArea.text = ""
                    structureArea.text = ""
                    matchResultArea.text = ""
                }
                
                is ViewState.Success -> {
                    errorLabel.text = ""
                    errorDetailArea.text = ""
                    
                    explanationArea.text = state.explanation
                    structureArea.text = state.structure
                    
                    matchResultArea.text = if (state.matches.isEmpty()) {
                        "无匹配"
                    } else {
                        buildString {
                            appendLine("找到 ${state.matches.size} 个匹配:")
                            appendLine()
                            state.matches.take(1000).forEachIndexed { index, match ->
                                appendLine("Match ${index + 1}: \"${match.value}\" (位置: ${match.range.start}-${match.range.endInclusive})")
                                if (match.groups.size > 1) {
                                    match.groups.drop(1).forEach { group ->
                                        val nameStr = if (group.name != null) " (${group.name})" else ""
                                        appendLine("  Group ${group.index}$nameStr: \"${group.value ?: "null"}\"")
                                    }
                                }
                                appendLine()
                            }
                        }
                    }
                    
                    // 显示警告
                    if (state.warnings.isNotEmpty()) {
                        errorLabel.text = "警告: ${state.warnings.first()}"
                        errorLabel.foreground = Color.ORANGE
                    } else {
                        errorLabel.foreground = Color.RED
                    }
                }
            }
        }
    }
    
    fun dispose() {
        viewModel.dispose()
    }
}
