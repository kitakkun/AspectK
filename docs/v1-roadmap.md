# AspectK v1 Design and Roadmap

This document records design decisions and the implementation plan for AspectK v1.0. It is the source of truth that survives any single working session.

---

## Core idea

> **The string DSL is dropped. Pointcuts are composed from annotations, and bindings (receivers / value arguments / context parameters) are extracted via explicit binding annotations on the advice function's parameters.**

A set of *matching* annotations on the advice function describes which targets it applies to (visibility, modifiers, name patterns, annotation markers, logical composition). A set of *binding* annotations on the advice function's parameters describes what the advice extracts from each matched call site. Together they form the pointcut.

### Example

```kotlin
@Aspect
class TxAspect {
    @Around
    @Visibility(Visibility.Kind.PUBLIC)
    @MethodName("save*")
    @Annotated(Transactional::class)
    fun tx(
        @DispatchReceiver repo: BaseRepo,
        @ValueArgument(0) user: User,
    ): Unit = interceptableAdvice {
        log("starting tx for $user on $repo")
        proceed()
    }
}
```

This advice matches a target method when:

- it is `public`,
- its name starts with `save`,
- it carries the `@Transactional` annotation,
- its dispatch receiver is a subtype of `BaseRepo` (carried in the `@DispatchReceiver`-bound parameter's type),
- its first value parameter is assignable to `User`,
- and its return type is `Unit`.

The advice's function name (`tx`) is irrelevant to matching — it is just an identifier.

---

## Pointcut composition

### Matching annotations (where to match)

Stacked on the advice function declaration.

| Annotation | Role |
|---|---|
| `@Before` / `@After` / `@Around` | Advice kind |
| `@Visibility(vararg Visibility.Kind)` | Visibility (multiple values = OR within the dimension) |
| `@Modality(vararg Modality.Kind)` | Modality (`FINAL` / `OPEN` / `ABSTRACT` / `SEALED`) |
| `@Modifiers(vararg Modifier.Kind)` | Other modifiers (`SUSPEND` / `INLINE` / `INFIX` / `OPERATOR` / `TAILREC` / `EXTERNAL`) |
| `@Package(pattern: String)` | Package name pattern. `*` matches one segment, `**` matches zero or more segments. |
| `@ClassName(pattern: String)` | Enclosing class simple-name pattern. `*` wildcard. |
| `@MethodName(pattern: String)` | Method simple-name pattern. `*` wildcard. |
| `@Annotated(vararg KClass<out Annotation>)` | Target carries one of the given annotations |
| `@Any([...])` / `@Not(...)` | Logical composition (Phase 5) |
| `@Pointcut` (meta-annotation marker) | Declares a reusable pointcut (Phase 6) |

`@ClassName` lives alongside `@DispatchReceiver` because they serve different purposes:

| Need | Use |
|---|---|
| Exact class (with type information) | `@DispatchReceiver T: ConcreteClass` |
| Subtype matching across an inheritance hierarchy | `@DispatchReceiver T: BaseClass` (covariant) |
| Name-pattern matching across unrelated types | `@ClassName("*Repository")` |
| Constrain to "any class member" | `@DispatchReceiver _: Any` or `@ClassName("*")` |

For an exact-class match, prefer `@DispatchReceiver` (more type-safe). Use `@ClassName` when name conventions cut across types that have no shared supertype.

### Binding annotations (what to extract)

Stacked on the advice function's parameters.

| Annotation | Binds to |
|---|---|
| `@DispatchReceiver` | Target call site's dispatch receiver. Parameter type acts as a covariant subtype constraint on matching. |
| `@ExtensionReceiver` | Target call site's extension receiver (if any). |
| `@ContextParameter(index: Int = -1, name: String = "")` | One of the target's context parameters (by index or by name). |
| `@ValueArgument(index: Int = -1, name: String = "")` | One of the target's value arguments (by index or by name). |

For `@ValueArgument` and `@ContextParameter`, the user must specify exactly one of `index` / `name`. A FIR checker rejects the empty / both-set forms.

### Reuse via meta-annotation

```kotlin
@Pointcut
@Visibility(Visibility.Kind.PUBLIC)
@Package(pattern = "com.example.repo.**")
@Annotated(Transactional::class)
annotation class PublicTransactionalRepoCall

@Aspect
class TxAspect {
    @Around
    @PublicTransactionalRepoCall          // one line, multi-dimensional constraint
    @MethodName("save*")                  // further narrowing
    fun tx(
        @DispatchReceiver repo: BaseRepo,
        @ValueArgument(0) user: User,
    ): Unit = interceptableAdvice {
        proceed()
    }
}
```

---

## `@Around` advice

`@Before` and `@After` advice **observe** the target call but cannot prevent, skip, or modify it. Only `@Around` carries interception semantics — to skip the target, do not call `proceed()`; to replace the return value, return a different one from the advice.

### Body shape

`@Around` advice opts into proceed semantics by wrapping its body in the `interceptableAdvice { ... }` DSL builder. The lambda receiver scope exposes `proceed()` and the `replace*` family of override functions.

```kotlin
@Aspect
class GreetingTracer {
    @Around
    @ClassName("Greeter")
    @MethodName("greet")
    fun aroundGreet(
        @ValueArgument(0) name: String,
    ): String = interceptableAdvice {
        replaceValueArgument(0, name.uppercase())
        proceed()
    }
}
```

If the advice body does not contain exactly one top-level `interceptableAdvice { ... }` invocation, a FIR checker reports an error.

### Overriding bindings before `proceed()`

The `interceptableAdvice` scope exposes explicit override functions for each kind of binding:

| Function | Role |
|---|---|
| `replaceValueArgument(index: Int, value: Any?)` | Replace the N-th value argument |
| `replaceValueArgument(name: String, value: Any?)` | Replace the named value argument |
| `replaceDispatchReceiver(value: Any?)` | Replace the dispatch receiver |
| `replaceExtensionReceiver(value: Any?)` | Replace the extension receiver |
| `replaceContextArgument(index: Int, value: Any?)` | Replace the N-th context argument |
| `replaceContextArgument(name: String, value: Any?)` | Replace the named context argument |
| `proceed()` | Call the target with the current overrides applied |

A single advice may call `proceed()` multiple times; each call applies the override state currently set.

### Type safety: bind-first rule

**To override a binding, the advice must first declare a binding parameter for the same slot.** This is enforced by a FIR checker:

- `replaceValueArgument(0, "X")` is allowed only if the advice has a `@ValueArgument(0) ...` parameter.
- The override value's type must be assignable to the binding parameter's declared type.

Because the check operates on the local binding type, it works the same for concrete and wildcard pattern matches — no FIR-time target resolution is needed.

If the user wants to override without reading the original, they declare a binding parameter and simply ignore it (Kotlin's `_` rename or `@Suppress("UNUSED_PARAMETER")`).

### Fallback runtime API

The synthesized typed `replace*` shapes are visible only when the AspectK compiler plugin is on the classpath. For environments where it is not (rare; the plugin is required at compile time), a runtime API exists with `Any?`-typed value parameters at the same names. Both forms lower to the same IR.

---

## IDE feedback

To make woven call sites visible in IntelliJ without a custom IDE plugin, the FIR side emits an **INFO**-severity diagnostic at every call site where any advice's pointcut matches.

```
greeter.greet("world")
        ~~~~~  INFO: intercepted by GreetingTracer.beforeGreet (@Before)
```

This works because Kotlin's IDE integration surfaces compiler-plugin diagnostics in the editor automatically.

Implementation requires running pointcut matching at FIR (currently it runs only at IR). The Phase 3 follow-up unifies matching so that the FIR side computes the match set and the IR side consumes it — no double-execution.

---

## Locked design decisions

| ID | Decision |
|---|---|
| **A** | Upgrade to the **latest Kotlin** (2.3.21). Staying on 1.9.22 would keep context parameters experimental and force users to opt in. |
| **B** | Open PRs at the time of the pivot: #9 (named pointcut FIR resolution) is **closed** — it disappears with the DSL. #11 and #12 are **merged** first, then v1 work continues on a fresh branch. |
| **C** | v1.0 is a **hard break**. No deprecation window for the old string DSL — pre-1.0 makes this acceptable, and carrying both surfaces is worse than a clean reset. |
| **D** | The advice function name is an **identifier only**; it does not participate in matching. |
| **E** | Bindings (receivers, value args, context args) are extracted via **explicit binding annotations on parameters** (`@DispatchReceiver`, `@ExtensionReceiver`, `@ContextParameter`, `@ValueArgument`) rather than via Kotlin language elements (extension receivers, context parameters, positional value args). The annotation form is more explicit, supports partial binding, and composes cleanly with the `replace*` override API. |
| **F** | Annotation names are kept **short** under `com.github.kitakkun.aspectk.annotations` (`@DispatchReceiver`, not `@AspectKDispatchReceiver`). IDE autocomplete handles disambiguation. |
| **G** | `@Package` / `@ClassName` / `@MethodName` take a **single `pattern: String` argument** with `*` (single segment / wildcard) and `**` (multi-segment, `@Package` only). Earlier multi-parameter forms (`startsWith` / `endsWith` / `contains` / `regex`) are dropped. |
| **H** | `@Around` advice bodies must contain exactly one top-level `interceptableAdvice { ... }` invocation. Bindings can be overridden inside the scope via `replace*` functions, and `proceed()` calls the target with the current override state. |
| **I** | To override a binding, the advice must first **bind it**. FIR enforces this and checks the override value's type against the binding parameter's declared type. No target signature resolution is needed at FIR time. |
| **J** | Return-type matching is **covariant**: the target's return type must be assignable to the advice's declared return type. |
| **K** | IDE feedback uses **FIR-emitted INFO diagnostics** at woven call sites. No JetBrains IDE plugin is required. Pointcut matching is computed once at FIR and consumed at IR. |
| **L** | Aspect execution order is **undefined** in v1.0 (IR visit order). Order control (`@AspectOrder` or similar) can be added later if a real need surfaces. |
| **M** | Constraining the target's *extension* receiver and *context* receivers via dedicated matching annotations is **deferred to v1.1+**. (Binding-via-annotation works in v1.0 for both, but a name-pattern equivalent of `@ClassName` for these slots is not in scope.) |

---

## What goes, what stays

### Removed

- The entire `aspectk-expression` module (lexer / parser / matcher, several thousand lines).
- The string argument on `@Pointcut("...")` and the `named()` / `&&` / `||` / `execution()` / `args()` syntax it serves.
- The current `AdviceSignatureChecker` implementation (rewritten — the new one validates the binding annotations).
- FIR-side DSL syntax checking and named-pointcut resolution.
- The earlier multi-parameter form of `@Package` / `@ClassName` / `@MethodName` (`startsWith` / `endsWith` / `contains` / `regex`).
- Signature-driven binding via Kotlin language elements (extension receivers, positional value params) — replaced by explicit binding annotations.

### Kept

- `@Aspect`, `@Before`, `@After`, `@Around` annotations themselves.
- The runtime `JoinPoint` / `ProceedingJoinPoint` types (field shape, partial — the v1 surface is the `interceptableAdvice` scope rather than a directly-injected JoinPoint).
- The cross-module aspect discovery via marker classes (PR #12, kept as infrastructure even though the API on top changes).
- The Pattern B test infrastructure landed in #13.

---

## Implementation roadmap

### Phase 0 — Kotlin upgrade — ✓ shipped (PR #14)

Bumped Kotlin and related coordinates (KSP, BuildKonfig, Gradle tasks). All modules compile against the new compiler API. Existing test sample was reset to a stub; full test infrastructure is reinstated in Phase 7.

### Phase 1 — New annotation definitions — ✓ shipped (PR #15)

Added to `aspectk-annotations/`:

- `Visibility.kt`, `Modality.kt`, `Modifiers.kt`
- `Package.kt`, `ClassName.kt`, `MethodName.kt` (single `pattern: String`)
- `Annotated.kt`
- `Pointcut.kt` (redefined as a meta-annotation marker)
- `Advice.kt` (in Phase 3, became parameterless)

### Phase 2 — FIR checker on the new model — ✓ shipped (PR #16)

- `AdviceScopeChecker`: errors when advice is declared outside an `@Aspect` class.
- `PointcutAnnotationChecker`: validates the single `pattern: String` is non-empty on `@Package` / `@ClassName` / `@MethodName`.

### Phase 3 — IR transformer rewrite — ✓ minimum-viable shipped (PR #17)

The IR-time advice applier is back from the Phase 0 no-op stub.

- **AspectAnalyzer**: walks the `IrModuleFragment`, finds every `@Aspect` class, reads each advice's stacked pointcut annotations.
- **AspectKTransformer**: visits every `IrSimpleFunction` outside an `@Aspect` class, asks **PointcutMatcher** whether each advice's filter matches, and (when matched) prepends an inline advice invocation to the target's body.
- **NamePattern**: converts a `*` / `**` glob into a single anchored `Regex`.

Wired up in minimum-viable form:

- Advice kind: `@Before` only.
- Matching: `@ClassName(pattern)` + `@MethodName(pattern)` (name-pattern only).
- Bindings: none — advice must be parameterless on an aspect with a no-arg primary constructor.

### Phase 3 follow-up (in progress)

| Sub-phase | Scope |
|---|---|
| 3.1 | **Signature-driven binding** for `@Before`. Implement `@DispatchReceiver` / `@ExtensionReceiver` / `@ContextParameter` / `@ValueArgument` binding extraction at the call site; pass the values to the advice. FIR checker for binding annotation arguments. |
| 3.2 | **`@After` advice** (runs after, both normal-return and exception paths). |
| 3.3 | **`@Around` advice** including `interceptableAdvice { ... proceed() ... }` scope, `replace*` override functions, and the FIR-side bind-first-rule check. |
| 3.4 | Additional matching annotations: **`@Package`** (incl. recursive `**`), **`@Visibility`**, **`@Modality`**, **`@Modifiers`**, **`@Annotated`**. |
| 3.5 | **Matching at FIR + INFO diagnostic** at every woven call site (replaces IR-only matching with a single FIR-computed match set that IR consumes). |
| 3.6 | **Aspect instance caching** across call sites. |
| 3.7 | **"Matched zero declarations" diagnostic.** |

Order is roughly 3.1 → 3.2 → 3.3 → 3.4 → 3.5 → 3.6 → 3.7 but 3.4 can land in parallel with 3.2/3.3 because it touches a different code path.

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
- Reinstate the Pattern B compiler test harness for v1.

### Phase 8 — Extension / context receiver target name-pattern matching (v1.1)

- Name-pattern annotations matching the role `@ClassName` plays for the dispatch receiver, but for the extension / context receivers. Binding-via-annotation already works in v1.0 for both slots.
- Driven by user feedback after v1.0.

---

## Suggested PR sequence

| PR | Status | Contents |
|---|---|---|
| #14 | open | Phase 0 (Kotlin upgrade) |
| #15 | open, stacked on #14 | Phase 1 (annotation set) |
| #16 | open, stacked on #15 | Phase 2 (FIR checker) |
| #17 | open, stacked on #16 | Phase 3 minimum-viable (IR transformer) |
| this PR | this branch | Phase 3 follow-up design doc |
| 3.1 PR | future | Signature-driven binding |
| 3.2 PR | future | `@After` advice |
| 3.3 PR | future | `@Around` advice + `interceptableAdvice` + `replace*` + FIR bind-first check |
| 3.4 PR | future | Additional matching annotations |
| 3.5 PR | future | FIR matching unification + INFO diagnostic |
| 3.6 PR | future | Aspect instance caching |
| 3.7 PR | future | Zero-match diagnostic |
| 4 PR | future | Phase 4 — delete `aspectk-expression` |
| 5 PR | future | Phase 5 — logical composition |
| 6 PR | future | Phase 6 — meta-annotation expansion |
| 7 PR | future | Phase 7 — docs + sample + test harness |

---

## Open questions to revisit

- Whether `suspend` and `inline` targets should be supported, refused with a diagnostic, or simply skipped.
- How to handle generic erasure when the target's return type or parameter type involves type parameters (`List<T>`, `Map<K, V>`, etc.) at the binding level.
- Whether the runtime fallback (`Any?`-typed `replace*` API) is needed at all, given the AspectK compiler plugin is required at compile time anyway.

---

Last updated: 2026-05-26.
