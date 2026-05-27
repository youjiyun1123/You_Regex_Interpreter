package com.github.youjiyun1123.youregexinterpreter.engine

import com.github.youjiyun1123.youregexinterpreter.core.model.*
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Java 正则表达式引擎实现
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
                    hint = "Check the syntax at position ${e.index}"
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
                listOf(createMatchResult(m))
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
                results.add(createMatchResult(m))
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun createMatchResult(m: java.util.regex.Matcher): MatchResult {
        val groups = mutableListOf<GroupMatch>()
        
        groups.add(GroupMatch(
            index = 0, name = null, value = m.group(),
            range = m.start()..m.end()
        ))
        
        for (i in 1..m.groupCount()) {
            val groupValue = m.group(i)
            groups.add(GroupMatch(
                index = i, name = null, value = groupValue,
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
}

/**
 * 正则引擎注册表
 */
object RegexEngineRegistry {
    private val engines = mutableMapOf<RegexLanguage, RegexEngine>()
    
    init {
        engines[RegexLanguage.JAVA] = JavaRegexEngine()
    }
    
    fun getEngine(language: RegexLanguage): RegexEngine = 
        engines[language] ?: throw IllegalArgumentException("Unsupported: $language")
    
    fun getDefaultEngine(): RegexEngine = engines[RegexLanguage.JAVA]!!
}
