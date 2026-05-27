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
    private val useButton = JButton("使用此模板")
    private val copyButton = JButton("复制正则")
    
    init {
        layout = BorderLayout(5, 5)
        border = BorderFactory.createTitledBorder("模板详情")
        
        val placeholder = JLabel("选择一个模板查看详情")
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
            TemplateCategory.IDENTITY -> "身份认证"
            TemplateCategory.NETWORK -> "网络相关"
            TemplateCategory.TEXT -> "文本处理"
            TemplateCategory.SECURITY -> "密码安全"
            TemplateCategory.DEVELOPMENT -> "开发相关"
        }
        
        val infoPanel = JPanel()
        infoPanel.layout = BoxLayout(infoPanel, BoxLayout.Y_AXIS)
        infoPanel.border = EmptyBorder(10, 10, 10, 10)
        
        nameLabel.text = "<html><h2>${template.name}</h2></html>"
        nameLabel.alignmentX = Component.LEFT_ALIGNMENT
        infoPanel.add(nameLabel)
        infoPanel.add(Box.createVerticalStrut(5))
        
        categoryLabel.text = "分类: $categoryStr"
        categoryLabel.alignmentX = Component.LEFT_ALIGNMENT
        categoryLabel.foreground = Color.GRAY
        infoPanel.add(categoryLabel)
        infoPanel.add(Box.createVerticalStrut(10))
        
        descriptionLabel.text = "<html><b>说明:</b><br/>${template.description}</html>"
        descriptionLabel.alignmentX = Component.LEFT_ALIGNMENT
        infoPanel.add(descriptionLabel)
        infoPanel.add(Box.createVerticalStrut(10))
        
        patternLabel.text = "<html><b>正则表达式:</b><br/><code>${escapeHtml(template.pattern)}</code></html>"
        patternLabel.alignmentX = Component.LEFT_ALIGNMENT
        infoPanel.add(patternLabel)
        infoPanel.add(Box.createVerticalStrut(10))
        
        exampleLabel.text = "<html><b>示例:</b> <code>${escapeHtml(template.example)}</code></html>"
        exampleLabel.alignmentX = Component.LEFT_ALIGNMENT
        infoPanel.add(exampleLabel)
        
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
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
        
        JOptionPane.showMessageDialog(
            this,
            "正则表达式已复制到剪贴板",
            "复制成功",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
}
