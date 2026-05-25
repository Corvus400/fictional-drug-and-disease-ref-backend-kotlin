package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

private const val DATABASE_CONNECTION_TIMEOUT_MS = 1_000L
private const val DATABASE_VALIDATION_TIMEOUT_MS = 1_000L

fun hikariDataSource(cfg: DatabaseConfig): HikariDataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = cfg.url
            username = cfg.user
            password = cfg.password
            maximumPoolSize = cfg.maxPoolSize
            connectionTimeout = DATABASE_CONNECTION_TIMEOUT_MS
            validationTimeout = DATABASE_VALIDATION_TIMEOUT_MS
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        },
    )

fun Application.configureDatabase(databaseDispatcher: CoroutineDispatcher) {
    val cfg: DatabaseConfig by dependencies
    val dataSource = hikariDataSource(cfg)
    Flyway.configure()
        .dataSource(dataSource)
        .placeholderReplacement(false)
        .load()
        .migrate()
    val database = Database.connect(dataSource)
    configureDataLayerDependencies(
        dataSource = dataSource,
        database = database,
        databaseDispatcher = databaseDispatcher,
    )
}

suspend fun <T> dbQuery(
    database: Database? = null,
    databaseDispatcher: CoroutineDispatcher,
    block: suspend () -> T,
): T =
    withContext(databaseDispatcher + MDCContext()) {
        if (database == null) {
            suspendTransaction {
                block()
            }
        } else {
            suspendTransaction(db = database) {
                block()
            }
        }
    }
