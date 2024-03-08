package com.github.kitakkun.aspectk.expression.expressionparser

import com.github.kitakkun.aspectk.expression.FunctionModifier
import com.github.kitakkun.aspectk.expression.NameExpression
import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.lexer.AspectKLexer
import org.junit.Test
import kotlin.test.assertEquals

class PointcutExpressionParserTest {
    @Test
    fun test() {
        val tokens = AspectKLexer("execution(public com/example/foo(..): Unit) && args(String)").analyze()
        val parser = PointcutExpressionParser(tokens)
        val actual = parser.expression()
        val expected =
            PointcutExpression.And(
                left =
                    PointcutExpression.Execution(
                        modifiers = listOf(FunctionModifier.PUBLIC),
                        packageNames = listOf(NameExpression.fromString("com"), NameExpression.fromString("example")),
                        classNames = emptyList(),
                        functionName = NameExpression.fromString("foo"),
                        args =
                            PointcutExpression.Args(
                                args = listOf(ArgMatchingExpression.NoneOrMore),
                                lastIsVarArg = false,
                            ),
                        returnTypePackageNames = emptyList(),
                        returnTypeClassNames = listOf(NameExpression.fromString("Unit")),
                    ),
                right =
                    PointcutExpression.Args(
                        args =
                            listOf(
                                ArgMatchingExpression.Class(
                                    packageNames = emptyList(),
                                    classNames = listOf(NameExpression.fromString("String")),
                                ),
                            ),
                        lastIsVarArg = false,
                    ),
            )

        assertEquals(expected, actual)
        println(actual)
    }
}
