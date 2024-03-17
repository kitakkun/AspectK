package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.NameSequenceExpression
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NameSequenceExpressionMatcherTest {
    @Test
    fun testBasic() {
        val nameExpressions = NameSequenceExpression.fromString("com/example")
        val matcher = NameSequenceExpressionMatcher(nameExpressions)
        assertTrue { matcher.matches(listOf("com", "example")) }
        assertFalse { matcher.matches(listOf("com", "examplee")) }
        assertFalse { matcher.matches(listOf("com", "example", "Hoge")) }
    }

    @Test
    fun testWithAnySingle() {
        val nameExpressions = NameSequenceExpression.fromString("com/*")
        val matcher = NameSequenceExpressionMatcher(nameExpressions)
        assertTrue { matcher.matches(listOf("com", "example")) }
        assertTrue { matcher.matches(listOf("com", "examplee")) }
        assertFalse { matcher.matches(listOf("com", "example", "hoge")) }
    }

    @Test
    fun testRecursive() {
        val nameExpressions = NameSequenceExpression.fromString("com/example/**")
        val matcher = NameSequenceExpressionMatcher(nameExpressions)
        assertTrue { matcher.matches(listOf("com", "example")) }
        assertTrue { matcher.matches(listOf("com", "example", "hoge")) }
        assertTrue { matcher.matches(listOf("com", "example", "fuga")) }
        assertFalse { matcher.matches(listOf("com", "examplee", "fuga")) }
    }
}
