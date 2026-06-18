package com.github.youjiyun1123.youregexinterpreter.ui.component

import com.github.youjiyun1123.youregexinterpreter.template.RegexTemplate
import com.github.youjiyun1123.youregexinterpreter.template.TemplateCategory
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

/**
 * 模板详情面板
 */
class TemplateDetailPanel(
    private val onUseTemplate: (RegexTemplate) -> Unit
) : JPanel() {
    
    private val nameLabel = JLabel()
    private val categoryLabel = JLabel()
    private val descriptionLabel = JLabel()
    private val patternLabel = JLabel()
    private val exampleLabel = JLabel()
    private val useButton = JButton(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.template.use"))
    private val copyButton = JButton(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.template.copy"))
    
    init {
        layout = BorderLayout(5, 5)
        border = BorderFactory.createTitledBorder(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.template.detail.title"))
        
        val placeholder = JLabel(com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.template.detail.placeholder"))
        placeholder.horizontalAlignment = SwingConstants.CENTER
        placeholder.foreground = Color.GRAY
        add(placeholder, BorderLayout.CENTER)
    }
    
    /**
     * 显示模板详情
     */
    fun showTemplate(template: RegexTemplate) {
        removeAll()
        
        layout = BorderLayout(5, 5)
        
        val categoryStr = when (template.category) {
            TemplateCategory.IDENTITY -> com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.template.category.identity")
            TemplateCategory.NETWORK -> com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.template.category.network")
            TemplateCategory.TEXT -> com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.template.category.text")
            TemplateCategory.NUMERIC -> com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.template.category.numeric")
            TemplateCategory.SECURITY -> com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.template.category.security")
            TemplateCategory.DEVELOPMENT -> com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.template.category.development")
        }
        
        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        infoPanel.border = EmptyBorder(10, 10, 10, 10)
        
        nameLabel.text = "<html><h2>${template.name}</h2></html>"
        nameLabel.alignmentX = Component.LEFT_ALIGNMENT
        infoPanel.add(nameLabel)
        infoPanel.add(Box.createVerticalStrut(5))
        
        categoryLabel.text = com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.template.category.label", categoryStr)
        categoryLabel.alignmentX = Component.LEFT_ALIGNMENT
        categoryLabel.foreground = Color.GRAY
        infoPanel.add(categoryLabel)
        infoPanel.add(Box.createVerticalStrut(10))
        
        descriptionLabel.text = "<html><b>${com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.template.description.label")}</b><br/>${template.description}</html>"
        descriptionLabel.alignmentX = Component.LEFT_ALIGNMENT
        infoPanel.add(descriptionLabel)
        infoPanel.add(Box.createVerticalStrut(10))
        
        patternLabel.text = "<html><b>${com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.template.pattern.label")}</b><br/><code>${escapeHtml(template.pattern)}</code></html>"
        patternLabel.alignmentX = Component.LEFT_ALIGNMENT
        infoPanel.add(patternLabel)
        infoPanel.add(Box.createVerticalStrut(10))
        
        exampleLabel.text = "<html><b>${com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("ui.template.example.label")}</b> <code>${escapeHtml(template.example)}</code></html>"
        exampleLabel.alignmentX = Component.LEFT_ALIGNMENT
        infoPanel.add(exampleLabel)
        
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))

        // Avoid stacking listeners when the same panel is re-rendered for multiple templates.
        useButton.actionListeners.forEach { useButton.removeActionListener(it) }
        copyButton.actionListeners.forEach { copyButton.removeActionListener(it) }

        useButton.addActionListener { onUseTemplate(template) }
        copyButton.addActionListener { copyPattern(template.pattern) }

        buttonPanel.add(useButton)
        buttonPanel.add(copyButton)
        
        val topPanel = JPanel(BorderLayout())
        topPanel.add(infoPanel, BorderLayout.NORTH)
        topPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        add(topPanel, BorderLayout.CENTER)
        
        revalidate()
        repaint()
    }
    
    private fun escapeHtml(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
    
    private fun copyPattern(pattern: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = java.awt.datatransfer.StringSelection(pattern)
        clipboard.setContents(selection, selection)
        
        val title = com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("copy.success.title")
        val content = com.github.youjiyun1123.youregexinterpreter.YouRegexBundle.message("copy.success.content")

        // Use IDE notification instead of modal dialog to avoid repeated popups.
        com.intellij.notification.Notifications.Bus.notify(
            com.intellij.notification.Notification(
                "YouRegexInterpreter",
                title,
                content,
                com.intellij.notification.NotificationType.INFORMATION
            )
        )
    }
}
