package com.github.kitakkun.aspectk.expression.model

data class ClassSignature(
    val packageName: String,
    val className: String,
    val superTypes: List<ClassSignature> = emptyList(),
)
