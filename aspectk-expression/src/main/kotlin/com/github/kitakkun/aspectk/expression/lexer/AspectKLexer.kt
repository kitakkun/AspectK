package com.github.kitakkun.aspectk.expression.lexer

class AspectKLexer(private val expression: String) {
    private val tokens = mutableListOf<AspectKToken>()
    private var start = 0
    private var current = 0
    private val isAtEnd: Boolean get() = current >= expression.length

    fun analyze(): List<AspectKToken> {
        while (!isAtEnd) {
            start = current
            scanToken()
        }

        return tokens
    }

    private fun advance(): Char {
        return expression[current++]
    }

    private fun addToken(type: AspectKTokenType) {
        val lexeme = expression.substring(start, current)
        tokens.add(AspectKToken(type = type, lexeme = lexeme))
    }

    private fun match(expected: Char): Boolean {
        if (isAtEnd) return false
        if (expression[current] != expected) return false

        current++
        return true
    }

    private fun peek(): Char {
        if (isAtEnd) return '\u0000'
        return expression[current]
    }

    private fun scanToken() {
        when (advance()) {
            '(' -> addToken(AspectKTokenType.LEFT_PAREN)
            ')' -> addToken(AspectKTokenType.RIGHT_PAREN)

            // pointcut operators
            '&' -> if (match('&')) addToken(AspectKTokenType.AND)
            '|' -> if (match('|')) addToken(AspectKTokenType.OR)
            '!' -> addToken(AspectKTokenType.NOT)

            ':' -> addToken(AspectKTokenType.COLON) // function signature and its return type separator

            '/' -> addToken(AspectKTokenType.SLASH) // package separator

            ' ' -> {
                while (peek() == ' ') {
                    advance()
                }
            }

            ',' -> addToken(AspectKTokenType.COMMA) // argument separator

            '*' -> if (match('*')) addToken(AspectKTokenType.DOUBLE_STAR) else addToken(AspectKTokenType.STAR)

            '.' -> if (match('.')) {
                if (match('.')) {
                    addToken(AspectKTokenType.TRIPLE_DOT) // vararg
                } else {
                    addToken(AspectKTokenType.DOUBLE_DOT) // match zero or more
                }
            } else {
                addToken(AspectKTokenType.DOT) // class name and function name separator
            }

            else -> identifier()
        }
    }

    private fun identifier() {
        while (!isAtEnd && isIdentifierChar(peek())) {
            advance()
        }
        addToken(AspectKTokenType.IDENTIFIER)
    }

    private fun isIdentifierChar(c: Char): Boolean {
        // FIXME: No Japanese or Chinese name support
        return c in 'a'..'z' || c in 'A'..'Z' || c == '_' || c in '0'..'9'
    }
}
