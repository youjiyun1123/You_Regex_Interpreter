package com.github.youjiyun1123.youregexinterpreter.ui.viewmodel

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
 */
class RegexViewModel {
    
    private val engine = JavaRegexEngine()
    private val interpreter = NaturalLanguageInterpreter()
    
    // State
    var pattern: String = ""
        private set
    var testInput: String = ""
        private set
    var flags: Set<RegexFlag> = emptySet()
        private set
    var showDetailedExplanation: Boolean = false
        private set
    
    // Listeners
    private val listeners = mutableListOf<ViewModelListener>()
    
    // Debounce
    private var debounceTimer: Timer? = null
    private val debounceDelay = 150L
    
    /**
     * 更新正则表达式
     */
    fun updatePattern(newPattern: String) {
        if (pattern == newPattern) return
        pattern = newPattern
        scheduleUpdate()
    }
    
    /**
     * 更新测试字符串
     */
    fun updateTestInput(newInput: String) {
        if (testInput == newInput) return
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
        
        val delay = if (pattern.length >= 200) debounceDelay else 0
        
        debounceTimer = Timer()
        debounceTimer?.schedule(object : TimerTask() {
            override fun run() {
                SwingUtilities.invokeLater {
                    notifyListeners()
                }
            }
        }, delay)
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
        return if (pattern.isEmpty()) {
            ViewState.Empty
        } else {
            val errors = RegexParserFacade.validate(pattern)
            val parseResult = RegexParserFacade.parse(pattern)
            
            if (errors.isNotEmpty()) {
                ViewState.Error(errors)
            } else {
                val explanation = if (showDetailedExplanation && parseResult.syntaxTree != null) {
                    interpreter.interpretDetailed(parseResult.syntaxTree)
                } else if (parseResult.syntaxTree != null) {
                    interpreter.interpret(parseResult.syntaxTree)
                } else {
                    ""
                }
                
                val structure = if (parseResult.syntaxTree != null) {
                    StructureAnalyzer.analyze(parseResult.syntaxTree)
                } else {
                    ""
                }
                
                val matches = try {
                    engine.findAll(pattern, testInput, flags)
                } catch (e: Exception) {
                    emptyList()
                }
                
                ViewState.Success(
                    explanation = explanation,
                    structure = structure,
                    matches = matches,
                    warnings = parseResult.warnings
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
        val warnings: List<String> = emptyList()
    ) : ViewState()
}

/**
 * ViewModel 监听器
 */
interface ViewModelListener {
    fun onStateChanged(state: ViewState)
}
