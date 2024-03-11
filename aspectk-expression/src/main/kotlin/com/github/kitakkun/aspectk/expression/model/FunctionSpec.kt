package com.github.kitakkun.aspectk.expression.model

import com.github.kitakkun.aspectk.expression.FunctionModifier

sealed class FunctionSpec {
    abstract val modifiers: Set<FunctionModifier>
    abstract val functionName: String
    abstract val returnType: ClassSignature
    abstract val lastArgumentIsVararg: Boolean
    abstract val args: List<ClassSignature>

    data class TopLevel(
        val packageName: String,
        override val functionName: String,
        override val modifiers: Set<FunctionModifier>,
        override val returnType: ClassSignature,
        override val lastArgumentIsVararg: Boolean,
        override val args: List<ClassSignature>,
    ) : FunctionSpec()

    data class Member(
        val classSignature: ClassSignature,
        override val functionName: String,
        override val modifiers: Set<FunctionModifier>,
        override val returnType: ClassSignature,
        override val lastArgumentIsVararg: Boolean,
        override val args: List<ClassSignature>,
    ) : FunctionSpec()
}
