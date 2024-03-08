# Advices

An `advice` is an action taken by an aspect at a particular join point.
Currently, AspectK supports following types of advices:

- `@Before`
- `@After`

## `@Before` advice

`@Before` advice is executed before the join point.

#### Aspect class:

```kotlin
@Aspect
class MyAspect {
    @Before("execution(public MyClass.myMethod(String))")
    fun beforeMyMethod(joinPoint: JoinPoint) {
        // you can access call information via `joinPoint`
        println("args: ${joinPoint.args}")
    }
}
```

#### Target class:

```kotlin
class MyClass {
    fun myMethod(name: String) {
        println("Hello, AspectK!")
    }
}
```

#### Compiled result:

```kotlin
class MyClass {
    fun myMethod(name: String) {
        val aspect = MyAspect()
        val joinPoint = JoinPoint(...)
        aspect.beforeMyMethod(joinPoint)
        println("Hello, AspectK!")
    }
}
```

## `@After` advice

`@After` advice is executed after the join point.

#### Aspect class:

```kotlin
@Aspect
class MyAspect {
    @After("execution(public MyClass.myMethod(String))")
    fun afterMyMethod(joinPoint: JoinPoint) {
        // you can access call information via `joinPoint`
        println("args: ${joinPoint.args}")
    }
}
```

#### Target class:

```kotlin
class MyClass {
    fun myMethod(name: String) {
        println("Hello, AspectK!")
    }
}
```

#### Compiled result:

```kotlin
class MyClass {
    fun myMethod(name: String) {
        println("Hello, AspectK!")
        val aspect = MyAspect()
        val joinPoint = JoinPoint(...)
        aspect.afterMyMethod(joinPoint)
    }
}
```
