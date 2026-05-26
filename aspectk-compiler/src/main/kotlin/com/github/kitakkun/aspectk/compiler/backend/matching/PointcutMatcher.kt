package com.github.kitakkun.aspectk.compiler.backend.matching

import com.github.kitakkun.aspectk.compiler.backend.analyzer.PointcutFilter
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.parentClassOrNull

internal object PointcutMatcher {
    fun matches(
        filter: PointcutFilter,
        target: IrSimpleFunction,
    ): Boolean {
        filter.classNamePattern?.let { pattern ->
            val containingClassName = target.parentClassOrNull?.name?.asString() ?: return false
            if (!NamePattern.matches(pattern, containingClassName)) return false
        }
        filter.methodNamePattern?.let { pattern ->
            if (!NamePattern.matches(pattern, target.name.asString())) return false
        }
        return true
    }
}
