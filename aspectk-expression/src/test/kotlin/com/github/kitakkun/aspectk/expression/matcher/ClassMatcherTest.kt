package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.NameExpression
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClassMatcherTest {
    @Test
    fun normalCase() {
        val packageNames = listOf("com", "example").map { NameExpression.fromString(it) }
        val classNames = listOf("Foo").map { NameExpression.fromString(it) }

        val matcher = ClassMatcher(packageNames, classNames)

        assertTrue { matcher.matches("com.example", "Foo") }
        assertTrue { matcher.matches("com/example", "Foo") }
        assertFalse { matcher.matches("com.examplee", "Foo") }
        assertFalse { matcher.matches("com/examplee", "Foo") }
        assertFalse { matcher.matches("com.example.hoge", "Foo") }
        assertFalse { matcher.matches("com/example.hoge", "Foo") }
        assertFalse { matcher.matches("com.example", "FooBar") }
        assertFalse { matcher.matches("com/example", "FooBar") }
        assertFalse { matcher.matches("com.example", "Foo.Bar") }
        assertFalse { matcher.matches("com/example", "Foo.Bar") }
    }

    @Test
    fun singleStarInPackage() {
        val packageNames = listOf("com", "example", "*").map { NameExpression.fromString(it) }
        val classNames = listOf("Foo").map { NameExpression.fromString(it) }

        val matcher = ClassMatcher(packageNames, classNames)
        assertTrue { matcher.matches("com.example.hoge", "Foo") }
        assertTrue { matcher.matches("com.example.fuga", "Foo") }
        assertFalse { matcher.matches("com.example.hoge.fuga", "Foo") }
        assertFalse { matcher.matches("com.example.hoge", "FooBar") }
        assertFalse { matcher.matches("com.example", "Foo") }
    }
}
