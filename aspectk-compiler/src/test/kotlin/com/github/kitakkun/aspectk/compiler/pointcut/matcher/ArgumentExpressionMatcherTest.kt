package com.github.kitakkun.aspectk.compiler.pointcut.matcher

import com.github.kitakkun.aspectk.compiler.pointcut.expression.ArgumentExpression
import com.github.kitakkun.aspectk.compiler.pointcut.expression.ArgumentMatchingExpression
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
        val expression = ArgumentExpression(
            listOf(
                ArgumentMatchingExpression.Type(classId("kotlin", "String")),
                ArgumentMatchingExpression.Type(classId("kotlin", "Int")),
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
        val expression = ArgumentExpression(
            listOf(
                ArgumentMatchingExpression.Type(classId("kotlin", "String")),
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
        val expression = ArgumentExpression(
            listOf(
                ArgumentMatchingExpression.Type(classId("kotlin", "String")),
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
        val expression = ArgumentExpression(
            listOf(
                ArgumentMatchingExpression.Type(classId("kotlin", "String")),
                ArgumentMatchingExpression.Vararg(classId("kotlin", "Int")),
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
        val expression = ArgumentExpression(
            listOf(
                ArgumentMatchingExpression.AnyZeroOrMore,
                ArgumentMatchingExpression.Type(classId("kotlin", "Int")),
                ArgumentMatchingExpression.AnyZeroOrMore,
            )
        )
        val argsMatcher = ArgumentExpressionMatcher(expression)
        val result = argsMatcher.matches(valueParameterClassIds = valueParameterClassIds, lastIsVarArg = false)
        assertTrue { result }
    }
}
