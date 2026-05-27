package com.github.kitakkun.aspectk.compiler.backend.analyzer

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import org.jetbrains.kotlin.backend.jvm.ir.getStringConstArgument
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid

/**
 * Walks the IR module fragment, finds every `@Aspect` class and the advice
 * functions it declares, converts each advice's stacked pointcut annotations
 * into a [PointcutFilter], and collects per-parameter [Binding]s from the
 * advice's value parameters.
 *
 * Phase 3.1 scope: `@ClassName` / `@MethodName` matching plus full binding
 * support for `@DispatchReceiver` / `@ExtensionReceiver` / `@ContextParameter` /
 * `@ValueParameter`. The remaining matching annotations (`@Visibility`,
 * `@Package`, `@Annotated`, …) arrive in Phase 3.4.
 */
internal class AspectAnalyzer {
    fun analyze(moduleFragment: IrModuleFragment): List<AspectMetadata> {
        val aspects = mutableListOf<AspectMetadata>()
        moduleFragment.acceptVoid(
            object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    element.acceptChildren(this, null)
                }

                override fun visitClass(declaration: IrClass) {
                    if (declaration.hasAnnotation(AspectKAnnotations.ASPECT_FQ_NAME)) {
                        aspects.add(buildAspectMetadata(declaration))
                    }
                    declaration.acceptChildren(this, null)
                }
            },
        )
        return aspects
    }

    private fun buildAspectMetadata(aspectClass: IrClass): AspectMetadata {
        val advices = aspectClass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .mapNotNull { fn ->
                val kind = adviceKindOf(fn) ?: return@mapNotNull null
                AdviceMetadata(
                    function = fn,
                    kind = kind,
                    pointcut = pointcutOf(fn),
                    bindings = bindingsOf(fn),
                )
            }
        return AspectMetadata(aspectClass = aspectClass, advices = advices)
    }

    private fun adviceKindOf(fn: IrSimpleFunction): AdviceKind? =
        when {
            fn.hasAnnotation(AspectKAnnotations.BEFORE_FQ_NAME) -> AdviceKind.BEFORE
            fn.hasAnnotation(AspectKAnnotations.AFTER_FQ_NAME) -> AdviceKind.AFTER
            fn.hasAnnotation(AspectKAnnotations.AROUND_FQ_NAME) -> AdviceKind.AROUND
            else -> null
        }

    private fun pointcutOf(fn: IrSimpleFunction): PointcutFilter {
        val className = fn
            .getAnnotation(AspectKAnnotations.CLASS_NAME_CLASS_ID.asSingleFqName())
            ?.getStringConstArgument(0)
        val methodName = fn
            .getAnnotation(AspectKAnnotations.METHOD_NAME_CLASS_ID.asSingleFqName())
            ?.getStringConstArgument(0)
        return PointcutFilter(
            classNamePattern = className,
            methodNamePattern = methodName,
        )
    }

    private fun bindingsOf(fn: IrSimpleFunction): List<Binding> =
        fn.parameters
            .filter { it.kind == IrParameterKind.Regular }
            .mapNotNull { bindingForParameter(it) }

    private fun bindingForParameter(param: IrValueParameter): Binding? {
        param.getAnnotation(AspectKAnnotations.DISPATCH_RECEIVER_FQ_NAME)?.let {
            return Binding.DispatchReceiver(param)
        }
        param.getAnnotation(AspectKAnnotations.EXTENSION_RECEIVER_FQ_NAME)?.let {
            return Binding.ExtensionReceiver(param)
        }
        param.getAnnotation(AspectKAnnotations.CONTEXT_PARAMETER_FQ_NAME)?.let { ann ->
            val (index, name) = readIndexNameArgs(ann)
            return Binding.ContextParameter(param, index, name)
        }
        param.getAnnotation(AspectKAnnotations.VALUE_PARAMETER_FQ_NAME)?.let { ann ->
            val (index, name) = readIndexNameArgs(ann)
            return Binding.ValueParameter(param, index, name)
        }
        return null
    }

    private fun readIndexNameArgs(annotation: IrConstructorCall): Pair<Int?, String?> {
        val index = (annotation.namedArg(AspectKAnnotations.INDEX.asString()) as? IrConst)?.value as? Int
        val name = (annotation.namedArg(AspectKAnnotations.NAME.asString()) as? IrConst)?.value as? String
        val resolvedIndex = if (index != null && index >= 0) index else null
        val resolvedName = if (!name.isNullOrEmpty()) name else null
        return resolvedIndex to resolvedName
    }
}

private fun IrConstructorCall.namedArg(name: String) =
    symbol.owner.parameters
        .indexOfFirst { it.name.asString() == name }
        .takeIf { it >= 0 }
        ?.let { arguments[it] }

private fun IrElement.acceptVoid(visitor: IrVisitorVoid) {
    accept(visitor, null)
}
