package com.github.youjiyun1123.youregexinterpreter.core.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class SyntaxErrorDetectorTest {

    @Nested
    @DisplayName("错误检测")
    inner class ErrorDetectionTests {
        
        @Test
        @DisplayName("未闭合分组")
        fun unclosedGroup() {
            val errors = SyntaxErrorDetector.detect("(abc")
            assertThat(errors).isNotEmpty()
            assertThat(errors.any { it.code == ErrorCode.UNCLOSED_GROUP }).isTrue()
        }
        
        @Test
        @DisplayName("多余闭合括号")
        fun extraClosingParen() {
            val errors = SyntaxErrorDetector.detect("abc)")
            assertThat(errors).isNotEmpty()
            assertThat(errors.any { it.code == ErrorCode.UNCLOSED_GROUP }).isTrue()
        }
        
        @Test
        @DisplayName("未闭合字符类")
        fun unclosedCharClass() {
            val errors = SyntaxErrorDetector.detect("[abc")
            assertThat(errors).isNotEmpty()
            assertThat(errors.any { it.code == ErrorCode.UNCLOSED_CHAR_CLASS }).isTrue()
        }
        
        @Test
        @DisplayName("孤立量词")
        fun orphanQuantifier() {
            val errors = SyntaxErrorDetector.detect("*abc")
            assertThat(errors).isNotEmpty()
            assertThat(errors.any { it.code == ErrorCode.ORPHAN_QUANTIFIER }).isTrue()
        }
        
        @Test
        @DisplayName("无效范围量词")
        fun invalidRangeQuantifier() {
            val errors = SyntaxErrorDetector.detect("a{5,3}")
            assertThat(errors).isNotEmpty()
            assertThat(errors.any { it.code == ErrorCode.INVALID_QUANTIFIER }).isTrue()
        }
        
        @Test
        @DisplayName("空选择")
        fun emptyAlternation() {
            val errors = SyntaxErrorDetector.detect("a||b")
            assertThat(errors).isNotEmpty()
            assertThat(errors.any { it.code == ErrorCode.EMPTY_ALTERNATION }).isTrue()
        }
    }

    @Nested
    @DisplayName("有效模式")
    inner class ValidPatternTests {
        
        @Test
        @DisplayName("简单模式")
        fun simplePattern() {
            val errors = SyntaxErrorDetector.detect("abc")
            assertThat(errors).isEmpty()
        }
        
        @Test
        @DisplayName("复杂模式")
        fun complexPattern() {
            val errors = SyntaxErrorDetector.detect("^1[3-9]\\d{9}$")
            assertThat(errors).isEmpty()
        }
        
        @Test
        @DisplayName("邮箱模式")
        fun emailPattern() {
            val errors = SyntaxErrorDetector.detect("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
            assertThat(errors).isEmpty()
        }
        
        @Test
        @DisplayName("域名模式 - 不应报孤立量词错误")
        fun domainPattern() {
            val pattern = "^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
            val errors = SyntaxErrorDetector.detect(pattern)
            assertThat(errors).isEmpty()
        }
    }

    @Nested
    @DisplayName("错误格式化")
    inner class ErrorFormattingTests {
        
        @Test
        @DisplayName("格式化输出")
        fun formatError() {
            val errors = SyntaxErrorDetector.detect("(abc")
            assertThat(errors).isNotEmpty()
            val formatted = SyntaxErrorDetector.formatError(errors.first(), "(abc")
            assertThat(formatted).contains("Error at position")
            assertThat(formatted).contains("Pattern:")
        }
    }
}
