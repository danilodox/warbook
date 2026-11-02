import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktor) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
}

// Versão derivada da última tag vX.Y.Z:
// - no commit da tag: "0.1.0"; depois dela: "0.1.0-dev.3" (3 commits à frente);
// - sem tag ou sem git: "0.0.0-dev".
// versionCode = X * 10000 + Y * 100 + Z (sem tag: 1).
val gitDescribe: String = runCatching {
    providers.exec {
        commandLine("git", "describe", "--tags", "--long", "--match", "v[0-9]*.[0-9]*.[0-9]*")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}.getOrDefault("")

val versionMatch = Regex("""^v(\d+)\.(\d+)\.(\d+)-(\d+)-g[0-9a-f]+$""").matchEntire(gitDescribe)

val appVersionName: String = versionMatch?.destructured?.let { (major, minor, patch, ahead) ->
    if (ahead == "0") "$major.$minor.$patch" else "$major.$minor.$patch-dev.$ahead"
} ?: "0.0.0-dev"

val appVersionCode: Int = versionMatch?.destructured?.let { (major, minor, patch) ->
    major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt()
} ?: 1

extra["warbook.versionName"] = appVersionName
extra["warbook.versionCode"] = appVersionCode

// Kotlin com que o Detekt 1.23.x foi compilado; ele roda isolado com essa versão.
val detektKotlinVersion = "2.0.21"

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        source.setFrom(files("src"))
    }
    tasks.withType<Detekt>().configureEach {
        exclude("**/build/**")
    }
    configurations.matching { it.name == "detekt" }.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") useVersion(detektKotlinVersion)
        }
    }

    configure<KtlintExtension> {
        filter {
            exclude { it.file.path.contains("${File.separator}build${File.separator}") }
        }
    }
}
