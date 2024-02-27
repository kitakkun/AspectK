# `execution` pointcut

`execution` is most commonly used pointcut expression.
If you learn `execution` pointcut, it means that you can do most of the things with AspectK.

`execution` pointcut follows the following pattern:

```
execution(modifiers? package?/class?.function(args?) : returnType? (extends package?/class)?)
```

`?` means that its part can be omitted in a specific case.

Also, you can use `*` for wildcard matching. It's available for `package`, `class`, `function`, `args`, `returnType`, and `extension-package` parts respectively.

It looks like a bit complex, but it's not so difficult and flexible enough!

## Basics

### Basic top-level function example

`execution(com/example/foo())`

```kotlin
package com.example

fun foo() {}
```

### Basic class method example

`execution(com/example/A.foo())`

```kotlin
package com.example

class A {
    fun foo() {}
}
```

### Nested class example

`execution(com/example/A.B.foo())`

```kotlin
package com.example

class A {
    class B {
        fun foo() {}
    }
}
```

### Nested top-level function example

`execution(com/example/parent().foo())`

```kotlin
package com.example

fun parent() {
    fun foo() {}
}
```

### Extension function example

`execution(com/example/extension/foo() extends com/example/A)`

```kotlin
package com.example

class A
```

```kotlin
package com.example.extension

fun A.foo() {}
```

## Wildcard examples

You can use '`*`' to match any pattern (0 or more characters).
It's available in `package`, `class`, `function`, `args`, `returnType`, and `extension-package` parts.

### Package

- `com/example` matches all declarations in `com.example` package
- `com/example*` matches all declarations in package whose name starts with `com.example` (ex: `com.example1`)
- `com/example/*` matches all declarations in direct subpackages of `com.example` package (ex: `com.example.a`)
- `com/example/..` matches all declarations in `com.example` package and its subpackages

### Class

- `Hoge` matches all classes named `Hoge`
- `Hoge*` matches all classes whose name starts with `Hoge`
- `*Hoge` matches all classes whose name ends with `Hoge`

### Function

- `foo()` matches all functions named `foo`
- `foo*()` matches all functions whose name starts with `foo`
- `*foo()` matches all functions whose name ends with `foo`

## Abbreviation

Abbreviation rule is based on Kotlin's default visibility modifier and return type.

### Modifiers

If modifiers are omitted, AspectK assumes that it is `public` by default.

```kotlin
package com.example

fun foo() {}
```

So, two expressions below are equivalent.
```
execution(public com/example/foo())
execution(com/example/foo())
```

### Return type

If return type is omitted, AspectK assumes that it is `Unit` by default.

```kotlin
package com.example

fun foo() {}
```

So, two expressions below are equivalent.
```
execution(com/example/foo() : Unit)
execution(com/example/foo())
```

### Package

If declaration is top-level, and does not have package, you can omit package part.
```kotlin
fun foo() {}

class A {
    fun foo() {}
}
```

So, you can simply write like this.
```
execution(foo())
execution(A.foo())
```

## Modifiers

You can specify the modifier for the function.

- visibility: `public`, `protected`, `private`, `internal`
- other: `final`, `open`, `abstract`, `override`, `inline`, `infix`, `operator`, `suspend`, `actual`, `expect`, `tailrec` ...

Applying Multiple modifiers is allowed. example:

```
execution(public override com/example/A.foo())
```

## Argument Matcher

Argument matcher is expressed in a same rule as args pointcut expression.
See [args pointcut](args_pointcut.md) for more details.

basic-examples:
- `()` function has no parameter
- `(Int)` function has one parameter of type `Int`
- `(Int, String)` function has two parameters of type `Int` and `String`

advanced-examples:
- `(..)` function has any number of parameters
- `(String...)` function has single vararg parameter of type `String`
- `(.., String...)` function has any number of parameters and vararg parameter of type `String` as the last parameter

## Return type

basic-example:

- `: Int` function returns `Int`
- `: Unit` function returns `Unit`
- `: *` function returns any type
