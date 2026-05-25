package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

fun hikariDataSource(cfg: DatabaseConfig): HikariDataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = cfg.url
            username = cfg.user
            password = cfg.password
            maximumPoolSize = cfg.maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        },
    )

fun Application.configureDatabase() {
    val cfg: DatabaseConfig by dependencies
    val dataSource = hikariDataSource(cfg)
    Flyway.configure()
        .dataSource(dataSource)
        .placeholderReplacement(false)
        .load()
        .migrate()
    val database = Database.connect(dataSource)
    configureDataLayerDependencies(dataSource = dataSource, database = database)
}

suspend fun <T> dbQuery(
    database: Database? = null,
    block: suspend () -> T,
): T =
    withContext(Dispatchers.IO) {
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
