package com.github.kitakkun.aspectk.compiler.backend.transformer

import com.github.kitakkun.aspectk.compiler.AspectKConsts
import com.github.kitakkun.aspectk.compiler.backend.AspectKIrPluginContext
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AdviceType
import com.github.kitakkun.aspectk.compiler.backend.utils.irBlockBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

context(AspectKIrPluginContext)
class ApplyAdviceTransformer(
    private val targetFunction: IrSimpleFunction,
    private val aspectClass: IrClass,
    private val adviceFunction: IrSimpleFunction,
    private val adviceType: AdviceType,
) : IrElementTransformerVoid() {
    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (declaration.symbol != targetFunction.symbol) return declaration

        val irBuilder = declaration.irBlockBuilder(context)
        val aspectInstance = irBuilder.generateAspectInstance(aspectClass)
        val joinPointVariable = irBuilder.generateJoinPointVariable(declaration)

        val adviceCall = irBuilder.adviceCall(
            functionDeclaration = adviceFunction,
            aspectInstance = aspectInstance,
            joinPointVariable = joinPointVariable,
        )

        (declaration.body as? IrBlockBody)?.statements?.addAll(0, listOf(aspectInstance, joinPointVariable))

        when (adviceType) {
            AdviceType.AFTER -> AfterAdviceFunctionBodyTransformer(targetFunction, adviceCall).visitSimpleFunction(declaration)
            AdviceType.BEFORE -> BeforeAdviceTransformer(targetFunction, adviceCall).visitSimpleFunction(declaration)
            AdviceType.AROUND -> {
                // TODO: Implement AroundAdviceTransformer
                BeforeAdviceTransformer(targetFunction, adviceCall).visitSimpleFunction(declaration)
                AfterAdviceFunctionBodyTransformer(targetFunction, adviceCall).visitSimpleFunction(declaration)
            }
        }

        return declaration
    }
}

private fun IrStatementsBuilder<*>.generateAspectInstance(aspectClass: IrClass): IrVariable {
    return irTemporary(
        value = if (aspectClass.isObject) {
            irGetObject(aspectClass.symbol)
        } else {
            val constructor = aspectClass.primaryConstructor ?: error("Primary constructor for ${aspectClass.classId} not found")
            irCallConstructor(constructor.symbol, emptyList())
        },
        origin = IrDeclarationOrigin.DEFINED,
    )
}

context(AspectKIrPluginContext)
private fun IrStatementsBuilder<*>.generateJoinPointVariable(declaration: IrFunction): IrVariable {
    val methodName = declaration.name.asString()
    val targetClassName = declaration.parentClassOrNull?.classId?.asFqNameString() ?: ""
    val signature = buildSignatureText(declaration, methodName, targetClassName)
    val contextReceiverCount = declaration.contextReceiverParametersCount
    val contextParams = declaration.valueParameters.take(contextReceiverCount)
    val valueParams = declaration.valueParameters.drop(contextReceiverCount)

    return irTemporary(
        irCallConstructor(joinPointClassConstructor, emptyList()).apply {
            putValueArgument(0, declaration.dispatchReceiverParameter?.let { irJoinPointArgument(it) } ?: irNull())
            putValueArgument(1, declaration.extensionReceiverParameter?.let { irJoinPointArgument(it) } ?: irNull())
            putValueArgument(2, irJoinPointArgumentList(contextParams))
            putValueArgument(3, irJoinPointArgumentList(valueParams))
            putValueArgument(4, irString(methodName))
            putValueArgument(5, irString(targetClassName))
            putValueArgument(6, irString(signature))
        },
    )
}

context(AspectKIrPluginContext)
private fun IrBuilderWithScope.irJoinPointArgument(parameter: IrValueParameter): IrExpression {
    return irCallConstructor(joinPointArgumentClassConstructor, emptyList()).apply {
        putValueArgument(0, irString(parameter.name.asString()))
        putValueArgument(1, irKClassReference(parameter.type))
        putValueArgument(2, irGet(parameter))
        putValueArgument(3, irAnnotationList(parameter.annotations))
    }
}

context(AspectKIrPluginContext)
private fun IrBuilderWithScope.irJoinPointArgumentList(params: List<IrValueParameter>): IrExpression {
    val argumentType = referenceClass(AspectKConsts.JOIN_POINT_ARGUMENT_CLASS_ID)!!.defaultType
    return irCall(listOfFunction).apply {
        putValueArgument(
            index = 0,
            valueArgument = irVararg(argumentType, params.map { irJoinPointArgument(it) }),
        )
    }
}

private fun IrBuilderWithScope.irKClassReference(type: IrType): IrExpression {
    val erased = type.makeNotNull()
    return IrClassReferenceImpl(
        startOffset = startOffset,
        endOffset = endOffset,
        type = context.irBuiltIns.kClassClass.typeWith(erased),
        symbol = erased.classifierOrFail,
        classType = erased,
    )
}

context(AspectKIrPluginContext)
private fun IrBuilderWithScope.irAnnotationList(annotations: List<IrConstructorCall>): IrExpression {
    return irCall(listOfFunction).apply {
        putValueArgument(
            index = 0,
            valueArgument = irVararg(
                irBuiltIns.annotationType,
                annotations.map { it.deepCopyWithVariables() },
            ),
        )
    }
}

private fun buildSignatureText(
    declaration: IrFunction,
    methodName: String,
    targetClassName: String,
): String {
    val contextReceiverCount = declaration.contextReceiverParametersCount
    val contextFqns = declaration.valueParameters.take(contextReceiverCount)
        .map { it.type.classOrNull?.owner?.classId?.asFqNameString() ?: "?" }
    val extensionFqn = declaration.extensionReceiverParameter
        ?.type?.classOrNull?.owner?.classId?.asFqNameString()
    val valueFqns = declaration.valueParameters.drop(contextReceiverCount)
        .map { it.type.classOrNull?.owner?.classId?.asFqNameString() ?: "?" }
    val returnFqn = declaration.returnType.classOrNull?.owner?.classId?.asFqNameString() ?: "?"

    val contextPart = if (contextFqns.isEmpty()) "" else "context(${contextFqns.joinToString(", ")}) "
    val receiverPart = extensionFqn?.let { "$it." } ?: ""
    val qualifier = when {
        receiverPart.isNotEmpty() -> "$receiverPart$methodName"
        targetClassName.isEmpty() -> methodName
        else -> "$targetClassName.$methodName"
    }
    return "$contextPart$qualifier(${valueFqns.joinToString(", ")}): $returnFqn"
}

private fun IrBuilderWithScope.adviceCall(
    functionDeclaration: IrSimpleFunction,
    aspectInstance: IrVariable,
    joinPointVariable: IrVariable,
): IrExpression {
    return irCall(functionDeclaration.symbol).apply {
        dispatchReceiver = irGet(aspectInstance)
        if (functionDeclaration.valueParameters.firstOrNull()?.type?.classOrNull?.owner?.classId == AspectKConsts.JOIN_POINT_CLASS_ID) {
            putValueArgument(0, irGet(joinPointVariable))
        }
    }
}
