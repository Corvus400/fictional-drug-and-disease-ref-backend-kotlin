package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.ktor.server.application.Application

data class AppConfig(
    val environment: String,
)

fun Application.loadAppConfig(): AppConfig =
    AppConfig(
        environment = resolveConfig("APP_ENV", "app.environment", default = "local"),
    )

internal fun Application.resolveConfig(
    envVar: String,
    configPath: String,
    default: String?,
): String =
    System.getenv(envVar)
        ?: environment.config.propertyOrNull(configPath)?.getString()
        ?: default
        ?: error("Required config missing: env=$envVar / config=$configPath")
