package com.github.youjiyun1123.youregexinterpreter.engine.multilang

import com.github.youjiyun1123.youregexinterpreter.core.model.*
import com.github.youjiyun1123.youregexinterpreter.engine.RegexEngine
import com.github.youjiyun1123.youregexinterpreter.engine.JavaRegexEngine

/**
 * Python 正则引擎接口
 * 注意: 在 IntelliJ 插件环境中，Python 引擎仅用于语法转换和解释
 * 实际匹配仍由 Java 正则引擎执行
 */
class PythonRegexEngine : RegexEngine {
    override val language: RegexLanguage = RegexLanguage.PYTHON
    
    override fun validate(pattern: String): ValidationResult {
        val converted = convertToJava(pattern)
        return try {
            java.util.regex.Pattern.compile(converted)
            ValidationResult(isValid = true)
        } catch (e: java.util.regex.PatternSyntaxException) {
            ValidationResult(
                isValid = false,
                error = RegexError(
                    message = e.description ?: "Invalid pattern",
                    position = e.index,
                    hint = "Python regex syntax may differ from Java"
                )
            )
        }
    }
    
    override fun match(pattern: String, input: CharSequence, flags: Set<RegexFlag>): List<MatchResult> {
        val converted = convertToJava(pattern)
        val javaFlags = flags.mapNotNull { it.toPythonFlag() }.toSet()
        val engine = JavaRegexEngine()
        return engine.match(converted, input, javaFlags)
    }
    
    override fun findAll(pattern: String, input: CharSequence, flags: Set<RegexFlag>): List<MatchResult> {
        val converted = convertToJava(pattern)
        val javaFlags = flags.mapNotNull { it.toPythonFlag() }.toSet()
        val engine = JavaRegexEngine()
        return engine.findAll(converted, input, javaFlags)
    }
    
    /**
     * 将 Python 正则转换为 Java 正则
     */
    private fun convertToJava(pattern: String): String {
        var result = pattern
        
        // Python 特有的命名组格式: (?P<name>...) -> (?<name>...)
        val namedGroupRegex = Regex("""\(\?P<([^>]+)>""")
        result = namedGroupRegex.replace(result) { 
            "(?<${it.groupValues[1]}>" 
        }
        
        // 命名组回溯引用: (?P=name) -> \k<name>
        val namedRefRegex = Regex("""\(\?P=([^)]+)\)""")
        result = namedRefRegex.replace(result) {
            "\\k<${it.groupValues[1]}>"
        }
        
        return result
    }
    
    private fun RegexFlag.toPythonFlag(): RegexFlag? {
        return when (this) {
            RegexFlag.CASE_INSENSITIVE -> RegexFlag.CASE_INSENSITIVE
            RegexFlag.MULTILINE -> RegexFlag.MULTILINE
            RegexFlag.DOTALL -> RegexFlag.DOTALL
            RegexFlag.UNICODE_CASE -> RegexFlag.UNICODE_CASE
            else -> null
        }
    }
    
    /**
     * 获取 Python 特定的语法说明
     */
    fun getPythonSyntaxNotes(): String {
        return """
            Python re 模块正则表达式说明：
            
            1. 基础语法
               - r'pattern' 原始字符串（推荐）
               - re.compile(pattern, flags)
            
            2. 支持的特性
               - 捕获组: (...) - 支持回溯引用
               - 非捕获组: (?:...) 
               - 命名捕获组: (?P<name>...) 或 (?<name>...)
               - 前瞻断言: (?=...), (?!...)
               - 后顾断言: (?<=...), (?<!...)
               - 条件匹配: (?(group)yes|no)
               
            3. Python 特有的语法
               - (?P<name>...) 命名组（兼容旧版）
               - (?P=name) 命名组回溯引用
               - \number 引用（最多 99 个组）
               
            4. 与 Java 的差异
               - 命名组语法略有不同
               - Python 支持更多高级特性如条件匹配
               - Unicode 标志行为可能不同
        """.trimIndent()
    }
}
