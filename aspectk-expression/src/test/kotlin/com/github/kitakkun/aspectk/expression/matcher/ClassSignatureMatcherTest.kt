package com.github.kitakkun.aspectk.expression.matcher

import com.github.kitakkun.aspectk.expression.ClassSignatureExpression
import com.github.kitakkun.aspectk.expression.NameSequenceExpression
import com.github.kitakkun.aspectk.expression.model.ClassSignature
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClassSignatureMatcherTest {
    @Test
    fun normalCase() {
        val packageNames = NameSequenceExpression.fromString("com/example")
        val classNames = NameSequenceExpression.fromString("Foo")

        val matcher = ClassSignatureMatcher(ClassSignatureExpression.Normal(packageNames, classNames))

        assertTrue { matcher.matches(ClassSignature("com.example", "Foo")) }
        assertTrue { matcher.matches(ClassSignature("com/example", "Foo")) }
        assertFalse { matcher.matches(ClassSignature("com.examplee", "Foo")) }
        assertFalse { matcher.matches(ClassSignature("com/examplee", "Foo")) }
        assertFalse { matcher.matches(ClassSignature("com.example.hoge", "Foo")) }
        assertFalse { matcher.matches(ClassSignature("com/example.hoge", "Foo")) }
        assertFalse { matcher.matches(ClassSignature("com.example", "FooBar")) }
        assertFalse { matcher.matches(ClassSignature("com/example", "FooBar")) }
        assertFalse { matcher.matches(ClassSignature("com.example", "Foo.Bar")) }
        assertFalse { matcher.matches(ClassSignature("com/example", "Foo.Bar")) }
    }

    @Test
    fun singleStarInPackage() {
        val packageNames = NameSequenceExpression.fromString("com/example/*")
        val classNames = NameSequenceExpression.fromString("Foo")

        val matcher = ClassSignatureMatcher(ClassSignatureExpression.Normal(packageNames, classNames))
        assertTrue { matcher.matches(ClassSignature("com.example.hoge", "Foo")) }
        assertTrue { matcher.matches(ClassSignature("com.example.fuga", "Foo")) }
        assertFalse { matcher.matches(ClassSignature("com.example.hoge.fuga", "Foo")) }
        assertFalse { matcher.matches(ClassSignature("com.example.hoge", "FooBar")) }
        assertFalse { matcher.matches(ClassSignature("com.example", "Foo")) }
    }
}
