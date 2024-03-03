package com.github.kitakkun.aspectk.expression.tokenparser

import com.github.kitakkun.aspectk.expression.lexer.AspectKToken
import com.github.kitakkun.aspectk.expression.lexer.AspectKTokenType

data class ArgsToken(
    val type: ArgsTokenType,
    val lexeme: String,
)

enum class ArgsTokenType {
    PACKAGE_PART,
    CLASS,
    SINGLE_ANY_ARG,
    NONE_OR_MORE_ARGS,
    VAR_ARG,
    ARG_SEPARATOR,
}

class ArgsTokenParser(
    private val rawTokens: List<AspectKToken>,
) {
    private val tokens = mutableListOf<ArgsToken>()
    private var current = 0
    private val isAtEnd get() = current >= rawTokens.size

    fun parseTokens(): List<ArgsToken> {
        while (!isAtEnd) {
            scanToken()
        }

        return tokens.dropLastWhile { it.type == ArgsTokenType.ARG_SEPARATOR }
    }

    private fun scanToken() {
        val token = advance()

        when (token.type) {
            AspectKTokenType.STAR -> {
                when {
                    match(AspectKTokenType.SLASH) -> addToken(ArgsTokenType.PACKAGE_PART, token.lexeme)
                    match(AspectKTokenType.DOT) -> addToken(ArgsTokenType.CLASS, token.lexeme)
                    match(AspectKTokenType.COMMA) || isAtEnd -> {
                        when(tokens.lastOrNull()?.type) {
                            ArgsTokenType.CLASS, ArgsTokenType.PACKAGE_PART -> addToken(ArgsTokenType.CLASS, token.lexeme)
                            ArgsTokenType.ARG_SEPARATOR, null -> addToken(ArgsTokenType.SINGLE_ANY_ARG, token.lexeme)
                            else -> error("Unexpected token type $token")
                        }
                        if (previous().type == AspectKTokenType.COMMA) {
                            addToken(ArgsTokenType.ARG_SEPARATOR, previous().lexeme)
                        }
                    }
                }
            }


            AspectKTokenType.DOUBLE_STAR -> {
                if (match(AspectKTokenType.SLASH)) {
                    addToken(ArgsTokenType.PACKAGE_PART, token.lexeme)
                } else {
                    error("expected slash after double star")
                }
            }

            AspectKTokenType.COMMA -> addToken(ArgsTokenType.ARG_SEPARATOR, token.lexeme)

            AspectKTokenType.DOUBLE_DOT -> {
                if (match(AspectKTokenType.COMMA)) {
                    addToken(ArgsTokenType.NONE_OR_MORE_ARGS, token.lexeme)
                    addToken(ArgsTokenType.ARG_SEPARATOR, previous().lexeme)
                } else if (isAtEnd) {
                    addToken(ArgsTokenType.NONE_OR_MORE_ARGS, token.lexeme)
                } else {
                    error("expected comma after double dot")
                }
            }

            AspectKTokenType.IDENTIFIER -> {
                when {
                    match(AspectKTokenType.SLASH) -> {
                        // package part
                        // ex) com/
                        addToken(ArgsTokenType.PACKAGE_PART, token.lexeme)
                    }

                    match(AspectKTokenType.DOT) -> {
                        // nested class
                        // ex) Foo
                        addToken(ArgsTokenType.CLASS, token.lexeme)
                    }

                    match(AspectKTokenType.TRIPLE_DOT) -> {
                        // varargs
                        // ex) String...
                        addToken(ArgsTokenType.CLASS, token.lexeme)
                        addToken(ArgsTokenType.VAR_ARG, previous().lexeme)
                    }

                    match(AspectKTokenType.COMMA) -> {
                        // single argument
                        // ex) String,
                        addToken(ArgsTokenType.CLASS, token.lexeme)
                        addToken(ArgsTokenType.ARG_SEPARATOR, previous().lexeme)
                    }

                    else -> {
                        addToken(ArgsTokenType.CLASS, token.lexeme)
                    }
                }
            }

            else -> error("Unexpected token type $token")
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
        return peek().type == type
    }

    private fun peek(): AspectKToken {
        return rawTokens[current]
    }

    private fun previous(): AspectKToken {
        return rawTokens[current - 1]
    }

    private fun addToken(type: ArgsTokenType, lexeme: String) {
        tokens.add(ArgsToken(type, lexeme))
    }
}
