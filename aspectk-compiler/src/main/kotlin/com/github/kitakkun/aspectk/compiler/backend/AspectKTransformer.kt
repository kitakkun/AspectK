package com.github.kitakkun.aspectk.compiler.backend

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AspectClass
import com.github.kitakkun.aspectk.compiler.backend.transformer.ApplyAdviceTransformer
import com.github.kitakkun.aspectk.compiler.backend.utils.callableId
import com.github.kitakkun.aspectk.compiler.backend.utils.toClassSignature
import com.github.kitakkun.aspectk.expression.FunctionModifier
import com.github.kitakkun.aspectk.expression.PointcutExpression
import com.github.kitakkun.aspectk.expression.model.FunctionSpec
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isVararg
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

context(AspectKIrPluginContext)
class AspectKTransformer(
    private val aspectClasses: List<AspectClass>,
) : IrElementTransformerVoid() {
    private val namedPointcutResolver = { namedPointcut: PointcutExpression.Named ->
        val callableId = namedPointcut.callableId
        val aspectClass = aspectClasses.find { it.classId == callableId.classId }
        aspectClass?.pointcuts?.find { it.name == callableId.callableName.identifier }?.expression
    }

    override fun visitElement(element: IrElement): IrElement {
        element.transformChildrenVoid()
        return element
    }

    override fun visitClass(declaration: IrClass): IrClass {
        declaration.transformChildrenVoid()
        return declaration
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrSimpleFunction {
        if (declaration.isAspectRelevantDeclaration()) return declaration

        val functionSpec = declaration.toFunctionSpec()

        aspectClasses.forEach { aspectClass ->
            aspectClass.advices
                .filter { advice -> advice.matcher.matches(functionSpec, namedPointcutResolver) }
                .forEach { advice ->
                    ApplyAdviceTransformer(
                        targetFunction = declaration,
                        aspectClass = aspectClass.classDeclaration,
                        adviceFunction = advice.functionDeclaration,
                        adviceType = advice.type,
                    ).visitSimpleFunction(declaration)
                }
        }

        return declaration
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
    val parentClass = this.parentClassOrNull
    val name = this.name.asString()
    val args = this.valueParameters.map { it.type.classOrFail.owner.toClassSignature() }
    val returnType = this.returnType.classOrFail.owner.toClassSignature()
    val modifiers = this.modifiers()
    val lastArgumentIsVararg = this.valueParameters.lastOrNull()?.isVararg ?: false
    return if (parentClass != null) {
        FunctionSpec.Member(
            classSignature = parentClass.toClassSignature(),
            functionName = name,
            args = args,
            returnType = returnType,
            modifiers = modifiers,
            lastArgumentIsVararg = lastArgumentIsVararg,
        )
    } else {
        FunctionSpec.TopLevel(
            packageName = this.getPackageFragment().packageFqName.asString(),
            functionName = name,
            args = args,
            returnType = returnType,
            modifiers = modifiers,
            lastArgumentIsVararg = lastArgumentIsVararg,
        )
    }
}

private fun IrSimpleFunction.modifiers(): Set<FunctionModifier> {
    val modifiers = mutableSetOf<FunctionModifier>()
    when (this.visibility) {
        DescriptorVisibilities.DEFAULT_VISIBILITY -> modifiers.add(FunctionModifier.PUBLIC)
        DescriptorVisibilities.PUBLIC -> modifiers.add(FunctionModifier.PUBLIC)
        DescriptorVisibilities.PRIVATE -> modifiers.add(FunctionModifier.PRIVATE)
        DescriptorVisibilities.PROTECTED -> modifiers.add(FunctionModifier.PROTECTED)
        DescriptorVisibilities.INTERNAL -> modifiers.add(FunctionModifier.INTERNAL)
        // FIXME: Not tested
        DescriptorVisibilities.INHERITED ->
            this.overriddenSymbols.mapNotNull { it.owner.modifiers().firstOrNull() }
                .firstOrNull {
                    it in
                        listOf(
                            FunctionModifier.PUBLIC,
                            FunctionModifier.PROTECTED,
                            FunctionModifier.INTERNAL,
                        )
                }?.let { modifiers.add(it) }
    }
    if (this.isInfix) modifiers.add(FunctionModifier.INFIX)
    if (this.isSuspend) modifiers.add(FunctionModifier.SUSPEND)
    if (this.isTailrec) modifiers.add(FunctionModifier.TAILREC)
    if (this.isOperator) modifiers.add(FunctionModifier.OPERATOR)
    if (this.isInline) modifiers.add(FunctionModifier.INLINE)
    if (this.isExternal) modifiers.add(FunctionModifier.EXTERNAL)

    return modifiers
}
