package com.github.youjiyun1123.youregexinterpreter.core.parser

import com.github.youjiyun1123.youregexinterpreter.core.model.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RegexParserTest {

    private fun parse(input: String): ParseResult {
        return RegexParser(input).parse()
    }

    @Nested
    @DisplayName("字面量解析")
    inner class LiteralTests {
        
        @Test
        @DisplayName("单个字符")
        fun singleChar() {
            val result = parse("a")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            assertThat(success.root).isInstanceOf(Literal::class.java)
            assertThat((success.root as Literal).chars).isEqualTo("a")
        }
        
        @Test
        @DisplayName("多个字符")
        fun multipleChars() {
            val result = parse("abc")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            assertThat(success.root).isInstanceOf(Sequence::class.java)
            val sequence = success.root as Sequence
            assertThat(sequence.children).hasSize(3)
            assertThat(sequence.children).allSatisfy { 
                assertThat(it).isInstanceOf(Literal::class.java) 
            }
        }
        
        @Test
        @DisplayName("特殊字符转义")
        fun escapedSpecialChar() {
            val result = parse("\\.")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            assertThat(success.root).isInstanceOf(Literal::class.java)
            assertThat((success.root as Literal).chars).isEqualTo(".")
        }
    }

    @Nested
    @DisplayName("字符类解析")
    inner class CharClassTests {
        
        @Test
        @DisplayName("预定义字符类 \\d")
        fun predefinedDigit() {
            val result = parse("\\d")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            assertThat(success.root).isInstanceOf(CharClass::class.java)
            val charClass = success.root as CharClass
            assertThat(charClass.predefined).isEqualTo(PredefinedClass.DIGIT)
        }
        
        @Test
        @DisplayName("预定义字符类 \\w")
        fun predefinedWord() {
            val result = parse("\\w")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            val charClass = success.root as CharClass
            assertThat(charClass.predefined).isEqualTo(PredefinedClass.WORD)
        }
        
        @Test
        @DisplayName("预定义字符类 \\s")
        fun predefinedSpace() {
            val result = parse("\\s")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            val charClass = success.root as CharClass
            assertThat(charClass.predefined).isEqualTo(PredefinedClass.WHITESPACE)
        }
        
        @Test
        @DisplayName("点号匹配任意字符")
        fun dotMatchesAny() {
            val result = parse(".")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            val charClass = success.root as CharClass
            assertThat(charClass.predefined).isEqualTo(PredefinedClass.ANY)
        }
    }

    @Nested
    @DisplayName("量词解析")
    inner class QuantifierTests {
        
        @Test
        @DisplayName("星号 *")
        fun starQuantifier() {
            val result = parse("a*")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            assertThat(success.root).isInstanceOf(Quantifier::class.java)
            val quantifier = success.root as Quantifier
            assertThat(quantifier.min).isEqualTo(0)
            assertThat(quantifier.max).isNull()
            assertThat(quantifier.child).isInstanceOf(Literal::class.java)
        }
        
        @Test
        @DisplayName("加号 +")
        fun plusQuantifier() {
            val result = parse("a+")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            val quantifier = success.root as Quantifier
            assertThat(quantifier.min).isEqualTo(1)
            assertThat(quantifier.max).isNull()
        }
        
        @Test
        @DisplayName("问号 ?")
        fun questionQuantifier() {
            val result = parse("a?")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            val quantifier = success.root as Quantifier
            assertThat(quantifier.min).isEqualTo(0)
            assertThat(quantifier.max).isEqualTo(1)
        }
        
        @Test
        @DisplayName("范围量词 {n}")
        fun exactQuantifier() {
            val result = parse("a{3}")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            val quantifier = success.root as Quantifier
            assertThat(quantifier.min).isEqualTo(3)
            assertThat(quantifier.max).isEqualTo(3)
        }
        
        @Test
        @DisplayName("范围量词 {n,m}")
        fun rangeQuantifier() {
            val result = parse("a{2,5}")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            val quantifier = success.root as Quantifier
            assertThat(quantifier.min).isEqualTo(2)
            assertThat(quantifier.max).isEqualTo(5)
        }
        
        @Test
        @DisplayName("范围量词 {n,}")
        fun minQuantifier() {
            val result = parse("a{3,}")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            val quantifier = success.root as Quantifier
            assertThat(quantifier.min).isEqualTo(3)
            assertThat(quantifier.max).isNull()
        }
    }

    @Nested
    @DisplayName("分组解析")
    inner class GroupTests {
        
        @Test
        @DisplayName("捕获分组")
        fun capturingGroup() {
            val result = parse("(abc)")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            assertThat(success.root).isInstanceOf(Group::class.java)
            val group = success.root as Group
            assertThat(group.type).isEqualTo(GroupType.CAPTURING)
        }
        
        @Test
        @DisplayName("非捕获分组")
        fun nonCapturingGroup() {
            val result = parse("(?:abc)")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            assertThat(success.root).isInstanceOf(Group::class.java)
            val group = success.root as Group
            assertThat(group.type).isEqualTo(GroupType.NON_CAPTURING)
        }
        
        @Test
        @DisplayName("正向前瞻")
        fun positiveLookahead() {
            val result = parse("(?=abc)")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            assertThat(success.root).isInstanceOf(Group::class.java)
            val group = success.root as Group
            assertThat(group.type).isEqualTo(GroupType.LOOKAHEAD)
        }
        
        @Test
        @DisplayName("负向前瞻")
        fun negativeLookahead() {
            val result = parse("(?!abc)")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            assertThat(success.root).isInstanceOf(Group::class.java)
            val group = success.root as Group
            assertThat(group.type).isEqualTo(GroupType.NEGATIVE_LOOKAHEAD)
        }
    }

    @Nested
    @DisplayName("锚点解析")
    inner class AnchorTests {
        
        @Test
        @DisplayName("行首锚点 ^")
        fun lineStart() {
            val result = parse("^abc")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            assertThat(success.root).isInstanceOf(Sequence::class.java)
            val sequence = success.root as Sequence
            assertThat(sequence.children[0]).isInstanceOf(Anchor::class.java)
            val anchor = sequence.children[0] as Anchor
            assertThat(anchor.type).isEqualTo(AnchorType.LINE_START)
        }
        
        @Test
        @DisplayName("行尾锚点 $")
        fun lineEnd() {
            val result = parse("abc\$")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            val sequence = success.root as Sequence
            val anchor = sequence.children.last() as Anchor
            assertThat(anchor.type).isEqualTo(AnchorType.LINE_END)
        }
        
        @Test
        @DisplayName("单词边界")
        fun wordBoundary() {
            val result = parse("\\bword\\b")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            assertThat(success.root).isInstanceOf(Sequence::class.java)
        }
    }

    @Nested
    @DisplayName("选择解析")
    inner class AlternationTests {
        
        @Test
        @DisplayName("基本选择")
        fun basicAlternation() {
            val result = parse("a|b")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            assertThat(success.root).isInstanceOf(Alternation::class.java)
            val alternation = success.root as Alternation
            assertThat(alternation.alternatives).hasSize(2)
        }
        
        @Test
        @DisplayName("多选")
        fun multipleAlternation() {
            val result = parse("a|b|c")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            val alternation = success.root as Alternation
            assertThat(alternation.alternatives).hasSize(3)
        }
    }

    @Nested
    @DisplayName("回溯引用解析")
    inner class BackReferenceTests {
        
        @Test
        @DisplayName("数字回溯引用")
        fun numericBackReference() {
            val result = parse("\\1")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            assertThat(success.root).isInstanceOf(BackReference::class.java)
            val backRef = success.root as BackReference
            assertThat(backRef.index).isEqualTo(1)
        }
        
        @Test
        @DisplayName("命名回溯引用")
        fun namedBackReference() {
            val result = parse("\\k<word>")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
            val success = result as ParseResult.Success
            assertThat(success.root).isInstanceOf(BackReference::class.java)
            val backRef = success.root as BackReference
            assertThat(backRef.name).isEqualTo("word")
        }
    }

    @Nested
    @DisplayName("复杂模式解析")
    inner class ComplexPatternTests {
        
        @Test
        @DisplayName("手机号正则")
        fun phonePattern() {
            val result = parse("^1[3-9]\\d{9}\$")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        }
        
        @Test
        @DisplayName("邮箱正则")
        fun emailPattern() {
            val result = parse("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        }
        
        @Test
        @DisplayName("URL 正则")
        fun urlPattern() {
            val result = parse("^https?://[^\\s/$.?#].[^\\s]*\$")
            assertThat(result).isInstanceOf(ParseResult.Success::class.java)
        }
    }
}
