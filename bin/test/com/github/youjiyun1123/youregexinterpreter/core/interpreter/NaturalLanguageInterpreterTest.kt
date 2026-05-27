package com.github.youjiyun1123.youregexinterpreter.core.interpreter

import com.github.youjiyun1123.youregexinterpreter.core.model.*
import com.github.youjiyun1123.youregexinterpreter.core.parser.RegexParserFacade
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class NaturalLanguageInterpreterTest {

    private val interpreter = NaturalLanguageInterpreter()

    private fun interpret(pattern: String): String {
        val result = RegexParserFacade.parse(pattern)
        return if (result.isSuccess && result.syntaxTree != null) {
            interpreter.interpret(result.syntaxTree)
        } else {
            "Error: ${result.errors}"
        }
    }

    @Nested
    @DisplayName("字面量解释")
    inner class LiteralInterpretationTests {
        
        @Test
        @DisplayName("简单字符")
        fun simpleLiteral() {
            val explanation = interpret("abc")
            assertThat(explanation).contains("匹配")
        }
        
        @Test
        @DisplayName("转义字符")
        fun escapedChar() {
            val explanation = interpret("\\.")
            assertThat(explanation).contains(".")
        }
    }

    @Nested
    @DisplayName("预定义字符类解释")
    inner class PredefinedCharClassTests {
        
        @Test
        @DisplayName("\\d 解释")
        fun digitExplanation() {
            val explanation = interpret("\\d")
            assertThat(explanation).contains("数字")
        }
        
        @Test
        @DisplayName("\\w 解释")
        fun wordExplanation() {
            val explanation = interpret("\\w")
            assertThat(explanation).contains("单词")
        }
        
        @Test
        @DisplayName("\\s 解释")
        fun spaceExplanation() {
            val explanation = interpret("\\s")
            assertThat(explanation).contains("空白")
        }
    }

    @Nested
    @DisplayName("量词解释")
    inner class QuantifierInterpretationTests {
        
        @Test
        @DisplayName("星号解释")
        fun starExplanation() {
            val explanation = interpret("a*")
            assertThat(explanation).contains("零次")
            assertThat(explanation).contains("多次")
        }
        
        @Test
        @DisplayName("加号解释")
        fun plusExplanation() {
            val explanation = interpret("a+")
            assertThat(explanation).contains("一次")
            assertThat(explanation).contains("多次")
        }
        
        @Test
        @DisplayName("问号解释")
        fun questionExplanation() {
            val explanation = interpret("a?")
            assertThat(explanation).contains("零次")
            assertThat(explanation).contains("一次")
        }
        
        @Test
        @DisplayName("精确次数")
        fun exactExplanation() {
            val explanation = interpret("a{3}")
            assertThat(explanation).contains("3")
        }
        
        @Test
        @DisplayName("范围次数")
        fun rangeExplanation() {
            val explanation = interpret("a{2,5}")
            assertThat(explanation).contains("2")
            assertThat(explanation).contains("5")
        }
    }

    @Nested
    @DisplayName("锚点解释")
    inner class AnchorInterpretationTests {
        
        @Test
        @DisplayName("行首锚点")
        fun lineStartExplanation() {
            val explanation = interpret("^abc")
            assertThat(explanation).contains("行首")
        }
        
        @Test
        @DisplayName("行尾锚点")
        fun lineEndExplanation() {
            val explanation = interpret("abc\$")
            assertThat(explanation).contains("行尾")
        }
        
        @Test
        @DisplayName("单词边界")
        fun wordBoundaryExplanation() {
            val explanation = interpret("\\bword\\b")
            assertThat(explanation).contains("单词边界")
        }
    }

    @Nested
    @DisplayName("分组解释")
    inner class GroupInterpretationTests {
        
        @Test
        @DisplayName("捕获分组")
        fun capturingGroupExplanation() {
            val explanation = interpret("(abc)")
            assertThat(explanation).contains("捕获组")
        }
        
        @Test
        @DisplayName("非捕获分组")
        fun nonCapturingGroupExplanation() {
            val explanation = interpret("(?:abc)")
            assertThat(explanation).contains("非捕获组")
        }
        
        @Test
        @DisplayName("正向前瞻")
        fun positiveLookaheadExplanation() {
            val explanation = interpret("(?=abc)")
            assertThat(explanation).contains("前瞻断言")
        }
    }

    @Nested
    @DisplayName("选择解释")
    inner class AlternationInterpretationTests {
        
        @Test
        @DisplayName("基本选择")
        fun basicAlternationExplanation() {
            val explanation = interpret("a|b")
            assertThat(explanation).contains("或者")
        }
    }

    @Nested
    @DisplayName("完整模式解释")
    inner class FullPatternTests {
        
        @Test
        @DisplayName("手机号解释")
        fun phoneExplanation() {
            val explanation = interpret("^1[3-9]\\d{9}\$")
            assertThat(explanation).isNotEmpty()
        }
        
        @Test
        @DisplayName("邮箱解释")
        fun emailExplanation() {
            val explanation = interpret("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")
            assertThat(explanation).isNotEmpty()
        }
    }
}
