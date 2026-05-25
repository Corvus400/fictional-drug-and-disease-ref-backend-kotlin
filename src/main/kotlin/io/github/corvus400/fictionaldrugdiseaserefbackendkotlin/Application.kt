package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureCors
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureDI
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureDatabase
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureForwardedHeaders
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureLogging
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureObservability
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureOpenAPI
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureRateLimit
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureRouting
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureSecurity
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureSerialization
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    moduleWithDatabaseDispatcher(databaseDispatcher = productionDatabaseDispatcher())
}

fun Application.moduleWithDatabaseDispatcher(databaseDispatcher: CoroutineDispatcher) {
    configureForwardedHeaders()
    configureLogging()
    configureSerialization()
    configureCors()
    configureStatusPages()
    configureDI()
    configureDatabase(databaseDispatcher = databaseDispatcher)
    val metricsRegistry: PrometheusMeterRegistry by dependencies
    configureObservability(metricsRegistry)
    configureSecurity()
    configureRateLimit()
    configureOpenAPI()
    configureRouting()
}

@Suppress("InjectDispatcher")
private fun productionDatabaseDispatcher(): CoroutineDispatcher = Dispatchers.IO
