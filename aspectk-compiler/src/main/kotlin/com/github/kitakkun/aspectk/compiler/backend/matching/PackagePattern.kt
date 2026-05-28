package com.github.kitakkun.aspectk.compiler.backend.matching

/**
 * Glob-style package matching for `@Package` patterns.
 *
 * Pattern syntax:
 * - A literal name (`"com.example.repo"`) matches that package exactly.
 * - `*` matches a single whole segment (e.g. `"com.example.*"` matches
 *   `com.example.foo` but not `com.example.foo.bar` and not `com.example`).
 * - `**` matches zero or more whole segments. When immediately surrounded by
 *   dots the surrounding dot is treated as part of the wildcard — so
 *   `"com.**.repo"` matches `com.repo`, `com.foo.repo`, `com.foo.bar.repo`,
 *   and `"com.**"` matches `com`, `com.foo`, `com.foo.bar`.
 * - Root package (top-level declarations with no package): use `pattern = ""`.
 *
 * The pattern is anchored — the entire package name must match.
 */
internal object PackagePattern {
    fun matches(
        pattern: String,
        packageName: String,
    ): Boolean {
        if (pattern == packageName) return true
        if (pattern.isEmpty()) return packageName.isEmpty()
        if (pattern == "**") return true
        if ('*' !in pattern) return false
        return compile(pattern).matches(packageName)
    }

    private fun compile(pattern: String): Regex {
        val sb = StringBuilder("^")
        var i = 0
        while (i < pattern.length) {
            val ch = pattern[i]
            when {
                i + 1 < pattern.length && ch == '*' && pattern[i + 1] == '*' -> {
                    val precededByEscapedDot = sb.endsWith("\\.")
                    val followedByDot = i + 2 < pattern.length && pattern[i + 2] == '.'
                    when {
                        precededByEscapedDot && followedByDot -> {
                            // `.**.` — strip the trailing `\.`, replace with the
                            // group that swallows the preceding dot too.
                            sb.setLength(sb.length - 2)
                            sb.append("(\\.[^.]+)*\\.")
                            i += 3
                        }
                        precededByEscapedDot -> {
                            // `.**` at the end of the pattern.
                            sb.setLength(sb.length - 2)
                            sb.append("(\\.[^.]+)*")
                            i += 2
                        }
                        followedByDot -> {
                            // `**.` at the start of the pattern.
                            sb.append("([^.]+\\.)*")
                            i += 3
                        }
                        else -> {
                            sb.append(".*")
                            i += 2
                        }
                    }
                }
                ch == '*' -> {
                    sb.append("[^.]*")
                    i++
                }
                ch == '.' -> {
                    sb.append("\\.")
                    i++
                }
                else -> {
                    sb.append(Regex.escape(ch.toString()))
                    i++
                }
            }
        }
        sb.append("$")
        return Regex(sb.toString())
    }
}
