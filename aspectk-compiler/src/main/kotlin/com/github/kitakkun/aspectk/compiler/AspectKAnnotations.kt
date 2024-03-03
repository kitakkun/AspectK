package com.github.kitakkun.aspectk.compiler

import org.jetbrains.kotlin.javac.resolve.classId

object AspectKAnnotations {
    val ASPECT_CLASS_ID = classId("com.github.kitakkun.aspectk.annotations", "Aspect")
    val BEFORE_CLASS_ID = classId("com.github.kitakkun.aspectk.annotations", "Before")
    val AFTER_CLASS_ID = classId("com.github.kitakkun.aspectk.annotations", "After")
    val AROUND_CLASS_ID = classId("com.github.kitakkun.aspectk.annotations", "Around")
    val POINTCUT_CLASS_ID = classId("com.github.kitakkun.aspectk.annotations", "Pointcut")

    val ASPECT_FQ_NAME = ASPECT_CLASS_ID.asSingleFqName()
    val BEFORE_FQ_NAME = BEFORE_CLASS_ID.asSingleFqName()
    val AFTER_FQ_NAME = AFTER_CLASS_ID.asSingleFqName()
    val AROUND_FQ_NAME = AROUND_CLASS_ID.asSingleFqName()
    val POINTCUT_FQ_NAME = POINTCUT_CLASS_ID.asSingleFqName()
}
