package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.FunctionModifier
import com.github.kitakkun.aspectk.expression.NameExpression
import com.github.kitakkun.aspectk.expression.PointcutExpression
import org.jetbrains.kotlin.javac.resolve.classId
import org.junit.Ignore
import org.junit.Test

class ExecutionExpressionMatcherTest {
    @Test
    fun testTopLevel() {
        val expression =
            PointcutExpression.Execution.TopLevelFunction(
                modifiers = listOf(FunctionModifier.PUBLIC),
                packageNames = emptyList(),
                functionName = NameExpression.Normal("test"),
                args = PointcutExpression.Args(emptyList(), false),
                returnTypePackageNames = emptyList(),
                returnTypeClassNames = listOf(NameExpression.Normal("Unit")),
            )
        val matcher = ExecutionExpressionMatcher(expression)
        val result =
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
        assert(result)
    }

    @Test
    fun testTopLevelNoMatch() {
        val expression =
            PointcutExpression.Execution.TopLevelFunction(
                modifiers = listOf(FunctionModifier.PUBLIC),
                packageNames = emptyList(),
                functionName = NameExpression.Normal("test"),
                args = PointcutExpression.Args(emptyList(), false),
                returnTypePackageNames = emptyList(),
                returnTypeClassNames = listOf(NameExpression.Normal("Unit")),
            )
        val matcher = ExecutionExpressionMatcher(expression)
        val result =
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
        assert(!result)
    }

    @Test
    fun testClassMethod() {
        val expression =
            PointcutExpression.Execution.MemberFunction(
                modifiers = emptyList(),
                packageNames = listOf(NameExpression.Normal("com"), NameExpression.Normal("example")),
                classNames = listOf(NameExpression.Normal("TestClass")),
                functionName = NameExpression.Normal("test"),
                args = PointcutExpression.Args(emptyList(), false),
                returnTypePackageNames = emptyList(),
                returnTypeClassNames = listOf(NameExpression.Normal("Unit")),
                includeSubClass = false,
            )
        val matcher = ExecutionExpressionMatcher(expression)
        val result =
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
        assert(result)
    }

    @Test
    fun testClassMethodNoMatch() {
        val expression =
            PointcutExpression.Execution.MemberFunction(
                modifiers = emptyList(),
                packageNames = listOf(NameExpression.Normal("com"), NameExpression.Normal("example")),
                classNames = listOf(NameExpression.Normal("TestClass")),
                functionName = NameExpression.Normal("test"),
                args = PointcutExpression.Args(emptyList(), false),
                returnTypePackageNames = emptyList(),
                returnTypeClassNames = listOf(NameExpression.Normal("Unit")),
                includeSubClass = false,
            )
        val matcher = ExecutionExpressionMatcher(expression)
        val result =
            matcher.matches(
                FunctionSpec(
                    packageName = "com/example",
                    className = "TestClass",
                    functionName = "testNoMatch",
                    args = listOf(),
                    returnType = classId("kotlin", "Unit"),
                    modifiers = setOf(FunctionModifier.PUBLIC),
                    lastArgumentIsVararg = false,
                ),
            )
        assert(!result)
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
