package com.github.youjiyun1123.youregexinterpreter.core

import com.github.youjiyun1123.youregexinterpreter.core.model.MatchResult
import com.github.youjiyun1123.youregexinterpreter.core.model.RegexFlag
import com.github.youjiyun1123.youregexinterpreter.core.parser.RegexParserFacade
import com.github.youjiyun1123.youregexinterpreter.engine.JavaRegexEngine

/**
 * 性能配置
 */
object PerformanceConfig {
    // 匹配结果数量限制
    const val MAX_MATCH_RESULTS = 1000
    
    // 测试字符串长度限制
    const val MAX_INPUT_LENGTH = 100_000 // 100KB
    
    // 测试字符串警告阈值
    const val INPUT_WARNING_THRESHOLD = 10_000 // 10KB
    
    // 防抖延迟（毫秒）
    const val DEBOUNCE_DELAY_SHORT = 0L
    const val DEBOUNCE_DELAY_LONG = 150L
    
    // 长正则表达式阈值
    const val LONG_PATTERN_THRESHOLD = 200
    
    // 缓存大小限制
    const val MAX_CACHE_SIZE = 100
}

/**
 * 性能监控器
 */
class PerformanceMonitor {
    
    private var lastParseTime: Long = 0
    private var lastMatchTime: Long = 0
    private var lastInputLength: Int = 0
    private var lastMatchCount: Int = 0
    
    val parseTimeMs: Long get() = lastParseTime
    val matchTimeMs: Long get() = lastMatchTime
    val inputLength: Int get() = lastInputLength
    val matchCount: Int get() = lastMatchCount
    
    fun recordParse(timeNs: Long) {
        lastParseTime = timeNs / 1_000_000 // 转换为毫秒
    }
    
    fun recordMatch(timeNs: Long, inputLen: Int, matchCount: Int) {
        lastMatchTime = timeNs / 1_000_000
        lastInputLength = inputLen
        lastMatchCount = matchCount
    }
    
    fun getStatus(): PerformanceStatus {
        return when {
            lastMatchTime > 1000 -> PerformanceStatus.SLOW
            lastMatchTime > 100 -> PerformanceStatus.MODERATE
            else -> PerformanceStatus.GOOD
        }
    }
}

enum class PerformanceStatus {
    GOOD,      // < 100ms
    MODERATE,  // 100-1000ms
    SLOW       // > 1000ms
}

/**
 * 输入验证器
 */
object InputValidator {
    
    data class ValidationResult(
        val isValid: Boolean,
        val warning: String? = null,
        val error: String? = null
    )
    
    fun validateInput(input: String): ValidationResult {
        // 检查长度
        when {
            input.length > PerformanceConfig.MAX_INPUT_LENGTH -> {
                return ValidationResult(
                    isValid = false,
                    error = "输入过长（最大 ${PerformanceConfig.MAX_INPUT_LENGTH / 1024}KB）"
                )
            }
            input.length > PerformanceConfig.INPUT_WARNING_THRESHOLD -> {
                return ValidationResult(
                    isValid = true,
                    warning = "输入较长（${input.length / 1024}KB），可能导致性能下降"
                )
            }
        }
        
        return ValidationResult(isValid = true)
    }
    
    fun validatePattern(pattern: String): ValidationResult {
        if (pattern.isEmpty()) {
            return ValidationResult(isValid = false, error = "正则表达式不能为空")
        }
        
        // 检查常见错误模式
        if (pattern == ".*") {
            return ValidationResult(
                isValid = true,
                warning = "通配符模式可能匹配过多内容"
            )
        }
        
        if (pattern.contains(".*.*")) {
            return ValidationResult(
                isValid = true,
                warning = "嵌套通配符可能导致性能问题"
            )
        }
        
        return ValidationResult(isValid = true)
    }
}

/**
 * 结果限制器
 */
object ResultLimiter {
    
    /**
     * 限制匹配结果数量
     */
    fun limitMatches(matches: List<MatchResult>, maxResults: Int = PerformanceConfig.MAX_MATCH_RESULTS): List<MatchResult> {
        return if (matches.size > maxResults) {
            matches.take(maxResults)
        } else {
            matches
        }
    }
    
    /**
     * 截断过长输入
     */
    fun truncateInput(input: String, maxLength: Int = PerformanceConfig.MAX_INPUT_LENGTH): String {
        return if (input.length > maxLength) {
            input.take(maxLength)
        } else {
            input
        }
    }
}
