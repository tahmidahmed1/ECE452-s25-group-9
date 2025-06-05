plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("io.ktor.plugin") version "2.3.7"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("com.diffplug.spotless") version "6.25.0"
    application
}

group = "com.example.gooddeedfeed"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.example.gooddeedfeed.ApplicationKt")
    applicationName = "server"
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:2.3.7")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("org.jetbrains.exposed:exposed-core:0.47.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.47.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.47.0")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("io.ktor:ktor-server-auth:2.3.7")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.7")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")
}

repositories {
    mavenCentral()
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint("0.50.0")
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

tasks.register("klint") {
    group = "verification"
    description = "Runs ktlintCheck for linting."
    dependsOn("ktlintCheck")
}
tasks.register("format") {
    group = "formatting"
    description = "Runs spotlessApply for formatting."
    dependsOn("spotlessApply")
}
