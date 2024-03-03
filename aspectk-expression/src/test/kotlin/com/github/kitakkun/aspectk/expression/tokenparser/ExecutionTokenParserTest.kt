package com.github.kitakkun.aspectk.expression.tokenparser

import com.github.kitakkun.aspectk.expression.lexer.AspectKToken
import com.github.kitakkun.aspectk.expression.lexer.AspectKTokenType
import org.junit.Test
import kotlin.test.assertContentEquals

class ExecutionTokenParserTest {
    @Test
    fun topLevelFunction() {
        val parser = ExecutionTokenParser(
            expressionTokens = listOf(
                AspectKToken(AspectKTokenType.IDENTIFIER, "public"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "com"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "example"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "foo"),
                AspectKToken(AspectKTokenType.LEFT_PAREN, "("),
                AspectKToken(AspectKTokenType.IDENTIFIER, "Int"),
                AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"),
                AspectKToken(AspectKTokenType.COLON, ":"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "String"),
            )
        )

        val actual = parser.parseToExecutionTokens()
        val expectedExecutionTokens = listOf(
            ExecutionToken(type = ExecutionTokenType.MODIFIER, lexeme = "public"),
            ExecutionToken(type = ExecutionTokenType.PACKAGE_PART, lexeme = "com"),
            ExecutionToken(type = ExecutionTokenType.PACKAGE_PART, lexeme = "example"),
            ExecutionToken(type = ExecutionTokenType.FUNCTION, lexeme = "foo"),
            ExecutionToken(type = ExecutionTokenType.RETURN_TYPE_START, lexeme = ":"),
            ExecutionToken(type = ExecutionTokenType.CLASS, lexeme = "String"),
            ExecutionToken(type = ExecutionTokenType.RETURN_TYPE_END, lexeme = ""),
        )
        val expectedArgsTokens = listOf(
            ArgsToken(type = ArgsTokenType.CLASS, lexeme = "Int"),
        )

        assertContentEquals(expected = expectedExecutionTokens, actual = actual.first)
        assertContentEquals(expected = expectedArgsTokens, actual = actual.second)
    }

    @Test
    fun classMethod() {
        val parser = ExecutionTokenParser(
            expressionTokens = listOf(
                AspectKToken(AspectKTokenType.IDENTIFIER, "public"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "com"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "example"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "A"),
                AspectKToken(AspectKTokenType.DOT, "."),
                AspectKToken(AspectKTokenType.IDENTIFIER, "foo"),
                AspectKToken(AspectKTokenType.LEFT_PAREN, "("),
                AspectKToken(AspectKTokenType.IDENTIFIER, "Int"),
                AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"),
                AspectKToken(AspectKTokenType.COLON, ":"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "String"),
            )
        )

        val actual = parser.parseToExecutionTokens()
        val expectedExecutionTokens = listOf(
            ExecutionToken(type = ExecutionTokenType.MODIFIER, lexeme = "public"),
            ExecutionToken(type = ExecutionTokenType.PACKAGE_PART, lexeme = "com"),
            ExecutionToken(type = ExecutionTokenType.PACKAGE_PART, lexeme = "example"),
            ExecutionToken(type = ExecutionTokenType.CLASS, lexeme = "A"),
            ExecutionToken(type = ExecutionTokenType.FUNCTION, lexeme = "foo"),
            ExecutionToken(type = ExecutionTokenType.RETURN_TYPE_START, lexeme = ":"),
            ExecutionToken(type = ExecutionTokenType.CLASS, lexeme = "String"),
            ExecutionToken(type = ExecutionTokenType.RETURN_TYPE_END, lexeme = ""),
        )
        val expectedArgsTokens = listOf(
            ArgsToken(type = ArgsTokenType.CLASS, lexeme = "Int"),
        )

        assertContentEquals(expected = expectedExecutionTokens, actual = actual.first)
        assertContentEquals(expected = expectedArgsTokens, actual = actual.second)
    }

    @Test
    fun nestedClassMethod() {
        val parser = ExecutionTokenParser(
            expressionTokens = listOf(
                AspectKToken(AspectKTokenType.IDENTIFIER, "public"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "com"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "example"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "A"),
                AspectKToken(AspectKTokenType.DOT, "."),
                AspectKToken(AspectKTokenType.IDENTIFIER, "B"),
                AspectKToken(AspectKTokenType.DOT, "."),
                AspectKToken(AspectKTokenType.IDENTIFIER, "foo"),
                AspectKToken(AspectKTokenType.LEFT_PAREN, "("),
                AspectKToken(AspectKTokenType.IDENTIFIER, "Int"),
                AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"),
                AspectKToken(AspectKTokenType.COLON, ":"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "String"),
            )
        )

        val actual = parser.parseToExecutionTokens()
        val expectedExecutionTokens = listOf(
            ExecutionToken(type = ExecutionTokenType.MODIFIER, lexeme = "public"),
            ExecutionToken(type = ExecutionTokenType.PACKAGE_PART, lexeme = "com"),
            ExecutionToken(type = ExecutionTokenType.PACKAGE_PART, lexeme = "example"),
            ExecutionToken(type = ExecutionTokenType.CLASS, lexeme = "A"),
            ExecutionToken(type = ExecutionTokenType.CLASS, lexeme = "B"),
            ExecutionToken(type = ExecutionTokenType.FUNCTION, lexeme = "foo"),
            ExecutionToken(type = ExecutionTokenType.RETURN_TYPE_START, lexeme = ":"),
            ExecutionToken(type = ExecutionTokenType.CLASS, lexeme = "String"),
            ExecutionToken(type = ExecutionTokenType.RETURN_TYPE_END, lexeme = ""),
        )
        val expectedArgsTokens = listOf(
            ArgsToken(type = ArgsTokenType.CLASS, lexeme = "Int"),
        )

        assertContentEquals(expected = expectedExecutionTokens, actual = actual.first)
        assertContentEquals(expected = expectedArgsTokens, actual = actual.second)
    }

    @Test
    fun wildcardOnExecutionClass() {
        val parser = ExecutionTokenParser(
            expressionTokens = listOf(
                AspectKToken(AspectKTokenType.IDENTIFIER, "public"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "com"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "example"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.STAR, "*"),
                AspectKToken(AspectKTokenType.DOT, "."),
                AspectKToken(AspectKTokenType.IDENTIFIER, "foo"),
                AspectKToken(AspectKTokenType.LEFT_PAREN, "("),
                AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"),
            )
        )

        val actual = parser.parseToExecutionTokens()
        val expectedExecutionTokens = listOf(
            ExecutionToken(type = ExecutionTokenType.MODIFIER, lexeme = "public"),
            ExecutionToken(type = ExecutionTokenType.PACKAGE_PART, lexeme = "com"),
            ExecutionToken(type = ExecutionTokenType.PACKAGE_PART, lexeme = "example"),
            ExecutionToken(type = ExecutionTokenType.CLASS, lexeme = "*"),
            ExecutionToken(type = ExecutionTokenType.FUNCTION, lexeme = "foo"),
        )
        val expectedArgsTokens = emptyList<ArgsToken>()

        assertContentEquals(expected = expectedExecutionTokens, actual = actual.first)
        assertContentEquals(expected = expectedArgsTokens, actual = actual.second)
    }

    @Test
    fun wildcardOnExecutionPackage() {
        val parser = ExecutionTokenParser(
            expressionTokens = listOf(
                AspectKToken(AspectKTokenType.IDENTIFIER, "public"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "com"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.STAR, "*"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "foo"),
                AspectKToken(AspectKTokenType.LEFT_PAREN, "("),
                AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"),
            )
        )

        val actual = parser.parseToExecutionTokens()
        val expectedExecutionTokens = listOf(
            ExecutionToken(type = ExecutionTokenType.MODIFIER, lexeme = "public"),
            ExecutionToken(type = ExecutionTokenType.PACKAGE_PART, lexeme = "com"),
            ExecutionToken(type = ExecutionTokenType.PACKAGE_PART, lexeme = "*"),
            ExecutionToken(type = ExecutionTokenType.FUNCTION, lexeme = "foo"),
        )
        val expectedArgsTokens = emptyList<ArgsToken>()

        assertContentEquals(expected = expectedExecutionTokens, actual = actual.first)
        assertContentEquals(expected = expectedArgsTokens, actual = actual.second)
    }

    @Test
    fun wildcardOnExecutionPackage2() {
        val parser = ExecutionTokenParser(
            expressionTokens = listOf(
                AspectKToken(AspectKTokenType.IDENTIFIER, "public"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "com"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "example"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.DOUBLE_STAR, "**"),
                AspectKToken(AspectKTokenType.SLASH, "/"),
                AspectKToken(AspectKTokenType.IDENTIFIER, "foo"),
                AspectKToken(AspectKTokenType.LEFT_PAREN, "("),
                AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"),
            )
        )

        val actual = parser.parseToExecutionTokens()
        val expectedExecutionTokens = listOf(
            ExecutionToken(type = ExecutionTokenType.MODIFIER, lexeme = "public"),
            ExecutionToken(type = ExecutionTokenType.PACKAGE_PART, lexeme = "com"),
            ExecutionToken(type = ExecutionTokenType.PACKAGE_PART, lexeme = "example"),
            ExecutionToken(type = ExecutionTokenType.PACKAGE_PART, lexeme = "**"),
            ExecutionToken(type = ExecutionTokenType.FUNCTION, lexeme = "foo"),
        )
        val expectedArgsTokens = emptyList<ArgsToken>()

        assertContentEquals(expected = expectedExecutionTokens, actual = actual.first)
        assertContentEquals(expected = expectedArgsTokens, actual = actual.second)
    }
}
