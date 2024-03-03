package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.NameExpression
import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.expressionparser.ArgMatchingExpression
import org.jetbrains.kotlin.javac.resolve.classId
import kotlin.test.Test
import kotlin.test.assertTrue

class ArgsExpressionMatcherTest {
    @Test
    fun basic() {
        val valueParameterClassIds = listOf(
            classId("kotlin", "String"),
            classId("kotlin", "Int"),
        )
        val expression = PointcutExpression.Args(
            args = listOf(
                ArgMatchingExpression.Class(
                    packageNames = listOf(NameExpression.fromString("kotlin")),
                    classNames = listOf(NameExpression.fromString("String"))
                ),
                ArgMatchingExpression.Class(
                    packageNames = listOf(NameExpression.fromString("kotlin")),
                    classNames = listOf(NameExpression.fromString("Int"))
                ),
            ),
            lastIsVarArg = false,
        )
        val argsMatcher = ArgsExpressionMatcher(expression)
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
            args = listOf(
                ArgMatchingExpression.Class(
                    packageNames = listOf(NameExpression.fromString("kotlin")),
                    classNames = listOf(NameExpression.fromString("String"))
                ),
                ArgMatchingExpression.AnySingle
            ),
            lastIsVarArg = false,
        )
        val argsMatcher = ArgsExpressionMatcher(expression)
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
            args = listOf(
                ArgMatchingExpression.Class(
                    packageNames = listOf(NameExpression.fromString("kotlin")),
                    classNames = listOf(NameExpression.fromString("String"))
                ),
                ArgMatchingExpression.NoneOrMore
            ),
            lastIsVarArg = false,
        )
        val argsMatcher = ArgsExpressionMatcher(expression)
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
            args = listOf(
                ArgMatchingExpression.Class(
                    packageNames = listOf(NameExpression.fromString("kotlin")),
                    classNames = listOf(NameExpression.fromString("String"))
                ),
                ArgMatchingExpression.NoneOrMore
            ),
            lastIsVarArg = true,
        )
        val argsMatcher = ArgsExpressionMatcher(expression)
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
            args = listOf(
                ArgMatchingExpression.NoneOrMore,
                ArgMatchingExpression.Class(
                    packageNames = listOf(NameExpression.fromString("kotlin")),
                    classNames = listOf(NameExpression.fromString("Int"))
                ),
                ArgMatchingExpression.NoneOrMore
            ),
            lastIsVarArg = false,
        )
        val argsMatcher = ArgsExpressionMatcher(expression)
        val result = argsMatcher.matches(valueParameterClassIds = valueParameterClassIds, lastIsVarArg = false)
        assertTrue { result }
    }
}
