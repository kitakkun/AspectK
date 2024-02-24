package com.github.kitakkun.aspectk.compiler.lexer

import com.github.kitakkun.aspectk.compiler.pointcut.lexer.AspectKPointcutExpressionLexer
import com.github.kitakkun.aspectk.compiler.pointcut.lexer.AspectKToken
import com.github.kitakkun.aspectk.compiler.pointcut.lexer.AspectKTokenType
import org.junit.Test
import kotlin.test.assertEquals

class PointcutExpressionLexerTest {
    @Test
    fun testExecution() {
        val scanner = AspectKPointcutExpressionLexer("execution(public * *(..))")
        val tokens = scanner.analyze()
        assertEquals(9, tokens.size)
        assertEquals(AspectKToken(AspectKTokenType.EXECUTION, "execution"), tokens[0])
        assertEquals(AspectKToken(AspectKTokenType.LEFT_PAREN, "("), tokens[1])
        assertEquals(AspectKToken(AspectKTokenType.IDENTIFIER, "public"), tokens[2])
        assertEquals(AspectKToken(AspectKTokenType.STAR, "*"), tokens[3])
        assertEquals(AspectKToken(AspectKTokenType.STAR, "*"), tokens[4])
        assertEquals(AspectKToken(AspectKTokenType.LEFT_PAREN, "("), tokens[5])
        assertEquals(AspectKToken(AspectKTokenType.DOUBLE_DOT, ".."), tokens[6])
        assertEquals(AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"), tokens[7])
        assertEquals(AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"), tokens[8])
    }

    @Test
    fun testAnnotatedArgs() {
        val scanner = AspectKPointcutExpressionLexer("@args(com.example.TestAnnotation)")
        val tokens = scanner.analyze()
        assertEquals(8, tokens.size)
        assertEquals(AspectKToken(AspectKTokenType.ANNOTATED_ARGS, "@args"), tokens[0])
        assertEquals(AspectKToken(AspectKTokenType.LEFT_PAREN, "("), tokens[1])
        assertEquals(AspectKToken(AspectKTokenType.IDENTIFIER, "com"), tokens[2])
        assertEquals(AspectKToken(AspectKTokenType.DOT, "."), tokens[3])
        assertEquals(AspectKToken(AspectKTokenType.IDENTIFIER, "example"), tokens[4])
        assertEquals(AspectKToken(AspectKTokenType.DOT, "."), tokens[5])
        assertEquals(AspectKToken(AspectKTokenType.IDENTIFIER, "TestAnnotation"), tokens[6])
        assertEquals(AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"), tokens[7])
    }

    @Test
    fun testThis() {
        val scanner = AspectKPointcutExpressionLexer("this(com.example.TestClass)")
        val tokens = scanner.analyze()
        assertEquals(8, tokens.size)
        assertEquals(AspectKToken(AspectKTokenType.THIS, "this"), tokens[0])
        assertEquals(AspectKToken(AspectKTokenType.LEFT_PAREN, "("), tokens[1])
        assertEquals(AspectKToken(AspectKTokenType.IDENTIFIER, "com"), tokens[2])
        assertEquals(AspectKToken(AspectKTokenType.DOT, "."), tokens[3])
        assertEquals(AspectKToken(AspectKTokenType.IDENTIFIER, "example"), tokens[4])
        assertEquals(AspectKToken(AspectKTokenType.DOT, "."), tokens[5])
        assertEquals(AspectKToken(AspectKTokenType.IDENTIFIER, "TestClass"), tokens[6])
        assertEquals(AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"), tokens[7])
    }

    @Test
    fun testTarget() {
        val scanner = AspectKPointcutExpressionLexer("target(com.example.TestClass)")
        val tokens = scanner.analyze()
        assertEquals(8, tokens.size)
        assertEquals(AspectKToken(AspectKTokenType.TARGET, "target"), tokens[0])
        assertEquals(AspectKToken(AspectKTokenType.LEFT_PAREN, "("), tokens[1])
        assertEquals(AspectKToken(AspectKTokenType.IDENTIFIER, "com"), tokens[2])
        assertEquals(AspectKToken(AspectKTokenType.DOT, "."), tokens[3])
        assertEquals(AspectKToken(AspectKTokenType.IDENTIFIER, "example"), tokens[4])
        assertEquals(AspectKToken(AspectKTokenType.DOT, "."), tokens[5])
        assertEquals(AspectKToken(AspectKTokenType.IDENTIFIER, "TestClass"), tokens[6])
        assertEquals(AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"), tokens[7])
    }
}
