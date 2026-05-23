package com.github.kitakkun.aspectk.compiler.fir

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import com.github.kitakkun.aspectk.compiler.AspectKGeneratedDeclarationKey
import com.github.kitakkun.aspectk.compiler.AspectKGeneratedRefs
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createTopLevelClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class AspectReferenceGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val aspectDeclarationPredicate: DeclarationPredicate = DeclarationPredicate.create {
        annotated(AspectKAnnotations.ASPECT_FQ_NAME)
    }

    private val aspectLookupPredicate: LookupPredicate = LookupPredicate.create {
        annotated(AspectKAnnotations.ASPECT_FQ_NAME)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(aspectDeclarationPredicate)
    }

    private fun aspectSymbols(): List<FirRegularClassSymbol> {
        return session.predicateBasedProvider.getSymbolsByPredicate(aspectLookupPredicate)
            .filterIsInstance<FirRegularClassSymbol>()
    }

override fun getTopLevelClassIds(): Set<ClassId> {
        return aspectSymbols().mapTo(mutableSetOf()) { aspectClassIdToRefClassId(it.classId) }
    }

    override fun hasPackage(packageFqName: FqName): Boolean {
        return packageFqName == AspectKGeneratedRefs.PACKAGE
    }

override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId.packageFqName != AspectKGeneratedRefs.PACKAGE) return null
        val expectedSimpleName = classId.shortClassName.asString()
        val originalFqn = AspectKGeneratedRefs.aspectFqnFor(expectedSimpleName) ?: return null
        val matched = aspectSymbols().firstOrNull { it.classId.asFqNameString() == originalFqn } ?: return null
        if (aspectClassIdToRefClassId(matched.classId) != classId) return null
        return createTopLevelClass(classId = classId, key = AspectKGeneratedDeclarationKey).symbol
    }

    private fun aspectClassIdToRefClassId(aspectClassId: ClassId): ClassId {
        val refName = AspectKGeneratedRefs.classNameFor(aspectClassId.asFqNameString())
        return ClassId(AspectKGeneratedRefs.PACKAGE, refName)
    }
}
