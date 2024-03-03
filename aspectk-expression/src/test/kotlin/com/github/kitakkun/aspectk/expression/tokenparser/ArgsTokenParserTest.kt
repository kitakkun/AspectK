package com.github.kitakkun.aspectk.expression.tokenparser

import com.github.kitakkun.aspectk.expression.lexer.AspectKToken
import com.github.kitakkun.aspectk.expression.lexer.AspectKTokenType
import org.junit.Test
import kotlin.test.assertContentEquals

class ArgsTokenParserTest {
    @Test
    fun singleToken() {
        val parser = ArgsTokenParser(
            listOf(
                AspectKToken(type = AspectKTokenType.IDENTIFIER, lexeme = "Int"),
            )
        )
        val actual = parser.parseTokens()
        val expected = listOf(ArgsToken(type = ArgsTokenType.CLASS, lexeme = "Int"))

        assertContentEquals(expected = expected, actual = actual)
    }

    @Test
    fun testVarargAndNoneOrMore() {
        val parser = ArgsTokenParser(
            listOf(
                AspectKToken(type = AspectKTokenType.IDENTIFIER, lexeme = "com"),
                AspectKToken(type = AspectKTokenType.SLASH, lexeme = "/"),
                AspectKToken(type = AspectKTokenType.IDENTIFIER, lexeme = "example"),
                AspectKToken(type = AspectKTokenType.SLASH, lexeme = "/"),
                AspectKToken(type = AspectKTokenType.IDENTIFIER, lexeme = "Hoge"),
                AspectKToken(type = AspectKTokenType.COMMA, lexeme = ","),
                AspectKToken(type = AspectKTokenType.IDENTIFIER, lexeme = "Int"),
                AspectKToken(type = AspectKTokenType.COMMA, lexeme = ","),
                AspectKToken(type = AspectKTokenType.DOUBLE_DOT, lexeme = ".."),
                AspectKToken(type = AspectKTokenType.COMMA, lexeme = ","),
                AspectKToken(type = AspectKTokenType.IDENTIFIER, lexeme = "String"),
                AspectKToken(type = AspectKTokenType.TRIPLE_DOT, lexeme = "..."),
            )
        )
        val actual = parser.parseTokens()
        val expected = listOf(
            ArgsToken(type = ArgsTokenType.PACKAGE_PART, lexeme = "com"),
            ArgsToken(type = ArgsTokenType.PACKAGE_PART, lexeme = "example"),
            ArgsToken(type = ArgsTokenType.CLASS, lexeme = "Hoge"),
            ArgsToken(type = ArgsTokenType.ARG_SEPARATOR, lexeme = ","),
            ArgsToken(type = ArgsTokenType.CLASS, lexeme = "Int"),
            ArgsToken(type = ArgsTokenType.ARG_SEPARATOR, lexeme = ","),
            ArgsToken(type = ArgsTokenType.NONE_OR_MORE_ARGS, lexeme = ".."),
            ArgsToken(type = ArgsTokenType.ARG_SEPARATOR, lexeme = ","),
            ArgsToken(type = ArgsTokenType.CLASS, lexeme = "String"),
            ArgsToken(type = ArgsTokenType.VAR_ARG, lexeme = "..."),
        )

        assertContentEquals(expected = expected, actual = actual)
    }

    @Test
    fun testWildcardClassExpression() {
        val parser = ArgsTokenParser(
            listOf(
                AspectKToken(type = AspectKTokenType.STAR, lexeme = "*"),
                AspectKToken(type = AspectKTokenType.COMMA, lexeme = ","),
                AspectKToken(type = AspectKTokenType.IDENTIFIER, lexeme = "com"),
                AspectKToken(type = AspectKTokenType.SLASH, lexeme = "/"),
                AspectKToken(type = AspectKTokenType.IDENTIFIER, lexeme = "example"),
                AspectKToken(type = AspectKTokenType.SLASH, lexeme = "/"),
                AspectKToken(type = AspectKTokenType.STAR, lexeme = "*"),
            )
        )
        val actual = parser.parseTokens()
        val expected = listOf(
            ArgsToken(type = ArgsTokenType.SINGLE_ANY_ARG, lexeme = "*"),
            ArgsToken(type = ArgsTokenType.ARG_SEPARATOR, lexeme = ","),
            ArgsToken(type = ArgsTokenType.PACKAGE_PART, lexeme = "com"),
            ArgsToken(type = ArgsTokenType.PACKAGE_PART, lexeme = "example"),
            ArgsToken(type = ArgsTokenType.CLASS, lexeme = "*"),
        )

        assertContentEquals(expected = expected, actual = actual)
    }

    @Test
    fun testWildcardPackageExpression() {
        val parser = ArgsTokenParser(
            listOf(
                AspectKToken(type = AspectKTokenType.IDENTIFIER, lexeme = "com"),
                AspectKToken(type = AspectKTokenType.SLASH, lexeme = "/"),
                AspectKToken(type = AspectKTokenType.DOUBLE_STAR, lexeme = "**"),
                AspectKToken(type = AspectKTokenType.SLASH, lexeme = "/"),
                AspectKToken(type = AspectKTokenType.STAR, lexeme = "*"),
            )
        )
        val actual = parser.parseTokens()
        val expected = listOf(
            ArgsToken(type = ArgsTokenType.PACKAGE_PART, lexeme = "com"),
            ArgsToken(type = ArgsTokenType.PACKAGE_PART, lexeme = "**"),
            ArgsToken(type = ArgsTokenType.CLASS, lexeme = "*"),
        )

        assertContentEquals(expected = expected, actual = actual)
    }
}
