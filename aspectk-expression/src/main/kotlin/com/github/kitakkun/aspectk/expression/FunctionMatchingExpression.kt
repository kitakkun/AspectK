package com.github.kitakkun.aspectk.expression

sealed class FunctionMatchingExpression : AspectKExpression {
    abstract val modifiers: Set<FunctionModifier>
    abstract val argumentExpression: ArgumentExpression
    abstract val name: NameExpression
    abstract val returnTypeMatchingExpression: TypeMatchingExpression

    data class ClassMethod(
        override val modifiers: Set<FunctionModifier>,
        override val argumentExpression: ArgumentExpression,
        override val name: NameExpression,
        override val returnTypeMatchingExpression: TypeMatchingExpression,
        val classMatchingExpression: TypeMatchingExpression,
    ) : FunctionMatchingExpression()

    data class TopLevelFunction(
        override val modifiers: Set<FunctionModifier>,
        override val argumentExpression: ArgumentExpression,
        override val name: NameExpression,
        override val returnTypeMatchingExpression: TypeMatchingExpression,
    ) : FunctionMatchingExpression()

    data class ExtensionFunction(
        override val modifiers: Set<FunctionModifier>,
        override val argumentExpression: ArgumentExpression,
        override val name: NameExpression,
        override val returnTypeMatchingExpression: TypeMatchingExpression,
        val receiverType: TypeMatchingExpression,
    ) : FunctionMatchingExpression()
}

enum class FunctionModifier {
    ANY, PUBLIC, PROTECTED, PRIVATE,
}
