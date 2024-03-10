package com.github.kitakkun.aspectk.compiler.backend.transformer

import com.github.kitakkun.aspectk.compiler.AspectKConsts
import com.github.kitakkun.aspectk.compiler.backend.AspectKIrPluginContext
import com.github.kitakkun.aspectk.compiler.backend.analyzer.AdviceType
import com.github.kitakkun.aspectk.compiler.backend.utils.irBlockBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrStatementsBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.isObject
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

        val adviceCall =
            irBuilder.adviceCall(
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
        value =
            if (aspectClass.isObject) {
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
    return irTemporary(
        irCallConstructor(joinPointClassConstructor, emptyList()).apply {
            putValueArgument(0, declaration.dispatchReceiverParameter?.let { irGet(it) } ?: irNull())
            putValueArgument(
                1,
                irCall(listOfFunction).apply {
                    putValueArgument(index = 0, valueArgument = irVararg(irBuiltIns.anyNType, declaration.valueParameters.map { irGet(it) }))
                },
            )
        },
    )
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
