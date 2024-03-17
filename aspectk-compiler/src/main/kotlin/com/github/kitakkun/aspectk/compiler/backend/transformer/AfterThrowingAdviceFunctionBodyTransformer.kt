package com.github.kitakkun.aspectk.compiler.backend.transformer

import com.github.kitakkun.aspectk.compiler.backend.AspectKIrPluginContext
import com.github.kitakkun.aspectk.compiler.backend.utils.irBlockBuilder
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irComposite
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrThrow
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

context(AspectKIrPluginContext)
class AfterThrowingAdviceFunctionBodyTransformer(
    private val targetFunction: IrSimpleFunction,
    private val adviceCall: IrExpression,
) : IrElementTransformerVoid() {
    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (declaration.symbol != targetFunction.symbol) return declaration

        (declaration.body as? IrBlockBody)?.statements?.add(adviceCall)

        return declaration
    }

    override fun visitElement(element: IrElement): IrElement {
        element.transformChildrenVoid()
        return element
    }

    override fun visitThrow(expression: IrThrow): IrExpression {
        with(targetFunction.irBlockBuilder(context)) {
            return irComposite {
                val value = irTemporary(value = expression.value, origin = IrDeclarationOrigin.DEFINED)
                +adviceCall.deepCopyWithVariables()
                +irThrow(irGet(value))
            }
        }
    }
}
