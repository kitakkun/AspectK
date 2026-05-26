package com.github.kitakkun.aspectk.compiler.fir.checker

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class AspectKFirCheckerExtension(
    session: FirSession,
) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers = object : DeclarationCheckers() {
        override val simpleFunctionCheckers: Set<FirSimpleFunctionChecker> = setOf(
            AdviceScopeChecker,
            PointcutAnnotationChecker,
        )
    }
}
