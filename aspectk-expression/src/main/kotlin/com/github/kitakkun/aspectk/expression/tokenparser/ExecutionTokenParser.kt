package com.github.kitakkun.aspectk.expression.tokenparser

import com.github.kitakkun.aspectk.expression.lexer.AspectKToken
import com.github.kitakkun.aspectk.expression.lexer.AspectKTokenType

data class ExecutionToken(
    val type: ExecutionTokenType,
    val lexeme: String,
)

enum class ExecutionTokenType {
    MODIFIER,
    PACKAGE_PART,
    CLASS,
    CLASS_INCLUDING_SUBCLASS,
    FUNCTION,
    RETURN_TYPE_START,
    RETURN_TYPE_END,
}

private enum class ExecutionExpressionResolvingContext {
    MODIFIER,
    PACKAGE,
    CLASS,
    FUNCTION,
    RETURN_TYPE,
}

class ExecutionTokenParser(
    private val expressionTokens: List<AspectKToken>,
) {
    private var context: ExecutionExpressionResolvingContext = ExecutionExpressionResolvingContext.MODIFIER
    private val tokens = mutableListOf<ExecutionToken>()
    private val argsTokens = mutableListOf<ArgsToken>()
    private var current = 0
    private val isAtEnd get() = current >= expressionTokens.size

    private fun peek(): AspectKToken {
        return expressionTokens[current]
    }

    private fun peekNext(): AspectKToken? {
        if (isAtEnd) return null
        return expressionTokens[current + 1]
    }

    fun parseToExecutionTokens(): Pair<List<ExecutionToken>, List<ArgsToken>> {
        while (!isAtEnd) {
            scanToken()
        }
        return Pair(tokens, argsTokens)
    }

    private fun scanToken() {
        val token = advance()

        when (context) {
            ExecutionExpressionResolvingContext.MODIFIER -> tryParseModifier(token)
            ExecutionExpressionResolvingContext.PACKAGE -> tryParsePackage(token)
            ExecutionExpressionResolvingContext.CLASS -> tryParseClass(token)
            ExecutionExpressionResolvingContext.FUNCTION -> tryParseFunction(token)
            ExecutionExpressionResolvingContext.RETURN_TYPE -> tryParseReturnType(token)
        }
    }

    /**
     * ex) public com/example/A.B.foo(Int, String...): String
     *     ^^^^^^
     */
    private fun tryParseModifier(token: AspectKToken) {
        // modifier token must be an identifier or a star
        // ex: public, *
        if (token.type !in listOf(AspectKTokenType.IDENTIFIER, AspectKTokenType.STAR)) error("expected identifier or star")

        when (peek().type) {
            // if the next token is a slash, it's a package name
            AspectKTokenType.SLASH -> {
                context = ExecutionExpressionResolvingContext.PACKAGE
                tryParsePackage(token)
            }

            // if the next token is a double star, it's a package name
            AspectKTokenType.DOUBLE_STAR -> {
                context = ExecutionExpressionResolvingContext.PACKAGE
            }

            // if the next token is a dot, it's a class name
            AspectKTokenType.DOT -> {
                context = ExecutionExpressionResolvingContext.CLASS
                tryParseClass(token)
            }

            // if the next token is a left parenthesis, it's a function name
            AspectKTokenType.LEFT_PAREN -> {
                context = ExecutionExpressionResolvingContext.FUNCTION
                tryParseFunction(token)
            }

            else -> {
                addToken(ExecutionTokenType.MODIFIER, token.lexeme)
            }
        }
    }

    /**
     * ex) public com/example/A.B.foo(Int, String...): String
     *            ^^^ ^^^^^^^
     */
    private fun tryParsePackage(token: AspectKToken) {
        if (token.type !in listOf(AspectKTokenType.IDENTIFIER, AspectKTokenType.DOUBLE_STAR, AspectKTokenType.STAR)) return
        if (match(AspectKTokenType.SLASH)) {
            addToken(ExecutionTokenType.PACKAGE_PART, token.lexeme)
        } else {
            error("expected slash after package part")
        }

        when (peekNext()?.type) {
            AspectKTokenType.SLASH -> {
                context = ExecutionExpressionResolvingContext.PACKAGE
            }

            AspectKTokenType.DOT -> {
                context = ExecutionExpressionResolvingContext.CLASS
            }

            AspectKTokenType.LEFT_PAREN -> {
                context = ExecutionExpressionResolvingContext.FUNCTION
            }

            else -> {
                error("unexpected token")
            }
        }
    }

    /**
     * ex) public com/example/A.B.foo(Int, String...): String
     *                        ^ ^
     */
    private fun tryParseClass(token: AspectKToken) {
        if (token.type !in listOf(AspectKTokenType.IDENTIFIER, AspectKTokenType.STAR)) return

        when {
            match(AspectKTokenType.DOT) -> {
                addToken(ExecutionTokenType.CLASS, token.lexeme)
                context = when (peekNext()?.type) {
                    AspectKTokenType.DOT -> ExecutionExpressionResolvingContext.CLASS
                    AspectKTokenType.LEFT_PAREN -> ExecutionExpressionResolvingContext.FUNCTION
                    else -> error("unexpected token")
                }
            }

            match(AspectKTokenType.PLUS) && match(AspectKTokenType.DOT) -> {
                addToken(ExecutionTokenType.CLASS_INCLUDING_SUBCLASS, token.lexeme)
                context = ExecutionExpressionResolvingContext.FUNCTION
            }

            else -> error("expected '.' or '+.' character after class name")
        }
    }

    /**
     * ex) public com/example/A.B.foo(Int, String...): String
     *                            ^^^^^^^^^^^^^^^^^^^
     */
    private fun tryParseFunction(token: AspectKToken) {
        if (token.type !in listOf(AspectKTokenType.IDENTIFIER, AspectKTokenType.STAR)) return
        if (match(AspectKTokenType.LEFT_PAREN)) {
            addToken(ExecutionTokenType.FUNCTION, token.lexeme)
        } else {
            error("expected left parenthesis after function name")
        }

        val start = current

        var depth = 1
        while (!isAtEnd) {
            when (advance().type) {
                AspectKTokenType.LEFT_PAREN -> depth++
                AspectKTokenType.RIGHT_PAREN -> depth--
                else -> {}
            }
            if (depth == 0) break
        }
        val end = current - 1

        val argsRawTokens = expressionTokens.subList(start, end)
        val argsTokenTransformer = ArgsTokenParser(argsRawTokens)
        argsTokens.addAll(argsTokenTransformer.parseTokens())

        context = ExecutionExpressionResolvingContext.RETURN_TYPE
    }

    private fun tryParseReturnType(token: AspectKToken) {
        when (token.type) {
            AspectKTokenType.COLON -> {
                addToken(ExecutionTokenType.RETURN_TYPE_START, token.lexeme)
            }

            AspectKTokenType.IDENTIFIER -> {
                if (match(AspectKTokenType.SLASH)) {
                    addToken(ExecutionTokenType.PACKAGE_PART, token.lexeme)
                } else if (match(AspectKTokenType.DOT)) {
                    addToken(ExecutionTokenType.CLASS, token.lexeme)
                } else {
                    addToken(ExecutionTokenType.CLASS, token.lexeme)
                    addToken(ExecutionTokenType.RETURN_TYPE_END, "")
                }
            }

            else -> {
                error("unexpected token")
            }
        }
    }

    private fun addToken(
        type: ExecutionTokenType,
        lexeme: String,
    ) {
        tokens.add(ExecutionToken(type, lexeme))
    }

    private fun advance(): AspectKToken {
        return expressionTokens[current++]
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
}
