package com.github.youjiyun1123.youregexinterpreter.ui.component

import com.github.youjiyun1123.youregexinterpreter.core.model.RegexNode
import com.github.youjiyun1123.youregexinterpreter.core.visualization.NodeType
import com.github.youjiyun1123.youregexinterpreter.core.visualization.SyntaxTreeTransformer
import com.github.youjiyun1123.youregexinterpreter.core.visualization.TreeNodeModel
import java.awt.Color
import java.awt.Component
import java.awt.Font
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.DefaultTreeCellRenderer

/**
 * 语法树可视化面板
 */
class SyntaxTreePanel : JPanel() {
    
    private val tree = JTree()
    private val scrollPane = JScrollPane(tree)
    private val label = javax.swing.JLabel("语法树结构")
    
    init {
        layout = java.awt.BorderLayout()
        
        label.font = Font("Microsoft YaHei", Font.BOLD, 14)
        label.border = javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5)
        add(label, java.awt.BorderLayout.NORTH)
        
        tree.isRootVisible = true
        tree.showsRootHandles = true
        tree.rowHeight = 22
        
        val renderer = SyntaxTreeCellRenderer()
        tree.cellRenderer = renderer
        
        add(scrollPane, java.awt.BorderLayout.CENTER)
        
        setEmpty()
    }
    
    /**
     * 显示语法树
     */
    fun showTree(root: RegexNode) {
        val treeModel = SyntaxTreeTransformer.transform(root)
        val jTreeNode = toJTreeNode(treeModel)
        tree.model = DefaultTreeModel(jTreeNode)
        
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }
    }
    
    /**
     * 设置空状态
     */
    fun setEmpty() {
        val emptyNode = DefaultMutableTreeNode("输入正则表达式查看语法树")
        tree.model = DefaultTreeModel(emptyNode)
    }
    
    private fun toJTreeNode(model: TreeNodeModel): DefaultMutableTreeNode {
        val displayText = buildDisplayText(model)
        val node = DefaultMutableTreeNode(displayText)
        model.children.forEach { child ->
            node.add(toJTreeNode(child))
        }
        return node
    }
    
    private fun buildDisplayText(model: TreeNodeModel): String {
        return if (model.details.isNotEmpty()) {
            "${model.label} (${model.details})"
        } else {
            model.label
        }
    }
}

/**
 * 语法树单元格渲染器
 */
class SyntaxTreeCellRenderer : DefaultTreeCellRenderer() {
    
    private val typeColors = mapOf(
        NodeType.LITERAL to Color(76, 175, 80),
        NodeType.CHAR_CLASS to Color(33, 150, 243),
        NodeType.QUANTIFIER to Color(255, 152, 0),
        NodeType.GROUP to Color(156, 39, 176),
        NodeType.ALTERNATION to Color(244, 67, 54),
        NodeType.SEQUENCE to Color(0, 150, 136),
        NodeType.ANCHOR to Color(121, 85, 72),
        NodeType.BACK_REFERENCE to Color(96, 125, 139),
        NodeType.ESCAPE to Color(255, 193, 7),
        NodeType.ROOT to Color(158, 158, 158)
    )
    
    override fun getTreeCellRendererComponent(
        tree: JTree,
        value: Any,
        selected: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component {
        val c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
        
        if (value is DefaultMutableTreeNode) {
            val text = value.userObject?.toString() ?: ""
            
            val color = when {
                text.startsWith("Literal") -> typeColors[NodeType.LITERAL]
                text.startsWith("CharClass") -> typeColors[NodeType.CHAR_CLASS]
                text.startsWith("Quantifier") -> typeColors[NodeType.QUANTIFIER]
                text.contains("组") || text.contains("前瞻") || text.contains("后顾") -> typeColors[NodeType.GROUP]
                text.contains("选择") -> typeColors[NodeType.ALTERNATION]
                text.startsWith("序列") -> typeColors[NodeType.SEQUENCE]
                text.startsWith("Anchor") -> typeColors[NodeType.ANCHOR]
                text.startsWith("BackRef") -> typeColors[NodeType.BACK_REFERENCE]
                text.startsWith("Escape") -> typeColors[NodeType.ESCAPE]
                else -> typeColors[NodeType.ROOT]
            }
            
            if (color != null) {
                c.foreground = color
            }
        }
        
        return c
    }
}
