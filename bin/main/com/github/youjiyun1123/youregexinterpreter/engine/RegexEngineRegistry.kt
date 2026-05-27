package com.github.youjiyun1123.youregexinterpreter.engine

import com.github.youjiyun1123.youregexinterpreter.core.model.RegexLanguage

/**
 * 正则引擎注册表
 */
object RegexEngineRegistry {
    
    private val engines = mutableMapOf<RegexLanguage, RegexEngine>()
    
    init {
        engines[RegexLanguage.JAVA] = JavaRegexEngine()
        engines[RegexLanguage.JAVASCRIPT] = com.github.youjiyun1123.youregexinterpreter.engine.multilang.JavaScriptRegexEngine()
        engines[RegexLanguage.PYTHON] = com.github.youjiyun1123.youregexinterpreter.engine.multilang.PythonRegexEngine()
    }
    
    fun getEngine(language: RegexLanguage): RegexEngine {
        return engines[language] 
            ?: throw IllegalArgumentException("Unsupported language: $language")
    }
    
    fun getDefaultEngine(): RegexEngine = engines[RegexLanguage.JAVA]!!
    
    fun getAvailableLanguages(): List<RegexLanguage> = engines.keys.toList()
    
    fun getLanguageDisplayName(language: RegexLanguage): String {
        return when (language) {
            RegexLanguage.JAVA -> "Java"
            RegexLanguage.JAVASCRIPT -> "JavaScript"
            RegexLanguage.PYTHON -> "Python"
        }
    }
}
