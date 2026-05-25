package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureDI
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureDataLayerDependencies
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureLogging
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureRateLimit
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureRouting
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureSecurity
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureSerialization
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.configureStatusPages
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.isMetricsPeerAllowed
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport.withPostgresConfig
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.sql.Connection
import java.sql.SQLException
import java.util.logging.Logger
import javax.sql.DataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import ch.qos.logback.classic.Logger as LogbackLogger

class ObservabilityTest {
    @Test
    fun `metrics allowlist accepts loopback and RFC1918 peers`() {
        val allowedCidrs = listOf("127.0.0.1/32", "::1/128", "10.0.0.0/8")

        assertTrue(isMetricsPeerAllowed("127.0.0.1", allowedCidrs))
        assertTrue(isMetricsPeerAllowed("::1", allowedCidrs))
        assertTrue(isMetricsPeerAllowed("10.42.0.10", allowedCidrs))
    }

    @Test
    fun `metrics allowlist rejects public peers`() {
        val allowedCidrs = listOf("127.0.0.1/32", "::1/128", "10.0.0.0/8")

        assertFalse(isMetricsPeerAllowed("8.8.8.8", allowedCidrs))
    }

    @Test
    fun `metrics endpoint allows loopback and exposes prometheus metrics even when forwarded header is public`() =
        testApplication {
            withPostgresConfig()
            application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

            client.get("/health")
            val response = client.get("/metrics") {
                header(HttpHeaders.XForwardedFor, "8.8.8.8")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(body.contains("ktor_http_server"), body)
        }

    @Test
    fun `request id response header matches call id MDC in request log`() = testApplication {
        withPostgresConfig()
        val logger = LoggerFactory.getLogger("io.ktor.test") as LogbackLogger
        val appender = ListAppender<ILoggingEvent>().apply {
            start()
        }
        logger.addAppender(appender)
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        try {
            val response = client.get("/health")
            val requestId = response.headers[HttpHeaders.XRequestId]

            assertEquals(HttpStatusCode.OK, response.status)
            assertNotNull(requestId)
            assertTrue(
                appender.list.any { event ->
                    event.level == Level.INFO && event.mdcPropertyMap["call_id"] == requestId
                },
            )
        } finally {
            logger.detachAppender(appender)
        }
    }

    @Test
    fun `readiness endpoint returns ready when database is reachable`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/health/ready")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `readiness endpoint returns not ready when database is unreachable`() = testApplication {
        withPostgresConfig()
        application {
            configureLogging()
            configureSerialization()
            configureStatusPages()
            configureDI()
            configureDataLayerDependencies(
                dataSource = failingDataSource(),
                database = PostgresTestSupport.database,
                databaseDispatcher = PostgresTestSupport.databaseDispatcher,
            )
            configureSecurity()
            configureRateLimit()
            configureRouting()
        }

        val response = client.get("/health/ready")

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }

    private fun failingDataSource(): DataSource =
        object : DataSource {
            override fun getConnection(): Connection = throw SQLException("database unavailable")

            override fun getConnection(
                username: String?,
                password: String?,
            ): Connection = throw SQLException("database unavailable")

            override fun getLogWriter(): PrintWriter? = null

            override fun setLogWriter(out: PrintWriter?) = Unit

            override fun setLoginTimeout(seconds: Int) = Unit

            override fun getLoginTimeout(): Int = 0

            override fun getParentLogger(): Logger = Logger.getGlobal()

            override fun <T : Any?> unwrap(iface: Class<T>?): T = throw SQLException("Unsupported unwrap")

            override fun isWrapperFor(iface: Class<*>?): Boolean = false
        }
}
