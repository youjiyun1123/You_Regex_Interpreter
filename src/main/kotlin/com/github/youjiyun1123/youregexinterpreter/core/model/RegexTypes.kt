package com.github.youjiyun1123.youregexinterpreter.core.model

/**
 * 正则表达式的支持语言
 */
enum class RegexLanguage {
    JAVA, JAVASCRIPT, PYTHON
}

/**
 * 正则表达式标志位
 */
enum class RegexFlag {
    CASE_INSENSITIVE, MULTILINE, DOTALL, UNICODE_CASE, CANON_EQ, LITERAL, UNICODE
}

/**
 * 编译后的正则表达式模式
 */
data class CompiledPattern(
    val pattern: String,
    val flags: Set<RegexFlag> = emptySet(),
    val language: RegexLanguage = RegexLanguage.JAVA
)

/**
 * 匹配结果
 */
data class MatchResult(
    val value: String,
    val range: IntRange,
    val groups: List<GroupMatch>
)

/**
 * 捕获组匹配
 */
data class GroupMatch(
    val index: Int,
    val name: String?,
    val value: String?,
    val range: IntRange?
)

/**
 * 验证结果
 */
data class ValidationResult(
    val isValid: Boolean,
    val error: RegexError? = null
)

/**
 * 正则表达式错误
 */
data class RegexError(
    val message: String,
    val position: Int,
    val length: Int = 1,
    val hint: String? = null
)
