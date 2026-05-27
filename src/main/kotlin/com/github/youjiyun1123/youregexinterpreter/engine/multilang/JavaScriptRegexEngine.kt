package com.github.youjiyun1123.youregexinterpreter.engine.multilang

import com.github.youjiyun1123.youregexinterpreter.core.model.*
import com.github.youjiyun1123.youregexinterpreter.engine.RegexEngine
import com.github.youjiyun1123.youregexinterpreter.engine.JavaRegexEngine

/**
 * JavaScript 正则引擎接口
 * 注意: 在 IntelliJ 插件环境中，JavaScript 引擎仅用于语法转换和解释
 * 实际匹配仍由 Java 正则引擎执行
 */
class JavaScriptRegexEngine : RegexEngine {
    override val language: RegexLanguage = RegexLanguage.JAVASCRIPT
    
    override fun validate(pattern: String): ValidationResult {
        // 转换为 Java 语法进行验证
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
                    hint = "JavaScript regex syntax differs from Java"
                )
            )
        }
    }
    
    override fun match(pattern: String, input: CharSequence, flags: Set<RegexFlag>): List<MatchResult> {
        // JavaScript 模式使用 Java 引擎执行
        val converted = convertToJava(pattern)
        val javaFlags = flags.mapNotNull { it.toJavaScriptFlag() }.toSet()
        val engine = JavaRegexEngine()
        return engine.match(converted, input, javaFlags)
    }
    
    override fun findAll(pattern: String, input: CharSequence, flags: Set<RegexFlag>): List<MatchResult> {
        val converted = convertToJava(pattern)
        val javaFlags = flags.mapNotNull { it.toJavaScriptFlag() }.toSet()
        val engine = JavaRegexEngine()
        return engine.findAll(converted, input, javaFlags)
    }
    
    /**
     * 将 JavaScript 正则转换为 Java 正则
     */
    private fun convertToJava(pattern: String): String {
        var result = pattern
        
        // JavaScript 特有的转义处理
        // \d, \w, \s 等在两者中相同
        // \b 在 JavaScript 中是字边界，但行为略有不同
        
        // 命名捕获组: (?<name>...) -> (?<name>...) (Java 也支持)
        
        // 断言处理
        // (?=...) 正向前瞻 - Java 和 JS 都支持
        // (?!...) 负向前瞻 - Java 和 JS 都支持
        // (?<=...) 正向后顾 - Java 7+ 支持
        // (?<!...) 负向后顾 - Java 7+ 支持
        
        return result
    }
    
    private fun RegexFlag.toJavaScriptFlag(): RegexFlag? {
        return when (this) {
            RegexFlag.CASE_INSENSITIVE -> RegexFlag.CASE_INSENSITIVE
            RegexFlag.MULTILINE -> RegexFlag.MULTILINE
            RegexFlag.DOTALL -> RegexFlag.DOTALL
            else -> null // JavaScript 不支持的其他标志
        }
    }
    
    /**
     * 获取 JavaScript 特定的语法说明
     */
    fun getJavaScriptSyntaxNotes(): String {
        return """
            JavaScript 正则表达式说明：
            
            1. 基础语法
               - /pattern/flags 格式（但在转换时使用纯字符串）
               - 支持 g (全局), i (忽略大小写), m (多行)
            
            2. 支持的特性
               - 捕获组: (...) - 支持回溯引用
               - 非捕获组: (?:...) 
               - 命名捕获组: (?<name>...) - ES2018+
               - 前瞻断言: (?=...), (?!...)
               - 后顾断言: (?<=...), (?<!...) - ES2018+
               
            3. 不支持的特性
               - 占有量词 (possessive): a*+
               - 原子组 (atomic): (?>...)
               - 条件匹配: (?(1)yes|no)
            
            4. 与 Java 的差异
               - JavaScript 的 \b 是字边界，与 Java 相同
               - Unicode 属性转义: \p{...} - Java 不支持
               - 标志位名称不同但概念相似
        """.trimIndent()
    }
}
