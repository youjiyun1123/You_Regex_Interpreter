package com.github.youjiyun1123.youregexinterpreter.core.model

/**
 * 词法单元类型
 */
enum class TokenType {
    // 字面量
    LITERAL,
    
    // 元字符
    DOT,
    
    // 选择
    ALTERNATION,
    
    // 量词
    QUANTIFIER_STAR,
    QUANTIFIER_PLUS,
    QUANTIFIER_QUESTION,
    QUANTIFIER_RANGE,
    
    // 分组
    GROUP_OPEN,
    GROUP_CLOSE,
    GROUP_NON_CAPTURING,
    GROUP_LOOKAHEAD,
    GROUP_NEG_LOOKAHEAD,
    GROUP_LOOKBEHIND,
    GROUP_NEG_LOOKBEHIND,
    GROUP_NAMED_START,
    GROUP_QUESTION,
    
    // 字符类
    CHAR_CLASS,
    
    // 锚点
    ANCHOR_CARET,
    ANCHOR_DOLLAR,
    
    // 转义序列
    ESCAPE_DIGIT,
    ESCAPE_NON_DIGIT,
    ESCAPE_WORD,
    ESCAPE_NON_WORD,
    ESCAPE_SPACE,
    ESCAPE_NON_SPACE,
    ESCAPE_WORD_BOUNDARY,
    ESCAPE_NON_WORD_BOUNDARY,
    ESCAPE_INPUT_START,
    ESCAPE_INPUT_END,
    ESCAPE_INPUT_END_STRICT,
    ESCAPE_N,
    ESCAPE_R,
    ESCAPE_T,
    ESCAPE_F,
    ESCAPE_0,
    ESCAPE_UNICODE,
    ESCAPE_HEX,
    ESCAPE_LITERAL,
    
    // 回溯引用
    BACK_REFERENCE_NUMERIC,
    BACK_REFERENCE_NAMED,
    
    // 边界
    EOF
}

/**
 * 词法单元
 */
data class Token(
    val type: TokenType,
    val value: String,
    val position: Int = 0
)
