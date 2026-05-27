package com.github.kitakkun.aspectk.compiler.backend.analyzer

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import org.jetbrains.kotlin.backend.jvm.ir.getStringConstArgument
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid

/**
 * Walks the IR module fragment, finds every `@Aspect` class and the advice
 * functions it declares, converts each advice's stacked pointcut annotations
 * into a [PointcutFilter], and collects per-parameter [Binding]s from the
 * advice's value parameters.
 *
 * Phase 3.4 scope: full reading of every v1 matching annotation
 * (`@Package` / `@ClassName` / `@MethodName` / `@Visibility` / `@Modality` /
 * `@Modifiers` / `@Annotated`) plus per-parameter binding extraction.
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
        val packagePattern = fn
            .getAnnotation(AspectKAnnotations.PACKAGE_CLASS_ID.asSingleFqName())
            ?.getStringConstArgument(0)
        val classNamePattern = fn
            .getAnnotation(AspectKAnnotations.CLASS_NAME_CLASS_ID.asSingleFqName())
            ?.getStringConstArgument(0)
        val methodNamePattern = fn
            .getAnnotation(AspectKAnnotations.METHOD_NAME_CLASS_ID.asSingleFqName())
            ?.getStringConstArgument(0)
        val visibilities = enumValuesArg(fn, AspectKAnnotations.VISIBILITY_CLASS_ID.asSingleFqName())
        val modalities = enumValuesArg(fn, AspectKAnnotations.MODALITY_CLASS_ID.asSingleFqName())
        val modifiers = enumValuesArg(fn, AspectKAnnotations.MODIFIERS_CLASS_ID.asSingleFqName())
        val annotatedFqNames = annotatedFqNamesArg(fn)
        return PointcutFilter(
            packagePattern = packagePattern,
            classNamePattern = classNamePattern,
            methodNamePattern = methodNamePattern,
            visibilities = visibilities,
            modalities = modalities,
            modifiers = modifiers,
            annotatedFqNames = annotatedFqNames,
        )
    }

    private fun enumValuesArg(
        fn: IrSimpleFunction,
        annotationFqName: org.jetbrains.kotlin.name.FqName,
    ): List<String> {
        val ann = fn.getAnnotation(annotationFqName) ?: return emptyList()
        val vararg = ann.arguments.firstOrNull { it is IrVararg } as? IrVararg ?: return emptyList()
        return vararg.elements.mapNotNull {
            (it as? IrGetEnumValue)
                ?.symbol
                ?.owner
                ?.name
                ?.asString()
        }
    }

    private fun annotatedFqNamesArg(fn: IrSimpleFunction): List<String> {
        val ann = fn.getAnnotation(AspectKAnnotations.ANNOTATED_CLASS_ID.asSingleFqName()) ?: return emptyList()
        val vararg = ann.arguments.firstOrNull { it is IrVararg } as? IrVararg ?: return emptyList()
        return vararg.elements.mapNotNull { element ->
            (element as? IrClassReference)
                ?.classType
                ?.classOrNull
                ?.owner
                ?.kotlinFqName
                ?.asString()
        }
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
        param.getAnnotation(AspectKAnnotations.VALUE_PARAMETERS_FQ_NAME)?.let {
            return Binding.ValueParameters(param)
        }
        param.getAnnotation(AspectKAnnotations.CONTEXT_PARAMETERS_FQ_NAME)?.let {
            return Binding.ContextParameters(param)
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
