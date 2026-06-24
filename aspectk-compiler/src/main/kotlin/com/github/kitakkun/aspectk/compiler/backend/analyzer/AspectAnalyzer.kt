package com.github.kitakkun.aspectk.compiler.backend.analyzer

import com.github.kitakkun.aspectk.compiler.AspectKAnnotations
import com.github.kitakkun.aspectk.compiler.AspectKGeneratedRefs
import com.github.kitakkun.aspectk.expression.expressionparser.PointcutExpressionParser
import com.github.kitakkun.aspectk.expression.lexer.AspectKLexer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.getStringConstArgument
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.simpleFunctions
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

class AspectAnalyzer private constructor() : IrElementVisitorVoid {
    companion object {
        fun analyze(
            moduleFragment: IrModuleFragment,
            pluginContext: IrPluginContext,
        ): List<AspectClass> {
            with(AspectAnalyzer()) {
                moduleFragment.acceptChildrenVoid(this)
                collectExternalAspects(pluginContext)
                return aspectClassList
            }
        }
    }

    private val mutableAspectClassList = mutableListOf<AspectClass>()
    private val seenClassIds = mutableSetOf<ClassId>()
    val aspectClassList: List<AspectClass> get() = mutableAspectClassList

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        if (declaration.hasAnnotation(AspectKAnnotations.ASPECT_FQ_NAME)) {
            registerAspect(declaration)
        }
        declaration.acceptChildrenVoid(this)
    }

    private fun registerAspect(declaration: IrClass) {
        val classId = declaration.classId ?: return
        if (!seenClassIds.add(classId)) return
        mutableAspectClassList.add(analyzeClass(declaration))
    }

    private fun collectExternalAspects(pluginContext: IrPluginContext) {
        val pkgDescriptor = pluginContext.moduleDescriptor.getPackage(AspectKGeneratedRefs.PACKAGE)
        val classifiers = pkgDescriptor.memberScope.getContributedDescriptors(
            kindFilter = DescriptorKindFilter.CLASSIFIERS,
            nameFilter = { true },
        ).filterIsInstance<ClassDescriptor>()

        for (refDescriptor in classifiers) {
            val simpleName = refDescriptor.name.asString()
            val aspectFqn = AspectKGeneratedRefs.aspectFqnFor(simpleName) ?: continue
            val aspectClassId = resolveAspectClassId(aspectFqn, pluginContext) ?: continue
            if (aspectClassId in seenClassIds) continue

            val aspectSymbol = pluginContext.referenceClass(aspectClassId) ?: continue
            val aspectIrClass = aspectSymbol.owner
            if (!aspectIrClass.hasAnnotation(AspectKAnnotations.ASPECT_FQ_NAME)) continue

            registerAspect(aspectIrClass)
        }
    }

    private fun resolveAspectClassId(fqn: String, pluginContext: IrPluginContext): ClassId? {
        // Try top-level resolution first (covers the common case). For nested aspects, callers
        // can still register them by ensuring the marker survives — but they need an explicit
        // nested-aware ClassId lookup which is not implemented here yet.
        if (fqn.isBlank()) return null
        val topLevel = runCatching { ClassId.topLevel(FqName(fqn)) }.getOrNull() ?: return null
        if (pluginContext.referenceClass(topLevel) != null) return topLevel

        // Fallback: walk the FQN from the right, treating the right-most segment as a nested
        // class name. Only one level of nesting is attempted.
        val lastDot = fqn.lastIndexOf('.')
        if (lastDot <= 0) return null
        val packageFqName = FqName(fqn.substring(0, fqn.lastIndexOf('.', lastDot - 1).let { if (it == -1) lastDot else it }))
        val relative = fqn.removePrefix(packageFqName.asString() + ".")
        val nested = ClassId(packageFqName, FqName(relative), false)
        return if (pluginContext.referenceClass(nested) != null) nested else null
    }

    private fun analyzeClass(declaration: IrClass): AspectClass {
        val classId = declaration.classId ?: error("ClassId is null")

        val pointcuts = declaration.simpleFunctions()
            .filter { it.hasAnnotation(AspectKAnnotations.POINTCUT_FQ_NAME) }
            .map { it.name.asString() to it.getAnnotation(AspectKAnnotations.POINTCUT_FQ_NAME)?.getStringConstArgument(0) }
            .mapNotNull { (name, expression) ->
                val pointcutExpression = expression?.let {
                    val tokens = AspectKLexer(it).analyze()
                    PointcutExpressionParser(tokens).expression()
                }
                if (pointcutExpression != null) {
                    Pointcut(name, pointcutExpression)
                } else {
                    null
                }
            }

        val beforeAdvices = declaration.simpleFunctions()
            .filter { it.hasAnnotation(AspectKAnnotations.BEFORE_FQ_NAME) }
            .associateWith { it.getAnnotation(AspectKAnnotations.BEFORE_FQ_NAME)?.getStringConstArgument(0) }
            .mapNotNull { (declaration, expression) ->
                val pointcutExpression = expression?.let {
                    val tokens = AspectKLexer(it).analyze()
                    PointcutExpressionParser(tokens).expression()
                }
                if (pointcutExpression != null) {
                    Advice(AdviceType.BEFORE, pointcutExpression, declaration)
                } else {
                    null
                }
            }

        val afterAdvices = declaration.simpleFunctions()
            .filter { it.hasAnnotation(AspectKAnnotations.AFTER_FQ_NAME) }
            .associateWith { it.getAnnotation(AspectKAnnotations.AFTER_FQ_NAME)?.getStringConstArgument(0) }
            .mapNotNull { (declaration, expression) ->
                val pointcutExpression = expression?.let {
                    val tokens = AspectKLexer(it).analyze()
                    PointcutExpressionParser(tokens).expression()
                }
                if (pointcutExpression != null) {
                    Advice(AdviceType.AFTER, pointcutExpression, declaration)
                } else {
                    null
                }
            }

        val aroundAdvices = declaration.simpleFunctions()
            .filter { it.hasAnnotation(AspectKAnnotations.AROUND_FQ_NAME) }
            .associateWith { it.getAnnotation(AspectKAnnotations.AROUND_FQ_NAME)?.getStringConstArgument(0) }
            .mapNotNull { (declaration, expression) ->
                val pointcutExpression = expression?.let {
                    val tokens = AspectKLexer(it).analyze()
                    PointcutExpressionParser(tokens).expression()
                }
                if (pointcutExpression != null) {
                    Advice(AdviceType.AROUND, pointcutExpression, declaration)
                } else {
                    null
                }
            }

        val advices = beforeAdvices + afterAdvices + aroundAdvices

        return AspectClass(
            classId = classId,
            pointcuts = pointcuts,
            advices = advices,
            classDeclaration = declaration,
        )
    }
}
