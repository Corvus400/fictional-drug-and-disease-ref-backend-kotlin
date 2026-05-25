plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
    application
}

group = "io.github.corvus400.fictionaldrugdiseaserefbackendkotlin"
version = "0.1.0"

application {
    mainClass.set("io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.di)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.logback.classic)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}

ktor {
    fatJar {
        archiveFileName.set("fictional-drug-and-disease-ref-backend-kotlin-all.jar")
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$projectDir/config/detekt/detekt.yml")
    source.setFrom(
        "src/main/kotlin",
        "src/test/kotlin",
    )
}

spotless {
    val ratchetBase = "origin/main"
    val ratchetEnabled = providers.environmentVariable("CI").orNull == "true" ||
        providers.gradleProperty("spotless.ratchet").orNull == "true"
    if (ratchetEnabled) {
        ratchetFrom(ratchetBase)
    }
    kotlin {
        target("src/**/*.kt")
        targetExclude("build/**/*.kt")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}

tasks.test {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}
