package com.github.kitakkun.aspectk.compiler

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object AspectKGeneratedRefs {
    val PACKAGE: FqName = FqName("com.github.kitakkun.aspectk.generated.refs")

    private const val PREFIX = "Ref_"

    fun classNameFor(aspectFqn: String): Name {
        return Name.identifier(PREFIX + aspectFqn.encodeToByteArray().joinToString("") {
            (it.toInt() and 0xFF).toString(16).padStart(2, '0')
        })
    }

    fun aspectFqnFor(className: String): String? {
        if (!className.startsWith(PREFIX)) return null
        val hex = className.substring(PREFIX.length)
        if (hex.isEmpty() || hex.length % 2 != 0) return null
        return runCatching {
            ByteArray(hex.length / 2) { i ->
                hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
            }.decodeToString()
        }.getOrNull()
    }
}

object AspectKGeneratedDeclarationKey : GeneratedDeclarationKey() {
    override fun toString(): String = "AspectKAspectRef"
}
