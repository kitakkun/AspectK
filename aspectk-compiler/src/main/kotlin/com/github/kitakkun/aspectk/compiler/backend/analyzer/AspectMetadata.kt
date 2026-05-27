package com.github.kitakkun.aspectk.compiler.backend.analyzer

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

internal data class AspectMetadata(
    val aspectClass: IrClass,
    val advices: List<AdviceMetadata>,
)

internal data class AdviceMetadata(
    val function: IrSimpleFunction,
    val kind: AdviceKind,
    val pointcut: PointcutFilter,
)

internal enum class AdviceKind { BEFORE, AFTER, AROUND }

/**
 * Phase 3 (minimum-viable) subset of the pointcut filter. Only the constraints
 * driven directly by string-pattern annotations land here; visibility, modality,
 * modifiers, package, annotation-marker and signature-driven filters are added
 * in Phase 3 follow-ups.
 */
internal data class PointcutFilter(
    val classNamePattern: String?,
    val methodNamePattern: String?,
)
