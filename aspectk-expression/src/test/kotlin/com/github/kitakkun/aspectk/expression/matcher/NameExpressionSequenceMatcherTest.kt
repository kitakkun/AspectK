package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.NameExpression
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NameExpressionSequenceMatcherTest {
    @Test
    fun testBasic() {
        val nameExpressions = listOf("com", "example").map { NameExpression.fromString(it) }
        val matcher = NameExpressionSequenceMatcher(nameExpressions)
        assertTrue { matcher.matches(listOf("com", "example")) }
        assertFalse { matcher.matches(listOf("com", "examplee")) }
        assertFalse { matcher.matches(listOf("com", "example", "Hoge")) }
    }

    @Test
    fun testWithAnySingle() {
        val nameExpressions = listOf("com", "*").map { NameExpression.fromString(it) }
        val matcher = NameExpressionSequenceMatcher(nameExpressions)
        assertTrue { matcher.matches(listOf("com", "example")) }
        assertTrue { matcher.matches(listOf("com", "examplee")) }
        assertFalse { matcher.matches(listOf("com", "example", "hoge")) }
    }

    @Test
    fun testRecursive() {
        val nameExpressions = listOf("com", "example", "**").map { NameExpression.fromString(it) }
        val matcher = NameExpressionSequenceMatcher(nameExpressions)
        assertTrue { matcher.matches(listOf("com", "example")) }
        assertTrue { matcher.matches(listOf("com", "example", "hoge")) }
        assertTrue { matcher.matches(listOf("com", "example", "fuga")) }
        assertFalse { matcher.matches(listOf("com", "examplee", "fuga")) }
    }
}
