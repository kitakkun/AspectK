package com.github.kitakkun.aspectk.expression.tokenparser

import com.github.kitakkun.aspectk.expression.lexer.AspectKToken
import com.github.kitakkun.aspectk.expression.lexer.AspectKTokenType
import org.junit.Test
import kotlin.test.assertContentEquals

class NamedPointcutTokenParserTest {
    @Test
    fun test() {
        val rawTokens =
            listOf(
                AspectKToken(AspectKTokenType.IDENTIFIER, "com"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "example"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "ClassA"),
                AspectKToken(AspectKTokenType.DOT, "."),
                AspectKToken(AspectKTokenType.IDENTIFIER, "ClassB"),
                AspectKToken(AspectKTokenType.DOT, "."),
                AspectKToken(AspectKTokenType.IDENTIFIER, "customPointcut"),
            )
        val parser = NamedPointcutTokenParser(rawTokens)
        val actual = parser.parseToNamedPointcutTokens()
        val expected =
            listOf(
                NamedPointcutToken(NamedPointcutTokenType.PACKAGE, "com"),
                NamedPointcutToken(NamedPointcutTokenType.PACKAGE, "example"),
                NamedPointcutToken(NamedPointcutTokenType.CLASS, "ClassA"),
                NamedPointcutToken(NamedPointcutTokenType.CLASS, "ClassB"),
                NamedPointcutToken(NamedPointcutTokenType.FUNCTION, "customPointcut"),
            )

        assertContentEquals(expected = expected, actual = actual)
    }
}
