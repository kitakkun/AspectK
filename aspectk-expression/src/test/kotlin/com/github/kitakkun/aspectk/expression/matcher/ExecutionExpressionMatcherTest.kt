package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.FunctionModifier
import com.github.kitakkun.aspectk.expression.NameExpression
import com.github.kitakkun.aspectk.expression.NameSequenceExpression
import com.github.kitakkun.aspectk.expression.PointcutExpression
import org.jetbrains.kotlin.javac.resolve.classId
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExecutionExpressionMatcherTest {
    @Test
    fun testTopLevel() {
        val expression =
            PointcutExpression.Execution.TopLevelFunction(
                modifiers = listOf(FunctionModifier.PUBLIC),
                packageNames = NameSequenceExpression.Empty,
                functionName = NameExpression.Normal("test"),
                args = PointcutExpression.Args(emptyList(), false),
                returnTypePackageNames = NameSequenceExpression.Empty,
                returnTypeClassNames = NameSequenceExpression.fromString("Unit"),
            )
        val matcher = ExecutionExpressionMatcher(expression)
        val matchResult =
            matcher.matches(
                FunctionSpec(
                    packageName = "",
                    className = "",
                    functionName = "test",
                    args = listOf(),
                    returnType = classId("kotlin", "Unit"),
                    modifiers = setOf(FunctionModifier.PUBLIC),
                    lastArgumentIsVararg = false,
                ),
            )
        val noMatchResult =
            matcher.matches(
                FunctionSpec(
                    packageName = "",
                    className = "",
                    functionName = "test",
                    args = listOf(),
                    returnType = classId("kotlin", "Unit"),
                    modifiers = setOf(FunctionModifier.PRIVATE),
                    lastArgumentIsVararg = false,
                ),
            )
        assertTrue { matchResult }
        assertFalse { noMatchResult }
    }

    @Test
    fun testClassMethod() {
        val expression =
            PointcutExpression.Execution.MemberFunction(
                modifiers = emptyList(),
                packageNames = NameSequenceExpression.fromString("com/example"),
                classNames = NameSequenceExpression.fromString("TestClass"),
                functionName = NameExpression.Normal("test"),
                args = PointcutExpression.Args(emptyList(), false),
                returnTypePackageNames = NameSequenceExpression.Empty,
                returnTypeClassNames = NameSequenceExpression.fromString("Unit"),
                includeSubClass = false,
            )
        val matcher = ExecutionExpressionMatcher(expression)
        val matchResult =
            matcher.matches(
                FunctionSpec(
                    packageName = "com/example",
                    className = "TestClass",
                    functionName = "test",
                    args = listOf(),
                    returnType = classId("kotlin", "Unit"),
                    modifiers = setOf(FunctionModifier.PUBLIC),
                    lastArgumentIsVararg = false,
                ),
            )
        val noMatchResult =
            matcher.matches(
                FunctionSpec(
                    packageName = "com/example",
                    className = "TestClass",
                    functionName = "test",
                    args = listOf(),
                    returnType = classId("kotlin", "Unit"),
                    modifiers = setOf(FunctionModifier.PRIVATE),
                    lastArgumentIsVararg = false,
                ),
            )
        assert(matchResult)
        assertFalse { noMatchResult }
    }

    @Ignore("Not supported yet")
    @Test
    fun testExtensionFunction() {
    }

    @Ignore("Not supported yet")
    @Test
    fun testExtensionFunctionNoMatch() {
    }
}
