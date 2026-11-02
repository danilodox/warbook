import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.owasp)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "io.github.danilodox.warbook"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.danilodox.warbook"
        //mis sdk = 24 -> is below safe levels. Recommended at least API 29
        //Which makes it vulnerable to attacks like "StrandHogg 2.0"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiKey = localProperties.getProperty("API_KEY", "")
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

}

dependencyCheck {
    // check by plugin to use the value from the 'nvdApiKey' Gradle property
    // 'findProperty' makes it safe to run locally without the key
    nvd.apiKey = findProperty("nvdApiKey") as? String
    nvd.delay = 7000
}

dependencies {

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material3)
    implementation(libs.liveData)
    implementation(libs.composableNavigation)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    //koin
    implementation (libs.koin.koin.android)
    implementation(libs.koin.compose)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
    implementation(libs.koin.androidx.navigation)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // OkHttp (for logging)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Gson
    implementation(libs.gson)
    implementation(libs.androidx.foundation.layout.android)

    // Room

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.android.database.sqlcipher)

    ksp(libs.room.compiler)







    //test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.7")

    implementation(libs.androidx.security.crypto)
}