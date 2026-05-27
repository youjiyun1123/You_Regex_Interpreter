package com.github.youjiyun1123.youregexinterpreter.engine

import com.github.youjiyun1123.youregexinterpreter.core.model.RegexFlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class JavaRegexEngineTest {

    private val engine = JavaRegexEngine()

    @Nested
    @DisplayName("匹配测试")
    inner class MatchTests {
        
        @Test
        @DisplayName("精确匹配")
        fun exactMatch() {
            val matches = engine.findAll("abc", "abc def abc ghi")
            assertThat(matches).hasSize(2)
            assertThat(matches[0].value).isEqualTo("abc")
            assertThat(matches[1].value).isEqualTo("abc")
        }
        
        @Test
        @DisplayName("数字匹配")
        fun digitMatch() {
            val matches = engine.findAll("\\d+", "abc 123 def 456")
            assertThat(matches).hasSize(2)
            assertThat(matches[0].value).isEqualTo("123")
            assertThat(matches[1].value).isEqualTo("456")
        }
        
        @Test
        @DisplayName("手机号匹配")
        fun phoneMatch() {
            val matches = engine.findAll("1[3-9]\\d{9}", "电话: 13812345678 和 13987654321")
            assertThat(matches).hasSize(2)
        }
        
        @Test
        @DisplayName("邮箱匹配")
        fun emailMatch() {
            val matches = engine.findAll("[a-z]+@[a-z]+\\.[a-z]+", "联系: user@example.com")
            assertThat(matches).hasSize(1)
            assertThat(matches[0].value).isEqualTo("user@example.com")
        }
    }

    @Nested
    @DisplayName("标志位测试")
    inner class FlagTests {
        
        @Test
        @DisplayName("大小写不敏感")
        fun caseInsensitive() {
            val matches = engine.findAll("abc", "ABC abc Abc", setOf(RegexFlag.CASE_INSENSITIVE))
            assertThat(matches).hasSize(3)
        }
        
        @Test
        @DisplayName("多行模式")
        fun multiline() {
            val matches = engine.findAll("^line", "line1\nline2\nline3", setOf(RegexFlag.MULTILINE))
            assertThat(matches).hasSize(3)
        }
    }

    @Nested
    @DisplayName("验证测试")
    inner class ValidationTests {
        
        @Test
        @DisplayName("有效正则")
        fun validPattern() {
            val result = engine.validate("^\\d+$")
            assertThat(result.isValid).isTrue()
        }
        
        @Test
        @DisplayName("无效正则 - 未闭合分组")
        fun invalidPatternUnclosedGroup() {
            val result = engine.validate("(abc")
            assertThat(result.isValid).isFalse()
            assertThat(result.error).isNotNull()
        }
        
        @Test
        @DisplayName("无效正则 - 未闭合字符类")
        fun invalidPatternUnclosedCharClass() {
            val result = engine.validate("[abc")
            assertThat(result.isValid).isFalse()
        }
    }

    @Nested
    @DisplayName("实用方法测试")
    inner class UtilityTests {
        
        @Test
        @DisplayName("完全匹配检查")
        fun isFullMatch() {
            assertThat(engine.isFullMatch("\\d+", "123")).isTrue()
            assertThat(engine.isFullMatch("\\d+", "123abc")).isFalse()
        }
        
        @Test
        @DisplayName("匹配计数")
        fun countMatches() {
            val count = engine.countMatches("\\d+", "abc 123 def 456 ghi 789")
            assertThat(count).isEqualTo(3)
        }
        
        @Test
        @DisplayName("替换")
        fun replace() {
            val result = engine.replace("\\d+", "abc 123 def 456", "X")
            assertThat(result).isEqualTo("abc X def X")
        }
        
        @Test
        @DisplayName("分割")
        fun split() {
            val parts = engine.split("\\s+", "a b  c   d")
            assertThat(parts).containsExactly("a", "b", "c", "d")
        }
    }
}
