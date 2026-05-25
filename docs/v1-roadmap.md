# AspectK v1 Design and Roadmap

This document records design decisions and the implementation plan for AspectK v1.0. It is the source of truth that survives any single working session.

---

## Core idea

> **Type information lives in the Kotlin type system. Everything else is composable annotations. The string DSL is dropped.**

The signature of an advice function expresses type-level constraints against the target. A set of annotations on the advice function expresses everything else — visibility, modifiers, name patterns, annotation markers, logical composition. Together they form the pointcut.

### Example

```kotlin
@Aspect
class TxAspect {
    @Around
    @Visibility(PUBLIC)
    @MethodName(startsWith = "save")
    @Annotated(Transactional::class)
    context(jp: ProceedingJoinPoint)
    fun BaseRepo.tx(user: User): Unit {
        log("starting tx for $user on $this")
        proceed()
    }
}
```

This advice matches a target method when:

- it is `public`,
- its name starts with `save`,
- it carries the `@Transactional` annotation,
- its dispatch receiver is a subtype of `BaseRepo`,
- its first value parameter is of type `User`,
- and its return type is `Unit`.

---

## Signature vs annotation: who owns what

### Kotlin language elements → target constraints

| Element | Constraint on target | Access inside the advice body |
|---|---|---|
| `context(jp: ProceedingJoinPoint)` parameter | Supplies the join point and `proceed()` | `jp`, `proceed()` (via context) |
| extension receiver (`fun T.advice()`) | target's **dispatch receiver** is a subtype of `T` | `this` |
| value parameters | target's **value arguments** match by position, type and name | parameter name |
| return type | target's **return type** is assignable to the declared type (covariant) | the advice's return value |

### Annotations → name, modifier, marker constraints

| Annotation | Role |
|---|---|
| `@Before` / `@After` / `@Around` | Advice kind |
| `@Visibility(PUBLIC, INTERNAL, ...)` | Visibility (array values = OR within the dimension) |
| `@Modality(FINAL, OPEN, ABSTRACT, SEALED)` | Modality |
| `@Modifiers(SUSPEND, INLINE, INFIX, OPERATOR, ...)` | Other modifiers |
| `@Package(value=, prefix=, recursive=)` | Package (exact, prefix, recursive) |
| `@ClassName(value=, startsWith=, endsWith=, contains=, regex=)` | Class name pattern |
| `@MethodName(value=, startsWith=, endsWith=, contains=, regex=)` | Method name pattern |
| `@Annotated(vararg KClass<out Annotation>)` | Annotation-marker targeting |
| `@Any([...])`, `@Not(...)` | Logical composition |
| `@Pointcut` (meta-annotation marker) | Declares a reusable pointcut |

### Reuse via meta-annotation

```kotlin
@Pointcut
@Visibility(PUBLIC)
@Package(prefix = "com.example.repo", recursive = true)
@Annotated(Transactional::class)
annotation class PublicTransactionalRepoCall

@Aspect
class TxAspect {
    @Around
    @PublicTransactionalRepoCall              // one line, multi-dimensional constraint
    @MethodName(startsWith = "save")          // further narrowing
    context(jp: ProceedingJoinPoint)
    fun BaseRepo.tx(user: User): Unit { ... }
}
```

---

## Locked design decisions

| ID | Decision |
|---|---|
| **A** | Upgrade to the **latest Kotlin**. Staying on 1.9.22 would keep `context` parameters in `-Xcontext-receivers` experimental territory and force users to opt in. |
| **B** | Open PRs at the time of the pivot: #9 (named pointcut FIR resolution) is **closed** — it disappears with the DSL. #11 and #12 are **merged** first, then v1 work continues on a fresh branch. |
| **C** | v1.0 is a **hard break**. No deprecation window for the old string DSL — pre-1.0 makes this acceptable, and carrying both surfaces is worse than a clean reset. |
| **D** | The join point is supplied via an **explicit context parameter** (`context(jp: ProceedingJoinPoint)`). No magic injection. |
| **E** | Constraining the target's extension receiver is **deferred to v1.1+** (will likely come back as `@ExtensionTarget` or similar). |
| **F** | Constraining the target's context receivers is also **deferred to v1.1+**. |
| **G** | Annotation names are kept **short** (`@Visibility`, not `@AspectKVisibility`). They live under the `com.github.kitakkun.aspectk.annotations` package, and IDE autocomplete handles disambiguation. |
| **H** | Wildcard support uses **discrete params** (`startsWith` / `endsWith` / `contains` / `value`) as the primary form, with a `regex` escape hatch for the few cases that need it. |
| **I** | Return-type matching is **covariant**: the target's return type must be assignable to the advice's declared return type. |
| **J** | Aspect execution order is **undefined** in v1.0 (IR visit order). Order control (`@AspectOrder` or similar) can be added later if a real need surfaces. |

---

## What goes, what stays

### Removed

- The entire `aspectk-expression` module (lexer / parser / matcher, several thousand lines).
- The string argument on `@Pointcut("...")` and the `named()` / `&&` / `||` / `execution()` / `args()` syntax it serves.
- The current `AdviceSignatureChecker` implementation (rewritten — the new one drives off the signature, not annotation arguments).
- FIR-side DSL syntax checking and named-pointcut resolution (the latter is what #9 / T3 was about).

### Kept

- `@Aspect`, `@Before`, `@After`, `@Around` annotations themselves.
- The runtime `JoinPoint` / `JoinPointArgument` / `ProceedingJoinPoint` types (field shape).
- The IR transformer's `@Around` lambda-lift mechanism introduced by T5.
- The cross-module aspect discovery via marker classes (T7 / PR #12).
- The Pattern B test infrastructure landed in #13.

---

## Implementation roadmap

### Phase 0 — Kotlin upgrade (in progress)

- Branch `feat/upgrade-to-kotlin-latest`.
- Bump Kotlin and all related coordinates (KSP, BuildKonfig, Gradle tasks).
- Get every module compiling against the new compiler API.
- Get the existing test suite (Pattern B + expression tests) green.
- Phase 0 must complete before any v1 design work starts.

### Phase 1 — New annotation definitions

Add to `aspectk-annotations/`:

- `Visibility.kt` — `enum class Visibility { PUBLIC, INTERNAL, PROTECTED, PRIVATE }` and `@Visibility(vararg)`.
- `Modality.kt` — `enum class Modality { FINAL, OPEN, ABSTRACT, SEALED }` and `@Modality(vararg)`.
- `Modifiers.kt` — `enum class Modifier { SUSPEND, INLINE, INFIX, OPERATOR, TAILREC, EXTERNAL }` and `@Modifiers(vararg)`.
- `Package.kt` — `@Package(value=, prefix=, recursive=)`.
- `ClassName.kt` — `@ClassName(value=, startsWith=, endsWith=, contains=, regex=)`.
- `MethodName.kt` — `@MethodName(value=, startsWith=, endsWith=, contains=, regex=)`.
- `Annotated.kt` — `@Annotated(vararg KClass<out Annotation>)`.
- `Pointcut.kt` — redefined as a meta-annotation marker.
- `Any.kt`, `Not.kt` — logical composition.

### Phase 2 — FIR checker on the new model

- Validate that the advice signature is consistent with the surrounding annotations.
- Detect conflicting annotations (e.g. `@Visibility(PUBLIC)` and `@Visibility(PRIVATE)` on the same advice).
- Expand `@Pointcut`-marked meta-annotations transitively.
- Emit a warning when a pointcut matches zero declarations.

### Phase 3 — IR transformer rewrite

- Drop the string DSL parser entirely.
- Build a pointcut filter from annotation values plus signature types.
- Match it against the IR module fragment.
- Invoke the advice with the join point as a context argument and bind the signature's value parameters from the call site.

### Phase 4 — Remove `aspectk-expression`

- Delete the module.
- Drop `include(":aspectk-expression")` from `settings.gradle.kts`.
- Remove cross-module dependencies on it.

### Phase 5 — Logical composition (`@Any`, `@Not`)

- Resolve nested annotation structures.
- Lint-warn excessive nesting depth (≥3 levels).

### Phase 6 — Meta-annotation expansion

- An annotation carrying `@Pointcut` together with other pointcut annotations becomes a reusable pointcut.
- Expansion is recursive (meta-annotations can reference further meta-annotations).
- Detect cycles.

### Phase 7 — Documentation and samples

- Rewrite the README around the new model.
- Provide a migration guide mapping old DSL constructs to the new annotations.
- Rewrite the `test/` sample module.

### Phase 8 — Extension / context receiver targeting (v1.1)

- `@ExtensionTarget(A::class)` or an equivalent way to constrain the target's extension receiver.
- A way to bind the target's context receivers into the advice.
- Driven by user feedback after v1.0.

---

## Suggested PR sequence (after Phase 0)

| PR | Contents |
|---|---|
| PR-1 | Phase 1 (annotation type definitions only, plugin not yet wired up). |
| PR-2 | First half of Phase 3 — annotation-based pointcut evaluation in the IR transformer, running alongside the existing string DSL. |
| PR-3 | Phase 2 (FIR checker rewrite) plus the second half of Phase 3 (signature-driven type binding). |
| PR-4 | Phase 4 — delete `aspectk-expression` and excise the old DSL paths. |
| PR-5 | Phase 5 (logical composition). |
| PR-6 | Phase 6 (meta-annotation expansion). |
| PR-7 | Phase 7 (docs and sample rewrite). |

---

## Open questions to revisit after Phase 0

- The exact stability tier of Kotlin's context parameters / context receivers on the chosen Kotlin version, and the final shape of the join-point injection mechanism it allows.
- How to handle generic erasure when the target's return type or parameter type involves type parameters (`List<T>`, `Map<K, V>`, etc.).
- Whether `suspend` and `inline` targets should be supported, refused with a diagnostic, or simply skipped.

These are revisited once Phase 0 stabilises on a concrete Kotlin version.

---

Last updated: 2026-05-25.
