package com.github.kitakkun.aspectk.compiler.backend

import com.github.kitakkun.aspectk.compiler.AspectKConsts
import com.github.kitakkun.aspectk.compiler.backend.utils.irBlockBuilder
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

context(AspectKIrPluginContext)
class AfterAdviceFunctionBodyTransformer private constructor(
    private val targetFunction: IrSimpleFunction,
    private val adviceExpression: IrExpression,
) : IrElementTransformerVoid() {
    companion object {
        context(AspectKIrPluginContext)
        fun transform(
            declaration: IrSimpleFunction,
            aspectClass: IrClass,
            adviceFunction: IrSimpleFunction,
        ) {
            val irBuilder = declaration.irBlockBuilder(context)
            val aspectInstance = irBuilder.generateAspectInstance(aspectClass)
            val joinPointVariable = irBuilder.generateJoinPointVariable(declaration)

            (declaration.body as? IrBlockBody)?.statements?.addAll(0, listOf(aspectInstance, joinPointVariable))

            val adviceCall =
                irBuilder.adviceCall(
                    functionDeclaration = adviceFunction,
                    aspectInstance = aspectInstance,
                    joinPointVariable = joinPointVariable,
                )

            (declaration.body as? IrBlockBody)?.statements?.add(adviceCall)

            declaration.transformChildrenVoid(
                AfterAdviceFunctionBodyTransformer(
                    targetFunction = declaration,
                    adviceExpression = adviceCall,
                ),
            )
        }
    }

    override fun visitElement(element: IrElement): IrElement {
        element.transformChildrenVoid()
        return element
    }

    override fun visitReturn(expression: IrReturn): IrExpression {
        if (expression.returnTargetSymbol != targetFunction.symbol) return expression

        with(targetFunction.irBlockBuilder(context)) {
            return irComposite {
                val returnValue = irTemporary(value = expression.value, origin = IrDeclarationOrigin.DEFINED)
                +adviceExpression.deepCopyWithVariables()
                +irReturn(irGet(returnValue))
            }
        }
    }

    override fun visitThrow(expression: IrThrow): IrExpression {
        with(targetFunction.irBlockBuilder(context)) {
            return irComposite {
                val value = irTemporary(value = expression.value, origin = IrDeclarationOrigin.DEFINED)
                +adviceExpression.deepCopyWithVariables()
                +irThrow(irGet(value))
            }
        }
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
