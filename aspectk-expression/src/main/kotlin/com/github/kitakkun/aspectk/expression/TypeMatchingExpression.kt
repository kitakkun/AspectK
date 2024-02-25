package com.github.kitakkun.aspectk.expression

sealed class TypeMatchingExpression : AspectKExpression {
    /**
     * single "*" expression
     */
    data object Any : TypeMatchingExpression()

    /**
     * basic package expression
     * ex) "com.example.*"
     */
    data class InPackage(val packageName: String) : TypeMatchingExpression()

    /**
     * under package expression
     * ex) "com.example..*"
     */
    data class UnderPackage(val packageName: String) : TypeMatchingExpression()

    /**
     * type expression
     * ex) "kotlin/String"
     */
    data class Class(
        val packageName: String,
        val className: String,
    ) : TypeMatchingExpression()

    /**
     * FIXME: not well-defined
     * class pattern expression
     * ex) "com.example.*Controller"
     */
    data class ClassPattern(val packageName: String, val prefix: String, val suffix: String) : TypeMatchingExpression()
}
