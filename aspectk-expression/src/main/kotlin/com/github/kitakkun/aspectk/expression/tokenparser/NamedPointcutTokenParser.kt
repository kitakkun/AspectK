package com.github.kitakkun.aspectk.expression.tokenparser

import com.github.kitakkun.aspectk.expression.lexer.AspectKToken
import com.github.kitakkun.aspectk.expression.lexer.AspectKTokenType

data class NamedPointcutToken(
    val type: NamedPointcutTokenType,
    val lexeme: String,
)

enum class NamedPointcutTokenType {
    PACKAGE,
    CLASS,
    FUNCTION,
}

class NamedPointcutTokenParser(
    private val rawTokens: List<AspectKToken>,
) {
    private val tokens = mutableListOf<NamedPointcutToken>()
    private var current = 0
    private val isAtEnd get() = current >= rawTokens.size

    fun parseToNamedPointcutTokens(): List<NamedPointcutToken> {
        while (!isAtEnd) {
            scanToken()
        }
        return tokens
    }

    private fun scanToken() {
        val token = advance()

        when {
            match(AspectKTokenType.SLASH) -> {
                addToken(NamedPointcutTokenType.PACKAGE, token.lexeme)
            }

            match(AspectKTokenType.DOT) -> {
                addToken(NamedPointcutTokenType.CLASS, token.lexeme)
            }

            isAtEnd -> {
                addToken(NamedPointcutTokenType.FUNCTION, token.lexeme)
            }
        }
    }

    private fun advance(): AspectKToken {
        return rawTokens[current++]
    }

    private fun match(vararg types: AspectKTokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun check(type: AspectKTokenType): Boolean {
        if (isAtEnd) return false
        return rawTokens[current].type == type
    }

    private fun addToken(
        type: NamedPointcutTokenType,
        lexeme: String,
    ) {
        tokens.add(NamedPointcutToken(type, lexeme))
    }
}
