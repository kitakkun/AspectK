package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.NameExpression
import com.github.kitakkun.aspectk.expression.NameSequenceExpression

class NameSequenceExpressionMatcher(private val expression: NameSequenceExpression) {
    fun matches(name: String): Boolean {
        val nameSequence = name.split(".", "/").filter { it.isNotEmpty() }

        return matches(nameSequence)
    }

    fun matches(nameSequence: List<String>): Boolean {
        when (expression) {
            is NameSequenceExpression.Empty -> return nameSequence.isEmpty()
            is NameSequenceExpression.Sequence -> {
                nameSequence.forEachIndexed { index, name ->
                    val correspondingExpression = expression.names.getOrElse(index) { return false }
                    val matcher = NameExpressionMatcher(correspondingExpression)
                    if (!matcher.matches(name)) {
                        return false
                    }
                    if (correspondingExpression is NameExpression.Recursive) {
                        // check if the remaining names match with the remaining expression without consuming the current name expression
                        val remainingExpressions = expression.names.subList(index, expression.names.size)
                        val remainingNames = nameSequence.subList(index + 1, nameSequence.size)
                        val remainingNamesMatcher = NameSequenceExpressionMatcher(NameSequenceExpression.fromExpressions(remainingExpressions))
                        if (remainingNamesMatcher.matches(remainingNames)) {
                            return true
                        }
                    }
                }

                val remainingExpressions = expression.names.subList(nameSequence.size, expression.names.size)
                return remainingExpressions.all { it is NameExpression.Recursive }
            }
        }
    }
}
