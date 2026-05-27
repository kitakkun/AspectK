package com.github.kitakkun.aspectk.test

class Greeter {
    fun greet(name: String): String = "hello $name"
}

fun main() {
    println(Greeter().greet("world"))
}
