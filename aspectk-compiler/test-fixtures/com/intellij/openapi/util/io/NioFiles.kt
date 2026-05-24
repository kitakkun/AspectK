@file:JvmName("NioFiles")

package com.intellij.openapi.util.io

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFilePermission

/**
 * **TEST-ONLY shim**.
 *
 * `kotlin-compiler-internal-test-framework:1.9.22` calls
 * [com.intellij.openapi.util.io.NioFiles.deleteRecursively] (Path overload),
 * but the [NioFiles] class shipped inside `kotlin-compiler:1.9.22.jar` only provides
 * [createDirectories] and [setExecutable] — no `deleteRecursively`. This is an
 * upstream packaging mismatch in the published 1.9.22 artifacts.
 *
 * This file declares a same-FQN replacement with all three methods. Because the
 * test-fixtures classpath entry appears before the kotlin-compiler jar in Gradle's
 * test runtime classpath, the JVM resolves [NioFiles] to this class and the
 * original is shadowed. Drop this file once we move off Kotlin 1.9.22.
 */

@Throws(IOException::class)
public fun createDirectories(path: Path): Path = Files.createDirectories(path)

@Throws(IOException::class)
public fun setExecutable(path: Path) {
    val perms = Files.getPosixFilePermissions(path).toMutableSet()
    perms.add(PosixFilePermission.OWNER_EXECUTE)
    perms.add(PosixFilePermission.GROUP_EXECUTE)
    perms.add(PosixFilePermission.OTHERS_EXECUTE)
    Files.setPosixFilePermissions(path, perms)
}

@Throws(IOException::class)
public fun deleteRecursively(path: Path) {
    if (!Files.exists(path)) return
    Files.walkFileTree(
        path,
        object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.deleteIfExists(file)
                return FileVisitResult.CONTINUE
            }
            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                if (exc != null) throw exc
                Files.deleteIfExists(dir)
                return FileVisitResult.CONTINUE
            }
        },
    )
}
