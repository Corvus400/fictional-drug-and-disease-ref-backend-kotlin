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
import javax.sql.DataSource

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
        .load()
        .migrate()
    val database = Database.connect(dataSource)
    dependencies {
        provide<DataSource> { dataSource }
        provide<Database> { database }
    }
}

suspend fun <T> dbQuery(block: suspend () -> T): T =
    withContext(Dispatchers.IO) {
        suspendTransaction {
            block()
        }
    }
