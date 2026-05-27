package com.github.youjiyun1123.youregexinterpreter.engine

import com.github.youjiyun1123.youregexinterpreter.core.model.*

/**
 * 正则表达式引擎接口
 */
interface RegexEngine {
    val language: RegexLanguage
    
    /**
     * 验证正则表达式是否有效
     */
    fun validate(pattern: String): ValidationResult
    
    /**
     * 匹配整个输入
     */
    fun match(pattern: String, input: CharSequence, flags: Set<RegexFlag> = emptySet()): List<MatchResult>
    
    /**
     * 查找所有匹配
     */
    fun findAll(pattern: String, input: CharSequence, flags: Set<RegexFlag> = emptySet()): List<MatchResult>
}
