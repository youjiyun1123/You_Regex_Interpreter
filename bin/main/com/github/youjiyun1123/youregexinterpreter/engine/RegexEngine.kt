package com.github.youjiyun1123.youregexinterpreter.engine

import com.github.youjiyun1123.youregexinterpreter.core.model.*

/**
 * 正则表达式引擎接口
 */
interface RegexEngine {
    val language: RegexLanguage
    fun validate(pattern: String): ValidationResult
    fun match(pattern: String, input: CharSequence, flags: Set<RegexFlag> = emptySet()): List<MatchResult>
    fun findAll(pattern: String, input: CharSequence, flags: Set<RegexFlag> = emptySet()): List<MatchResult>
}
