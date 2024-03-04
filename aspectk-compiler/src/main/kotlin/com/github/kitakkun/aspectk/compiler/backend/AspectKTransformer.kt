package com.github.kitakkun.aspectk.compiler.backend

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AdviceType
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AspectClass
import com.github.kitakkun.aspectk.expression.FunctionModifier
import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.matcher.FunctionSpec
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

context(MessageCollector)
class AspectKTransformer(private val aspectClasses: List<AspectClass>) : IrElementTransformerVoid() {
    private val namedPointcutResolver = { namedPointcut: PointcutExpression.Named ->
        aspectClasses.find { it.classId == namedPointcut.classId }?.pointcuts?.find {
            it.name == namedPointcut.functionName.name
        }?.expression
    }

    override fun visitElement(element: IrElement): IrElement {
        element.transformChildrenVoid()
        return element
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (declaration.isAspectRelevantDeclaration()) return declaration

        aspectClasses.forEach { aspectClass ->
            aspectClass.advices.forEach { advice ->
                if (advice.matcher.matches(declaration.toFunctionSpec(), namedPointcutResolver)) {
                    // FIXME: experimental implementation
                    when (advice.type) {
                        AdviceType.AFTER -> {
                            (declaration.body as? IrBlockBody)?.statements?.addAll(advice.functionDeclaration.body?.statements.orEmpty())
                        }

                        AdviceType.BEFORE -> {
                            (declaration.body as? IrBlockBody)?.statements?.addAll(0, advice.functionDeclaration.body?.statements.orEmpty())
                        }

                        AdviceType.AROUND -> {
                            (declaration.body as? IrBlockBody)?.statements?.addAll(0, advice.functionDeclaration.body?.statements.orEmpty())
                            (declaration.body as? IrBlockBody)?.statements?.addAll(advice.functionDeclaration.body?.statements.orEmpty())
                        }
                    }
                    report(CompilerMessageSeverity.WARNING, "AspectK: Matched $advice to ${declaration.name}")
                }
            }
        }
        return super.visitSimpleFunction(declaration)
    }
}

private fun IrSimpleFunction.isAspectRelevantDeclaration(): Boolean {
    return this.parentClassOrNull?.hasAnnotation(AspectKAnnotations.ASPECT_FQ_NAME) == true ||
        this.hasAnnotation(AspectKAnnotations.POINTCUT_FQ_NAME) ||
        this.hasAnnotation(AspectKAnnotations.BEFORE_FQ_NAME) ||
        this.hasAnnotation(AspectKAnnotations.AFTER_FQ_NAME) ||
        this.hasAnnotation(AspectKAnnotations.AROUND_FQ_NAME)
}

private fun IrSimpleFunction.toFunctionSpec(): FunctionSpec {
    return FunctionSpec(
        packageName = this.getPackageFragment().packageFqName.asString(),
        className = this.parentClassOrNull?.kotlinFqName?.asString() ?: "",
        functionName = this.name.asString(),
        args = this.valueParameters.map { it.type.classOrFail.owner.classId!! },
        returnType = this.returnType.classOrFail.owner.classId!!,
        modifiers = this.modifiers(),
        lastArgumentIsVararg = this.valueParameters.lastOrNull()?.isVararg ?: false,
    )
}

private fun IrSimpleFunction.modifiers(): Set<FunctionModifier> {
    val modifiers = mutableSetOf<FunctionModifier>()
    when (this.visibility) {
        DescriptorVisibilities.PUBLIC -> modifiers.add(FunctionModifier.PUBLIC)
        DescriptorVisibilities.PRIVATE -> modifiers.add(FunctionModifier.PRIVATE)
        DescriptorVisibilities.PROTECTED -> modifiers.add(FunctionModifier.PROTECTED)
        DescriptorVisibilities.INTERNAL -> modifiers.add(FunctionModifier.INTERNAL)
        // FIXME: Not tested
        DescriptorVisibilities.INHERITED -> this.overriddenSymbols.mapNotNull { it.owner.modifiers().firstOrNull() }
            .firstOrNull { it in listOf(FunctionModifier.PUBLIC, FunctionModifier.PROTECTED, FunctionModifier.INTERNAL) }?.let { modifiers.add(it) }
    }
    if (this.isInfix) modifiers.add(FunctionModifier.INFIX)
    if (this.isSuspend) modifiers.add(FunctionModifier.SUSPEND)
    if (this.isTailrec) modifiers.add(FunctionModifier.TAILREC)
    if (this.isOperator) modifiers.add(FunctionModifier.OPERATOR)
    if (this.isInline) modifiers.add(FunctionModifier.INLINE)
    if (this.isExternal) modifiers.add(FunctionModifier.EXTERNAL)

    return modifiers
}

