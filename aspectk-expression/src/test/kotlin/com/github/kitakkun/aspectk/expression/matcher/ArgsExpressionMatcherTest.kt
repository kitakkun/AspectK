package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.ClassSignatureExpression
import com.github.kitakkun.aspectk.expression.NameSequenceExpression
import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.expressionparser.ArgMatchingExpression
import com.github.kitakkun.aspectk.expression.model.ClassSignature
import kotlin.test.Test
import kotlin.test.assertTrue

class ArgsExpressionMatcherTest {
    @Test
    fun basic() {
        val valueParameterSignatures = listOf(
            ClassSignature("kotlin", "String"),
            ClassSignature("kotlin", "Int"),
        )
        val expression = PointcutExpression.Args(
            args = listOf(
                ArgMatchingExpression.Class(
                    expression = ClassSignatureExpression.Normal(
                        packageNames = NameSequenceExpression.fromString("kotlin"),
                        classNames = NameSequenceExpression.fromString("String"),
                    ),
                ),
                ArgMatchingExpression.Class(
                    expression = ClassSignatureExpression.Normal(
                        packageNames = NameSequenceExpression.fromString("kotlin"),
                        classNames = NameSequenceExpression.fromString("Int"),
                    ),
                ),
            ),
            lastIsVarArg = false,
        )
        val argsMatcher = ArgsExpressionMatcher(expression)
        val result = argsMatcher.matches(valueParameterClassSignatures = valueParameterSignatures, lastIsVarArg = false)
        assertTrue { result }
    }

    @Test
    fun withSingleAnyMatching() {
        val valueParameterSignatures = listOf(
            ClassSignature("kotlin", "String"),
            ClassSignature("kotlin", "Int"),
        )
        val expression = PointcutExpression.Args(
            args = listOf(
                ArgMatchingExpression.Class(
                    expression = ClassSignatureExpression.Normal(
                        packageNames = NameSequenceExpression.fromString("kotlin"),
                        classNames = NameSequenceExpression.fromString("String"),
                    ),
                ),
                ArgMatchingExpression.AnySingle,
            ),
            lastIsVarArg = false,
        )
        val argsMatcher = ArgsExpressionMatcher(expression)
        val result = argsMatcher.matches(valueParameterClassSignatures = valueParameterSignatures, lastIsVarArg = false)
        assertTrue { result }
    }

    @Test
    fun withAnyZeroOrMoreMatching() {
        val valueParameterSignatures = listOf(
            ClassSignature("kotlin", "String"),
            ClassSignature("kotlin", "Int"),
        )
        val expression = PointcutExpression.Args(
            args = listOf(
                ArgMatchingExpression.Class(
                    expression = ClassSignatureExpression.Normal(
                        packageNames = NameSequenceExpression.fromString("kotlin"),
                        classNames = NameSequenceExpression.fromString("String"),
                    ),
                ),
                ArgMatchingExpression.NoneOrMore,
            ),
            lastIsVarArg = false,
        )
        val argsMatcher = ArgsExpressionMatcher(expression)
        val result = argsMatcher.matches(valueParameterClassSignatures = valueParameterSignatures, lastIsVarArg = false)
        assertTrue { result }
    }

    @Test
    fun withVarargMatching() {
        val valueParameterSignatures = listOf(
            ClassSignature("kotlin", "String"),
            ClassSignature("kotlin", "Int"),
        )
        val expression = PointcutExpression.Args(
            args = listOf(
                ArgMatchingExpression.Class(
                    expression = ClassSignatureExpression.Normal(
                        packageNames = NameSequenceExpression.fromString("kotlin"),
                        classNames = NameSequenceExpression.fromString("String"),
                    ),
                ),
                ArgMatchingExpression.NoneOrMore,
            ),
            lastIsVarArg = true,
        )
        val argsMatcher = ArgsExpressionMatcher(expression)
        val result = argsMatcher.matches(valueParameterClassSignatures = valueParameterSignatures, lastIsVarArg = true)
        assertTrue { result }
    }

    @Test
    fun anyZeroOrMoreSandwiched() {
        val valueParameterSignatures = listOf(
            ClassSignature("kotlin", "String"),
            ClassSignature("kotlin", "Int"),
        )
        val expression = PointcutExpression.Args(
            args = listOf(
                ArgMatchingExpression.NoneOrMore,
                ArgMatchingExpression.Class(
                    expression = ClassSignatureExpression.Normal(
                        packageNames = NameSequenceExpression.fromString("kotlin"),
                        classNames = NameSequenceExpression.fromString("Int"),
                    ),
                ),
                ArgMatchingExpression.NoneOrMore,
            ),
            lastIsVarArg = false,
        )
        val argsMatcher = ArgsExpressionMatcher(expression)
        val result = argsMatcher.matches(valueParameterClassSignatures = valueParameterSignatures, lastIsVarArg = false)
        assertTrue { result }
    }
}
