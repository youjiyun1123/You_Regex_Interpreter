package com.github.youjiyun1123.youregexinterpreter.engine

import com.github.youjiyun1123.youregexinterpreter.core.model.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Java 正则表达式引擎实现
 * 支持 Java Pattern 的所有特性
 */
class JavaRegexEngine : RegexEngine {
    override val language: RegexLanguage = RegexLanguage.JAVA
    
    override fun validate(pattern: String): ValidationResult {
        return try {
            Pattern.compile(pattern)
            ValidationResult(isValid = true)
        } catch (e: PatternSyntaxException) {
            ValidationResult(
                isValid = false,
                error = RegexError(
                    message = e.description ?: "Invalid pattern",
                    position = e.index,
                    hint = getHintForError(e)
                )
            )
        }
    }
    
    override fun match(pattern: String, input: CharSequence, flags: Set<RegexFlag>): List<MatchResult> {
        return try {
            val javaFlags = flags.toJavaFlags()
            val p = Pattern.compile(pattern, javaFlags)
            val m = p.matcher(input)
            
            if (m.matches()) {
                listOf(createMatchResult(m, input.toString()))
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    override fun findAll(pattern: String, input: CharSequence, flags: Set<RegexFlag>): List<MatchResult> {
        return try {
            val javaFlags = flags.toJavaFlags()
            val p = Pattern.compile(pattern, javaFlags)
            val m = p.matcher(input)
            
            val results = mutableListOf<MatchResult>()
            while (m.find()) {
                results.add(createMatchResult(m, input.toString()))
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 替换匹配的内容
     */
    fun replace(pattern: String, input: CharSequence, replacement: String, flags: Set<RegexFlag> = emptySet()): String {
        return try {
            val javaFlags = flags.toJavaFlags()
            val p = Pattern.compile(pattern, javaFlags)
            p.matcher(input).replaceAll(replacement)
        } catch (e: Exception) {
            input.toString()
        }
    }
    
    /**
     * 分割字符串
     */
    fun split(pattern: String, input: CharSequence, flags: Set<RegexFlag> = emptySet()): List<String> {
        return try {
            val javaFlags = flags.toJavaFlags()
            val p = Pattern.compile(pattern, javaFlags)
            p.split(input).toList()
        } catch (e: Exception) {
            listOf(input.toString())
        }
    }
    
    /**
     * 获取匹配计数
     */
    fun countMatches(pattern: String, input: CharSequence, flags: Set<RegexFlag> = emptySet()): Int {
        return findAll(pattern, input, flags).size
    }
    
    /**
     * 检查是否完全匹配
     */
    fun isFullMatch(pattern: String, input: CharSequence, flags: Set<RegexFlag> = emptySet()): Boolean {
        return try {
            val javaFlags = flags.toJavaFlags()
            val p = Pattern.compile(pattern, javaFlags)
            p.matcher(input).matches()
        } catch (e: Exception) {
            false
        }
    }
    
    private fun createMatchResult(m: java.util.regex.Matcher, input: String): MatchResult {
        val groups = mutableListOf<GroupMatch>()
        
        // 整个匹配
        groups.add(GroupMatch(
            index = 0,
            name = null,
            value = m.group(),
            range = m.start()..m.end()
        ))
        
        // 捕获组
        for (i in 1..m.groupCount()) {
            val groupValue = m.group(i)
            groups.add(GroupMatch(
                index = i,
                name = null,
                value = groupValue,
                range = if (groupValue != null) m.start(i)..m.end(i) else null
            ))
        }
        
        return MatchResult(
            value = m.group(),
            range = m.start()..m.end(),
            groups = groups
        )
    }
    
    private fun Set<RegexFlag>.toJavaFlags(): Int {
        var flags = 0
        for (flag in this) {
            flags = flags or when (flag) {
                RegexFlag.CASE_INSENSITIVE -> Pattern.CASE_INSENSITIVE
                RegexFlag.MULTILINE -> Pattern.MULTILINE
                RegexFlag.DOTALL -> Pattern.DOTALL
                RegexFlag.UNICODE_CASE -> Pattern.UNICODE_CASE
                RegexFlag.CANON_EQ -> Pattern.CANON_EQ
                RegexFlag.LITERAL -> Pattern.LITERAL
                RegexFlag.UNICODE -> Pattern.UNICODE_CHARACTER_CLASS
            }
        }
        return flags
    }
    
    private fun getHintForError(e: PatternSyntaxException): String? {
        return when {
            e.description?.contains("Unclosed group") == true -> 
                "Check if all '(' have matching ')'"
            e.description?.contains("Unclosed character class") == true ->
                "Check if all '[' have matching ']'"
            e.description?.contains("Dangling") == true ->
                "This meta-character needs something before it, or should be escaped"
            else -> null
        }
    }
}
