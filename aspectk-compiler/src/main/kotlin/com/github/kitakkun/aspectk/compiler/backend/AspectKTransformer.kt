package com.github.kitakkun.aspectk.compiler.backend

import com.github.kitakkun.aspectk.compiler.backend.analyzer.AspectClass
import com.github.kitakkun.aspectk.expression.matcher.FunctionSpec
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

context(MessageCollector)
class AspectKTransformer(private val aspectClasses: List<AspectClass>) : IrElementTransformerVoid() {
    override fun visitElement(element: IrElement): IrElement {
        element.transformChildrenVoid()
        return element
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        aspectClasses.forEach { aspectClass ->
            aspectClass.advices.forEach { advice ->
                if (advice.matcher.matches(declaration.toFunctionSpec())) {
                    report(CompilerMessageSeverity.WARNING, "AspectK: Matched ${advice} to ${declaration.name}")
                }
            }
        }
        return super.visitSimpleFunction(declaration)
    }
}

private fun IrSimpleFunction.toFunctionSpec(): FunctionSpec {
    return FunctionSpec(
        packageName = this.getPackageFragment().packageFqName.asString(),
        className = this.parentClassOrNull?.kotlinFqName?.asString() ?: "",
        functionName = this.name.asString(),
        args = this.valueParameters.map { it.type.classOrFail.owner.classId!! },
        returnType = this.returnType.classOrFail.owner.classId!!,
        modifiers = emptySet(),
        lastArgumentIsVararg = this.valueParameters.lastOrNull()?.isVararg ?: false,
    )
}
