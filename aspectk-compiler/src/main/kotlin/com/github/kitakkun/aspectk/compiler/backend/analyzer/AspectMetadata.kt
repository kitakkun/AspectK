package com.github.kitakkun.aspectk.compiler.backend.analyzer

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter

internal data class AspectMetadata(
    val aspectClass: IrClass,
    val advices: List<AdviceMetadata>,
)

internal data class AdviceMetadata(
    val function: IrSimpleFunction,
    val kind: AdviceKind,
    val pointcut: PointcutFilter,
    val bindings: List<Binding>,
)

internal enum class AdviceKind { BEFORE, AFTER, AROUND }

/**
 * Pointcut filter assembled from the matching annotations stacked on the
 * advice function. Each field corresponds to one annotation; a `null` /
 * empty list means "no constraint from that dimension".
 */
internal data class PointcutFilter(
    val packagePattern: String?,
    val classNamePattern: String?,
    val methodNamePattern: String?,
    val visibilities: List<String>,
    val modalities: List<String>,
    val modifiers: List<String>,
    val annotatedFqNames: List<String>,
)

/**
 * One entry per advice function parameter, recording where its value must be
 * extracted from at the matched target call site.
 */
internal sealed interface Binding {
    /** The advice's own parameter that the extracted value is forwarded into. */
    val adviceParameter: IrValueParameter

    data class DispatchReceiver(
        override val adviceParameter: IrValueParameter,
    ) : Binding

    data class ExtensionReceiver(
        override val adviceParameter: IrValueParameter,
    ) : Binding

    data class ContextParameter(
        override val adviceParameter: IrValueParameter,
        val index: Int?,
        val name: String?,
    ) : Binding

    data class ValueParameter(
        override val adviceParameter: IrValueParameter,
        val index: Int?,
        val name: String?,
    ) : Binding

    /** Catch-all binding capturing every value parameter as a `List<Any?>`. */
    data class ValueParameters(
        override val adviceParameter: IrValueParameter,
    ) : Binding

    /** Catch-all binding capturing every context parameter as a `List<Any?>`. */
    data class ContextParameters(
        override val adviceParameter: IrValueParameter,
    ) : Binding
}
