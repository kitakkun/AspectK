package com.github.kitakkun.aspectk.compiler.lexer

import com.github.kitakkun.aspectk.compiler.pointcut.lexer.AspectKPointcutExpressionLexer
import com.github.kitakkun.aspectk.compiler.pointcut.lexer.AspectKToken
import com.github.kitakkun.aspectk.compiler.pointcut.lexer.AspectKTokenType
import org.junit.Test
import kotlin.test.assertEquals

class PointcutExpressionLexerTest {
    @Test
    fun testExecution() {
        val scanner = AspectKPointcutExpressionLexer("execution(public *  *(..))")
        val tokens = scanner.analyze()
        assertEquals(4, tokens.size)
        assertEquals(AspectKToken(AspectKTokenType.EXECUTION, "execution"), tokens[0])
        assertEquals(AspectKToken(AspectKTokenType.LEFT_PAREN, "("), tokens[1])
        assertEquals(AspectKToken(AspectKTokenType.POINTCUT_STRING_LITERAL, "public *  *(..)"), tokens[2])
        assertEquals(AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"), tokens[3])
    }

    @Test
    fun emptyExecution() {
        val scanner = AspectKPointcutExpressionLexer("execution()")
        val tokens = scanner.analyze()
        assertEquals(4, tokens.size)
        assertEquals(AspectKToken(AspectKTokenType.EXECUTION, "execution"), tokens[0])
        assertEquals(AspectKToken(AspectKTokenType.LEFT_PAREN, "("), tokens[1])
        assertEquals(AspectKToken(AspectKTokenType.POINTCUT_STRING_LITERAL, ""), tokens[2])
        assertEquals(AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"), tokens[3])
    }

    @Test
    fun testAnnotatedArgs() {
        val scanner = AspectKPointcutExpressionLexer("@args(com/example/TestAnnotation)")
        val tokens = scanner.analyze()
        assertEquals(4, tokens.size)
        assertEquals(AspectKToken(AspectKTokenType.ANNOTATED_ARGS, "@args"), tokens[0])
        assertEquals(AspectKToken(AspectKTokenType.LEFT_PAREN, "("), tokens[1])
        assertEquals(AspectKToken(AspectKTokenType.POINTCUT_STRING_LITERAL, "com/example/TestAnnotation"), tokens[2])
        assertEquals(AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"), tokens[3])
    }

    @Test
    fun testThis() {
        val scanner = AspectKPointcutExpressionLexer("this(com/example/TestClass)")
        val tokens = scanner.analyze()
        assertEquals(4, tokens.size)
        assertEquals(AspectKToken(AspectKTokenType.THIS, "this"), tokens[0])
        assertEquals(AspectKToken(AspectKTokenType.LEFT_PAREN, "("), tokens[1])
        assertEquals(AspectKToken(AspectKTokenType.POINTCUT_STRING_LITERAL, "com/example/TestClass"), tokens[2])
        assertEquals(AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"), tokens[3])
    }

    @Test
    fun testTarget() {
        val scanner = AspectKPointcutExpressionLexer("target(com/example/TestClass)")
        val tokens = scanner.analyze()
        assertEquals(4, tokens.size)
        assertEquals(AspectKToken(AspectKTokenType.TARGET, "target"), tokens[0])
        assertEquals(AspectKToken(AspectKTokenType.LEFT_PAREN, "("), tokens[1])
        assertEquals(AspectKToken(AspectKTokenType.POINTCUT_STRING_LITERAL, "com/example/TestClass"), tokens[2])
        assertEquals(AspectKToken(AspectKTokenType.RIGHT_PAREN, ")"), tokens[3])
    }
}
