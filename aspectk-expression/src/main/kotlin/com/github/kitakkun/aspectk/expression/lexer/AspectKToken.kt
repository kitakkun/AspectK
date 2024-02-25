package com.github.kitakkun.aspectk.expression.lexer

data class AspectKToken(
    val type: AspectKTokenType,
    val lexeme: String,
)

enum class AspectKTokenType {
    // parentheses
    LEFT_PAREN, RIGHT_PAREN,

    // primitive pointcut keywords
    EXECUTION, WITHIN, ARGS, TARGET, THIS,
    ANNOTATED_ARGS, ANNOTATED_METHOD, ANNOTATED_TARGET, ANNOTATED_WITHIN,

    // user-defined pointcut keywords
    UNKNOWN_POINTCUT,

    // pointcut string literal
    POINTCUT_STRING_LITERAL,

    // pointcut operators
    AND, OR, NOT,
}
