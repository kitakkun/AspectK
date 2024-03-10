package com.github.kitakkun.aspectk.compiler.backend.utils

import com.github.kitakkun.aspectk.expression.model.ClassSignature
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.getPackageFragment

fun IrClass.toClassSignature(): ClassSignature {
    val packageName = this.getPackageFragment().packageFqName.asString()
    val className = this.name.asString()
    val superTypes = superTypes.map {
        it.classOrNull?.owner?.toClassSignature() ?: error("Cannot resolve class signature from $it")
    }
    return ClassSignature(packageName, className, superTypes)
}
