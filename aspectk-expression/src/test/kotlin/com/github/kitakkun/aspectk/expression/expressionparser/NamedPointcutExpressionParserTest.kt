package com.github.kitakkun.aspectk.expression.expressionparser

import com.github.kitakkun.aspectk.expression.NameExpression
import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.tokenparser.NamedPointcutToken
import com.github.kitakkun.aspectk.expression.tokenparser.NamedPointcutTokenType
import org.junit.Test
import kotlin.test.assertEquals

class NamedPointcutExpressionParserTest {
    @Test
    fun test() {
        val tokens =
            listOf(
                NamedPointcutToken(NamedPointcutTokenType.PACKAGE, "com"),
                NamedPointcutToken(NamedPointcutTokenType.PACKAGE, "example"),
                NamedPointcutToken(NamedPointcutTokenType.CLASS, "ClassA"),
                NamedPointcutToken(NamedPointcutTokenType.CLASS, "ClassB"),
                NamedPointcutToken(NamedPointcutTokenType.FUNCTION, "customPointcut"),
            )
        val parser = NamedPointcutExpressionParser(tokens)
        val actual = parser.parse()
        val expected =
            PointcutExpression.Named(
                packageNames = listOf(NameExpression.Normal("com"), NameExpression.Normal("example")),
                classNames = listOf(NameExpression.Normal("ClassA"), NameExpression.Normal("ClassB")),
                functionName = NameExpression.Normal("customPointcut"),
            )

        assertEquals(expected = expected, actual = actual)
    }
}
