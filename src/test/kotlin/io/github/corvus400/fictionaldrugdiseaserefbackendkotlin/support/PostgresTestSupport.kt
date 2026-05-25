package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support

import com.zaxxer.hikari.HikariDataSource
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.DatabaseConfig
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.hikariDataSource
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.TestApplicationBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.containers.PostgreSQLContainer

object PostgresTestSupport {
    private val postgres: PostgreSQLContainer<Nothing> by lazy {
        PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            start()
        }
    }

    val databaseConfig: DatabaseConfig
        get() =
            DatabaseConfig(
                url = postgres.jdbcUrl,
                user = postgres.username,
                password = postgres.password,
                maxPoolSize = 4,
            )

    val dataSource: HikariDataSource by lazy {
        hikariDataSource(databaseConfig).also { dataSource ->
            Flyway.configure()
                .dataSource(dataSource)
                .placeholderReplacement(false)
                .load()
                .migrate()
        }
    }

    val database: Database by lazy {
        Database.connect(dataSource)
    }

    @Suppress("InjectDispatcher")
    val databaseDispatcher: CoroutineDispatcher = Dispatchers.IO

    fun TestApplicationBuilder.withPostgresConfig() {
        environment {
            config = MapApplicationConfig(
                "app.environment" to "test",
                "database.url" to databaseConfig.url,
                "database.user" to databaseConfig.user,
                "database.password" to databaseConfig.password,
                "database.maxPoolSize" to databaseConfig.maxPoolSize.toString(),
            )
        }
    }
}
