package com.github.kitakkun.aspectk.expression.lexer

class AspectKPointcutExpressionLexer(private val expression: String) {
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

    private fun peekNext(): Char {
        if (current + 1 >= expression.length) return '\u0000'
        return expression[current + 1]
    }

    private fun scanToken() {
        when (advance()) {
            '(' -> pointcutLiteral()

            ')' -> {
                // Right parenthesis is handled when matching left parenthesis
                // FIXME: should throw an error if right parenthesis is found
            }

            '&' -> if (match('&')) addToken(AspectKTokenType.AND)
            '|' -> if (match('|')) addToken(AspectKTokenType.OR)
            '!' -> addToken(AspectKTokenType.NOT)
            ' ' -> while (peek() == ' ') {
                advance()
            }

            '@' -> annotation()
            else -> identifier()
        }
    }

    private fun pointcutLiteral() {
        addToken(AspectKTokenType.LEFT_PAREN)
        var parenthesisCount = 1

        while (!isAtEnd && parenthesisCount > 0) {
            val c = advance()
            if (c == '(') {
                parenthesisCount++
            } else if (c == ')') {
                parenthesisCount--
            }
        }

        if (parenthesisCount > 0) {
            error("Unmatched left parenthesis exists")
        }

        start += 1
        current -= 1
        addToken(AspectKTokenType.POINTCUT_STRING_LITERAL)

        start = current
        current += 1
        addToken(AspectKTokenType.RIGHT_PAREN)
    }

    private fun annotation() {
        while (!isAtEnd && isAlpha(peek())) {
            advance()
        }
        when (expression.substring(start + 1, current)) {
            "args" -> addToken(AspectKTokenType.ANNOTATED_ARGS)
            "annotation" -> addToken(AspectKTokenType.ANNOTATED_METHOD)
            "target" -> addToken(AspectKTokenType.ANNOTATED_TARGET)
            "within" -> addToken(AspectKTokenType.ANNOTATED_WITHIN)
            else -> error("Invalid annotation")
        }
    }

    private fun identifier() {
        while (!isAtEnd && isAlpha(peek())) {
            advance()
        }
        when (expression.substring(start, current)) {
            // pointcut keywords
            "execution" -> addToken(AspectKTokenType.EXECUTION)
            "within" -> addToken(AspectKTokenType.WITHIN)
            "args" -> addToken(AspectKTokenType.ARGS)
            "target" -> addToken(AspectKTokenType.TARGET)
            "this" -> addToken(AspectKTokenType.THIS)
            else -> addToken(AspectKTokenType.UNKNOWN_POINTCUT)
        }
    }

    private fun isAlpha(c: Char): Boolean {
        // FIXME: No Japanese or Chinese name support
        return c in 'a'..'z' || c in 'A'..'Z' || c == '_'
    }
}
