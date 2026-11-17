plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
}

group = "io.github.danilodox.warbook"
version = rootProject.extra["warbook.versionName"] as String
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

// Passa as linhas CHAVE=valor do server/.env (fora do git) como variáveis de ambiente do `run`.
// O arquivo só é lido na execução, para os valores não irem para o cache de configuração.
tasks.named<JavaExec>("run") {
    val envFile = layout.projectDirectory.file(".env").asFile
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
    doFirst {
        val task = this as JavaExec
        if (envFile.isFile) {
            envFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && "=" in it }
                .forEach { task.environment(it.substringBefore("=").trim(), it.substringAfter("=").trim()) }
        }
    }
}

dependencies {
    api(project(":shared"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.ktor.serverStatusPages)
    implementation(libs.ktor.serverAuth)
    implementation(libs.ktor.serverAuthJwt)
    implementation(libs.ktor.serverRateLimit)
    implementation(libs.ktor.clientCore)
    implementation(libs.ktor.clientCio)
    implementation(libs.ktor.clientContentNegotiation)
    implementation(libs.caffeine)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javaTime)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.postgresql)
    implementation(libs.hikari)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.ktor.clientMock)
    testImplementation(libs.kotlin.testJunit)
    // Postgres de verdade nos testes, sem Docker. Só os binários de Windows (PC) e Linux (CI), na mesma
    // versão do Postgres instalado; o padrão do zonky baixaria também os de macOS e Alpine.
    testImplementation(libs.embeddedPostgres) {
        exclude(group = "io.zonky.test.postgres")
    }
    testRuntimeOnly(libs.embeddedPostgres.windows)
    testRuntimeOnly(libs.embeddedPostgres.linux)
}
