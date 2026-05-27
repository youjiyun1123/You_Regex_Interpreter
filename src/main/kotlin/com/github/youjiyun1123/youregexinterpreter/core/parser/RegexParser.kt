package com.github.youjiyun1123.youregexinterpreter.core.parser

import com.github.youjiyun1123.youregexinterpreter.core.model.*

/**
 * 解析结果
 */
sealed class ParseResult {
    data class Success(val root: RegexNode) : ParseResult()
    data class Failure(val error: ParseError) : ParseResult()
}

/**
 * 解析错误
 */
data class ParseError(
    val message: String,
    val position: Int,
    val length: Int = 1,
    val hint: String? = null
)

/**
 * 递归下降解析器
 */
class RegexParser(private val input: String) {
    private val lexer = Lexer(input)
    private var tokens = listOf<Token>()
    private var pos = 0
    private var groupIndex = 0
    
    fun parse(): ParseResult {
        tokens = lexer.tokenize()
        pos = 0
        groupIndex = 0
        
        return try {
            val result = parseAlternation()
            if (!isAtEnd()) {
                ParseResult.Failure(ParseError("Unexpected: ${peek().value}", peek().position))
            } else {
                ParseResult.Success(result)
            }
        } catch (e: ParseException) {
            ParseResult.Failure(e.error)
        }
    }
    
    private fun parseAlternation(): RegexNode {
        val alternatives = mutableListOf<RegexNode>()
        alternatives.add(parseSequence())
        
        while (match(TokenType.ALTERNATION)) {
            alternatives.add(parseSequence())
        }
        
        return if (alternatives.size == 1) alternatives[0] else Alternation(alternatives)
    }
    
    private fun parseSequence(): RegexNode {
        val items = mutableListOf<RegexNode>()
        
        while (!isAtEnd() && !isAlternation() && !isGroupClose()) {
            val item = parseAtomWithQuantifier()
            if (item != null) items.add(item)
            if (isAlternation() || isGroupClose()) break
        }
        
        return when {
            items.isEmpty() -> Literal("")
            items.size == 1 -> items[0]
            else -> Sequence(items)
        }
    }
    
    private fun parseAtomWithQuantifier(): RegexNode? {
        val atom = parseAtom() ?: return null
        return applyQuantifier(atom)
    }
    
    private fun parseAtom(): RegexNode? {
        if (isAtEnd()) return null
        
        return when {
            match(TokenType.DOT) -> CharClass(predefined = PredefinedClass.ANY)
            match(TokenType.LITERAL) -> Literal(prev().value)
            
            // 转义序列 - 预定义字符类
            match(TokenType.ESCAPE_DIGIT) -> CharClass(predefined = PredefinedClass.DIGIT)
            match(TokenType.ESCAPE_NON_DIGIT) -> CharClass(predefined = PredefinedClass.NON_DIGIT)
            match(TokenType.ESCAPE_WORD) -> CharClass(predefined = PredefinedClass.WORD)
            match(TokenType.ESCAPE_NON_WORD) -> CharClass(predefined = PredefinedClass.NON_WORD)
            match(TokenType.ESCAPE_SPACE) -> CharClass(predefined = PredefinedClass.WHITESPACE)
            match(TokenType.ESCAPE_NON_SPACE) -> CharClass(predefined = PredefinedClass.NON_WHITESPACE)
            
            // 锚点
            match(TokenType.ESCAPE_WORD_BOUNDARY) -> Anchor(AnchorType.WORD_BOUNDARY)
            match(TokenType.ESCAPE_NON_WORD_BOUNDARY) -> Anchor(AnchorType.NON_WORD_BOUNDARY)
            match(TokenType.ESCAPE_INPUT_START) -> Anchor(AnchorType.INPUT_START)
            match(TokenType.ESCAPE_INPUT_END) -> Anchor(AnchorType.INPUT_END)
            match(TokenType.ESCAPE_INPUT_END_STRICT) -> Anchor(AnchorType.INPUT_END_ANY)
            
            match(TokenType.ANCHOR_CARET) -> Anchor(AnchorType.LINE_START)
            match(TokenType.ANCHOR_DOLLAR) -> Anchor(AnchorType.LINE_END)
            
            // 字面量转义
            match(TokenType.ESCAPE_LITERAL) -> Literal(escapeLiteral(prev().value))
            match(TokenType.ESCAPE_N) -> Escape(EscapeType.N)
            match(TokenType.ESCAPE_R) -> Escape(EscapeType.R)
            match(TokenType.ESCAPE_T) -> Escape(EscapeType.T)
            match(TokenType.ESCAPE_F) -> Escape(EscapeType.F)
            match(TokenType.ESCAPE_0) -> Escape(EscapeType.OCTAL, "0")
            match(TokenType.ESCAPE_UNICODE) -> Literal(escapeUnicode(prev().value))
            match(TokenType.ESCAPE_HEX) -> Literal(escapeHex(prev().value))
            
            // 回溯引用
            match(TokenType.BACK_REFERENCE_NUMERIC) -> BackReference(index = prev().value.substring(1).toIntOrNull() ?: 1)
            match(TokenType.BACK_REFERENCE_NAMED) -> BackReference(name = extractBackRefName(prev().value))
            
            // 字符类
            match(TokenType.CHAR_CLASS) -> parseCharClassValue(prev().value)
            
            // 分组
            match(TokenType.GROUP_OPEN) -> parseCapturingGroup()
            match(TokenType.GROUP_NON_CAPTURING) -> parseNonCapturingGroup()
            match(TokenType.GROUP_LOOKAHEAD) -> parseLookahead(true)
            match(TokenType.GROUP_NEG_LOOKAHEAD) -> parseLookahead(false)
            match(TokenType.GROUP_LOOKBEHIND) -> parseLookbehind(true)
            match(TokenType.GROUP_NEG_LOOKBEHIND) -> parseLookbehind(false)
            match(TokenType.GROUP_NAMED_START) -> parseNamedCapturingGroup()
            
            else -> { advance(); null }
        }
    }
    
    private fun escapeLiteral(value: String): String {
        return if (value.length == 2) value[1].toString() else value.substring(1)
    }
    
    private fun escapeUnicode(value: String): String {
        return try {
            val hex = value.substring(2)
            val codePoint = hex.toInt(16)
            String(Character.toChars(codePoint))
        } catch (e: Exception) { value }
    }
    
    private fun escapeHex(value: String): String {
        return try {
            val hex = value.substring(2)
            val code = hex.toInt(16)
            code.toChar().toString()
        } catch (e: Exception) { value }
    }
    
    private fun extractBackRefName(value: String): String {
        val regex = Regex("\\k<([^>]+)>")
        return regex.find(value)?.groupValues?.get(1) ?: ""
    }
    
    private fun parseCharClassValue(value: String): RegexNode {
        if (value == "\\d") return CharClass(predefined = PredefinedClass.DIGIT)
        if (value == "\\D") return CharClass(predefined = PredefinedClass.NON_DIGIT)
        if (value == "\\w") return CharClass(predefined = PredefinedClass.WORD)
        if (value == "\\W") return CharClass(predefined = PredefinedClass.NON_WORD)
        if (value == "\\s") return CharClass(predefined = PredefinedClass.WHITESPACE)
        if (value == "\\S") return CharClass(predefined = PredefinedClass.NON_WHITESPACE)
        
        val negated = value.startsWith("[^")
        val ranges = mutableListOf<RegexCharRange>()
        
        var i = if (value.startsWith("[")) 1 else 0
        if (negated) i++
        
        while (i < value.length) {
            val ch = value[i]
            if (ch == ']') break
            
            if (ch == '\\' && i + 1 < value.length) {
                i++
                when (value[i]) {
                    'd' -> ranges.add(RegexCharRange('0', '9'))
                    'w' -> {
                        ranges.add(RegexCharRange('a', 'z'))
                        ranges.add(RegexCharRange('A', 'Z'))
                        ranges.add(RegexCharRange('0', '9'))
                        ranges.add(RegexCharRange('_', '_'))
                    }
                    's' -> {
                        ranges.add(RegexCharRange(' ', ' '))
                        ranges.add(RegexCharRange('\t', '\t'))
                        ranges.add(RegexCharRange('\n', '\n'))
                        ranges.add(RegexCharRange('\r', '\r'))
                    }
                }
            } else if (i + 2 < value.length && value[i + 1] == '-') {
                val start = value[i]
                val end = value[i + 2]
                if (start <= end) ranges.add(RegexCharRange(start, end))
                i += 2
            }
            i++
        }
        
        return CharClass(ranges = ranges, negated = negated)
    }
    
    private fun parseCapturingGroup(): RegexNode {
        groupIndex++
        val children = parseGroupContent()
        expect(TokenType.GROUP_CLOSE, "Unclosed capturing group")
        return Group(listOf(wrapInSequence(children)), GroupType.CAPTURING, index = groupIndex)
    }
    
    private fun parseNonCapturingGroup(): RegexNode {
        val children = parseGroupContent()
        expect(TokenType.GROUP_CLOSE, "Unclosed non-capturing group")
        return Group(listOf(wrapInSequence(children)), GroupType.NON_CAPTURING)
    }
    
    private fun parseLookahead(positive: Boolean): RegexNode {
        val children = parseGroupContent()
        expect(TokenType.GROUP_CLOSE, "Unclosed lookahead")
        return Group(children, if (positive) GroupType.LOOKAHEAD else GroupType.NEGATIVE_LOOKAHEAD)
    }
    
    private fun parseLookbehind(positive: Boolean): RegexNode {
        val children = parseGroupContent()
        expect(TokenType.GROUP_CLOSE, "Unclosed lookbehind")
        return Group(children, if (positive) GroupType.LOOKBEHIND else GroupType.NEGATIVE_LOOKBEHIND)
    }
    
    private fun parseNamedCapturingGroup(): RegexNode {
        val name = parseGroupName()
        groupIndex++
        val children = parseGroupContent()
        expect(TokenType.GROUP_CLOSE, "Unclosed named group")
        return Group(listOf(wrapInSequence(children)), GroupType.NAMED_CAPTURING, name = name, index = groupIndex)
    }
    
    private fun parseGroupName(): String {
        val sb = StringBuilder()
        while (!isAtEnd() && !check(TokenType.GROUP_CLOSE)) {
            val token = advance()
            if (token.type == TokenType.LITERAL) sb.append(token.value)
            if (token.value == ">") break
        }
        return sb.toString()
    }
    
    private fun parseGroupContent(): List<RegexNode> {
        val children = mutableListOf<RegexNode>()
        while (!isAtEnd() && !check(TokenType.GROUP_CLOSE)) {
            val item = parseAtomWithQuantifier()
            if (item != null) children.add(item)
        }
        return children
    }
    
    private fun wrapInSequence(children: List<RegexNode>): RegexNode {
        return when {
            children.isEmpty() -> Literal("")
            children.size == 1 -> children[0]
            else -> Sequence(children)
        }
    }
    
    private fun applyQuantifier(node: RegexNode): RegexNode {
        var result = node
        while (!isAtEnd()) {
            val q = parseQuantifierSuffix(result)
            if (q != null) result = q else break
        }
        return result
    }
    
    private fun parseQuantifierSuffix(base: RegexNode): RegexNode? {
        val type = when {
            match(TokenType.QUANTIFIER_STAR) -> {
                if (match(TokenType.QUANTIFIER_QUESTION)) QuantifierType.RELUCTANT else QuantifierType.GREEDY
            }
            match(TokenType.QUANTIFIER_PLUS) -> {
                if (match(TokenType.QUANTIFIER_QUESTION)) QuantifierType.RELUCTANT else QuantifierType.GREEDY
            }
            match(TokenType.QUANTIFIER_QUESTION) -> {
                if (match(TokenType.QUANTIFIER_QUESTION)) QuantifierType.RELUCTANT else QuantifierType.GREEDY
            }
            match(TokenType.QUANTIFIER_RANGE) -> {
                val reluctant = match(TokenType.QUANTIFIER_QUESTION)
                val range = parseRangeQuantifier() ?: return null
                return Quantifier(base, range.first, range.second, 
                    if (reluctant) QuantifierType.RELUCTANT else QuantifierType.GREEDY)
            }
            else -> return null
        }
        
        val (min, max) = when {
            prev().type == TokenType.QUANTIFIER_STAR -> 0 to null
            prev().type == TokenType.QUANTIFIER_PLUS -> 1 to null
            else -> 0 to 1
        }
        
        return Quantifier(base, min, max, type)
    }
    
    private fun parseRangeQuantifier(): Pair<Int, Int?>? {
        val content = prev().value
        val regex = Regex("\\{(\\d+)(,(\\d*)?)?\\}")
        val match = regex.find(content) ?: return null
        
        val min = match.groupValues[1].toInt()
        val hasComma = match.groupValues[2].isNotEmpty()
        val maxStr = match.groupValues[3]
        
        val max = if (hasComma) {
            if (maxStr.isEmpty()) null else maxStr.toIntOrNull()
        } else min
        
        return min to max
    }
    
    // Helper methods
    private fun peek() = tokens[pos]
    private fun prev() = tokens[pos - 1]
    private fun isAtEnd() = peek().type == TokenType.EOF
    private fun check(type: TokenType) = !isAtEnd() && peek().type == type
    
    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) { advance(); return true }
        }
        return false
    }
    
    private fun advance(): Token {
        if (!isAtEnd()) pos++
        return prev()
    }
    
    private fun expect(type: TokenType, msg: String): Token {
        if (check(type)) return advance()
        throw ParseException(ParseError(msg, peek().position))
    }
    
    private fun isAlternation() = check(TokenType.ALTERNATION)
    private fun isGroupClose() = check(TokenType.GROUP_CLOSE)
}

class ParseException(val error: ParseError) : RuntimeException(error.message)
