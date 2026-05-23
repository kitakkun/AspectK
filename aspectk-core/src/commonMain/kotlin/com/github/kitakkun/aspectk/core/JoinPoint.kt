package com.github.kitakkun.aspectk.core

import kotlin.reflect.KClass

sealed class JoinPoint(
    val dispatchReceiver: JoinPointArgument?,
    val extensionReceiver: JoinPointArgument?,
    val contextArguments: List<JoinPointArgument>,
    val valueArguments: List<JoinPointArgument>,
    val methodName: String,
    val targetClassName: String,
    val signature: String,
) {
    override fun toString(): String =
        "${this::class.simpleName ?: "JoinPoint"}(" +
            "dispatchReceiver=$dispatchReceiver, " +
            "extensionReceiver=$extensionReceiver, " +
            "contextArguments=$contextArguments, " +
            "valueArguments=$valueArguments, " +
            "methodName=$methodName, " +
            "targetClassName=$targetClassName, " +
            "signature=$signature" +
            ")"
}

class StaticJoinPoint(
    dispatchReceiver: JoinPointArgument?,
    extensionReceiver: JoinPointArgument?,
    contextArguments: List<JoinPointArgument>,
    valueArguments: List<JoinPointArgument>,
    methodName: String,
    targetClassName: String,
    signature: String,
) : JoinPoint(
    dispatchReceiver = dispatchReceiver,
    extensionReceiver = extensionReceiver,
    contextArguments = contextArguments,
    valueArguments = valueArguments,
    methodName = methodName,
    targetClassName = targetClassName,
    signature = signature,
)

data class JoinPointArgument(
    val name: String,
    val type: KClass<*>,
    val value: Any?,
    val annotations: List<Annotation>,
)
