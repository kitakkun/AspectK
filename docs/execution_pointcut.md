# `execution` pointcut

`execution` is most commonly used pointcut expression.
If you learn `execution` pointcut, it means that you can do most of the things with AspectK.

`execution` pointcut follows the following pattern:

```
execution(modifiers? scopeMatcher?::functionMatcher(argMatcher) : returnTypeMatcher?)
```

`?` means that its part can be omitted in a specific case.

## Basics

### Basic top-level function example
`execution(public com/example::foo() : Unit)`
```kotlin
package com.example

fun foo() {}
```

### Basic class method example
`execution(public com/example/A::foo() : Unit)`
```kotlin
package com.example

class A {
    fun foo() {}
}
```

### Nested class example
`execution(public com/example/A/B::foo() : Unit)`
```kotlin
package com.example

class A {
    class B {
        fun foo() {}
    }
}
```

### Nested top-level function example

`execution(public com/example::parent.foo() : Unit)`
```kotlin
package com.example

fun parent() {
    fun foo() {}
}
```


### Extension function example
`execution(public com/example/extension::com/example/A#foo() : Unit)`
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

- partial matching of function. (e.g. `foo*()` matches all functions whose name starts with `foo`)
- matching any class in package. (e.g. `com/example/*` matches all classes in `com.example` package)
- matching any class under package. (e.g. `com/example/**` matches all classes in `com.example` package and its subpackages)
- matching any class under package with specific name. (e.g. `com/example/**/A` matches all classes in `com.example` package and its subpackages that its name is `A`)
- matching any class that its name starts with `A`. (e.g. `com/example/A*` matches all classes in `com.example` package that its name starts with `A`) 
- matching any class that its name ends with `A`. (e.g. `com/example/*A` matches all classes in `com.example` package that its name ends with `A`)

## Abbreviation

### Visibility modifier and Return type
Let's think about the following function.

```kotlin
package com.example

fun foo() {}
```

We introduced the following matching expression previously.
However, its somewhat verbose.

```
execution(public com/example::foo() : Unit)
```

The default visibility modifier is `public`, so you can omit `public` part.
```
execution(com/example::foo() : Unit)
```

The default return type is `Unit`, so you can omit `: Unit` part as well.

```
execution(com/example::foo())
```

Now it's much simpler, isn't it?

### Scope

Though this is a not common case, you can also omit scope part in a specific case.
See the following example.

```kotlin
fun foo() {}

class A {
    fun foo() {}
}
```

These are defined directly under kotlin directory, and have no package.

In this case, you can omit package part (you can omit `::` part as well).

```
execution(foo())
```

However, if parent class is exist, you cannot omit `::` part.

```
execution(A::foo())
```

## Modifiers

You can specify the modifier for the function. Multiple modifiers is allowed.

- visibility: `public`, `protected`, `private`, `internal`
- other: `final`, `open`, `abstract`, `override`, `inline`, `infix`, `operator`, `suspend`, `actual`, `expect`, `tailrec` ...

example:
```
execution(public override com/example/A::foo() : Unit)
```

## Scope

Scope part is used to determine the scope of the function.
A function may be defined as a class method, a top-level function, or an extension function.

> [!NOTE]
> Scope part is responsible for understanding if it is a class method or a top-level function.
> Recognizing extension function is a job for function matcher.

basic-examples:
- top-level function: `com/example`
- method of a class: `com/example/A`
- method of a nested class: `com/example/A.B`

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
