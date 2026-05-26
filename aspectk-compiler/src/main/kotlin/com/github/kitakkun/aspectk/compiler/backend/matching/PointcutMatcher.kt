package com.github.kitakkun.aspectk.compiler.backend.matching

import com.github.kitakkun.aspectk.compiler.backend.analyzer.AdviceMetadata
import com.github.kitakkun.aspectk.compiler.backend.analyzer.Binding
import com.github.kitakkun.aspectk.compiler.backend.analyzer.PointcutFilter
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.kotlinFqName
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
        if (!packageMatches(filter.packagePattern, target)) return false
        if (!classNameMatches(filter.classNamePattern, target)) return false
        if (!methodNameMatches(filter.methodNamePattern, target)) return false
        if (!visibilityMatches(filter.visibilities, target)) return false
        if (!modalityMatches(filter.modalities, target)) return false
        if (!modifiersMatch(filter.modifiers, target)) return false
        if (!annotatedMatches(filter.annotatedFqNames, target)) return false
        return true
    }

    private fun packageMatches(
        pattern: String?,
        target: IrSimpleFunction,
    ): Boolean {
        if (pattern == null) return true
        val pkg = target.getPackageFragment().packageFqName.asString()
        return PackagePattern.matches(pattern, pkg)
    }

    private fun classNameMatches(
        pattern: String?,
        target: IrSimpleFunction,
    ): Boolean {
        if (pattern == null) return true
        val containingClassName = target.parentClassOrNull?.name?.asString() ?: return false
        return NamePattern.matches(pattern, containingClassName)
    }

    private fun methodNameMatches(
        pattern: String?,
        target: IrSimpleFunction,
    ): Boolean {
        if (pattern == null) return true
        return NamePattern.matches(pattern, target.name.asString())
    }

    /**
     * `@Visibility(vararg Kind)` — OR-combined across the listed kinds. The
     * target's visibility must be one of them.
     */
    private fun visibilityMatches(
        visibilities: List<String>,
        target: IrSimpleFunction,
    ): Boolean {
        if (visibilities.isEmpty()) return true
        val current = visibilityOf(target) ?: return false
        return current in visibilities
    }

    private fun visibilityOf(target: IrSimpleFunction): String? =
        when (target.visibility) {
            DescriptorVisibilities.PUBLIC -> "PUBLIC"
            DescriptorVisibilities.INTERNAL -> "INTERNAL"
            DescriptorVisibilities.PROTECTED -> "PROTECTED"
            DescriptorVisibilities.PRIVATE, DescriptorVisibilities.PRIVATE_TO_THIS -> "PRIVATE"
            else -> null
        }

    /**
     * `@Modality(vararg Kind)` — OR-combined. The target's modality must be one
     * of them. For top-level functions (no enclosing class) there is no
     * modality concept and the constraint never matches.
     */
    private fun modalityMatches(
        modalities: List<String>,
        target: IrSimpleFunction,
    ): Boolean {
        if (modalities.isEmpty()) return true
        val current = modalityOf(target) ?: return false
        return current in modalities
    }

    private fun modalityOf(target: IrSimpleFunction): String? =
        when (target.modality) {
            Modality.FINAL -> "FINAL"
            Modality.OPEN -> "OPEN"
            Modality.ABSTRACT -> "ABSTRACT"
            Modality.SEALED -> "SEALED"
        }

    /**
     * `@Modifiers(vararg Kind)` — AND-combined. The target must carry every
     * listed modifier.
     */
    private fun modifiersMatch(
        modifiers: List<String>,
        target: IrSimpleFunction,
    ): Boolean {
        if (modifiers.isEmpty()) return true
        for (modifier in modifiers) {
            val present = when (modifier) {
                "SUSPEND" -> target.isSuspend
                "INLINE" -> target.isInline
                "INFIX" -> target.isInfix
                "OPERATOR" -> target.isOperator
                "TAILREC" -> target.isTailrec
                "EXTERNAL" -> target.isExternal
                else -> false
            }
            if (!present) return false
        }
        return true
    }

    /**
     * `@Annotated(vararg KClass)` — OR-combined. The target must carry at
     * least one of the listed annotations.
     */
    private fun annotatedMatches(
        annotatedFqNames: List<String>,
        target: IrSimpleFunction,
    ): Boolean {
        if (annotatedFqNames.isEmpty()) return true
        val targetAnnotations = target.annotations
            .mapNotNull {
                it.symbol.owner.parentClassOrNull
                    ?.kotlinFqName
                    ?.asString()
            }.toSet()
        return annotatedFqNames.any { it in targetAnnotations }
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
