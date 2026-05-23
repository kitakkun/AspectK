package com.github.kitakkun.aspectk.core

import kotlin.reflect.KClass

data class JoinPoint(
    val dispatchReceiver: JoinPointArgument?,
    val extensionReceiver: JoinPointArgument?,
    val contextArguments: List<JoinPointArgument>,
    val valueArguments: List<JoinPointArgument>,
    val methodName: String,
    val targetClassName: String,
    val signature: String,
)

data class JoinPointArgument(
    val name: String,
    val type: KClass<*>,
    val value: Any?,
    val annotations: List<Annotation>,
)
