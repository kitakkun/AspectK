// FILE: PackageMatching.kt

package com.example.app

import com.github.kitakkun.aspectk.annotations.Aspect
import com.github.kitakkun.aspectk.annotations.Before
import com.github.kitakkun.aspectk.annotations.MethodName
import com.github.kitakkun.aspectk.annotations.Package

var hits = 0

class Owner {
    fun touch() {}
}

@Aspect
class PackageAspect {
    @Before
    @Package("com.example.**")
    @MethodName("touch")
    fun beforeTouch() {
        hits++
    }
}

fun box(): String {
    Owner().touch()
    if (hits != 1) return "FAIL: hits=$hits (expected 1)"
    return "OK"
}
