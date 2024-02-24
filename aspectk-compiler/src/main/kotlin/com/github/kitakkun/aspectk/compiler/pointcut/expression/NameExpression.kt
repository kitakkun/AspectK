package com.github.kitakkun.aspectk.compiler.pointcut.expression

sealed class NameExpression {
    data class Normal(val name: String) : NameExpression()
    data class Prefixed(val prefix: String) : NameExpression()
    data class Suffixed(val suffix: String) : NameExpression()
}
