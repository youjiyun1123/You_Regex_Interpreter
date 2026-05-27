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
import javax.swing.border.Border
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
    private val generateButton = JButton("生成测试字符串")
    private val explanationArea = JTextPane()
    private val matchResultArea = JTextArea(10, 30)
    private val errorLabel = JLabel("")
    private val errorDetailArea = JTextArea(3, 30)
    
    // Flags
    private val checkCaseInsensitive = JCheckBox("i", false)
    private val checkMultiline = JCheckBox("m", false)
    private val checkDotAll = JCheckBox("s", false)
    private val checkUnicode = JCheckBox("u", false)

    init {
        checkCaseInsensitive.toolTipText = com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.flags.tip.i")
        checkMultiline.toolTipText = com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.flags.tip.m")
        checkDotAll.toolTipText = com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.flags.tip.s")
        checkUnicode.toolTipText = com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.flags.tip.u")
    }
    
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
        regexPanel.add(JLabel(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.regex.label")), BorderLayout.NORTH)
        regexPanel.add(regexInput, BorderLayout.CENTER)
        inputPanel.add(regexPanel, BorderLayout.NORTH)
        
        // Options row
        val optionsPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
        
        optionsPanel.add(JLabel(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.language.label")))
        RegexEngineRegistry.getAvailableLanguages().forEach { lang ->
            languageCombo.addItem(lang)
        }
        languageCombo.selectedItem = RegexLanguage.JAVA
        languageCombo.addActionListener { onLanguageChanged() }
        optionsPanel.add(languageCombo)
        
        optionsPanel.add(JLabel(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.flags.label")))
        optionsPanel.add(checkCaseInsensitive)
        optionsPanel.add(checkMultiline)
        optionsPanel.add(checkDotAll)
        optionsPanel.add(checkUnicode)
        
        // Generate button
        generateButton.toolTipText = "根据当前正则表达式生成匹配的测试字符串"
        generateButton.addActionListener { onGenerateTestString() }
        optionsPanel.add(Box.createHorizontalStrut(20)) // Add spacing
        optionsPanel.add(generateButton)
        
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
        testPanel.add(JLabel(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.testString.label")), BorderLayout.NORTH)
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
        resultTabbedPane.addTab(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.tab.explain"), explanationScrollPane)
        
        // Syntax tree tab
        resultTabbedPane.addTab(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.tab.syntaxTree"), syntaxTreePanel)
        
        // Match results
        matchResultArea.isEditable = false
        // Use theme-aware colors for readability in both light/dark themes.
        matchResultArea.background = com.intellij.util.ui.UIUtil.getTextFieldBackground()
        matchResultArea.foreground = com.intellij.util.ui.UIUtil.getLabelForeground()
        matchResultArea.caretColor = com.intellij.util.ui.UIUtil.getLabelForeground()
        val matchScrollPane = JScrollPane(matchResultArea)
        resultTabbedPane.addTab(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.tab.matchResults"), matchScrollPane)
        
        leftPanel.add(resultTabbedPane, BorderLayout.CENTER)
        
        // Right: Template panel
        val rightPanel = JPanel(BorderLayout(5, 5))
        rightPanel.border = BorderFactory.createTitledBorder(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.templates.title"))
        
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
    
    private fun onGenerateTestString() {
        val result = viewModel.generateTestString()
        
        when (result) {
            is com.github.youjiyun1123.youregexinterpreter.core.generator.GenerationResult.Success -> {
                testInput.text = result.testString
                viewModel.updateTestInput(result.testString)
                JOptionPane.showMessageDialog(
                    null,
                    "已生成测试字符串 (${result.testString.length} 字符)",
                    "生成成功",
                    JOptionPane.INFORMATION_MESSAGE
                )
            }
            is com.github.youjiyun1123.youregexinterpreter.core.generator.GenerationResult.Failure -> {
                JOptionPane.showMessageDialog(
                    null,
                    result.message,
                    "生成失败",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        }
    }
    
    private fun useTemplate(template: RegexTemplate) {
        setPattern(template.pattern)
    }

    fun setPattern(pattern: String) {
        regexInput.text = pattern
        viewModel.updatePattern(pattern)
        updateSyntaxTree()
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
                    errorLabel.text = com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.error.prefix", firstError.message)
                    
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
                        com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.matches.none")
                    } else {
                        buildString {
                            appendLine(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.matches.found", state.matches.size))
                            appendLine()
                            state.matches.take(1000).forEachIndexed { index, match ->
                                appendLine(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message(
                                    "ui.match.item",
                                    index + 1,
                                    match.value,
                                    match.range.start,
                                    match.range.endInclusive
                                ))
                                if (match.groups.size > 1) {
                                    match.groups.drop(1).forEach { group ->
                                        val nameStr = if (group.name != null) " (${group.name})" else ""
                                        appendLine(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message(
                                            "ui.match.group.item",
                                            group.index,
                                            nameStr,
                                            group.value ?: "null"
                                        ))
                                    }
                                }
                                appendLine()
                            }
                        }
                    }
                    
                    if (state.warnings.isNotEmpty()) {
                        errorLabel.text = com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.warning.prefix", state.warnings.first())
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
