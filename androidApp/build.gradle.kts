import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":composeApp"))

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)

    testImplementation(libs.kotlin.testJunit)
}

// URL do servidor no build debug: emulador por padrão; para o celular, ponha no local.properties
// `warbook.serverUrl=http://<IP do PC na rede>:8080`.
val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}
val debugServerUrl = localProperties.getProperty("warbook.serverUrl") ?: "http://10.0.2.2:8080"

// Ainda não há servidor publicado; o endereço de produção entra no deploy (v0.9).
val releaseServerUrl = "https://warbook.invalid"

android {
    namespace = "io.github.danilodox.warbook"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.danilodox.warbook"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // Derivados da tag git no build.gradle.kts da raiz.
        versionCode = rootProject.extra["warbook.versionCode"] as Int
        versionName = rootProject.extra["warbook.versionName"] as String
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            buildConfigField("String", "SERVER_URL", "\"$debugServerUrl\"")
        }
        release {
            buildConfigField("String", "SERVER_URL", "\"$releaseServerUrl\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
