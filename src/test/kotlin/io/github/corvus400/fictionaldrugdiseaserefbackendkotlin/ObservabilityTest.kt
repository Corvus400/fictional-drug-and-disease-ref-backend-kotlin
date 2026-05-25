package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.isMetricsPeerAllowed
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport.withPostgresConfig
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun `metrics endpoint allows loopback even when forwarded header is public`() = testApplication {
        withPostgresConfig()
        application { module(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/metrics") {
            header(HttpHeaders.XForwardedFor, "8.8.8.8")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }
}
