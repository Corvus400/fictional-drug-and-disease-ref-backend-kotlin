package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.ktor.server.application.Application

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val maxPoolSize: Int,
)

fun Application.loadDatabaseConfig(): DatabaseConfig =
    DatabaseConfig(
        url = resolveConfig("DB_URL", "database.url", default = null),
        user = resolveConfig("DB_USER", "database.user", default = null),
        password = resolveConfig("DB_PASSWORD", "database.password", default = null),
        maxPoolSize = resolveConfig("DB_MAX_POOL_SIZE", "database.maxPoolSize", default = "10").toInt(),
    )
