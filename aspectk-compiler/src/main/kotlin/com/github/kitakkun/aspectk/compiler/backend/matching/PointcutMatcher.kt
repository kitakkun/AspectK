package com.github.kitakkun.aspectk.compiler.backend.matching

import com.github.kitakkun.aspectk.compiler.backend.analyzer.AdviceMetadata
import com.github.kitakkun.aspectk.compiler.backend.analyzer.Binding
import com.github.kitakkun.aspectk.compiler.backend.analyzer.PointcutFilter
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.parentClassOrNull

internal object PointcutMatcher {
    fun matches(
        advice: AdviceMetadata,
        target: IrSimpleFunction,
    ): Boolean {
        if (!filterMatches(advice.pointcut, target)) return false
        if (!bindingsMatch(advice.bindings, target)) return false
        return true
    }

    private fun filterMatches(
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

    /**
     * Presence-only check (Phase 3.1): the target must expose the slots that
     * the advice's bindings declare. Type compatibility between the binding's
     * declared type and the target's actual parameter type is deferred to a
     * Phase 3.1 follow-up.
     */
    private fun bindingsMatch(
        bindings: List<Binding>,
        target: IrSimpleFunction,
    ): Boolean {
        val targetParams = target.parameters
        val targetValueParams = targetParams.filter { it.kind == IrParameterKind.Regular }
        val targetContextParams = targetParams.filter { it.kind == IrParameterKind.Context }
        val targetHasDispatch = targetParams.any { it.kind == IrParameterKind.DispatchReceiver }
        val targetHasExtension = targetParams.any { it.kind == IrParameterKind.ExtensionReceiver }
        for (binding in bindings) {
            val ok = when (binding) {
                is Binding.DispatchReceiver -> targetHasDispatch
                is Binding.ExtensionReceiver -> targetHasExtension
                is Binding.ValueParameter -> when {
                    binding.index != null -> binding.index in targetValueParams.indices
                    binding.name != null -> targetValueParams.any { it.name.asString() == binding.name }
                    else -> false
                }
                is Binding.ContextParameter -> when {
                    binding.index != null -> binding.index in targetContextParams.indices
                    binding.name != null -> targetContextParams.any { it.name.asString() == binding.name }
                    else -> false
                }
            }
            if (!ok) return false
        }
        return true
    }
}
