package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin

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
import io.ktor.server.engine.CommandLineConfig
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.EngineConnectorBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.loadCommonConfiguration
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.di.dependencies
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

fun main(args: Array<String>) {
    createConfiguredServer(args).start(wait = true)
}

fun createConfiguredServer(
    args: Array<String>,
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> {
    val cli = CommandLineConfig(args)
    val deploymentConfig = cli.rootConfig.environment.config.config("ktor.deployment")
    val adminHost = cli.rootConfig.environment.config.property("security.adminHost").getString()
    val adminPort = cli.rootConfig.environment.config.property("security.adminPort").getString().toInt()
    return embeddedServer(Netty, rootConfig = cli.rootConfig) {
        takeFrom(cli.engineConfig)
        loadCommonConfiguration(deploymentConfig)
        connectors.add(
            EngineConnectorBuilder().apply {
                host = adminHost
                port = adminPort
            },
        )
    }
}

fun Application.module() {
    moduleWithDatabaseDispatcher(databaseDispatcher = productionDatabaseDispatcher())
}

fun Application.moduleWithDatabaseDispatcher(databaseDispatcher: CoroutineDispatcher) {
    configureForwardedHeaders()
    configureLogging()
    configureSerialization()
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
