package com.github.youjiyun1123.youregexinterpreter.ui.viewmodel

import com.github.youjiyun1123.youregexinterpreter.core.PerformanceConfig
import com.github.youjiyun1123.youregexinterpreter.core.PerformanceMonitor
import com.github.youjiyun1123.youregexinterpreter.core.PerformanceStatus
import com.github.youjiyun1123.youregexinterpreter.core.InputValidator
import com.github.youjiyun1123.youregexinterpreter.core.ResultLimiter
import com.github.youjiyun1123.youregexinterpreter.core.interpreter.NaturalLanguageInterpreter
import com.github.youjiyun1123.youregexinterpreter.core.interpreter.StructureAnalyzer
import com.github.youjiyun1123.youregexinterpreter.core.model.MatchResult
import com.github.youjiyun1123.youregexinterpreter.core.model.RegexFlag
import com.github.youjiyun1123.youregexinterpreter.core.parser.EnhancedParseError
import com.github.youjiyun1123.youregexinterpreter.core.parser.RegexParserFacade
import com.github.youjiyun1123.youregexinterpreter.engine.JavaRegexEngine
import java.util.Timer
import java.util.TimerTask
import javax.swing.SwingUtilities

/**
 * Regex ViewModel - 管理正则表达式工具窗口的状态
 * 集成了性能监控和边界处理
 */
class RegexViewModel {
    
    private val engine = JavaRegexEngine()
    private val interpreter = NaturalLanguageInterpreter()
    private val performanceMonitor = PerformanceMonitor()
    
    // State
    var pattern: String = ""
        private set
    var testInput: String = ""
        private set
    var flags: Set<RegexFlag> = emptySet()
        private set
    var showDetailedExplanation: Boolean = false
        private set
    
    // Validation warnings
    private var inputWarning: String? = null
    private var patternWarning: String? = null
    
    // Listeners
    private val listeners = mutableListOf<ViewModelListener>()
    
    // Debounce
    private var debounceTimer: Timer? = null
    
    // 防抖延迟（根据配置）
    private val debounceDelay: Long
        get() = if (pattern.length >= PerformanceConfig.LONG_PATTERN_THRESHOLD) {
            PerformanceConfig.DEBOUNCE_DELAY_LONG
        } else {
            PerformanceConfig.DEBOUNCE_DELAY_SHORT
        }
    
    /**
     * 获取性能监控器
     */
    fun getPerformanceMonitor(): PerformanceMonitor = performanceMonitor
    
    /**
     * 更新正则表达式
     */
    fun updatePattern(newPattern: String) {
        if (pattern == newPattern) return
        pattern = newPattern
        
        // 验证模式
        val validation = InputValidator.validatePattern(newPattern)
        patternWarning = validation.warning
        
        scheduleUpdate()
    }
    
    /**
     * 更新测试字符串
     */
    fun updateTestInput(newInput: String) {
        if (testInput == newInput) return
        
        // 验证输入
        val validation = InputValidator.validateInput(newInput)
        if (!validation.isValid) {
            inputWarning = validation.error
            notifyListeners()
            return
        }
        inputWarning = validation.warning
        
        testInput = newInput
        scheduleUpdate()
    }
    
    /**
     * 更新标志位
     */
    fun updateFlags(newFlags: Set<RegexFlag>) {
        if (flags == newFlags) return
        flags = newFlags
        scheduleUpdate()
    }
    
    /**
     * 切换详细解释
     */
    fun toggleDetailedExplanation() {
        showDetailedExplanation = !showDetailedExplanation
        notifyListeners()
    }
    
    /**
     * 立即更新（无防抖）
     */
    fun forceUpdate() {
        debounceTimer?.cancel()
        notifyListeners()
    }
    
    private fun scheduleUpdate() {
        debounceTimer?.cancel()
        
        debounceTimer = Timer()
        debounceTimer?.schedule(object : TimerTask() {
            override fun run() {
                SwingUtilities.invokeLater {
                    notifyListeners()
                }
            }
        }, debounceDelay)
    }
    
    fun addListener(listener: ViewModelListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: ViewModelListener) {
        listeners.remove(listener)
    }
    
    private fun notifyListeners() {
        val state = computeState()
        listeners.forEach { it.onStateChanged(state) }
    }
    
    private fun computeState(): ViewState {
        // 收集所有警告
        val allWarnings = mutableListOf<String>()
        inputWarning?.let { allWarnings.add(it) }
        patternWarning?.let { allWarnings.add(it) }
        
        return if (pattern.isEmpty()) {
            ViewState.Empty
        } else {
            val errors = RegexParserFacade.validate(pattern)
            val parseResult = RegexParserFacade.parse(pattern)
            
            if (errors.isNotEmpty()) {
                ViewState.Error(errors)
            } else {
                // 性能计时：解析
                val parseStart = System.nanoTime()
                val syntaxTree = parseResult.syntaxTree
                val parseEnd = System.nanoTime()
                performanceMonitor.recordParse(parseEnd - parseStart)
                
                val explanation = if (showDetailedExplanation && syntaxTree != null) {
                    interpreter.interpretDetailed(syntaxTree)
                } else if (syntaxTree != null) {
                    interpreter.interpret(syntaxTree)
                } else {
                    ""
                }
                
                val structure = if (syntaxTree != null) {
                    StructureAnalyzer.analyze(syntaxTree)
                } else {
                    ""
                }
                
                // 性能计时：匹配
                val matchStart = System.nanoTime()
                val matches = try {
                    val rawMatches = engine.findAll(pattern, testInput, flags)
                    // 限制结果数量
                    ResultLimiter.limitMatches(rawMatches)
                } catch (e: Exception) {
                    emptyList()
                }
                val matchEnd = System.nanoTime()
                performanceMonitor.recordMatch(
                    timeNs = matchEnd - matchStart,
                    inputLen = testInput.length,
                    matchCount = matches.size
                )
                
                // 添加性能警告
                val perfStatus = performanceMonitor.getStatus()
                if (perfStatus == PerformanceStatus.SLOW) {
                    allWarnings.add("性能较慢（${performanceMonitor.matchTimeMs}ms），考虑优化正则表达式")
                } else if (perfStatus == PerformanceStatus.MODERATE) {
                    allWarnings.add("匹配耗时较长（${performanceMonitor.matchTimeMs}ms）")
                }
                
                // 添加结果数量警告
                if (matches.size >= PerformanceConfig.MAX_MATCH_RESULTS) {
                    allWarnings.add("匹配结果已限制为 ${PerformanceConfig.MAX_MATCH_RESULTS} 条")
                }
                
                ViewState.Success(
                    explanation = explanation,
                    structure = structure,
                    matches = matches,
                    warnings = allWarnings,
                    parseTimeMs = performanceMonitor.parseTimeMs,
                    matchTimeMs = performanceMonitor.matchTimeMs
                )
            }
        }
    }
    
    /**
     * 销毁 ViewModel
     */
    fun dispose() {
        debounceTimer?.cancel()
        listeners.clear()
    }
}

/**
 * ViewModel 状态
 */
sealed class ViewState {
    object Empty : ViewState()
    data class Error(val errors: List<EnhancedParseError>) : ViewState()
    data class Success(
        val explanation: String,
        val structure: String,
        val matches: List<MatchResult>,
        val warnings: List<String> = emptyList(),
        val parseTimeMs: Long = 0,
        val matchTimeMs: Long = 0
    ) : ViewState()
}

/**
 * ViewModel 监听器
 */
interface ViewModelListener {
    fun onStateChanged(state: ViewState)
}
