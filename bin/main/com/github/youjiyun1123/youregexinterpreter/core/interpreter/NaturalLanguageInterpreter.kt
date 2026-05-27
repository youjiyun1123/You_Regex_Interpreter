package com.github.youjiyun1123.youregexinterpreter.core.interpreter

import com.github.youjiyun1123.youregexinterpreter.core.model.*

/**
 * 自然语言解释器
 */
class NaturalLanguageInterpreter : RegexVisitor {
    
    fun interpret(node: RegexNode): String = node.accept(this)
    
    override fun visit(node: Literal): String = "匹配字面量 \"${node.chars}\""
    
    override fun visit(node: CharClass): String {
        if (node.predefined != null) {
            val desc = when (node.predefined) {
                PredefinedClass.DIGIT -> "数字"
                PredefinedClass.NON_DIGIT -> "非数字"
                PredefinedClass.WORD -> "单词字符"
                PredefinedClass.NON_WORD -> "非单词字符"
                PredefinedClass.WHITESPACE -> "空白字符"
                PredefinedClass.NON_WHITESPACE -> "非空白字符"
                PredefinedClass.ANY -> "任意字符"
            }
            return if (node.negated) "匹配非$desc" else "匹配$desc"
        }
        val rangeStr = node.ranges.joinToString("、") { "${it.start}-${it.end}" }
        return if (node.negated) "不匹配 [$rangeStr]" else "匹配 [$rangeStr]"
    }
    
    override fun visit(node: Quantifier): String {
        val childDesc = node.child.accept(this)
        val greedyStr = if (node.type == QuantifierType.RELUCTANT) "（非贪婪）" else ""
        val quantDesc = when {
            node.min == 0 && node.max == null -> "零次或多次$greedyStr"
            node.min == 1 && node.max == null -> "一次或多次$greedyStr"
            node.min == 0 && node.max == 1 -> "零次或一次$greedyStr"
            node.max == null -> "至少 ${node.min} 次$greedyStr"
            node.min == node.max -> "恰好 ${node.min} 次$greedyStr"
            else -> " ${node.min} 到 ${node.max} 次$greedyStr"
        }
        return "$childDesc，重复$quantDesc"
    }
    
    override fun visit(node: Group): String {
        val typeStr = when (node.type) {
            GroupType.CAPTURING -> if (node.name != null) "捕获组 \"${node.name}\"" else "捕获组"
            GroupType.NON_CAPTURING -> "非捕获组"
            GroupType.NAMED_CAPTURING -> "命名捕获组 \"${node.name}\""
            GroupType.LOOKAHEAD -> "正向前瞻断言"
            GroupType.NEGATIVE_LOOKAHEAD -> "负向前瞻断言"
            GroupType.LOOKBEHIND -> "正向后顾断言"
            GroupType.NEGATIVE_LOOKBEHIND -> "负向后顾断言"
        }
        val childrenDesc = if (node.children.size == 1) {
            node.children[0].accept(this)
        } else {
            node.children.joinToString("，然后") { it.accept(this) }
        }
        return "$typeStr: $childrenDesc"
    }
    
    override fun visit(node: Alternation): String {
        return node.alternatives.joinToString(" 或者 ") { it.accept(this) }
    }
    
    override fun visit(node: Anchor): String {
        val desc = when (node.type) {
            AnchorType.LINE_START -> "行首"
            AnchorType.LINE_END -> "行尾"
            AnchorType.INPUT_START -> "输入开始"
            AnchorType.INPUT_END -> "输入结束（非换行）"
            AnchorType.INPUT_END_ANY -> "输入结束"
            AnchorType.WORD_BOUNDARY -> "单词边界"
            AnchorType.NON_WORD_BOUNDARY -> "非单词边界"
        }
        return "在${desc}"
    }
    
    override fun visit(node: BackReference): String {
        return when {
            node.name != null -> "回溯引用: ${node.name}"
            node.index != null -> "回溯引用: 第 ${node.index} 组"
            else -> "回溯引用"
        }
    }
    
    override fun visit(node: Escape): String {
        return when (node.type) {
            EscapeType.N -> "换行符"
            EscapeType.R -> "回车符"
            EscapeType.T -> "制表符"
            EscapeType.F -> "换页符"
            EscapeType.BACKSLASH -> "反斜杠"
            else -> "转义字符"
        }
    }
    
    override fun visit(node: Sequence): String {
        return node.children.joinToString("，然后") { it.accept(this) }
    }
}
