package com.github.youjiyun1123.youregexinterpreter.core.generator

import com.github.youjiyun1123.youregexinterpreter.core.model.*

/**
 * 测试字符串生成结果
 */
sealed class GenerationResult {
    data class Success(val testString: String, val generator: TestStringGenerator.GeneratorStats) : GenerationResult()
    data class Failure(val message: String, val position: Int? = null) : GenerationResult()
}

/**
 * 测试字符串生成器
 * 根据正则表达式自动生成符合的测试字符串
 */
class TestStringGenerator(
    private val maxLength: Int = 100,
    private val maxAttempts: Int = 1000
) {
    
    data class GeneratorStats(
        val iterations: Int = 0,
        val length: Int = 0
    )
    
    private var iterations = 0
    private var currentLength = 0
    
    /**
     * 生成测试字符串
     */
    fun generate(pattern: String): GenerationResult {
        iterations = 0
        currentLength = 0
        
        // 首先验证正则表达式是否有效
        val validation = validatePattern(pattern)
        if (validation != null) {
            return validation
        }
        
        // 解析正则表达式
        val parseResult = com.github.youjiyun1123.youregexinterpreter.core.parser.RegexParserFacade.parse(pattern)
        if (!parseResult.isSuccess || parseResult.syntaxTree == null) {
            val errors = parseResult.errors.joinToString(", ") { it.message }
            return GenerationResult.Failure("正则表达式解析失败: $errors")
        }
        
        // 生成测试字符串
        return try {
            val result = generateFromNode(parseResult.syntaxTree)
            GenerationResult.Success(result, GeneratorStats(iterations, currentLength))
        } catch (e: Exception) {
            GenerationResult.Failure("生成失败: ${e.message}")
        }
    }
    
    private fun validatePattern(pattern: String): GenerationResult.Failure? {
        val errors = com.github.youjiyun1123.youregexinterpreter.core.parser.SyntaxErrorDetector.detect(pattern)
        if (errors.isNotEmpty()) {
            return GenerationResult.Failure(
                message = "正则表达式错误: ${errors.first().message}",
                position = errors.first().position
            )
        }
        return null
    }
    
    private fun generateFromNode(node: RegexNode): String {
        iterations++
        if (iterations > maxAttempts) {
            throw IllegalStateException("生成超时，请简化正则表达式")
        }
        if (currentLength >= maxLength) {
            return ""
        }
        
        return when (node) {
            is Literal -> generateLiteral(node)
            is CharClass -> generateCharClass(node)
            is Quantifier -> generateQuantifier(node)
            is Group -> generateGroup(node)
            is Alternation -> generateAlternation(node)
            is Sequence -> generateSequence(node)
            is Anchor -> "" // 锚点不生成字符
            is BackReference -> generateBackReference(node)
            is Escape -> generateEscape(node)
        }
    }
    
    private fun generateLiteral(node: Literal): String {
        currentLength += node.chars.length
        return node.chars
    }
    
    private fun generateCharClass(node: CharClass): String {
        val char = when {
            node.predefined != null -> pickFromPredefined(node.predefined, node.negated).toString()
            node.ranges.isNotEmpty() -> pickFromRanges(node.ranges, node.negated).toString()
            else -> ""
        }
        currentLength += char.length
        return char
    }
    
    private fun pickFromPredefined(predefined: PredefinedClass, negated: Boolean): Char {
        val candidates = when (predefined) {
            PredefinedClass.DIGIT -> "0123456789"
            PredefinedClass.NON_DIGIT -> "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
            PredefinedClass.WORD -> "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_"
            PredefinedClass.NON_WORD -> " .!@#$%^&*()_+-=[]{}|;':\",/<>?`~"
            PredefinedClass.WHITESPACE -> " \t\n\r"
            PredefinedClass.NON_WHITESPACE -> "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            PredefinedClass.ANY -> "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        }
        return pickRandom(candidates, negated)
    }
    
    private fun pickFromRanges(ranges: List<RegexCharRange>, negated: Boolean): Char {
        val allChars = ranges.flatMap { range -> 
            (range.start..range.end).toList()
        }
        return pickRandom(allChars.joinToString(""), negated)
    }
    
    private fun pickRandom(source: String, negated: Boolean): Char {
        if (source.isEmpty()) return 'a'
        return if (negated) {
            // 排除常见字符，返回一个不太常用的字符
            val excluded = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 "
            val available = source.filter { it !in excluded }
            if (available.isNotEmpty()) available.random() else '@'
        } else {
            source.random()
        }
    }
    
    private fun generateQuantifier(node: Quantifier): String {
        val min = node.min
        val max = node.max ?: minOf(min + 3, 10) // 默认最多重复 10 次
        
        val times = min + (0 until maxOf(1, max - min + 1)).random()
        val sb = StringBuilder()
        
        for (i in 0 until times) {
            sb.append(generateFromNode(node.child))
        }
        
        return sb.toString()
    }
    
    private fun generateGroup(node: Group): String {
        return node.children.joinToString("") { generateFromNode(it) }
    }
    
    private fun generateAlternation(node: Alternation): String {
        if (node.alternatives.isEmpty()) return ""
        // 随机选择一个分支
        val choice = node.alternatives.random()
        return generateFromNode(choice)
    }
    
    private fun generateSequence(node: Sequence): String {
        return node.children.joinToString("") { generateFromNode(it) }
    }
    
    private fun generateBackReference(node: BackReference): String {
        // 回溯引用无法独立生成，返回通用占位符
        return "[REF]"
    }
    
    private fun generateEscape(node: Escape): String {
        return when (node.type) {
            EscapeType.N -> "\n"
            EscapeType.R -> "\r"
            EscapeType.T -> "\t"
            EscapeType.F -> "\u000C"
            EscapeType.BACKSLASH -> "\\"
            EscapeType.DOT -> "."
            EscapeType.STAR -> "*"
            EscapeType.PLUS -> "+"
            EscapeType.QUESTION -> "?"
            EscapeType.OCTAL -> "\u0000"
            EscapeType.HEX -> node.value ?: "\u0000"
            EscapeType.UNICODE -> node.value ?: "?"
        }
    }
}

/**
 * 便捷的测试字符串生成函数
 */
fun generateTestString(pattern: String, maxLength: Int = 100): GenerationResult {
    return TestStringGenerator(maxLength).generate(pattern)
}
