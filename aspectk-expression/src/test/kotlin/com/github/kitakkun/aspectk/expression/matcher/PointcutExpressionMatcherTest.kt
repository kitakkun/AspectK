package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.FunctionModifier
import com.github.kitakkun.aspectk.expression.expressionparser.PointcutExpressionParser
import com.github.kitakkun.aspectk.expression.lexer.AspectKLexer
import com.github.kitakkun.aspectk.expression.model.ClassSignature
import com.github.kitakkun.aspectk.expression.model.FunctionSpec
import org.junit.Test

class PointcutExpressionMatcherTest {
    @Test
    fun test() {
        val tokens = AspectKLexer("execution(public com/example/**/foo(..): Unit)").analyze()
        val expression = PointcutExpressionParser(tokens).expression()
        val matcher = PointcutExpressionMatcher(expression)

        assert(
            matcher.matches(
                functionSpec = FunctionSpec.TopLevel(
                    modifiers = setOf(FunctionModifier.PUBLIC),
                    packageName = "com/example",
                    functionName = "foo",
                    args = emptyList(),
                    returnType = ClassSignature("", "Unit"),
                    lastArgumentIsVararg = false,
                ),
                namedPointcutResolver = { null },
            ),
        )

        assert(
            matcher.matches(
                functionSpec = FunctionSpec.TopLevel(
                    modifiers = setOf(FunctionModifier.PUBLIC),
                    packageName = "com/example/hogehoge/submodule",
                    functionName = "foo",
                    args = emptyList(),
                    returnType = ClassSignature("", "Unit"),
                    lastArgumentIsVararg = false,
                ),
                namedPointcutResolver = { null },
            ),
        )
    }
}
