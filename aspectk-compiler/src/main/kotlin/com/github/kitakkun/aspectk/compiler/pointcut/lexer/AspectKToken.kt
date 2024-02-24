package com.github.kitakkun.aspectk.compiler.pointcut.lexer

data class AspectKToken(
    val type: AspectKTokenType,
    val lexeme: String,
)

enum class AspectKTokenType {
    LEFT_PAREN, RIGHT_PAREN,

    EXECUTION, WITHIN, ARGS, TARGET, THIS,
    ANNOTATED_ARGS, ANNOTATED_METHOD, ANNOTATED_TARGET, ANNOTATED_WITHIN,

    DOT, DOUBLE_DOT, COMMA, STAR, QUESTION, WHITESPACES,
    IDENTIFIER,

    AND, OR, NOT,
}
