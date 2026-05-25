package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureDI
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureDatabase
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureLogging
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureOpenAPI
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureRouting
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureSerialization
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureStatusPages
import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module(databaseDispatcher: CoroutineDispatcher = productionDatabaseDispatcher()) {
    configureLogging()
    configureSerialization()
    configureStatusPages()
    configureDI()
    configureDatabase(databaseDispatcher = databaseDispatcher)
    configureOpenAPI()
    configureRouting()
}

@Suppress("InjectDispatcher")
private fun productionDatabaseDispatcher(): CoroutineDispatcher = Dispatchers.IO
