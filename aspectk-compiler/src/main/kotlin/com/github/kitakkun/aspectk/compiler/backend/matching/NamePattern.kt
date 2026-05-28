package com.github.kitakkun.aspectk.compiler.backend.matching

/**
 * Glob-style name matching for `@ClassName` and `@MethodName` patterns.
 *
 * Pattern syntax:
 * - A literal name matches exactly (`"save"`).
 * - `*` matches zero or more characters in that position (`"save*"`, `"*Async"`,
 *   `"*find*"`, `"*"`).
 *
 * The pattern is anchored — the entire name must match.
 */
internal object NamePattern {
    fun matches(
        pattern: String,
        name: String,
    ): Boolean {
        if (pattern == name) return true
        if (pattern.isEmpty()) return name.isEmpty()
        if (pattern == "*") return true
        if ('*' !in pattern) return false

        val regex = pattern
            .split('*')
            .joinToString(separator = ".*") { Regex.escape(it) }
            .let { "^$it$".toRegex() }
        return regex.matches(name)
    }
}
