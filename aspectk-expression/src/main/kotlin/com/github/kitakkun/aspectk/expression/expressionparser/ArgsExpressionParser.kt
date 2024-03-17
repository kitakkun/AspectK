package com.github.kitakkun.aspectk.expression.expressionparser

import com.github.kitakkun.aspectk.expression.ClassSignatureExpression
import com.github.kitakkun.aspectk.expression.NameSequenceExpression
import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.tokenparser.ArgsToken
import com.github.kitakkun.aspectk.expression.tokenparser.ArgsTokenType

sealed class ArgMatchingExpression {
    data object AnySingle : ArgMatchingExpression()

    data object NoneOrMore : ArgMatchingExpression()

    data class Class(val expression: ClassSignatureExpression) : ArgMatchingExpression()
}

class ArgsExpressionParser(
    private val argsTokens: List<ArgsToken>,
) {
    fun parse(): PointcutExpression.Args {
        val mutableTokens = argsTokens.toMutableList()

        if (mutableTokens.isEmpty()) {
            return PointcutExpression.Args(emptyList(), false)
        }

        val lastIsVarArg = mutableTokens.last().type == ArgsTokenType.VAR_ARG
        if (lastIsVarArg) {
            mutableTokens.removeLast()
        }

        val argMatchingExpressions = mutableListOf<ArgMatchingExpression>()

        while (mutableTokens.isNotEmpty()) {
            val arg = mutableTokens.takeWhile { it.type != ArgsTokenType.ARG_SEPARATOR }

            mutableTokens.removeAll(arg)
            if (mutableTokens.firstOrNull()?.type == ArgsTokenType.ARG_SEPARATOR) {
                mutableTokens.removeFirst()
            }

            when (arg.singleOrNull()?.type) {
                ArgsTokenType.SINGLE_ANY_ARG -> {
                    argMatchingExpressions.add(ArgMatchingExpression.AnySingle)
                    continue
                }

                ArgsTokenType.NONE_OR_MORE_ARGS -> {
                    argMatchingExpressions.add(ArgMatchingExpression.NoneOrMore)
                    continue
                }

                else -> {
                    // do nothing
                }
            }

            val packageExpressions = arg.filter { it.type == ArgsTokenType.PACKAGE_PART }.joinToString("/") { it.lexeme }
            val classExpressions = arg.filter { it.type == ArgsTokenType.CLASS }.joinToString(".") { it.lexeme }

            if (packageExpressions.isEmpty() && classExpressions.isEmpty()) {
                continue
            }

            argMatchingExpressions.add(
                ArgMatchingExpression.Class(
                    expression = ClassSignatureExpression.Normal(
                        packageNames = NameSequenceExpression.fromString(packageExpressions),
                        classNames = NameSequenceExpression.fromString(classExpressions),
                    ),
                ),
            )
        }

        return PointcutExpression.Args(argMatchingExpressions, lastIsVarArg)
    }
}
