package com.github.kitakkun.aspectk.core

class ProceedingJoinPoint(
    dispatchReceiver: JoinPointArgument?,
    extensionReceiver: JoinPointArgument?,
    contextArguments: List<JoinPointArgument>,
    valueArguments: List<JoinPointArgument>,
    methodName: String,
    targetClassName: String,
    signature: String,
    private val proceedFn: () -> Any?,
) : JoinPoint(
    dispatchReceiver = dispatchReceiver,
    extensionReceiver = extensionReceiver,
    contextArguments = contextArguments,
    valueArguments = valueArguments,
    methodName = methodName,
    targetClassName = targetClassName,
    signature = signature,
) {
    fun proceed(): Any? = proceedFn()

    fun proceed(newArgs: List<Any?>): Any? {
        throw UnsupportedOperationException(
            "ProceedingJoinPoint.proceed(newArgs) is not supported yet; use proceed() to invoke the original method with captured arguments.",
        )
    }
}
