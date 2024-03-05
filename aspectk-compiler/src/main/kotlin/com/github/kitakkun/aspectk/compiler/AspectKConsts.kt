package com.github.kitakkun.aspectk.compiler

import org.jetbrains.kotlin.javac.resolve.classId
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object AspectKConsts {
    val JOIN_POINT_CLASS_ID = classId("com.github.kitakkun.aspectk.core", "JoinPoint")
    val JOIN_POINT_FQ_NAME = JOIN_POINT_CLASS_ID.asSingleFqName()
    val LIST_OF_FUNCTION_ID = CallableId(FqName("kotlin.collections"), Name.identifier("listOf"))
}
