package com.github.youjiyun1123.youregexinterpreter.core.parser

import com.github.youjiyun1123.youregexinterpreter.core.model.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RegexParserTest {

    private fun parse(input: String): RegexParseResult {
        return RegexParserFacade.parse(input)
    }

    @Nested
    @DisplayName("字面量解析")
    inner class LiteralTests {
        
        @Test
        @DisplayName("单个字符")
        fun singleChar() {
            val result = parse("a")
            assertThat(result.isSuccess).isTrue()
            val node = result.syntaxTree
            assertThat(node).isInstanceOf(Literal::class.java)
            assertThat((node as Literal).chars).isEqualTo("a")
        }
        
        @Test
        @DisplayName("多个字符")
        fun multipleChars() {
            val result = parse("abc")
            assertThat(result.isSuccess).isTrue()
            val node = result.syntaxTree
            assertThat(node).isInstanceOf(Sequence::class.java)
            val sequence = node as Sequence
            assertThat(sequence.children).hasSize(3)
        }
        
        @Test
        @DisplayName("转义字符")
        fun escapedChar() {
            val result = parse("\\.")
            assertThat(result.isSuccess).isTrue()
            val node = result.syntaxTree
            assertThat(node).isInstanceOf(Literal::class.java)
            assertThat((node as Literal).chars).isEqualTo(".")
        }
    }

    @Nested
    @DisplayName("字符类解析")
    inner class CharClassTests {
        
        @Test
        @DisplayName("预定义字符类 \\d")
        fun predefinedDigit() {
            val result = parse("\\d")
            assertThat(result.isSuccess).isTrue()
            val node = result.syntaxTree
            assertThat(node).isInstanceOf(CharClass::class.java)
            assertThat((node as CharClass).predefined).isEqualTo(PredefinedClass.DIGIT)
        }
        
        @Test
        @DisplayName("预定义字符类 \\w")
        fun predefinedWord() {
            val result = parse("\\w")
            assertThat(result.isSuccess).isTrue()
            val node = result.syntaxTree
            assertThat((node as CharClass).predefined).isEqualTo(PredefinedClass.WORD)
        }
        
        @Test
        @DisplayName("点号")
        fun dotMatchesAny() {
            val result = parse(".")
            assertThat(result.isSuccess).isTrue()
            val node = result.syntaxTree
            assertThat((node as CharClass).predefined).isEqualTo(PredefinedClass.ANY)
        }
    }

    @Nested
    @DisplayName("量词解析")
    inner class QuantifierTests {
        
        @Test
        @DisplayName("星号 *")
        fun starQuantifier() {
            val result = parse("a*")
            assertThat(result.isSuccess).isTrue()
            val node = result.syntaxTree
            assertThat(node).isInstanceOf(Quantifier::class.java)
            val q = node as Quantifier
            assertThat(q.min).isEqualTo(0)
            assertThat(q.max).isNull()
        }
        
        @Test
        @DisplayName("加号 +")
        fun plusQuantifier() {
            val result = parse("a+")
            assertThat(result.isSuccess).isTrue()
            val q = result.syntaxTree as Quantifier
            assertThat(q.min).isEqualTo(1)
            assertThat(q.max).isNull()
        }
        
        @Test
        @DisplayName("问号 ?")
        fun questionQuantifier() {
            val result = parse("a?")
            assertThat(result.isSuccess).isTrue()
            val q = result.syntaxTree as Quantifier
            assertThat(q.min).isEqualTo(0)
            assertThat(q.max).isEqualTo(1)
        }
        
        @Test
        @DisplayName("精确次数 {n}")
        fun exactQuantifier() {
            val result = parse("a{3}")
            assertThat(result.isSuccess).isTrue()
            val q = result.syntaxTree as Quantifier
            assertThat(q.min).isEqualTo(3)
            assertThat(q.max).isEqualTo(3)
        }
        
        @Test
        @DisplayName("范围量词 {n,m}")
        fun rangeQuantifier() {
            val result = parse("a{2,5}")
            assertThat(result.isSuccess).isTrue()
            val q = result.syntaxTree as Quantifier
            assertThat(q.min).isEqualTo(2)
            assertThat(q.max).isEqualTo(5)
        }
        
        @Test
        @DisplayName("至少 n 次 {n,}")
        fun minQuantifier() {
            val result = parse("a{3,}")
            assertThat(result.isSuccess).isTrue()
            val q = result.syntaxTree as Quantifier
            assertThat(q.min).isEqualTo(3)
            assertThat(q.max).isNull()
        }
    }

    @Nested
    @DisplayName("分组解析")
    inner class GroupTests {
        
        @Test
        @DisplayName("捕获分组")
        fun capturingGroup() {
            val result = parse("(abc)")
            assertThat(result.isSuccess).isTrue()
            val node = result.syntaxTree
            assertThat(node).isInstanceOf(Group::class.java)
            assertThat((node as Group).type).isEqualTo(GroupType.CAPTURING)
        }
        
        @Test
        @DisplayName("非捕获分组")
        fun nonCapturingGroup() {
            val result = parse("(?:abc)")
            assertThat(result.isSuccess).isTrue()
            assertThat(result.syntaxTree).isInstanceOf(Group::class.java)
            val group = result.syntaxTree as Group
            assertThat(group.type).isEqualTo(GroupType.NON_CAPTURING)
        }
        
        @Test
        @DisplayName("正向前瞻")
        fun positiveLookahead() {
            val result = parse("(?=abc)")
            assertThat(result.isSuccess).isTrue()
            assertThat(result.syntaxTree).isInstanceOf(Group::class.java)
            val group = result.syntaxTree as Group
            assertThat(group.type).isEqualTo(GroupType.LOOKAHEAD)
        }
        
        @Test
        @DisplayName("负向前瞻")
        fun negativeLookahead() {
            val result = parse("(?!abc)")
            assertThat(result.isSuccess).isTrue()
            assertThat(result.syntaxTree).isInstanceOf(Group::class.java)
            val group = result.syntaxTree as Group
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
            assertThat(result.isSuccess).isTrue()
            val node = result.syntaxTree
            assertThat(node).isInstanceOf(Sequence::class.java)
            val sequence = node as Sequence
            assertThat(sequence.children[0]).isInstanceOf(Anchor::class.java)
            assertThat((sequence.children[0] as Anchor).type).isEqualTo(AnchorType.LINE_START)
        }
        
        @Test
        @DisplayName("行尾锚点 $")
        fun lineEnd() {
            val result = parse("abc\$")
            assertThat(result.isSuccess).isTrue()
            val sequence = result.syntaxTree as Sequence
            assertThat((sequence.children.last() as Anchor).type).isEqualTo(AnchorType.LINE_END)
        }
        
        @Test
        @DisplayName("单词边界")
        fun wordBoundary() {
            val result = parse("\\bword\\b")
            assertThat(result.isSuccess).isTrue()
        }
    }

    @Nested
    @DisplayName("选择解析")
    inner class AlternationTests {
        
        @Test
        @DisplayName("基本选择")
        fun basicAlternation() {
            val result = parse("a|b")
            assertThat(result.isSuccess).isTrue()
            val node = result.syntaxTree
            assertThat(node).isInstanceOf(Alternation::class.java)
            assertThat((node as Alternation).alternatives).hasSize(2)
        }
        
        @Test
        @DisplayName("多选")
        fun multipleAlternation() {
            val result = parse("a|b|c")
            assertThat(result.isSuccess).isTrue()
            assertThat((result.syntaxTree as Alternation).alternatives).hasSize(3)
        }
    }

    @Nested
    @DisplayName("复杂模式解析")
    inner class ComplexPatternTests {
        
        @Test
        @DisplayName("手机号正则")
        fun phonePattern() {
            val result = parse("^1[3-9]\\d{9}\$")
            assertThat(result.isSuccess).isTrue()
        }
        
        @Test
        @DisplayName("邮箱正则")
        fun emailPattern() {
            val result = parse("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")
            assertThat(result.isSuccess).isTrue()
        }
        
        @Test
        @DisplayName("URL 正则")
        fun urlPattern() {
            val result = parse("^https?://[^\\s/$.?#].[^\\s]*\$")
            assertThat(result.isSuccess).isTrue()
        }
    }
}
