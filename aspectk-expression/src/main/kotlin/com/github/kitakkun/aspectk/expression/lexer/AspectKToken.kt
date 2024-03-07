package com.github.kitakkun.aspectk.expression.lexer

data class AspectKToken(
    val type: AspectKTokenType,
    val lexeme: String,
)

enum class AspectKTokenType {
    // parentheses
    LEFT_PAREN,
    RIGHT_PAREN,

    // pointcut operators
    AND,
    OR,
    NOT,

    // wildcard
    STAR,
    DOUBLE_STAR,

    SLASH,
    COMMA,
    DOT,
    COLON,
    DOUBLE_DOT,
    TRIPLE_DOT,

    IDENTIFIER,
}
