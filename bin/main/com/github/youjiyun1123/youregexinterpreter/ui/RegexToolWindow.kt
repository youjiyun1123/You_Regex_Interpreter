package com.github.youjiyun1123.youregexinterpreter.ui

import com.github.youjiyun1123.youregexinterpreter.core.model.RegexFlag
import com.github.youjiyun1123.youregexinterpreter.core.model.RegexLanguage
import com.github.youjiyun1123.youregexinterpreter.core.parser.SyntaxErrorDetector
import com.github.youjiyun1123.youregexinterpreter.engine.RegexEngineRegistry
import com.github.youjiyun1123.youregexinterpreter.template.RegexTemplate
import com.github.youjiyun1123.youregexinterpreter.ui.component.SyntaxTreePanel
import com.github.youjiyun1123.youregexinterpreter.ui.component.TemplateDetailPanel
import com.github.youjiyun1123.youregexinterpreter.ui.component.TemplatePanel
import com.github.youjiyun1123.youregexinterpreter.ui.viewmodel.RegexViewModel
import com.github.youjiyun1123.youregexinterpreter.ui.viewmodel.ViewModelListener
import com.github.youjiyun1123.youregexinterpreter.ui.viewmodel.ViewState
import java.awt.*
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
    
    // Main components
    private val regexInput = JTextField(30)
    private val testInput = JTextArea(5, 30)
    private val explanationArea = JTextPane()
    private val matchResultArea = JTextArea(10, 30)
    private val errorLabel = JLabel("")
    private val errorDetailArea = JTextArea(3, 30)
    
    // Flags
    private val checkCaseInsensitive = JCheckBox("i", false)
    private val checkMultiline = JCheckBox("m", false)
    private val checkDotAll = JCheckBox("s", false)
    private val checkUnicode = JCheckBox("u", false)
    
    // Language selector
    private val languageCombo = JComboBox<RegexLanguage>()
    
    // Panels
    private val syntaxTreePanel = SyntaxTreePanel()
    private val templateDetailPanel = TemplateDetailPanel { template -> useTemplate(template) }
    
    private val viewModeTabbedPane = JTabbedPane()
    
    fun getContent(): JComponent {
        val mainPanel = JPanel(BorderLayout(10, 10))
        mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        mainPanel.add(createTopPanel(), BorderLayout.NORTH)
        mainPanel.add(createCenterPanel(), BorderLayout.CENTER)
        
        viewModel.addListener(StateListener())
        
        return mainPanel
    }
    
    private fun createTopPanel(): JPanel {
        val panel = JPanel(BorderLayout(5, 5))
        
        // Input row
        val inputPanel = JPanel(BorderLayout(5, 5))
        
        // Regex input
        val regexPanel = JPanel(BorderLayout(5, 5))
        regexPanel.add(JLabel("正则表达式:"), BorderLayout.NORTH)
        regexPanel.add(regexInput, BorderLayout.CENTER)
        inputPanel.add(regexPanel, BorderLayout.NORTH)
        
        // Options row
        val optionsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
        
        optionsPanel.add(JLabel("语言:"))
        RegexEngineRegistry.getAvailableLanguages().forEach { lang ->
            languageCombo.addItem(lang)
        }
        languageCombo.selectedItem = RegexLanguage.JAVA
        languageCombo.addActionListener { onLanguageChanged() }
        optionsPanel.add(languageCombo)
        
        optionsPanel.add(JLabel(" Flags:"))
        optionsPanel.add(checkCaseInsensitive)
        optionsPanel.add(checkMultiline)
        optionsPanel.add(checkDotAll)
        optionsPanel.add(checkUnicode)
        
        inputPanel.add(optionsPanel, BorderLayout.CENTER)
        
        // Error area
        val errorPanel = JPanel(BorderLayout())
        errorLabel.foreground = Color.RED
        errorLabel.font = errorLabel.font.deriveFont(java.awt.Font.BOLD)
        errorPanel.add(errorLabel, BorderLayout.NORTH)
        
        errorDetailArea.isEditable = false
        errorDetailArea.foreground = Color.RED.darker()
        errorDetailArea.background = Color(255, 240, 240)
        errorDetailArea.font = errorDetailArea.font.deriveFont(11f)
        errorPanel.add(JScrollPane(errorDetailArea), BorderLayout.CENTER)
        
        inputPanel.add(errorPanel, BorderLayout.SOUTH)
        
        // Listeners
        regexInput.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) = onPatternChanged()
            override fun insertUpdate(e: DocumentEvent?) = onPatternChanged()
            override fun removeUpdate(e: DocumentEvent?) = onPatternChanged()
        })
        
        checkCaseInsensitive.addActionListener { onFlagsChanged() }
        checkMultiline.addActionListener { onFlagsChanged() }
        checkDotAll.addActionListener { onFlagsChanged() }
        checkUnicode.addActionListener { onFlagsChanged() }
        
        return inputPanel
    }
    
    private fun createCenterPanel(): JPanel {
        val panel = JPanel(BorderLayout(10, 10))
        
        // Left: Test input and results
        val leftPanel = JPanel(BorderLayout(5, 5))
        
        val testPanel = JPanel(BorderLayout(5, 5))
        testPanel.add(JLabel("测试字符串:"), BorderLayout.NORTH)
        testInput.lineWrap = true
        testPanel.add(JScrollPane(testInput), BorderLayout.CENTER)
        leftPanel.add(testPanel, BorderLayout.NORTH)
        
        // Result area with tabs
        val resultTabbedPane = JTabbedPane()
        
        // Explanation tab
        val explanationScrollPane = JScrollPane()
        explanationArea.isEditable = false
        explanationArea.contentType = "text/html"
        explanationScrollPane.viewport.view = explanationArea
        resultTabbedPane.addTab("解释", explanationScrollPane)
        
        // Syntax tree tab
        resultTabbedPane.addTab("语法树", syntaxTreePanel)
        
        // Match results
        matchResultArea.isEditable = false
        matchResultArea.background = Color(240, 255, 240)
        val matchScrollPane = JScrollPane(matchResultArea)
        resultTabbedPane.addTab("匹配结果", matchScrollPane)
        
        leftPanel.add(resultTabbedPane, BorderLayout.CENTER)
        
        // Right: Template panel
        val rightPanel = JPanel(BorderLayout(5, 5))
        rightPanel.border = BorderFactory.createTitledBorder("模板库")
        
        val templatePanel = TemplatePanel { template ->
            templateDetailPanel.showTemplate(template)
        }
        rightPanel.add(templatePanel, BorderLayout.CENTER)
        rightPanel.add(templateDetailPanel, BorderLayout.SOUTH)
        
        // Split pane
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.leftComponent = leftPanel
        splitPane.rightComponent = rightPanel
        splitPane.resizeWeight = 0.7
        
        panel.add(splitPane, BorderLayout.CENTER)
        
        // Test input listener
        testInput.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) = onTestInputChanged()
            override fun insertUpdate(e: DocumentEvent?) = onTestInputChanged()
            override fun removeUpdate(e: DocumentEvent?) = onTestInputChanged()
        })
        
        return panel
    }
    
    private fun onPatternChanged() {
        viewModel.updatePattern(regexInput.text)
        updateSyntaxTree()
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
    
    private fun onLanguageChanged() {
        // Language change can trigger re-validation
        viewModel.forceUpdate()
    }
    
    private fun useTemplate(template: RegexTemplate) {
        regexInput.text = template.pattern
        viewModel.updatePattern(template.pattern)
    }
    
    private fun updateSyntaxTree() {
        val pattern = regexInput.text
        if (pattern.isEmpty()) {
            syntaxTreePanel.setEmpty()
            return
        }
        
        val result = com.github.youjiyun1123.youregexinterpreter.core.parser.RegexParserFacade.parse(pattern)
        if (result.isSuccess && result.syntaxTree != null) {
            syntaxTreePanel.showTree(result.syntaxTree)
        } else {
            syntaxTreePanel.setEmpty()
        }
    }
    
    private inner class StateListener : ViewModelListener {
        override fun onStateChanged(state: ViewState) {
            when (state) {
                is ViewState.Empty -> {
                    explanationArea.text = ""
                    matchResultArea.text = ""
                    errorLabel.text = ""
                    errorDetailArea.text = ""
                }
                
                is ViewState.Error -> {
                    val firstError = state.errors.first()
                    errorLabel.text = "错误: ${firstError.message}"
                    
                    val errorDetails = state.errors.joinToString("\n\n") { error ->
                        SyntaxErrorDetector.formatError(error, regexInput.text)
                    }
                    errorDetailArea.text = errorDetails
                    
                    explanationArea.text = ""
                    matchResultArea.text = ""
                }
                
                is ViewState.Success -> {
                    errorLabel.text = ""
                    errorDetailArea.text = ""
                    
                    explanationArea.text = "<html><body><pre>${state.explanation}</pre></body></html>"
                    
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
