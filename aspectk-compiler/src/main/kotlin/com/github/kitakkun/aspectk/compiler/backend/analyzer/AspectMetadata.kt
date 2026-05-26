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
 * Phase 3.1 subset of the pointcut filter. Carries the name-pattern matching
 * annotations plus signature-driven constraints derived from binding parameters
 * (a `@DispatchReceiver` binding implies the target has a dispatch receiver of
 * a compatible type, etc.). The remaining matching annotations
 * (`@Visibility` / `@Modality` / `@Modifiers` / `@Package` / `@Annotated`)
 * are added in Phase 3.4.
 */
internal data class PointcutFilter(
    val classNamePattern: String?,
    val methodNamePattern: String?,
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
}
