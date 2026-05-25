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

val seedSourceSet = sourceSets.create("seed") {
    java.srcDir("src/seed/kotlin")
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += output + compileClasspath
}

configurations.named(seedSourceSet.implementationConfigurationName) {
    extendsFrom(configurations.implementation.get())
}

configurations.named(seedSourceSet.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.runtimeOnly.get())
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.call.id)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.di)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.java.jwt)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.forwarded.header)
    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.kotlinx.coroutines.slf4j)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.ipaddress)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.json)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.postgresql)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.smiley4.ktor.openapi)
    implementation(libs.smiley4.ktor.swagger.ui)
    implementation(libs.smiley4.ktor.redoc)
    implementation(libs.smiley4.schema.kenerator.core)
    implementation(libs.smiley4.schema.kenerator.serialization)
    implementation(libs.smiley4.schema.kenerator.swagger)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit.jupiter)

    add(seedSourceSet.implementationConfigurationName, libs.ktor.client.core)
    add(seedSourceSet.implementationConfigurationName, libs.ktor.client.cio)
    add(seedSourceSet.implementationConfigurationName, libs.ktor.client.content.negotiation)
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

tasks.register<JavaExec>("exportSeedSql") {
    group = "seed"
    description = "Export fixed Flyway seed SQL from the mock-server API."
    dependsOn("compileSeedKotlin")
    classpath = seedSourceSet.runtimeClasspath
    mainClass.set("io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.tools.SeedExporterKt")
    args(
        "--base-url",
        providers.gradleProperty("seedBaseUrl").orElse("http://localhost:8080").get(),
        "--output-dir",
        layout.projectDirectory.dir("src/main/resources/db/migration").asFile.absolutePath,
    )
}
