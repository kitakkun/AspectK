package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.FunctionModifier
import com.github.kitakkun.aspectk.expression.expressionparser.PointcutExpressionParser
import com.github.kitakkun.aspectk.expression.lexer.AspectKLexer
import org.jetbrains.kotlin.javac.resolve.classId
import org.junit.Test
import kotlin.test.assertFalse

class PointcutExpressionMatcherTest {
    @Test
    fun test() {
        val tokens = AspectKLexer("execution(public com/example/**/foo(..): Unit)").analyze()
        val expression = PointcutExpressionParser(tokens).expression()
        val matcher = PointcutExpressionMatcher(expression)

        assert(
            matcher.matches(
                functionSpec =
                    FunctionSpec(
                        modifiers = setOf(FunctionModifier.PUBLIC),
                        packageName = "com/example",
                        className = "",
                        functionName = "foo",
                        args = emptyList(),
                        returnType = classId("", "Unit"),
                        lastArgumentIsVararg = false,
                    ),
                namedPointcutResolver = { null },
            ),
        )

        assert(
            matcher.matches(
                functionSpec =
                    FunctionSpec(
                        modifiers = setOf(FunctionModifier.PUBLIC),
                        packageName = "com/example/hogehoge/submodule",
                        className = "",
                        functionName = "foo",
                        args = emptyList(),
                        returnType = classId("", "Unit"),
                        lastArgumentIsVararg = false,
                    ),
                namedPointcutResolver = { null },
            ),
        )

        assertFalse {
            matcher.matches(
                functionSpec =
                    FunctionSpec(
                        modifiers = setOf(FunctionModifier.PUBLIC),
                        packageName = "com/example/hogehoge/submodule",
                        className = "SomeClass",
                        functionName = "foo",
                        args = emptyList(),
                        returnType = classId("", "Unit"),
                        lastArgumentIsVararg = false,
                    ),
                namedPointcutResolver = { null },
            )
        }
    }
}
