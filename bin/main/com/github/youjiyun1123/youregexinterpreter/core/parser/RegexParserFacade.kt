package com.github.youjiyun1123.youregexinterpreter.core.parser

import com.github.youjiyun1123.youregexinterpreter.core.model.RegexNode

/**
 * 统一的解析结果，包含语法树、错误和警告
 */
data class RegexParseResult(
    val isSuccess: Boolean,
    val syntaxTree: RegexNode?,
    val errors: List<EnhancedParseError> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    companion object {
        fun success(node: RegexNode): RegexParseResult {
            return RegexParseResult(
                isSuccess = true,
                syntaxTree = node,
                errors = emptyList()
            )
        }
        
        fun failure(errors: List<EnhancedParseError>): RegexParseResult {
            return RegexParseResult(
                isSuccess = false,
                syntaxTree = null,
                errors = errors
            )
        }
    }
}

/**
 * 解析器结果封装
 */
object RegexParserFacade {
    
    /**
     * 解析正则表达式，返回统一的结果
     */
    fun parse(pattern: String): RegexParseResult {
        // 首先进行语法错误检测
        val errors = SyntaxErrorDetector.detect(pattern)
        
        // 如果有错误，直接返回
        if (errors.isNotEmpty()) {
            return RegexParseResult.failure(errors)
        }
        
        // 进行解析
        val parser = RegexParser(pattern)
        val result = parser.parse()
        
        return when (result) {
            is ParseResult.Success -> RegexParseResult.success(result.root)
            is ParseResult.Failure -> RegexParseResult.failure(
                listOf(
                    EnhancedParseError(
                        message = result.error.message,
                        position = result.error.position,
                        hint = result.error.hint
                    )
                )
            )
        }
    }
    
    /**
     * 仅检测错误，不解析
     */
    fun validate(pattern: String): List<EnhancedParseError> {
        return SyntaxErrorDetector.detect(pattern)
    }
    
    /**
     * 检查是否有效
     */
    fun isValid(pattern: String): Boolean {
        return SyntaxErrorDetector.detect(pattern).isEmpty()
    }
}
