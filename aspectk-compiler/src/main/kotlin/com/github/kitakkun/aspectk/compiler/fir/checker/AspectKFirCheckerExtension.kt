package com.github.kitakkun.aspectk.compiler.fir.checker

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

/**
 * Phase 0 stub.
 *
 * The v0.x extension installed three checkers (advice/pointcut scope, empty/invalid
 * pointcut expression, aspect class entries) that all parsed the soon-to-be-deleted
 * string DSL. Those rules are being rewritten from scratch in Phase 2 against the
 * new annotation + signature model. See `docs/v1-roadmap.md`.
 */
class AspectKFirCheckerExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers = object : DeclarationCheckers() {}
}
