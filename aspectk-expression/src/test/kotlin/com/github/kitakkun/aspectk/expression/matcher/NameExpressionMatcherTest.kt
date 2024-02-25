package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.NameExpression
import org.junit.Test

class NameExpressionMatcherTest {
    @Test
    fun testCompleteMatch() {
        val matcher = NameExpressionMatcher(NameExpression.Normal("test"))
        assert(matcher.matches("test"))
    }

    @Test
    fun testPrefixMatch() {
        val matcher = NameExpressionMatcher(NameExpression.Prefixed("test"))
        assert(matcher.matches("test123"))
    }

    @Test
    fun testSuffixMatch() {
        val matcher = NameExpressionMatcher(NameExpression.Suffixed("123"))
        assert(matcher.matches("test123"))
    }
}
