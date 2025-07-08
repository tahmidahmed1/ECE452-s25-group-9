plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("dagger.hilt.android.plugin") version "2.51.1" apply false
    id("io.ktor.plugin") version "2.3.7" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0" apply false
    id("com.diffplug.spotless") version "6.25.0" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
    alias(libs.plugins.compose.hot.reload) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
} 