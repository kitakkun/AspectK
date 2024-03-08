package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.NameExpression

class NameExpressionSequenceMatcher(
    private val nameExpressions: List<NameExpression>,
) {
    fun matches(names: List<String>): Boolean {
        var current = 0

        names.forEachIndexed { index, name ->
            val nameExpression = nameExpressions.getOrNull(current) ?: return false
            if (NameExpressionMatcher(nameExpression).matches(name)) {
                current++
                if ((nameExpression is NameExpression.Recursive)) {
                    // FIXME: currently, recursive name expression is only supported at the end of the sequence
                    return true
                }
            }
        }

        val remainingExpressions = nameExpressions.subList(current, nameExpressions.size)
        return remainingExpressions.all { it is NameExpression.Recursive }
    }
}
