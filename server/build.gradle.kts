plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
}

group = "io.github.danilodox.warbook"
version = "1.0.0"
application {
    mainClass = "io.github.danilodox.warbook.ApplicationKt"
}

// Grava a versão num recurso para o /health responder com ela.
val generateVersionResource by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/version")
    val versionText = project.version.toString()
    inputs.property("version", versionText)
    outputs.dir(outputDir)
    doLast {
        outputDir.get().file("warbook-version.txt").asFile.writeText(versionText)
    }
}
sourceSets.main {
    resources.srcDir(generateVersionResource)
}

dependencies {
    api(project(":shared"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationKotlinxJson)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}
