import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class AspectKCommonConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            group = "com.github.kitakkun.aspectk"
            version = libs.findVersion("aspectk").get()

            with(pluginManager) {
                apply("org.jlleitschuh.gradle.ktlint")
            }

            configure<KtlintExtension> {
                version.set("1.2.1")
                ignoreFailures.set(true)
            }
        }
    }
}

internal val Project.libs get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
