package com.github.kitakkun.aspectk.compiler.pointcut.lexer

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
            '(' -> addToken(AspectKTokenType.LEFT_PAREN)
            ')' -> addToken(AspectKTokenType.RIGHT_PAREN)
            '&' -> if (match('&')) addToken(AspectKTokenType.AND)
            '|' -> if (match('|')) addToken(AspectKTokenType.OR)
            '!' -> addToken(AspectKTokenType.NOT)
            '*' -> addToken(AspectKTokenType.STAR)
            '.' -> if (match('.')) addToken(AspectKTokenType.DOUBLE_DOT) else addToken(AspectKTokenType.DOT)
            ',' -> addToken(AspectKTokenType.COMMA)
            '?' -> addToken(AspectKTokenType.QUESTION)
            ' ' -> {} /* Ignore whitespace */
            '@' -> annotation()
            else -> identifier()
        }
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
            "execution" -> addToken(AspectKTokenType.EXECUTION)
            "within" -> addToken(AspectKTokenType.WITHIN)
            "args" -> addToken(AspectKTokenType.ARGS)
            "target" -> addToken(AspectKTokenType.TARGET)
            "this" -> addToken(AspectKTokenType.THIS)
            else -> addToken(AspectKTokenType.IDENTIFIER)
        }
    }

    private fun isAlpha(c: Char): Boolean {
        // FIXME: No Japanese or Chinese name support
        return c in 'a'..'z' || c in 'A'..'Z' || c == '_'
    }
}
