package com.github.kitakkun.aspectk.expression.lexer

import org.junit.Test
import kotlin.test.assertContentEquals

class AspectKLexerTest {
    @Test
    fun testExecution() {
        val scanner = AspectKLexer("execution(public com/example/extension/foo(..): Unit receiver com/example/A)")
        val tokens = scanner.analyze()
        val expected =
            listOf(
                AspectKToken(AspectKTokenType.IDENTIFIER, "execution"),
                AspectKToken(AspectKTokenType.LEFT_PAREN, "("),
                AspectKToken(AspectKTokenType.IDENTIFIER, "public"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "com"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "example"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "extension"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "foo"),
                AspectKToken(AspectKTokenType.LEFT_PAREN, "("),
                AspectKToken(AspectKTokenType.DOUBLE_DOT, ".."),
                AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"),
                AspectKToken(AspectKTokenType.COLON, ":"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "Unit"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "receiver"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "com"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "example"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "A"),
                AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"),
            )
        assertContentEquals(expected = expected, actual = tokens)
    }

    @Test
    fun testArgs() {
        val scanner = AspectKLexer("args(Int, Boolean, .., String...)")
        val tokens = scanner.analyze()
        val expected =
            listOf(
                AspectKToken(AspectKTokenType.IDENTIFIER, "args"),
                AspectKToken(AspectKTokenType.LEFT_PAREN, "("),
                AspectKToken(AspectKTokenType.IDENTIFIER, "Int"),
                AspectKToken(AspectKTokenType.COMMA, ","),
                AspectKToken(AspectKTokenType.IDENTIFIER, "Boolean"),
                AspectKToken(AspectKTokenType.COMMA, ","),
                AspectKToken(AspectKTokenType.DOUBLE_DOT, ".."),
                AspectKToken(AspectKTokenType.COMMA, ","),
                AspectKToken(AspectKTokenType.IDENTIFIER, "String"),
                AspectKToken(AspectKTokenType.TRIPLE_DOT, "..."),
                AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"),
            )
        assertContentEquals(expected = expected, actual = tokens)
    }

    @Test
    fun testThis() {
        val scanner = AspectKLexer("this(com/example/Hoge.Fuga)")
        val tokens = scanner.analyze()
        val expected =
            listOf(
                AspectKToken(AspectKTokenType.IDENTIFIER, "this"),
                AspectKToken(AspectKTokenType.LEFT_PAREN, "("),
                AspectKToken(AspectKTokenType.IDENTIFIER, "com"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "example"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "Hoge"),
                AspectKToken(AspectKTokenType.DOT, "."),
                AspectKToken(AspectKTokenType.IDENTIFIER, "Fuga"),
                AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"),
            )
        assertContentEquals(expected = expected, actual = tokens)
    }

    @Test
    fun testTarget() {
        val scanner = AspectKLexer("target(com/example/Hoge)")
        val tokens = scanner.analyze()
        val expected =
            listOf(
                AspectKToken(AspectKTokenType.IDENTIFIER, "target"),
                AspectKToken(AspectKTokenType.LEFT_PAREN, "("),
                AspectKToken(AspectKTokenType.IDENTIFIER, "com"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "example"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "Hoge"),
                AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"),
            )
    }
}
