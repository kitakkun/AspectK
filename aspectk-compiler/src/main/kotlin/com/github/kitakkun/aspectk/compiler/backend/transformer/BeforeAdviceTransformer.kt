package com.github.kitakkun.aspectk.compiler.backend.transformer

import com.github.kitakkun.aspectk.compiler.backend.AspectKIrPluginContext
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

context(AspectKIrPluginContext)
class BeforeAdviceTransformer(
    private val targetFunction: IrSimpleFunction,
    private val adviceCall: IrExpression,
) : IrElementTransformerVoid() {
    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        if (declaration != targetFunction) return declaration

        // assume that at the beginning of the function,
        // aspect class initialization and join point variable generation is added
        (declaration.body as? IrBlockBody)?.statements?.add(2, adviceCall)

        return declaration
    }
}
