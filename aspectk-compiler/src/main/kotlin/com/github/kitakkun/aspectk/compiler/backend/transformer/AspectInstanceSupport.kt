package com.github.kitakkun.aspectk.compiler.backend.transformer

import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.primaryConstructor

/**
 * Single source of truth for "is this aspect class instantiable by the
 * AspectK weavers?" and how to materialise an instance expression for it.
 *
 * Shared between `AspectKTransformer` (for `@Before` / `@After`) and
 * `AroundAdviceWeaver` so the two paths stay in lockstep.
 */
internal object AspectInstanceSupport {
    /**
     * `true` if [aspectClass] is one of:
     *
     * - an `object` declaration (regular, companion, or sealed-object), whose
     *   singleton INSTANCE we can read via `IrGetObjectValue`;
     * - a regular `class` with a no-arg primary constructor, that we can
     *   construct on demand.
     */
    fun canInstantiate(aspectClass: IrClass): Boolean {
        if (aspectClass.kind == ClassKind.OBJECT) return true
        val ctor = aspectClass.primaryConstructor ?: return false
        return ctor.parameters.isEmpty()
    }

    /**
     * Builds the IR expression that yields an instance of [aspectClass] at
     * the weaving site:
     *
     * - `object` → `IrGetObjectValue` (singleton, naturally cached).
     * - `class` → fresh `IrCallConstructor` per call site.
     *
     * Returns `null` if [canInstantiate] would return false.
     */
    fun instanceExpression(
        aspectClass: IrClass,
        builder: DeclarationIrBuilder,
    ): IrExpression? {
        if (aspectClass.kind == ClassKind.OBJECT) {
            return builder.irGetObject(aspectClass.symbol)
        }
        val ctor = aspectClass.primaryConstructor ?: return null
        if (ctor.parameters.isNotEmpty()) return null
        return builder.irCallConstructor(ctor.symbol, emptyList())
    }
}
