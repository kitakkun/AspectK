package com.github.kitakkun.aspectk.expression.expressionparser

import com.github.kitakkun.aspectk.expression.FunctionModifier
import com.github.kitakkun.aspectk.expression.NameExpression
import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.tokenparser.ArgsToken
import com.github.kitakkun.aspectk.expression.tokenparser.ExecutionToken
import com.github.kitakkun.aspectk.expression.tokenparser.ExecutionTokenType

class ExecutionExpressionParser(
    private val tokens: List<ExecutionToken>,
    private val argsTokens: List<ArgsToken>,
) {
    fun parse(): PointcutExpression.Execution {
        val modifiers = tokens.filter { it.type == ExecutionTokenType.MODIFIER }.map { FunctionModifier.valueOf(it.lexeme.uppercase()) }
        val packageNames =
            tokens.takeWhile { it.type != ExecutionTokenType.RETURN_TYPE_START }
                .filter { it.type == ExecutionTokenType.PACKAGE_PART }
                .map { NameExpression.fromString(it.lexeme) }
        val classNames =
            tokens.takeWhile { it.type != ExecutionTokenType.RETURN_TYPE_START }
                .filter { it.type == ExecutionTokenType.CLASS || it.type == ExecutionTokenType.CLASS_INCLUDING_SUBCLASS }
                .map { NameExpression.fromString(it.lexeme) }
        val functionName = tokens.first { it.type == ExecutionTokenType.FUNCTION }.let { NameExpression.fromString(it.lexeme) }

        val returnTypeTokens =
            tokens.dropWhile { it.type != ExecutionTokenType.RETURN_TYPE_START }
                .drop(1)
                .takeWhile { it.type != ExecutionTokenType.RETURN_TYPE_END }

        val returnTypePackageNames =
            returnTypeTokens.filter {
                it.type == ExecutionTokenType.PACKAGE_PART
            }.map { NameExpression.fromString(it.lexeme) }
        val returnTypeNames = returnTypeTokens.filter { it.type == ExecutionTokenType.CLASS }.map { NameExpression.fromString(it.lexeme) }

        val args = ArgsExpressionParser(argsTokens).parse()

        val includeSubClass =
            tokens.takeWhile { it.type == ExecutionTokenType.RETURN_TYPE_START }
                .any { it.type == ExecutionTokenType.CLASS_INCLUDING_SUBCLASS }

        return when {
            classNames.isEmpty() ->
                PointcutExpression.Execution.TopLevelFunction(
                    modifiers = modifiers,
                    packageNames = packageNames,
                    functionName = functionName,
                    args = args,
                    returnTypePackageNames = returnTypePackageNames,
                    returnTypeClassNames = returnTypeNames,
                )

            else ->
                PointcutExpression.Execution.MemberFunction(
                    modifiers = modifiers,
                    packageNames = packageNames,
                    classNames = classNames,
                    functionName = functionName,
                    args = args,
                    returnTypePackageNames = returnTypePackageNames,
                    returnTypeClassNames = returnTypeNames,
                    includeSubClass = includeSubClass,
                )
        }
    }
}
