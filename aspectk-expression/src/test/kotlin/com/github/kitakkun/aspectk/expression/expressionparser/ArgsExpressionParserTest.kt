package com.github.kitakkun.aspectk.expression.expressionparser

import com.github.kitakkun.aspectk.expression.ClassSignatureExpression
import com.github.kitakkun.aspectk.expression.NameSequenceExpression
import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.tokenparser.ArgsToken
import com.github.kitakkun.aspectk.expression.tokenparser.ArgsTokenType
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ArgsExpressionParserTest {
    @Test
    fun test() {
        val inputTokens = listOf(
            ArgsToken(type = ArgsTokenType.PACKAGE_PART, lexeme = "com"),
            ArgsToken(type = ArgsTokenType.PACKAGE_PART, lexeme = "example"),
            ArgsToken(type = ArgsTokenType.CLASS, lexeme = "Hoge"),
            ArgsToken(type = ArgsTokenType.ARG_SEPARATOR, lexeme = ","),
            ArgsToken(type = ArgsTokenType.CLASS, lexeme = "Int"),
            ArgsToken(type = ArgsTokenType.ARG_SEPARATOR, lexeme = ","),
            ArgsToken(type = ArgsTokenType.NONE_OR_MORE_ARGS, lexeme = ".."),
            ArgsToken(type = ArgsTokenType.ARG_SEPARATOR, lexeme = ","),
            ArgsToken(type = ArgsTokenType.CLASS, lexeme = "String"),
            ArgsToken(type = ArgsTokenType.VAR_ARG, lexeme = "..."),
        )

        val parser = ArgsExpressionParser(inputTokens)
        val actual = parser.parse()
        val expected = PointcutExpression.Args(
            args = listOf(
                ArgMatchingExpression.Class(
                    expression = ClassSignatureExpression.Normal(
                        packageNames = NameSequenceExpression.fromString("com/example"),
                        classNames = NameSequenceExpression.fromString("Hoge"),
                    ),
                ),
                ArgMatchingExpression.Class(
                    expression = ClassSignatureExpression.Normal(
                        packageNames = NameSequenceExpression.Empty,
                        classNames = NameSequenceExpression.fromString("Int"),
                    ),
                ),
                ArgMatchingExpression.NoneOrMore,
                ArgMatchingExpression.Class(
                    expression = ClassSignatureExpression.Normal(
                        packageNames = NameSequenceExpression.Empty,
                        classNames = NameSequenceExpression.fromString("String"),
                    ),
                ),
            ),
            lastIsVarArg = true,
        )

        assertContentEquals(expected = expected.args, actual = actual.args)
        assertEquals(expected.lastIsVarArg, actual.lastIsVarArg)
    }
}
