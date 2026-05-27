package com.github.kitakkun.aspectk.gradle.extension

import org.gradle.api.provider.Property

abstract class AspectKExtension {
    val enabled: Boolean = true

    /**
     * When `true`, the `aspectKAggregateReport` task fails the build if any
     * advice in any project matched zero call sites across the aggregated
     * view (i.e. the advice is unused). When `false` (the default), the
     * unused advices are reported as a warning to stderr but the task still
     * succeeds.
     *
     * The strict mode is opt-in because legitimate uses (e.g. an advice
     * deliberately bound to APIs that only some build variants exercise)
     * may produce zero matches without being a mistake.
     */
    abstract val strictUnusedAspects: Property<Boolean>
}
