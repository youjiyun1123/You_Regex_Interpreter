package com.github.youjiyun1123.youregexinterpreter.ui

import com.github.youjiyun1123.youregexinterpreter.core.interpreter.NaturalLanguageInterpreter
import com.github.youjiyun1123.youregexinterpreter.core.model.RegexFlag
import com.github.youjiyun1123.youregexinterpreter.core.model.MatchResult
import com.github.youjiyun1123.youregexinterpreter.core.parser.ParseResult
import com.github.youjiyun1123.youregexinterpreter.core.parser.RegexParser
import com.github.youjiyun1123.youregexinterpreter.engine.JavaRegexEngine
import java.awt.BorderLayout
import java.awt.Color
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class RegexToolWindow(
    private val project: com.intellij.openapi.project.Project,
    private val toolWindow: com.intellij.openapi.wm.ToolWindow
) {
    private val regexEngine = JavaRegexEngine()
    private val interpreter = NaturalLanguageInterpreter()
    
    private val regexInput = JTextField(30)
    private val testInput = JTextArea(5, 30)
    private val explanationArea = JTextArea(3, 30)
    private val matchResultArea = JTextArea(10, 30)
    private val errorLabel = JLabel("")
    
    private val checkCaseInsensitive = JCheckBox("i", false)
    private val checkMultiline = JCheckBox("m", false)
    private val checkDotAll = JCheckBox("s", false)
    private val checkUnicode = JCheckBox("u", false)
    
    private var debounceTimer: Timer? = null
    private val debounceDelay = 150L
    
    fun getContent(): JComponent {
        val mainPanel = JPanel(BorderLayout(10, 10))
        mainPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        
        mainPanel.add(createRegexInputPanel(), BorderLayout.NORTH)
        mainPanel.add(createTestAndResultPanel(), BorderLayout.CENTER)
        
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
        inputRow.add(flagsPanel, BorderLayout.EAST)
        
        panel.add(inputRow, BorderLayout.CENTER)
        
        errorLabel.foreground = Color.RED
        panel.add(errorLabel, BorderLayout.SOUTH)
        
        regexInput.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) = scheduleUpdate()
            override fun insertUpdate(e: DocumentEvent?) = scheduleUpdate()
            override fun removeUpdate(e: DocumentEvent?) = scheduleUpdate()
        })
        
        checkCaseInsensitive.addActionListener { scheduleUpdate() }
        checkMultiline.addActionListener { scheduleUpdate() }
        checkDotAll.addActionListener { scheduleUpdate() }
        checkUnicode.addActionListener { scheduleUpdate() }
        
        return panel
    }
    
    private fun createTestAndResultPanel(): JPanel {
        val panel = JPanel(BorderLayout(10, 10))
        
        val testPanel = JPanel(BorderLayout(5, 5))
        testPanel.add(JLabel("测试字符串:"), BorderLayout.NORTH)
        testInput.lineWrap = true
        testPanel.add(JScrollPane(testInput), BorderLayout.CENTER)
        panel.add(testPanel, BorderLayout.NORTH)
        
        val centerPanel = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        centerPanel.topComponent = createExplanationPanel()
        centerPanel.bottomComponent = createMatchResultPanel()
        panel.add(centerPanel, BorderLayout.CENTER)
        
        testInput.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent?) = scheduleUpdate()
            override fun insertUpdate(e: DocumentEvent?) = scheduleUpdate()
            override fun removeUpdate(e: DocumentEvent?) = scheduleUpdate()
        })
        
        return panel
    }
    
    private fun createExplanationPanel(): JPanel {
        val panel = JPanel(BorderLayout(5, 5))
        panel.add(JLabel("解释:"), BorderLayout.NORTH)
        explanationArea.isEditable = false
        explanationArea.background = Color(240, 240, 240)
        panel.add(JScrollPane(explanationArea), BorderLayout.CENTER)
        return panel
    }
    
    private fun createMatchResultPanel(): JPanel {
        val panel = JPanel(BorderLayout(5, 5))
        panel.add(JLabel("匹配结果:"), BorderLayout.NORTH)
        matchResultArea.isEditable = false
        matchResultArea.background = Color(240, 240, 240)
        panel.add(JScrollPane(matchResultArea), BorderLayout.CENTER)
        return panel
    }
    
    private fun scheduleUpdate() {
        debounceTimer?.stop()
        
        val delay = if (regexInput.text.length >= 200) debounceDelay else 0
        
        debounceTimer = Timer(delay.toInt()) {
            SwingUtilities.invokeLater { updateResults() }
        }
        debounceTimer?.start()
    }
    
    private fun updateResults() {
        val pattern = regexInput.text
        val testText = testInput.text
        
        if (pattern.isEmpty()) {
            explanationArea.text = ""
            matchResultArea.text = ""
            errorLabel.text = ""
            return
        }
        
        val parser = RegexParser(pattern)
        val parseResult = parser.parse()
        
        when (parseResult) {
            is ParseResult.Success -> {
                errorLabel.text = ""
                
                val explanation = interpreter.interpret(parseResult.root)
                explanationArea.text = explanation
                
                val flags = getSelectedFlags()
                val matches = regexEngine.findAll(pattern, testText, flags)
                
                matchResultArea.text = if (matches.isEmpty()) {
                    "无匹配"
                } else {
                    buildString {
                        appendLine("找到 ${matches.size} 个匹配:")
                        appendLine()
                        matches.take(1000).forEachIndexed { index, match ->
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
            }
            
            is ParseResult.Failure -> {
                errorLabel.text = "错误: ${parseResult.error.message} (位置: ${parseResult.error.position})"
                explanationArea.text = ""
                matchResultArea.text = ""
            }
        }
    }
    
    private fun getSelectedFlags(): Set<RegexFlag> {
        val flags = mutableSetOf<RegexFlag>()
        if (checkCaseInsensitive.isSelected) flags.add(RegexFlag.CASE_INSENSITIVE)
        if (checkMultiline.isSelected) flags.add(RegexFlag.MULTILINE)
        if (checkDotAll.isSelected) flags.add(RegexFlag.DOTALL)
        if (checkUnicode.isSelected) flags.add(RegexFlag.UNICODE_CASE)
        return flags
    }
}
