package com.github.kitakkun.aspectk.compiler.backend.utils

import com.github.kitakkun.aspectk.expression.PointcutExpression
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

val PointcutExpression.Named.callableId
    get() = CallableId(
        packageName = FqName(packageNames.joinToString("/") { it.name }),
        className = FqName(classNames.joinToString(".") { it.name }),
        callableName = Name.identifier(functionName.name),
    )
