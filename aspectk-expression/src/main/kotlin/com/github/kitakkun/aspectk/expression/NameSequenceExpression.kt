package com.github.kitakkun.aspectk.expression

sealed class NameSequenceExpression {
    companion object {
        fun fromString(expression: String): NameSequenceExpression {
            val names = expression.split(".", "/").filter { it.isNotEmpty() }.map { NameExpression.fromString(it) }
            if (names.isEmpty()) return Empty
            return Sequence(names)
        }

        fun fromExpressions(expressions: List<NameExpression>): NameSequenceExpression {
            if (expressions.isEmpty()) return Empty
            return Sequence(expressions)
        }
    }

    data object Empty : NameSequenceExpression()

    data class Sequence(val names: List<NameExpression>) : NameSequenceExpression()
}
