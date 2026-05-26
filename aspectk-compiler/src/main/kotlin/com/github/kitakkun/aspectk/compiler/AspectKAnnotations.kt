package com.github.kitakkun.aspectk.compiler

import org.jetbrains.kotlin.javac.resolve.classId
import org.jetbrains.kotlin.name.Name

object AspectKAnnotations {
    private const val PKG = "com.github.kitakkun.aspectk.annotations"

    // advice / aspect markers (v0.x; kept for now, rewritten in Phase 3)
    val ASPECT_CLASS_ID = classId(PKG, "Aspect")
    val BEFORE_CLASS_ID = classId(PKG, "Before")
    val AFTER_CLASS_ID = classId(PKG, "After")
    val AROUND_CLASS_ID = classId(PKG, "Around")

    val ASPECT_FQ_NAME = ASPECT_CLASS_ID.asSingleFqName()
    val BEFORE_FQ_NAME = BEFORE_CLASS_ID.asSingleFqName()
    val AFTER_FQ_NAME = AFTER_CLASS_ID.asSingleFqName()
    val AROUND_FQ_NAME = AROUND_CLASS_ID.asSingleFqName()

    // v1 pointcut annotations
    val POINTCUT_CLASS_ID = classId(PKG, "Pointcut")
    val VISIBILITY_CLASS_ID = classId(PKG, "Visibility")
    val MODALITY_CLASS_ID = classId(PKG, "Modality")
    val MODIFIERS_CLASS_ID = classId(PKG, "Modifiers")
    val PACKAGE_CLASS_ID = classId(PKG, "Package")
    val CLASS_NAME_CLASS_ID = classId(PKG, "ClassName")
    val METHOD_NAME_CLASS_ID = classId(PKG, "MethodName")
    val ANNOTATED_CLASS_ID = classId(PKG, "Annotated")

    val POINTCUT_FQ_NAME = POINTCUT_CLASS_ID.asSingleFqName()

    val ADVICE_CLASS_IDS = setOf(BEFORE_CLASS_ID, AFTER_CLASS_ID, AROUND_CLASS_ID)

    // common annotation argument names
    val PATTERN = Name.identifier("pattern")
    val VALUES = Name.identifier("values")
}
