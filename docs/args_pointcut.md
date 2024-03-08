# `args` pointcut

`args` pointcut is used to match the join points based on the arguments of the method.
Its rule is same with args expression inside `execution` pointcut.

```
args(argPattern)
```

## Basics

### Basic argument matching

| Expression          | Description                                   | Matches                                                               |
|---------------------|-----------------------------------------------|-----------------------------------------------------------------------|
| `args(String)`      | method with a single `String` argument        | `fun foo(text: String)`                                               |
| `args(Int, String)` | method with two arguments, `Int` and `String` | `fun bar(n: Int, text: String)`, `fun baz(n: Int, text: String)`, ... |

### Wildcard matching

| Expression | Description                     | Matches                                         |
|------------|---------------------------------|-------------------------------------------------|
| `args(*)`  | method with any single argument | `fun foo(text: String)`, `fun bar(n: Int)`, ... |

### Any arguments matching

| Expression         | Description                                                                 | Matches                                                       |
|--------------------|-----------------------------------------------------------------------------|---------------------------------------------------------------|
| `args(..)`         | method with zero or more any arguments (ignoring types)                     | `fun foo()`, `fun bar(n: Int, text: String)`, ...             |
| `args(String, ..)` | method with a `String` as the first argument and zero or more any arguments | `fun foo(text: String)`, `fun bar(text: String, n: Int)`, ... |
| `args(.., String)` | method with a `String` as the last argument and zero or more any arguments  | `fun foo(text: String)`, `fun bar(n: Int, text: String)`, ... |

### Vararg matching

| Expression             | Description                                         | Matches                                                                               |
|------------------------|-----------------------------------------------------|---------------------------------------------------------------------------------------|
| `args(String...)`      | method with a single `String` vararg argument       | `fun foo(vararg texts: String)`                                                       |
| `args(Int, String...)` | method with an `Int` and a `String` vararg argument | `fun bar(n: Int, vararg texts: String)`, `fun baz(n: Int, vararg texts: String)`, ... |

