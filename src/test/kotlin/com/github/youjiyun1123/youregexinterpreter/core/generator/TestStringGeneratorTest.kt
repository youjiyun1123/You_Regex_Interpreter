package com.github.youjiyun1123.youregexinterpreter.core.generator

import com.github.youjiyun1123.youregexinterpreter.core.parser.RegexParserFacade
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TestStringGeneratorTest {

    private val generator = TestStringGenerator()

    private fun generate(pattern: String): String? {
        val result = generator.generate(pattern)
        return when (result) {
            is GenerationResult.Success -> result.testString
            is GenerationResult.Failure -> null
        }
    }

    @Nested
    @DisplayName("基本模式生成")
    inner class BasicPatternTests {
        
        @Test
        @DisplayName("字面量生成")
        fun literalGeneration() {
            val result = generator.generate("hello")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).isEqualTo("hello")
        }
        
        @Test
        @DisplayName("数字生成")
        fun digitGeneration() {
            val result = generator.generate("\\d")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).matches("\\d")
        }
        
        @Test
        @DisplayName("单词字符生成")
        fun wordGeneration() {
            val result = generator.generate("\\w+")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).matches("\\w+")
        }
    }

    @Nested
    @DisplayName("字符类生成")
    inner class CharClassTests {
        
        @Test
        @DisplayName("预定义字符类 \\d")
        fun predefinedDigit() {
            val result = generator.generate("\\d")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).matches("\\d")
        }
        
        @Test
        @DisplayName("字符范围")
        fun charRange() {
            val result = generator.generate("[a-z]")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString.length).isEqualTo(1)
        }
        
        @Test
        @DisplayName("点号匹配任意字符")
        fun dotMatchesAny() {
            val result = generator.generate("a.c")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).hasSize(3)
            assertThat(success.testString[0]).isEqualTo('a')
            assertThat(success.testString[2]).isEqualTo('c')
        }
    }

    @Nested
    @DisplayName("量词生成")
    inner class QuantifierTests {
        
        @Test
        @DisplayName("星号量词")
        fun starQuantifier() {
            val result = generator.generate("a*")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).matches("a*")
        }
        
        @Test
        @DisplayName("加号量词")
        fun plusQuantifier() {
            val result = generator.generate("a+")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).isNotEmpty()
            assertThat(success.testString).matches("a+")
        }
        
        @Test
        @DisplayName("问号量词")
        fun questionQuantifier() {
            val result = generator.generate("a?")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).matches("a?")
        }
        
        @Test
        @DisplayName("精确次数量词")
        fun exactQuantifier() {
            val result = generator.generate("a{3}")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).isEqualTo("aaa")
        }
    }

    @Nested
    @DisplayName("分组生成")
    inner class GroupTests {
        
        @Test
        @DisplayName("捕获分组")
        fun capturingGroup() {
            val result = generator.generate("(abc)")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).isEqualTo("abc")
        }
        
        @Test
        @DisplayName("非捕获分组")
        fun nonCapturingGroup() {
            val result = generator.generate("(?:abc)")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).isEqualTo("abc")
        }
    }

    @Nested
    @DisplayName("选择生成")
    inner class AlternationTests {
        
        @Test
        @DisplayName("基本选择")
        fun basicAlternation() {
            val result = generator.generate("a|b")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).isIn("a", "b")
        }
        
        @Test
        @DisplayName("多选")
        fun multipleAlternation() {
            val result = generator.generate("cat|dog|bird")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).isIn("cat", "dog", "bird")
        }
    }

    @Nested
    @DisplayName("锚点生成")
    inner class AnchorTests {
        
        @Test
        @DisplayName("行首锚点")
        fun lineStart() {
            val result = generator.generate("^hello")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).isEqualTo("hello")
        }
        
        @Test
        @DisplayName("行尾锚点")
        fun lineEnd() {
            val result = generator.generate("world$")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).isEqualTo("world")
        }
    }

    @Nested
    @DisplayName("复杂模式生成")
    inner class ComplexPatternTests {
        
        @Test
        @DisplayName("手机号正则")
        fun phonePattern() {
            val result = generator.generate("^1[3-9]\\d{9}$")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).matches("^1[3-9]\\d{9}$")
        }
        
        @Test
        @DisplayName("邮箱正则（简化）")
        fun emailPattern() {
            val result = generator.generate("\\w+@\\w+\\.\\w+")
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).matches("\\w+@\\w+\\.\\w+")
        }
    }

    @Nested
    @DisplayName("错误处理")
    inner class ErrorHandlingTests {
        
        @Test
        @DisplayName("无效正则 - 未闭合分组")
        fun unclosedGroup() {
            val result = generator.generate("(abc")
            assertThat(result).isInstanceOf(GenerationResult.Failure::class.java)
            val failure = result as GenerationResult.Failure
            assertThat(failure.message).contains("错误")
        }
        
        @Test
        @DisplayName("无效正则 - 孤立量词")
        fun orphanQuantifier() {
            val result = generator.generate("*abc")
            assertThat(result).isInstanceOf(GenerationResult.Failure::class.java)
            val failure = result as GenerationResult.Failure
            assertThat(failure.message).contains("错误")
        }
        
        @Test
        @DisplayName("空正则生成空字符串")
        fun emptyPattern() {
            val result = generator.generate("")
            // 空正则匹配空字符串，是有效正则
            assertThat(result).isInstanceOf(GenerationResult.Success::class.java)
            val success = result as GenerationResult.Success
            assertThat(success.testString).isEqualTo("")
        }
    }
}
