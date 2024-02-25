package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.ArgumentMatchingExpression
import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.TypeMatchingExpression
import org.jetbrains.kotlin.javac.resolve.classId
import org.junit.Test
import kotlin.test.assertTrue

class ArgumentExpressionMatcherTest {
    @Test
    fun basic() {
        val valueParameterClassIds = listOf(
            classId("kotlin", "String"),
            classId("kotlin", "Int"),
        )
        val expression = PointcutExpression.Args(
            listOf(
                ArgumentMatchingExpression.Type(TypeMatchingExpression.Class("kotlin", "String")),
                ArgumentMatchingExpression.Type(TypeMatchingExpression.Class("kotlin", "Int")),
            )
        )
        val argsMatcher = ArgumentExpressionMatcher(expression)
        val result = argsMatcher.matches(valueParameterClassIds = valueParameterClassIds, lastIsVarArg = false)
        assertTrue { result }
    }

    @Test
    fun withSingleAnyMatching() {
        val valueParameterClassIds = listOf(
            classId("kotlin", "String"),
            classId("kotlin", "Int"),
        )
        val expression = PointcutExpression.Args(
            listOf(
                ArgumentMatchingExpression.Type(TypeMatchingExpression.Class("kotlin", "String")),
                ArgumentMatchingExpression.AnySingle,
            )
        )
        val argsMatcher = ArgumentExpressionMatcher(expression)
        val result = argsMatcher.matches(valueParameterClassIds = valueParameterClassIds, lastIsVarArg = false)
        assertTrue { result }
    }

    @Test
    fun withAnyZeroOrMoreMatching() {
        val valueParameterClassIds = listOf(
            classId("kotlin", "String"),
            classId("kotlin", "Int"),
        )
        val expression = PointcutExpression.Args(
            listOf(
                ArgumentMatchingExpression.Type(TypeMatchingExpression.Class("kotlin", "String")),
                ArgumentMatchingExpression.AnyZeroOrMore,
            )
        )
        val argsMatcher = ArgumentExpressionMatcher(expression)
        val result = argsMatcher.matches(valueParameterClassIds = valueParameterClassIds, lastIsVarArg = false)
        assertTrue { result }
    }

    @Test
    fun withVarargMatching() {
        val valueParameterClassIds = listOf(
            classId("kotlin", "String"),
            classId("kotlin", "Int"),
        )
        val expression = PointcutExpression.Args(
            listOf(
                ArgumentMatchingExpression.Type(TypeMatchingExpression.Class("kotlin", "String")),
                ArgumentMatchingExpression.Vararg(TypeMatchingExpression.Class("kotlin", "Int")),
            )
        )
        val argsMatcher = ArgumentExpressionMatcher(expression)
        val result = argsMatcher.matches(valueParameterClassIds = valueParameterClassIds, lastIsVarArg = true)
        assertTrue { result }
    }

    @Test
    fun anyZeroOrMoreSandwiched() {
        val valueParameterClassIds = listOf(
            classId("kotlin", "String"),
            classId("kotlin", "Int"),
        )
        val expression = PointcutExpression.Args(
            listOf(
                ArgumentMatchingExpression.AnyZeroOrMore,
                ArgumentMatchingExpression.Type(TypeMatchingExpression.Class("kotlin", "Int")),
                ArgumentMatchingExpression.AnyZeroOrMore,
            )
        )
        val argsMatcher = ArgumentExpressionMatcher(expression)
        val result = argsMatcher.matches(valueParameterClassIds = valueParameterClassIds, lastIsVarArg = false)
        assertTrue { result }
    }
}
