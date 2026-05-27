package com.github.youjiyun1123.youregexinterpreter.core.visualization

import com.github.youjiyun1123.youregexinterpreter.core.model.*

/**
 * 语法树可视化模型
 */
data class TreeNodeModel(
    val id: String,
    val type: NodeType,
    val label: String,
    val details: String = "",
    val children: List<TreeNodeModel> = emptyList()
)

enum class NodeType {
    ROOT,
    LITERAL,
    CHAR_CLASS,
    QUANTIFIER,
    GROUP,
    ALTERNATION,
    SEQUENCE,
    ANCHOR,
    BACK_REFERENCE,
    ESCAPE
}

/**
 * 语法树转换器 - 将 RegexNode 转换为可视化模型
 */
object SyntaxTreeTransformer {
    
    private var nodeIdCounter = 0
    
    fun transform(node: RegexNode): TreeNodeModel {
        nodeIdCounter = 0
        return nodeToTree(node, isRoot = true)
    }
    
    private fun nodeToTree(node: RegexNode, isRoot: Boolean = false): TreeNodeModel {
        val id = "node_${nodeIdCounter++}"
        
        return when (node) {
            is Literal -> TreeNodeModel(
                id = id,
                type = NodeType.LITERAL,
                label = "Literal",
                details = "\"${escapeLabel(node.chars)}\""
            )
            
            is CharClass -> charClassToTree(node, id)
            
            is Quantifier -> quantifierToTree(node, id)
            
            is Group -> groupToTree(node, id)
            
            is Alternation -> alternationToTree(node, id)
            
            is Sequence -> sequenceToTree(node, id)
            
            is Anchor -> anchorToTree(node, id)
            
            is BackReference -> backReferenceToTree(node, id)
            
            is Escape -> escapeToTree(node, id)
        }
    }
    
    private fun charClassToTree(node: CharClass, id: String): TreeNodeModel {
        val children = mutableListOf<TreeNodeModel>()
        
        if (node.predefined != null) {
            val desc = when (node.predefined) {
                PredefinedClass.DIGIT -> "数字 [0-9]"
                PredefinedClass.NON_DIGIT -> "非数字"
                PredefinedClass.WORD -> "单词 [a-zA-Z0-9_]"
                PredefinedClass.NON_WORD -> "非单词"
                PredefinedClass.WHITESPACE -> "空白"
                PredefinedClass.NON_WHITESPACE -> "非空白"
                PredefinedClass.ANY -> "任意字符"
            }
            children.add(TreeNodeModel(
                id = "${id}_predefined",
                type = NodeType.CHAR_CLASS,
                label = "Predefined",
                details = desc
            ))
        }
        
        if (node.ranges.isNotEmpty()) {
            val rangeStr = node.ranges.joinToString(", ") { 
                if (it.start == it.end) "\"${it.start}\"" 
                else "\"${it.start}-${it.end}\"" 
            }
            children.add(TreeNodeModel(
                id = "${id}_ranges",
                type = NodeType.CHAR_CLASS,
                label = "Ranges",
                details = rangeStr
            ))
        }
        
        val negatedStr = if (node.negated) "否" else "是"
        return TreeNodeModel(
            id = id,
            type = NodeType.CHAR_CLASS,
            label = "CharClass",
            details = "匹配: $negatedStr",
            children = children
        )
    }
    
    private fun quantifierToTree(node: Quantifier, id: String): TreeNodeModel {
        val minMaxStr = when {
            node.min == 0 && node.max == null -> "0..∞"
            node.min == 1 && node.max == null -> "1..∞"
            node.max == null -> "${node.min}..∞"
            node.min == node.max -> "${node.min}"
            else -> "${node.min}..${node.max}"
        }
        
        val typeStr = when (node.type) {
            QuantifierType.GREEDY -> "贪婪"
            QuantifierType.RELUCTANT -> "非贪婪"
            QuantifierType.POSSESSIVE -> "占有"
        }
        
        return TreeNodeModel(
            id = id,
            type = NodeType.QUANTIFIER,
            label = "Quantifier",
            details = "次数: $minMaxStr, 类型: $typeStr",
            children = listOf(nodeToTree(node.child))
        )
    }
    
    private fun groupToTree(node: Group, id: String): TreeNodeModel {
        val typeStr = when (node.type) {
            GroupType.CAPTURING -> "捕获组"
            GroupType.NON_CAPTURING -> "非捕获组"
            GroupType.NAMED_CAPTURING -> "命名捕获"
            GroupType.LOOKAHEAD -> "正向前瞻"
            GroupType.NEGATIVE_LOOKAHEAD -> "负向前瞻"
            GroupType.LOOKBEHIND -> "正向后顾"
            GroupType.NEGATIVE_LOOKBEHIND -> "负向后顾"
        }
        
        val nameStr = node.name?.let { ", 名称: \"$it\"" } ?: ""
        val indexStr = node.index?.let { ", #$it" } ?: ""
        
        val children = node.children.map { nodeToTree(it) }
        
        return TreeNodeModel(
            id = id,
            type = NodeType.GROUP,
            label = typeStr,
            details = nameStr + indexStr,
            children = children
        )
    }
    
    private fun alternationToTree(node: Alternation, id: String): TreeNodeModel {
        val children = node.alternatives.mapIndexed { index, alt ->
            TreeNodeModel(
                id = "${id}_alt_${index}",
                type = NodeType.ALTERNATION,
                label = "选项 ${index + 1}",
                children = listOf(nodeToTree(alt))
            )
        }
        
        return TreeNodeModel(
            id = id,
            type = NodeType.ALTERNATION,
            label = "选择",
            details = "${node.alternatives.size} 个选项",
            children = children
        )
    }
    
    private fun sequenceToTree(node: Sequence, id: String): TreeNodeModel {
        val children = node.children.map { nodeToTree(it) }
        
        return TreeNodeModel(
            id = id,
            type = NodeType.SEQUENCE,
            label = "序列",
            details = "${node.children.size} 个元素",
            children = children
        )
    }
    
    private fun anchorToTree(node: Anchor, id: String): TreeNodeModel {
        val desc = when (node.type) {
            AnchorType.LINE_START -> "^ 行首"
            AnchorType.LINE_END -> "$ 行尾"
            AnchorType.INPUT_START -> "\\A 输入开始"
            AnchorType.INPUT_END -> "\\Z 输入结束"
            AnchorType.INPUT_END_ANY -> "\\z 输入结束"
            AnchorType.WORD_BOUNDARY -> "\\b 单词边界"
            AnchorType.NON_WORD_BOUNDARY -> "\\B 非单词边界"
        }
        
        return TreeNodeModel(
            id = id,
            type = NodeType.ANCHOR,
            label = "Anchor",
            details = desc
        )
    }
    
    private fun backReferenceToTree(node: BackReference, id: String): TreeNodeModel {
        val desc = when {
            node.name != null -> "\\k<${node.name}> 回溯引用"
            node.index != null -> "\\${node.index} 回溯引用"
            else -> "回溯引用"
        }
        
        return TreeNodeModel(
            id = id,
            type = NodeType.BACK_REFERENCE,
            label = "BackRef",
            details = desc
        )
    }
    
    private fun escapeToTree(node: Escape, id: String): TreeNodeModel {
        val desc = when (node.type) {
            EscapeType.N -> "\\n 换行"
            EscapeType.R -> "\\r 回车"
            EscapeType.T -> "\\t 制表"
            EscapeType.F -> "\\f 换页"
            EscapeType.BACKSLASH -> "\\\\ 反斜杠"
            else -> "转义"
        }
        
        return TreeNodeModel(
            id = id,
            type = NodeType.ESCAPE,
            label = "Escape",
            details = desc
        )
    }
    
    private fun escapeLabel(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
