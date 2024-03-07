package com.github.kitakkun.aspectk.expression.expressionparser

import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.lexer.AspectKToken
import com.github.kitakkun.aspectk.expression.lexer.AspectKTokenType
import com.github.kitakkun.aspectk.expression.tokenparser.ArgsTokenParser
import com.github.kitakkun.aspectk.expression.tokenparser.ExecutionTokenParser
import com.github.kitakkun.aspectk.expression.tokenparser.NamedPointcutTokenParser

class PointcutExpressionParser(
    private val tokens: List<AspectKToken>,
) {
    private var current = 0
    private val isAtEnd get() = current >= tokens.size

    fun expression(): PointcutExpression {
        if (tokens.isEmpty()) return PointcutExpression.Empty
        if (isAtEnd) throw IllegalStateException("parse operation can be called only once.")
        return or()
    }

    private fun or(): PointcutExpression {
        var left: PointcutExpression = and()

        while (match(AspectKTokenType.OR)) {
            val right = and()
            left = PointcutExpression.Or(left, right)
        }

        return left
    }

    private fun and(): PointcutExpression {
        var left: PointcutExpression = not()

        while (match(AspectKTokenType.AND)) {
            val right = not()
            left = PointcutExpression.And(left, right)
        }

        return left
    }

    private fun not(): PointcutExpression {
        if (match(AspectKTokenType.NOT)) {
            return PointcutExpression.Not(not())
        }

        return pointcut()
    }

    private fun pointcut(): PointcutExpression {
        if (!match(AspectKTokenType.IDENTIFIER)) {
            error("Expected pointcut expression")
        }

        val identifier = previous()

        if (!match(AspectKTokenType.LEFT_PAREN)) {
            error("Expected '(' after pointcut identifier")
        }

        val argTokensStart = current

        var depth = 1
        while (!isAtEnd) {
            when (advance().type) {
                AspectKTokenType.LEFT_PAREN -> depth++
                AspectKTokenType.RIGHT_PAREN -> depth--
                else -> {}
            }
            if (depth == 0) break
        }

        if (depth != 0) {
            error("Unclosed parenthesis")
        }

        val expressionTokens = tokens.subList(argTokensStart, current - 1)

        return processPointcutExpression(identifier, expressionTokens)
    }

    private fun processPointcutExpression(
        identifier: AspectKToken,
        expressionTokens: List<AspectKToken>,
    ): PointcutExpression {
        return when (identifier.lexeme) {
            "execution" -> execution(expressionTokens)
            "args" -> args(expressionTokens)
            "named" -> namedPointcut(expressionTokens)
            else -> throw IllegalStateException("Unknown pointcut identifier: ${identifier.lexeme}")
        }
    }

    private fun execution(expressionTokens: List<AspectKToken>): PointcutExpression.Execution {
        val (executionTokens, argsTokens) = ExecutionTokenParser(expressionTokens).parseToExecutionTokens()
        val expressionParser = ExecutionExpressionParser(executionTokens, argsTokens)

        return expressionParser.parse()
    }

    private fun args(expressionTokens: List<AspectKToken>): PointcutExpression.Args {
        val tokenParser = ArgsTokenParser(expressionTokens)
        val expressionParser = ArgsExpressionParser(tokenParser.parseTokens())
        return expressionParser.parse()
    }

    private fun namedPointcut(expressionTokens: List<AspectKToken>): PointcutExpression {
        val tokenParser = NamedPointcutTokenParser(expressionTokens)
        val expressionParser = NamedPointcutExpressionParser(tokenParser.parseToNamedPointcutTokens())
        return expressionParser.parse()
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

    private fun advance(): AspectKToken {
        return tokens[current++]
    }

    private fun peek(): AspectKToken {
        return tokens[current]
    }

    private fun previous(): AspectKToken {
        return tokens[current - 1]
    }
}
