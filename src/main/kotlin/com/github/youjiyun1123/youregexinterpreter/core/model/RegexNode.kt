package com.github.youjiyun1123.youregexinterpreter.core.model

/**
 * 正则表达式语法树节点基类
 */
sealed class RegexNode {
    abstract fun accept(visitor: RegexVisitor): String
}

/**
 * 字面量节点
 */
data class Literal(val chars: String) : RegexNode() {
    override fun accept(visitor: RegexVisitor): String = visitor.visit(this)
}

/**
 * 预定义字符类
 */
enum class PredefinedClass {
    DIGIT, NON_DIGIT, WORD, NON_WORD, WHITESPACE, NON_WHITESPACE, ANY
}

/**
 * 字符范围
 */
data class RegexCharRange(val start: Char, val end: Char)

/**
 * 字符类节点
 */
data class CharClass(
    val ranges: List<RegexCharRange> = emptyList(),
    val negated: Boolean = false,
    val predefined: PredefinedClass? = null
) : RegexNode() {
    override fun accept(visitor: RegexVisitor): String = visitor.visit(this)
}

/**
 * 量词类型
 */
enum class QuantifierType { GREEDY, RELUCTANT, POSSESSIVE }

/**
 * 量词节点
 */
data class Quantifier(
    val child: RegexNode,
    val min: Int,
    val max: Int?,
    val type: QuantifierType = QuantifierType.GREEDY
) : RegexNode() {
    init {
        require(min >= 0)
        require(max == null || max >= min)
    }
    override fun accept(visitor: RegexVisitor): String = visitor.visit(this)
}

/**
 * 分组类型
 */
enum class GroupType {
    CAPTURING, NON_CAPTURING, NAMED_CAPTURING,
    LOOKAHEAD, NEGATIVE_LOOKAHEAD, LOOKBEHIND, NEGATIVE_LOOKBEHIND
}

/**
 * 分组节点
 */
data class Group(
    val children: List<RegexNode>,
    val type: GroupType,
    val name: String? = null,
    val index: Int? = null
) : RegexNode() {
    override fun accept(visitor: RegexVisitor): String = visitor.visit(this)
}

/**
 * 选择节点
 */
data class Alternation(val alternatives: List<RegexNode>) : RegexNode() {
    override fun accept(visitor: RegexVisitor): String = visitor.visit(this)
}

/**
 * 锚点类型
 */
enum class AnchorType {
    LINE_START, LINE_END, INPUT_START, INPUT_END, INPUT_END_ANY,
    WORD_BOUNDARY, NON_WORD_BOUNDARY
}

/**
 * 锚点节点
 */
data class Anchor(val type: AnchorType) : RegexNode() {
    override fun accept(visitor: RegexVisitor): String = visitor.visit(this)
}

/**
 * 回溯引用节点
 */
data class BackReference(val index: Int? = null, val name: String? = null) : RegexNode() {
    override fun accept(visitor: RegexVisitor): String = visitor.visit(this)
}

/**
 * 转义类型
 */
enum class EscapeType {
    N, R, T, F, BACKSLASH, DOT, STAR, PLUS, QUESTION, OCTAL, HEX, UNICODE
}

/**
 * 转义序列节点
 */
data class Escape(val type: EscapeType, val value: String? = null) : RegexNode() {
    override fun accept(visitor: RegexVisitor): String = visitor.visit(this)
}

/**
 * 序列节点
 */
data class Sequence(val children: List<RegexNode>) : RegexNode() {
    override fun accept(visitor: RegexVisitor): String = visitor.visit(this)
}

/**
 * Visitor 模式接口
 */
interface RegexVisitor {
    fun visit(node: Literal): String
    fun visit(node: CharClass): String
    fun visit(node: Quantifier): String
    fun visit(node: Group): String
    fun visit(node: Alternation): String
    fun visit(node: Anchor): String
    fun visit(node: BackReference): String
    fun visit(node: Escape): String
    fun visit(node: Sequence): String
}
