package com.github.youjiyun1123.youregexinterpreter.core.parser

import com.github.youjiyun1123.youregexinterpreter.core.model.*

/**
 * 词法分析器 - 将正则表达式字符串转换为词法单元流
 */
class Lexer(private val input: String) {
    private var pos = 0
    
    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()
        
        while (pos < input.length) {
            val start = pos
            val token = nextToken()
            tokens.add(Token(token.type, token.value, start))
        }
        
        tokens.add(Token(TokenType.EOF, "", pos))
        return tokens
    }
    
    private fun nextToken(): Token {
        if (pos >= input.length) return Token(TokenType.EOF, "")
        
        val ch = input[pos]
        pos++
        
        return when (ch) {
            '^' -> Token(TokenType.ANCHOR_CARET, "^")
            '$' -> Token(TokenType.ANCHOR_DOLLAR, "$")
            '.' -> Token(TokenType.DOT, ".")
            '|' -> Token(TokenType.ALTERNATION, "|")
            '*' -> Token(TokenType.QUANTIFIER_STAR, "*")
            '+' -> Token(TokenType.QUANTIFIER_PLUS, "+")
            '?' -> Token(TokenType.QUANTIFIER_QUESTION, "?")
            '(' -> parseGroupStart()
            '[' -> parseCharClass()
            '\\' -> parseEscape()
            '{' -> parseQuantifierRange()
            else -> Token(TokenType.LITERAL, ch.toString())
        }
    }
    
    private fun parseGroupStart(): Token {
        val start = pos - 1
        
        if (pos >= input.length || input[pos] != '?') {
            return Token(TokenType.GROUP_OPEN, "(")
        }
        pos++
        
        return when {
            pos >= input.length -> Token(TokenType.GROUP_QUESTION, "(?")
            input[pos] == ':' -> { pos++; Token(TokenType.GROUP_NON_CAPTURING, "(?:") }
            input[pos] == '=' -> { pos++; Token(TokenType.GROUP_LOOKAHEAD, "(?=") }
            input[pos] == '!' -> { pos++; Token(TokenType.GROUP_NEG_LOOKAHEAD, "(?!") }
            input[pos] == '<' -> {
                pos++
                if (pos >= input.length) return Token(TokenType.GROUP_NAMED_START, "(?<")
                when (input[pos]) {
                    '=' -> { pos++; Token(TokenType.GROUP_LOOKBEHIND, "(?<=") }
                    '!' -> { pos++; Token(TokenType.GROUP_NEG_LOOKBEHIND, "(?<!") }
                    else -> Token(TokenType.GROUP_NAMED_START, "(?<")
                }
            }
            else -> Token(TokenType.GROUP_QUESTION, "(?")
        }
    }
    
    private fun parseCharClass(): Token {
        val start = pos - 1
        val sb = StringBuilder("[")
        
        while (pos < input.length) {
            val ch = input[pos]
            if (ch == ']') {
                sb.append(']')
                pos++
                return Token(TokenType.CHAR_CLASS, sb.toString(), start)
            }
            if (ch == '\\' && pos + 1 < input.length) {
                sb.append(input[pos])
                sb.append(input[pos + 1])
                pos += 2
            } else {
                sb.append(ch)
                pos++
            }
        }
        
        return Token(TokenType.CHAR_CLASS, sb.toString(), start)
    }
    
    private fun parseEscape(): Token {
        val start = pos - 1
        
        if (pos >= input.length) return Token(TokenType.ESCAPE_LITERAL, "\\")
        
        val ch = input[pos]
        pos++
        
        return when (ch) {
            'd' -> Token(TokenType.ESCAPE_DIGIT, "\\d")
            'D' -> Token(TokenType.ESCAPE_NON_DIGIT, "\\D")
            'w' -> Token(TokenType.ESCAPE_WORD, "\\w")
            'W' -> Token(TokenType.ESCAPE_NON_WORD, "\\W")
            's' -> Token(TokenType.ESCAPE_SPACE, "\\s")
            'S' -> Token(TokenType.ESCAPE_NON_SPACE, "\\S")
            'b' -> Token(TokenType.ESCAPE_WORD_BOUNDARY, "\\b")
            'B' -> Token(TokenType.ESCAPE_NON_WORD_BOUNDARY, "\\B")
            'A' -> Token(TokenType.ESCAPE_INPUT_START, "\\A")
            'Z' -> Token(TokenType.ESCAPE_INPUT_END, "\\Z")
            'z' -> Token(TokenType.ESCAPE_INPUT_END_STRICT, "\\z")
            'n' -> Token(TokenType.ESCAPE_N, "\\n")
            'r' -> Token(TokenType.ESCAPE_R, "\\r")
            't' -> Token(TokenType.ESCAPE_T, "\\t")
            'f' -> Token(TokenType.ESCAPE_F, "\\f")
            '0' -> Token(TokenType.ESCAPE_0, "\\0")
            'k' -> {
                if (pos < input.length && input[pos] == '<') {
                    pos++
                    val nameEnd = input.indexOf('>', pos)
                    if (nameEnd > pos) {
                        val name = input.substring(pos, nameEnd)
                        pos = nameEnd + 1
                        return Token(TokenType.BACK_REFERENCE_NAMED, "\\k<$name>", start)
                    }
                }
                Token(TokenType.ESCAPE_LITERAL, "\\k")
            }
            '1', '2', '3', '4', '5', '6', '7', '8', '9' -> 
                Token(TokenType.BACK_REFERENCE_NUMERIC, "\\$ch")
            'u' -> {
                if (pos + 4 <= input.length) {
                    val hex = input.substring(pos, pos + 4)
                    if (hex.matches(Regex("[0-9a-fA-F]{4}"))) {
                        pos += 4
                        return Token(TokenType.ESCAPE_UNICODE, "\\u$hex", start)
                    }
                }
                Token(TokenType.ESCAPE_LITERAL, "\\u")
            }
            'x' -> {
                if (pos + 2 <= input.length) {
                    val hex = input.substring(pos, pos + 2)
                    if (hex.matches(Regex("[0-9a-fA-F]{2}"))) {
                        pos += 2
                        return Token(TokenType.ESCAPE_HEX, "\\x$hex", start)
                    }
                }
                Token(TokenType.ESCAPE_LITERAL, "\\x")
            }
            else -> Token(TokenType.ESCAPE_LITERAL, "\\$ch")
        }
    }
    
    private fun parseQuantifierRange(): Token {
        val start = pos - 1
        val sb = StringBuilder("{")
        
        while (pos < input.length) {
            val ch = input[pos]
            if (ch == '}') {
                sb.append('}')
                pos++
                return Token(TokenType.QUANTIFIER_RANGE, sb.toString(), start)
            }
            if (ch.isDigit() || ch == ',') {
                sb.append(ch)
                pos++
            } else {
                return Token(TokenType.QUANTIFIER_RANGE, sb.toString(), start)
            }
        }
        
        return Token(TokenType.QUANTIFIER_RANGE, sb.toString(), start)
    }
}
