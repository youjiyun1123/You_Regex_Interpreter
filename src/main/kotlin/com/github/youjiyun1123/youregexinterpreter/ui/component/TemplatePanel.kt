package com.github.youjiyun1123.youregexinterpreter.ui.component

import com.github.youjiyun1123.youregexinterpreter.template.RegexTemplate
import com.github.youjiyun1123.youregexinterpreter.template.TemplateCategory
import com.github.youjiyun1123.youregexinterpreter.template.TemplateRegistry
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.DefaultListModel
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.ListSelectionModel
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder

/**
 * 模板选择监听器
 */
interface TemplateSelectionListener {
    fun onTemplateSelected(template: RegexTemplate)
}

/**
 * 模板库面板
 */
class TemplatePanel(
    private val onTemplateSelected: (RegexTemplate) -> Unit
) : JPanel() {
    
    private val registry = TemplateRegistry()
    private val listModel = DefaultListModel<RegexTemplate>()
    private val templateList = JList<RegexTemplate>(listModel)
    private val categoryCombo = JComboBox<TemplateCategory?>()
    private val searchField = JTextField(20)
    
    init {
        layout = java.awt.BorderLayout(5, 5)
        border = EmptyBorder(5, 5, 5, 5)
        
        add(createHeaderPanel(), java.awt.BorderLayout.NORTH)
        add(createListPanel(), java.awt.BorderLayout.CENTER)
        
        initData()
    }
    
    private fun createHeaderPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        
        val searchPanel = JPanel(java.awt.BorderLayout(5, 5))
        searchPanel.add(JLabel("搜索:"), java.awt.BorderLayout.WEST)
        searchField.addActionListener { performSearch() }
        searchPanel.add(searchField, java.awt.BorderLayout.CENTER)
        
        categoryCombo.addItem(null)
        TemplateCategory.entries.forEach { categoryCombo.addItem(it) }
        categoryCombo.addActionListener { filterByCategory() }
        
        panel.add(searchPanel)
        panel.add(Box.createVerticalStrut(5))
        panel.add(categoryCombo)
        
        return panel
    }
    
    private fun createListPanel(): JPanel {
        val panel = JPanel(java.awt.BorderLayout())
        
        templateList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        templateList.cellRenderer = TemplateListCellRenderer()
        templateList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val selected = templateList.selectedValue
                if (selected != null) {
                    onTemplateSelected(selected)
                }
            }
        }
        
        val scrollPane = JScrollPane(templateList)
        scrollPane.preferredSize = Dimension(300, 200)
        
        panel.add(scrollPane, java.awt.BorderLayout.CENTER)
        return panel
    }
    
    private fun initData() {
        updateList(registry.getAll())
    }
    
    private fun performSearch() {
        val keyword = searchField.text.trim()
        if (keyword.isEmpty()) {
            initData()
        } else {
            updateList(registry.search(keyword))
        }
    }
    
    private fun filterByCategory() {
        val category = categoryCombo.selectedItem as? TemplateCategory
        if (category == null) {
            initData()
        } else {
            updateList(registry.getByCategory(category))
        }
    }
    
    private fun updateList(templates: List<RegexTemplate>) {
        listModel.clear()
        templates.forEach { listModel.addElement(it) }
    }
    
    fun getTemplateCount(): Int = registry.getTemplateCount()
    fun getCategories(): List<TemplateCategory> = registry.getCategories()
}

/**
 * 模板列表单元格渲染器
 */
class TemplateListCellRenderer : javax.swing.ListCellRenderer<RegexTemplate> {
    
    private val label = JLabel()
    private val categoryLabels = mapOf(
        TemplateCategory.IDENTITY to "身份",
        TemplateCategory.NETWORK to "网络",
        TemplateCategory.TEXT to "文本",
        TemplateCategory.SECURITY to "安全",
        TemplateCategory.DEVELOPMENT to "开发"
    )
    
    override fun getListCellRendererComponent(
        list: JList<out RegexTemplate>,
        template: RegexTemplate,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        label.text = "<html><b>${template.name}</b><br/><font size=2 color=gray>${categoryLabels[template.category]}</font></html>"
        label.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        
        if (isSelected) {
            label.background = list.selectionBackground
            label.foreground = list.selectionForeground
        } else {
            label.background = if (index % 2 == 0) Color.WHITE else Color(245, 245, 245)
            label.foreground = Color.BLACK
        }
        label.isOpaque = true
        
        return label
    }
}
