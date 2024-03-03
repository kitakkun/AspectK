package com.github.kitakkun.aspectk.expression

sealed class NameExpression {
    companion object {
        fun fromString(name: String): NameExpression {
            return when {
                name == "*" -> Any
                name == "**" -> Recursive
                name.startsWith("*") -> Suffixed(name.substring(1))
                name.endsWith("*") -> Prefixed(name.substring(0, name.length - 1))
                else -> Normal(name)
            }
        }

    }
    data class Normal(val name: String) : NameExpression()
    data class Prefixed(val prefix: String) : NameExpression()
    data class Suffixed(val suffix: String) : NameExpression()
    data object Recursive: NameExpression()
    data object Any: NameExpression()
}
