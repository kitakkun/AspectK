package com.github.kitakkun.aspectk.compiler.pointcut.expression

import org.jetbrains.kotlin.name.ClassId

data class ArgumentExpression(val args: List<ArgumentMatchingExpression>)

sealed interface ArgumentMatchingExpression {
    /**
     * single "*" expression
     * matches any single argument
     */
    data object AnySingle : ArgumentMatchingExpression

    /**
     * ".." expression
     * matches any number of arguments
     * ex1) foo(..)
     * ex2) foo(.., Int)
     * ex3) foo(Int, ..)
     * ex4) foo(.., Int...)
     */
    data object AnyZeroOrMore : ArgumentMatchingExpression

    /**
     * "..." expression
     * matches vararg argument
     */
    data class Vararg(val classId: ClassId) : ArgumentMatchingExpression

    /**
     * type expression
     * matches specific type of argument
     * see [TypeMatchingExpression]
     */
    data class Type(val expression: TypeMatchingExpression) : ArgumentMatchingExpression
}

