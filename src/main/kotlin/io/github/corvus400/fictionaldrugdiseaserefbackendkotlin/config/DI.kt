package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies

fun Application.configureDI() {
    val appConfig = loadAppConfig()
    val databaseConfig = loadDatabaseConfig()
    dependencies {
        provide<AppConfig> { appConfig }
        provide<DatabaseConfig> { databaseConfig }
    }
}
