package com.github.kitakkun.aspectk.compiler.backend

import com.github.kitakkun.aspectk.compiler.AspectKConsts
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isVararg

class AspectKIrPluginContext(val context: IrPluginContext, val messageCollector: MessageCollector) : IrPluginContext by context {
    val joinPointClassConstructor = referenceClass(AspectKConsts.STATIC_JOIN_POINT_CLASS_ID)?.constructors?.first { it.owner.isPrimary }!!
    val joinPointArgumentClassConstructor =
        referenceClass(AspectKConsts.JOIN_POINT_ARGUMENT_CLASS_ID)?.constructors?.first { it.owner.isPrimary }!!
    val proceedingJoinPointClassConstructor =
        referenceClass(AspectKConsts.PROCEEDING_JOIN_POINT_CLASS_ID)?.constructors?.first { it.owner.isPrimary }!!
    val listOfFunction = referenceFunctions(AspectKConsts.LIST_OF_FUNCTION_ID).first {
        it.owner.valueParameters.size == 1 && it.owner.valueParameters.first().isVararg
    }
}
