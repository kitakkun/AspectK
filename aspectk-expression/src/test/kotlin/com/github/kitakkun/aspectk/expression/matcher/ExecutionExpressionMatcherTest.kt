package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.*
import org.jetbrains.kotlin.javac.resolve.classId
import org.junit.Test

class ExecutionExpressionMatcherTest {
    @Test
    fun testTopLevel() {
        val expression = ExecutionExpression(
            FunctionMatchingExpression.TopLevelFunction(
                modifiers = setOf(FunctionModifier.PUBLIC),
                name = NameExpression.Normal("test"),
                argumentExpression = ArgumentExpression(emptyList()),
                returnTypeMatchingExpression = TypeMatchingExpression.Class("kotlin", "Unit"),
            )
        )
        val matcher = ExecutionExpressionMatcher(expression)
        val result = matcher.matches(
            packageName = "",
            className = "",
            functionName = "test",
            argumentClassIds = listOf(),
            returnType = classId("kotlin", "Unit"),
            modifiers = setOf(FunctionModifier.PUBLIC),
            lastArgumentIsVararg = false,
        )
        assert(result)
    }

    @Test
    fun testTopLevelNoMatch() {
        val expression = ExecutionExpression(
            FunctionMatchingExpression.TopLevelFunction(
                modifiers = setOf(FunctionModifier.PUBLIC),
                name = NameExpression.Normal("test"),
                argumentExpression = ArgumentExpression(emptyList()),
                returnTypeMatchingExpression = TypeMatchingExpression.Class("kotlin", "Unit"),
            )
        )
        val matcher = ExecutionExpressionMatcher(expression)
        val result = matcher.matches(
            packageName = "",
            className = "",
            functionName = "test",
            argumentClassIds = listOf(),
            returnType = classId("kotlin", "Unit"),
            modifiers = setOf(FunctionModifier.PRIVATE),
            lastArgumentIsVararg = false,
        )
        assert(!result)
    }

    @Test
    fun testClassMethod() {
        val expression = ExecutionExpression(
            FunctionMatchingExpression.ClassMethod(
                modifiers = emptySet(),
                name = NameExpression.Normal("test"),
                argumentExpression = ArgumentExpression(emptyList()),
                returnTypeMatchingExpression = TypeMatchingExpression.Class("kotlin", "Unit"),
                classMatchingExpression = TypeMatchingExpression.Class("com.example", "TestClass"),
            )
        )
        val matcher = ExecutionExpressionMatcher(expression)
        val result = matcher.matches(
            packageName = "com.example",
            className = "TestClass",
            functionName = "test",
            argumentClassIds = listOf(),
            returnType = classId("kotlin", "Unit"),
            modifiers = setOf(),
            lastArgumentIsVararg = false,
        )
        assert(result)
    }

    @Test
    fun testClassMethodNoMatch() {
        val expression = ExecutionExpression(
            FunctionMatchingExpression.ClassMethod(
                modifiers = emptySet(),
                name = NameExpression.Normal("test"),
                argumentExpression = ArgumentExpression(emptyList()),
                returnTypeMatchingExpression = TypeMatchingExpression.Class("kotlin", "Unit"),
                classMatchingExpression = TypeMatchingExpression.Class("com.example", "TestClass"),
            )
        )
        val matcher = ExecutionExpressionMatcher(expression)
        val result = matcher.matches(
            packageName = "com.example",
            className = "TestClass",
            functionName = "testNoMatch",
            argumentClassIds = listOf(),
            returnType = classId("kotlin", "Unit"),
            modifiers = setOf(),
            lastArgumentIsVararg = false,
        )
        assert(!result)
    }

    @Test
    fun testExtensionFunction() {
        val expression = ExecutionExpression(
            FunctionMatchingExpression.ExtensionFunction(
                modifiers = emptySet(),
                name = NameExpression.Normal("test"),
                argumentExpression = ArgumentExpression(emptyList()),
                returnTypeMatchingExpression = TypeMatchingExpression.Class("kotlin", "Unit"),
                receiverType = TypeMatchingExpression.Class("com.example", "TestClass"),
            )
        )
        val matcher = ExecutionExpressionMatcher(expression)
        val result = matcher.matches(
            packageName = "com.example",
            className = "TestClass",
            functionName = "test",
            argumentClassIds = listOf(),
            returnType = classId("kotlin", "Unit"),
            modifiers = setOf(),
            lastArgumentIsVararg = false,
        )
        assert(result)
    }

    @Test
    fun testExtensionFunctionNoMatch() {
        val expression = ExecutionExpression(
            FunctionMatchingExpression.ExtensionFunction(
                modifiers = emptySet(),
                name = NameExpression.Normal("test"),
                argumentExpression = ArgumentExpression(emptyList()),
                returnTypeMatchingExpression = TypeMatchingExpression.Class("kotlin", "Unit"),
                receiverType = TypeMatchingExpression.Class("com.example", "TestClass"),
            )
        )
        val matcher = ExecutionExpressionMatcher(expression)
        val result = matcher.matches(
            packageName = "com.example",
            className = "TestClassNoMatch",
            functionName = "test",
            argumentClassIds = listOf(),
            returnType = classId("kotlin", "Unit"),
            modifiers = setOf(),
            lastArgumentIsVararg = false,
        )
        assert(!result)
    }
}
