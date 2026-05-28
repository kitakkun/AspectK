package com.github.kitakkun.aspectk.gradle.extension

import org.gradle.api.provider.Property

abstract class AspectKExtension {
    val enabled: Boolean = true

    /**
     * When `true`, the `aspectKAggregateReport` task fails the build if any
     * advice in any project matched zero call sites across the aggregated
     * view (i.e. the advice is unused). When `false` (the default), the
     * unused advices are reported as a warning but the task still succeeds.
     *
     * The strict mode is opt-in because legitimate uses (e.g. an advice
     * deliberately bound to APIs that only some build variants exercise)
     * may produce zero matches without being a mistake.
     *
     * **Known limitation**: until cross-module aspect discovery lands,
     * advices declared in an aspect-only module whose targets live in a
     * consumer module will be falsely flagged as unused (the IR weaver
     * doesn't currently see cross-module aspects). Enable strict mode only
     * for single-module setups or for projects where each aspect-bearing
     * module exercises its own advices.
     */
    abstract val strictUnusedAspects: Property<Boolean>
}
