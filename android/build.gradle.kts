import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("dagger.hilt.android.plugin")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.diffplug.spotless")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
}

android {
    namespace = "com.example.gooddeedfeed"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.gooddeedfeed"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val envFile = file("../.env")
        val envProps = Properties()
        if (envFile.exists()) {
            envFile.inputStream().use { envProps.load(it) }
        }

        val mapsApiKey: String =
            envProps.getProperty("GOOGLE_MAPS_API_KEY")
                ?: throw GradleException("Missing Maps API key. Define GOOGLE_MAPS_API_KEY in .env file.")
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"$mapsApiKey\"")
        manifestPlaceholders["googleMapsApiKey"] = mapsApiKey
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("boolean", "DEV_MODE", "true")
            buildConfigField("String", "DEV_BASE_URL", "\"http://10.0.2.2:8000\"")
        }
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "DEV_MODE", "false")
            buildConfigField("String", "DEV_BASE_URL", "\"\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-client-auth:2.3.7")
    implementation("io.ktor:ktor-client-logging:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    implementation("com.google.android.libraries.places:places:3.4.0")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("com.kizitonwose.calendar:compose:2.5.3")
    implementation("io.ktor:ktor-client-websockets:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    constraints {
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3") {
            because("Avoid using 1.7.x which requires Kotlin 2.0")
        }
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3") {
            because("Avoid using 1.7.x which requires Kotlin 2.0")
        }
    }
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
        force("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    }
}

kapt {
    correctErrorTypes = true
    arguments {
        arg("dagger.fastInit", "enabled")
        arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
    }
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("0.50.0").editorConfigOverride(
            mapOf("ktlint_standard_no-wildcard-imports" to "disabled"),
        )
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

tasks.register("ktlint") {
    group = "verification"
    description = "Runs ktlintCheck for linting."
    dependsOn("ktlintCheck")
}

tasks.register("format") {
    group = "formatting"
    description = "Runs spotlessApply for formatting."
    dependsOn("spotlessApply")
}
