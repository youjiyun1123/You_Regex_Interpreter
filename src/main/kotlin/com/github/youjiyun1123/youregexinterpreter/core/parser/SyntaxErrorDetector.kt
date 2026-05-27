package com.github.youjiyun1123.youregexinterpreter.core.parser

import com.github.youjiyun1123.youregexinterpreter.core.model.*

/**
 * 增强的解析错误信息
 */
data class EnhancedParseError(
    val message: String,
    val position: Int,
    val length: Int = 1,
    val hint: String? = null,
    val code: ErrorCode = ErrorCode.UNKNOWN
)

/**
 * 错误代码枚举
 */
enum class ErrorCode {
    UNKNOWN,
    UNCLOSED_GROUP,
    UNCLOSED_CHAR_CLASS,
    UNCLOSED_QUANTIFIER,
    INVALID_QUANTIFIER,
    ORPHAN_QUANTIFIER,
    INVALID_ESCAPE,
    INVALID_BACK_REFERENCE,
    UNEXPECTED_TOKEN,
    EMPTY_ALTERNATION,
    EMPTY_GROUP,
    RANGE_ORDER_ERROR,
    DUP_GROUP_NAME,
    INVALID_GROUP_NAME
}

/**
 * 语法错误检测器
 */
object SyntaxErrorDetector {
    
    /**
     * 检测常见语法错误
     */
    fun detect(pattern: String): List<EnhancedParseError> {
        val errors = mutableListOf<EnhancedParseError>()
        
        // 使用 Java Pattern 进行初步验证
        detectJavaPatternErrors(pattern, errors)
        
        // 自定义检测
        detectCustomErrors(pattern, errors)
        
        return errors.sortedBy { it.position }
    }
    
    private fun detectJavaPatternErrors(pattern: String, errors: MutableList<EnhancedParseError>) {
        try {
            java.util.regex.Pattern.compile(pattern)
        } catch (e: java.util.regex.PatternSyntaxException) {
            errors.add(
                EnhancedParseError(
                    message = e.description ?: "Invalid pattern",
                    position = e.index,
                    hint = getHintForError(e.description, e.index, pattern),
                    code = mapErrorCode(e.description)
                )
            )
        }
    }
    
    private fun detectCustomErrors(pattern: String, errors: MutableList<EnhancedParseError>) {
        // 检测孤立的量词
        detectOrphanQuantifiers(pattern, errors)
        
        // 检测未闭合的分组
        detectUnclosedGroups(pattern, errors)
        
        // 检测未闭合的字符类
        detectUnclosedCharClass(pattern, errors)
        
        // 检测空的选择
        detectEmptyAlternation(pattern, errors)
    }
    
    private fun detectOrphanQuantifiers(pattern: String, errors: MutableList<EnhancedParseError>) {
        var i = 0
        var inCharClass = false
        
        while (i < pattern.length) {
            val ch = pattern[i]
            
            // 处理字符类
            if (ch == '[' && !inCharClass && (i == 0 || pattern[i - 1] != '\\')) {
                inCharClass = true
                i++
                continue
            }
            if (ch == ']' && inCharClass && (i == 0 || pattern[i - 1] != '\\')) {
                inCharClass = false
                i++
                continue
            }
            
            if (inCharClass) {
                i++
                continue
            }
            
            // 跳过转义字符
            if (ch == '\\' && i + 1 < pattern.length) {
                i += 2
                continue
            }
            
            // 检测量词（只在开头或分组/选择符之后）
            if (ch in "*+?" && (i == 0 || pattern[i - 1] in "(|")) {
                // 但要跳过前瞻/后顾断言 (?=, ?!, ?<=, ?<!
                if (ch == '?' && i > 0 && pattern[i - 1] == '(') {
                    i++
                    continue
                }
                
                errors.add(
                    EnhancedParseError(
                        message = "Quantifier '$ch' has nothing to repeat",
                        position = i,
                        hint = "Add a character, group, or character class before the quantifier",
                        code = ErrorCode.ORPHAN_QUANTIFIER
                    )
                )
            }
            
            i++
        }
        
        // 检测 {n,m} 格式错误
        val rangeRegex = Regex("""\{(\d+)?,(\d+)?\}""")
        rangeRegex.findAll(pattern).forEach { match ->
            val content = match.value
            val commaIdx = content.indexOf(',')
            val minStr = content.substring(1, commaIdx)
            val maxStr = content.substring(commaIdx + 1, content.length - 1)
            
            val min = minStr.toIntOrNull()
            val max = maxStr.toIntOrNull()
            
            when {
                min == null && minStr.isNotEmpty() -> {
                    errors.add(
                        EnhancedParseError(
                            message = "Invalid quantifier range: missing number before comma",
                            position = match.range.first,
                            length = match.value.length,
                            code = ErrorCode.INVALID_QUANTIFIER
                        )
                    )
                }
                min != null && max != null && min > max -> {
                    errors.add(
                        EnhancedParseError(
                            message = "Invalid quantifier range: $min > $max",
                            position = match.range.first,
                            length = match.value.length,
                            hint = "The minimum value must be less than or equal to the maximum",
                            code = ErrorCode.INVALID_QUANTIFIER
                        )
                    )
                }
            }
        }
    }
    
    private fun detectUnclosedGroups(pattern: String, errors: MutableList<EnhancedParseError>) {
        var depth = 0
        var lastOpenPos = -1
        
        for (i in pattern.indices) {
            if (pattern[i] == '(' && (i == 0 || pattern[i - 1] != '\\')) {
                depth++
                if (lastOpenPos == -1) lastOpenPos = i
            } else if (pattern[i] == ')' && (i == 0 || pattern[i - 1] != '\\')) {
                depth--
                if (depth < 0) {
                    errors.add(
                        EnhancedParseError(
                            message = "Unmatched closing parenthesis ')'",
                            position = i,
                            hint = "Remove this ')' or add a matching '(' before it",
                            code = ErrorCode.UNCLOSED_GROUP
                        )
                    )
                    depth = 0
                }
            }
        }
        
        if (depth > 0 && lastOpenPos != -1) {
            errors.add(
                EnhancedParseError(
                    message = "Unclosed group starting at position $lastOpenPos",
                    position = lastOpenPos,
                    hint = "Add ')' to close the group",
                    code = ErrorCode.UNCLOSED_GROUP
                )
            )
        }
    }
    
    private fun detectUnclosedCharClass(pattern: String, errors: MutableList<EnhancedParseError>) {
        var inCharClass = false
        var charClassStart = -1
        
        for (i in pattern.indices) {
            if (pattern[i] == '[' && (i == 0 || pattern[i - 1] != '\\')) {
                inCharClass = true
                charClassStart = i
            } else if (pattern[i] == ']' && (i == 0 || pattern[i - 1] != '\\') && inCharClass) {
                inCharClass = false
            }
        }
        
        if (inCharClass && charClassStart != -1) {
            errors.add(
                EnhancedParseError(
                    message = "Unclosed character class '[...''",
                    position = charClassStart,
                    hint = "Add ']' to close the character class",
                    code = ErrorCode.UNCLOSED_CHAR_CLASS
                )
            )
        }
    }
    
    private fun detectEmptyAlternation(pattern: String, errors: MutableList<EnhancedParseError>) {
        val regex = Regex("""\|\||^\||\|$""")
        regex.findAll(pattern).forEach { match ->
            errors.add(
                EnhancedParseError(
                    message = "Empty alternation",
                    position = match.range.first,
                    hint = if (match.value == "||") "Remove one of the '|' characters"
                           else "Remove the leading or trailing '|'",
                    code = ErrorCode.EMPTY_ALTERNATION
                )
            )
        }
    }
    
    private fun getHintForError(description: String?, position: Int, pattern: String): String? {
        if (description == null) return null
        
        return when {
            description.contains("Unclosed") && description.contains("group") -> 
                "Check if all '(' have matching ')'"
            description.contains("Unclosed") && description.contains("character class") ->
                "Check if all '[' have matching ']'"
            description.contains("Quantifier") && description.contains("nothing") ->
                "Quantifiers (*+?) must follow an atom (character, group, or character class)"
            description.contains("Dangling") -> 
                "Remove the special character or escape it with \\"
            description.contains("Unmatched") -> 
                "Check matching brackets and parentheses"
            else -> null
        }
    }
    
    private fun mapErrorCode(description: String?): ErrorCode {
        if (description == null) return ErrorCode.UNKNOWN
        
        return when {
            description.contains("Unclosed") && description.contains("group") -> ErrorCode.UNCLOSED_GROUP
            description.contains("Unclosed") && description.contains("character class") -> ErrorCode.UNCLOSED_CHAR_CLASS
            description.contains("Quantifier") && description.contains("nothing") -> ErrorCode.ORPHAN_QUANTIFIER
            description.contains("Dangling") -> ErrorCode.UNEXPECTED_TOKEN
            else -> ErrorCode.UNKNOWN
        }
    }
    
    /**
     * 生成格式化错误消息
     */
    fun formatError(error: EnhancedParseError, pattern: String): String {
        val builder = StringBuilder()
        builder.appendLine("Error at position ${error.position}: ${error.message}")
        
        // 显示错误上下文
        if (error.position in pattern.indices) {
            val start = maxOf(0, error.position - 10)
            val end = minOf(pattern.length, error.position + error.length + 10)
            val context = pattern.substring(start, end)
            val pointer = " ".repeat(maxOf(0, error.position - start)) + "^"
            
            builder.appendLine("  Pattern: ...$context...")
            builder.appendLine("           $pointer")
        }
        
        if (error.hint != null) {
            builder.appendLine("  Hint: ${error.hint}")
        }
        
        return builder.toString()
    }
}
