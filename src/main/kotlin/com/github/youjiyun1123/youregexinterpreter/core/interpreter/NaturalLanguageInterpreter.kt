package com.github.youjiyun1123.youregexinterpreter.core.interpreter

import com.github.youjiyun1123.youregexinterpreter.core.model.*

/**
 * 自然语言解释器 - 将正则表达式语法树转换为可读的解释
 */
class NaturalLanguageInterpreter : RegexVisitor {
    
    /**
     * 遍历语法树并生成带序号的解释
     */
    fun interpret(node: RegexNode): String {
        val steps = mutableListOf<String>()
        collectSteps(node, steps)
        return steps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n")
    }
    
    /**
     * 收集每一步的解释
     */
    private fun collectSteps(node: RegexNode, steps: MutableList<String>) {
        when (node) {
            is Literal -> {
                steps.add(if (node.chars.length == 1) {
                    "匹配字符 \"${node.chars}\""
                } else {
                    "匹配字符串 \"${node.chars}\""
                })
            }
            
            is CharClass -> {
                steps.add(visitCharClass(node))
            }
            
            is Quantifier -> {
                val startIndex = steps.size
                collectSteps(node.child, steps)
                val childDesc = steps.subList(startIndex, steps.size).joinToString("，然后 ")
                steps.subList(startIndex, steps.size).clear()
                val greedyStr = when (node.type) {
                    QuantifierType.GREEDY -> ""
                    QuantifierType.RELUCTANT -> "（非贪婪，优先匹配更少）"
                    QuantifierType.POSSESSIVE -> "（占有，贪婪且不回溯）"
                }
                val quantDesc = when {
                    node.min == 0 && node.max == null -> "零次或多次$greedyStr"
                    node.min == 1 && node.max == null -> "一次或多次$greedyStr"
                    node.min == 0 && node.max == 1 -> "零次或一次（可选）$greedyStr"
                    node.max == null -> "至少 ${node.min} 次$greedyStr"
                    node.min == node.max -> "恰好 ${node.min} 次$greedyStr"
                    else -> "${node.min} 到 ${node.max} 次$greedyStr"
                }
                steps.add("$childDesc，重复 $quantDesc")
            }
            
            is Group -> {
                val typeStr = when (node.type) {
                    GroupType.CAPTURING -> {
                        if (node.name != null) "命名捕获组 \"${node.name}\""
                        else if (node.index != null) "捕获组 #${node.index}"
                        else "捕获组"
                    }
                    GroupType.NON_CAPTURING -> "非捕获组"
                    GroupType.NAMED_CAPTURING -> "命名捕获组 \"${node.name}\""
                    GroupType.LOOKAHEAD -> "正向前瞻断言（后面跟着...）"
                    GroupType.NEGATIVE_LOOKAHEAD -> "负向前瞻断言（后面不跟...）"
                    GroupType.LOOKBEHIND -> "正向后顾断言（前面是...）"
                    GroupType.NEGATIVE_LOOKBEHIND -> "负向后顾断言（前面不是...）"
                }
                
                if (node.children.isEmpty()) {
                    steps.add(typeStr)
                } else if (node.children.size == 1) {
                    collectSteps(node.children[0], steps)
                    val childDesc = steps.removeLast()
                    steps.add("$typeStr：$childDesc")
                } else {
                    val childDescs = mutableListOf<String>()
                    node.children.forEach { child ->
                        collectSteps(child, childDescs)
                    }
                    val childrenDesc = childDescs.joinToString("，然后 ")
                    steps.add("$typeStr：$childrenDesc")
                }
            }
            
            is Alternation -> {
                val startIndex = steps.size
                node.alternatives.forEach { alt ->
                    collectSteps(alt, steps)
                }
                val alternatives = steps.subList(startIndex, steps.size)
                val altStr = alternatives.joinToString(" | 或者 | ")
                alternatives.clear()
                steps.add(altStr)
            }
            
            is Sequence -> {
                if (node.children.isNotEmpty() && node.children.all { it is Literal }) {
                    val merged = node.children.joinToString(separator = "") { (it as Literal).chars }
                    steps.add(if (merged.length == 1) {
                        "匹配字符 \"${merged}\""
                    } else {
                        "匹配字符串 \"${merged}\""
                    })
                } else {
                    node.children.forEach { child ->
                        collectSteps(child, steps)
                    }
                }
            }
            
            is Anchor -> {
                steps.add(when (node.type) {
                    AnchorType.LINE_START -> "行首（^）"
                    AnchorType.LINE_END -> "行尾（$）"
                    AnchorType.INPUT_START -> "输入开头（\\A）"
                    AnchorType.INPUT_END -> "输入结尾，不含换行符（\\Z）"
                    AnchorType.INPUT_END_ANY -> "输入结尾（\\z）"
                    AnchorType.WORD_BOUNDARY -> "单词边界（\\b）"
                    AnchorType.NON_WORD_BOUNDARY -> "非单词边界（\\B）"
                })
            }
            
            is BackReference -> {
                steps.add(when {
                    node.name != null -> "回溯引用命名组 \"${node.name}\""
                    node.index != null -> "回溯引用捕获组 #${node.index}"
                    else -> "回溯引用"
                })
            }
            
            is Escape -> {
                steps.add(when (node.type) {
                    EscapeType.N -> "换行符（\\n）"
                    EscapeType.R -> "回车符（\\r）"
                    EscapeType.T -> "制表符（\\t）"
                    EscapeType.F -> "换页符（\\f）"
                    EscapeType.BACKSLASH -> "反斜杠（\\\\）"
                    EscapeType.DOT -> "点号（\\.）"
                    EscapeType.STAR -> "星号（\\*）"
                    EscapeType.PLUS -> "加号（\\+）"
                    EscapeType.QUESTION -> "问号（\\?）"
                    EscapeType.OCTAL -> "八进制字符（\\0）"
                    EscapeType.HEX -> "十六进制字符（\\x）"
                    EscapeType.UNICODE -> "Unicode 字符（\\u）"
                })
            }
        }
    }
    
    /**
     * 生成简明解释（不带序号）
     */
    fun interpretBrief(node: RegexNode): String {
        return node.accept(SimpleVisitor())
    }
    
    /**
     * 生成详细解释，包含示例
     */
    fun interpretDetailed(node: RegexNode): String {
        return buildString {
            append("正则表达式解释：\n\n")
            append(interpret(node))
            append("\n\n结构分析：\n")
            append(StructureAnalyzer.analyze(node))
        }
    }
    
    // 以下是 visitor 接口的实现，用于简单模式（不使用序号）
    override fun visit(node: Literal): String = node.accept(SimpleVisitor())
    override fun visit(node: CharClass): String = visitCharClass(node)
    override fun visit(node: Quantifier): String = node.accept(SimpleVisitor())
    override fun visit(node: Group): String = node.accept(SimpleVisitor())
    override fun visit(node: Alternation): String = node.accept(SimpleVisitor())
    override fun visit(node: Sequence): String = node.accept(SimpleVisitor())
    override fun visit(node: Anchor): String = node.accept(SimpleVisitor())
    override fun visit(node: BackReference): String = node.accept(SimpleVisitor())
    override fun visit(node: Escape): String = node.accept(SimpleVisitor())
    
    private fun visitCharClass(node: CharClass): String {
        if (node.predefined != null) {
            val desc = when (node.predefined) {
                PredefinedClass.DIGIT -> "数字字符（0-9）"
                PredefinedClass.NON_DIGIT -> "非数字字符"
                PredefinedClass.WORD -> "单词字符（字母、数字、下划线）"
                PredefinedClass.NON_WORD -> "非单词字符"
                PredefinedClass.WHITESPACE -> "空白字符（空格、制表符、换行等）"
                PredefinedClass.NON_WHITESPACE -> "非空白字符"
                PredefinedClass.ANY -> "任意字符（除换行符外）"
            }
            return if (node.negated) "不匹配：$desc" else "匹配：$desc"
        }
        
        if (node.ranges.isEmpty()) {
            return if (node.negated) "不匹配任何字符" else "匹配空字符集"
        }
        
        val rangeStr = node.ranges.joinToString("、") { charRangeDesc(it) }
        return if (node.negated) "不匹配：$rangeStr" else "匹配：$rangeStr"
    }
    
    private fun charRangeDesc(range: RegexCharRange): String {
        return when {
            range.start == range.end -> "\"${range.start}\""
            range.start == 'a' && range.end == 'z' -> "小写字母"
            range.start == 'A' && range.end == 'Z' -> "大写字母"
            range.start == '0' && range.end == '9' -> "数字"
            else -> "\"${range.start}\"-\"${range.end}\""
        }
    }
}

/**
 * 简单访客，用于不带序号的解释
 */
private class SimpleVisitor : RegexVisitor {
    
    override fun visit(node: Literal): String {
        return if (node.chars.length == 1) {
            "匹配字符 \"${node.chars}\""
        } else {
            "匹配字符串 \"${node.chars}\""
        }
    }
    
    override fun visit(node: CharClass): String {
        if (node.predefined != null) {
            val desc = when (node.predefined) {
                PredefinedClass.DIGIT -> "数字字符（0-9）"
                PredefinedClass.NON_DIGIT -> "非数字字符"
                PredefinedClass.WORD -> "单词字符（字母、数字、下划线）"
                PredefinedClass.NON_WORD -> "非单词字符"
                PredefinedClass.WHITESPACE -> "空白字符（空格、制表符、换行等）"
                PredefinedClass.NON_WHITESPACE -> "非空白字符"
                PredefinedClass.ANY -> "任意字符（除换行符外）"
            }
            return if (node.negated) "不匹配：$desc" else "匹配：$desc"
        }
        
        if (node.ranges.isEmpty()) {
            return if (node.negated) "不匹配任何字符" else "匹配空字符集"
        }
        
        val rangeStr = node.ranges.joinToString("、") { charRangeDesc(it) }
        return if (node.negated) "不匹配：$rangeStr" else "匹配：$rangeStr"
    }
    
    private fun charRangeDesc(range: RegexCharRange): String {
        return when {
            range.start == range.end -> "\"${range.start}\""
            range.start == 'a' && range.end == 'z' -> "小写字母"
            range.start == 'A' && range.end == 'Z' -> "大写字母"
            range.start == '0' && range.end == '9' -> "数字"
            else -> "\"${range.start}\"-\"${range.end}\""
        }
    }
    
    override fun visit(node: Quantifier): String {
        val childDesc = node.child.accept(this)
        val greedyStr = when (node.type) {
            QuantifierType.GREEDY -> ""
            QuantifierType.RELUCTANT -> "（非贪婪，优先匹配更少）"
            QuantifierType.POSSESSIVE -> "（占有，贪婪且不回溯）"
        }
        
        val quantDesc = when {
            node.min == 0 && node.max == null -> "零次或多次$greedyStr"
            node.min == 1 && node.max == null -> "一次或多次$greedyStr"
            node.min == 0 && node.max == 1 -> "零次或一次（可选）$greedyStr"
            node.max == null -> "至少 ${node.min} 次$greedyStr"
            node.min == node.max -> "恰好 ${node.min} 次$greedyStr"
            else -> "${node.min} 到 ${node.max} 次$greedyStr"
        }
        
        return "$childDesc，重复 $quantDesc"
    }
    
    override fun visit(node: Group): String {
        val typeStr = when (node.type) {
            GroupType.CAPTURING -> {
                if (node.name != null) "命名捕获组 \"${node.name}\""
                else if (node.index != null) "捕获组 #${node.index}"
                else "捕获组"
            }
            GroupType.NON_CAPTURING -> "非捕获组"
            GroupType.NAMED_CAPTURING -> "命名捕获组 \"${node.name}\""
            GroupType.LOOKAHEAD -> "正向前瞻断言（后面跟着...）"
            GroupType.NEGATIVE_LOOKAHEAD -> "负向前瞻断言（后面不跟...）"
            GroupType.LOOKBEHIND -> "正向后顾断言（前面是...）"
            GroupType.NEGATIVE_LOOKBEHIND -> "负向后顾断言（前面不是...）"
        }
        
        val childrenDesc = if (node.children.isEmpty()) {
            ""
        } else if (node.children.size == 1) {
            node.children[0].accept(this)
        } else {
            node.children.joinToString("，然后 ") { it.accept(this) }
        }
        
        return if (childrenDesc.isNotEmpty()) {
            "$typeStr：$childrenDesc"
        } else {
            typeStr
        }
    }
    
    override fun visit(node: Alternation): String {
        return node.alternatives.joinToString(" | 或者 | ") { alt ->
            alt.accept(this)
        }
    }
    
    override fun visit(node: Sequence): String {
        if (node.children.isNotEmpty() && node.children.all { it is Literal }) {
            val merged = node.children.joinToString(separator = "") { (it as Literal).chars }
            return visit(Literal(merged))
        }
        return node.children.joinToString("，然后 ") { it.accept(this) }
    }
    
    override fun visit(node: Anchor): String {
        return when (node.type) {
            AnchorType.LINE_START -> "行首（^）"
            AnchorType.LINE_END -> "行尾（$）"
            AnchorType.INPUT_START -> "输入开头（\\A）"
            AnchorType.INPUT_END -> "输入结尾，不含换行符（\\Z）"
            AnchorType.INPUT_END_ANY -> "输入结尾（\\z）"
            AnchorType.WORD_BOUNDARY -> "单词边界（\\b）"
            AnchorType.NON_WORD_BOUNDARY -> "非单词边界（\\B）"
        }
    }
    
    override fun visit(node: BackReference): String {
        return when {
            node.name != null -> "回溯引用命名组 \"${node.name}\""
            node.index != null -> "回溯引用捕获组 #${node.index}"
            else -> "回溯引用"
        }
    }
    
    override fun visit(node: Escape): String {
        return when (node.type) {
            EscapeType.N -> "换行符（\\n）"
            EscapeType.R -> "回车符（\\r）"
            EscapeType.T -> "制表符（\\t）"
            EscapeType.F -> "换页符（\\f）"
            EscapeType.BACKSLASH -> "反斜杠（\\\\）"
            EscapeType.DOT -> "点号（\\.）"
            EscapeType.STAR -> "星号（\\*）"
            EscapeType.PLUS -> "加号（\\+）"
            EscapeType.QUESTION -> "问号（\\?）"
            EscapeType.OCTAL -> "八进制字符（\\0）"
            EscapeType.HEX -> "十六进制字符（\\x）"
            EscapeType.UNICODE -> "Unicode 字符（\\u）"
        }
    }
}

/**
 * 结构分析器 - 生成结构化文本
 */
object StructureAnalyzer {
    
    fun analyze(node: RegexNode): String {
        val result = StringBuilder()
        analyzeNode(node, 0, result)
        return result.toString()
    }
    
    private fun analyzeNode(node: RegexNode, level: Int, result: StringBuilder) {
        val indent = "  ".repeat(level)
        
        when (node) {
            is Literal -> result.appendLine("$indent\u2500 Literal: \"${escapeString(node.chars)}\"")
            
            is CharClass -> {
                if (node.predefined != null) {
                    result.appendLine("$indent\u2500 CharClass: ${node.predefined}" + if (node.negated) " [NEGATED]" else "")
                } else {
                    val ranges = node.ranges.joinToString(", ") { "${it.start}-${it.end}" }
                    result.appendLine("$indent\u2500 CharClass: [$ranges]" + if (node.negated) " [NEGATED]" else "")
                }
            }
            
            is Quantifier -> {
                val maxStr = if (node.max == null) "\u221E" else node.max.toString()
                val typeStr = when (node.type) {
                    QuantifierType.GREEDY -> "greedy"
                    QuantifierType.RELUCTANT -> "reluctant"
                    QuantifierType.POSSESSIVE -> "possessive"
                }
                result.appendLine("$indent\u2500 Quantifier: {${node.min}, $maxStr} [$typeStr]")
                analyzeNode(node.child, level + 1, result)
            }
            
            is Group -> {
                val nameStr = if (node.name != null) " \"${node.name}\"" else ""
                result.appendLine("$indent\u2500 Group: ${node.type}$nameStr")
                node.children.forEach { analyzeNode(it, level + 1, result) }
            }
            
            is Alternation -> {
                result.appendLine("$indent\u2500 Alternation (${node.alternatives.size} options)")
                node.alternatives.forEachIndexed { i, alt ->
                    result.appendLine("$indent  \u251C\u2500 Option ${i + 1}:")
                    analyzeNode(alt, level + 2, result)
                }
            }
            
            is Sequence -> {
                result.appendLine("$indent\u2500 Sequence (${node.children.size} items)")
                node.children.forEach { analyzeNode(it, level + 1, result) }
            }
            
            is Anchor -> {
                result.appendLine("$indent\u2500 Anchor: ${node.type}")
            }
            
            is BackReference -> {
                val ref = if (node.name != null) "name=\"${node.name}\"" else "index=${node.index}"
                result.appendLine("$indent\u2500 BackReference: $ref")
            }
            
            is Escape -> {
                result.appendLine("$indent\u2500 Escape: ${node.type}")
            }
        }
    }
    
    private fun escapeString(s: String): String {
        return s.replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
