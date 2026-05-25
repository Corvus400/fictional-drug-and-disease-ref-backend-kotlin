package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport.withPostgresConfig
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SecurityRoutesTest {
    @Test
    fun `whoami without token returns 401 problem json`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/admin/whoami")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals(ContentType("application", "problem+json"), response.contentType()?.withoutParameters())
    }

    @Test
    fun `whoami with non admin scope returns 403`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/admin/whoami") {
            bearerAuth(mintToken(scope = "reader"))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(ContentType("application", "problem+json"), response.contentType()?.withoutParameters())
    }

    @Test
    fun `whoami with admin scope returns 200`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/admin/whoami") {
            bearerAuth(mintToken(scope = "admin"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `public endpoint over rate limit returns 429 problem json with Retry After`() = testApplication {
        withPostgresConfig(rateLimitLimit = 2)
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        repeat(2) {
            assertEquals(HttpStatusCode.OK, client.get("/v1/categories").status)
        }
        val response = client.get("/v1/categories")

        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        assertEquals(ContentType("application", "problem+json"), response.contentType()?.withoutParameters())
        assertNotNull(response.headers[HttpHeaders.RetryAfter])
    }

    @Test
    fun `public GET stays open without token`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        assertEquals(HttpStatusCode.OK, client.get("/v1/drugs").status)
    }

    private fun mintToken(
        secret: String = "test-secret-please-change",
        scope: String,
        sub: String = "admin-1",
    ): String =
        JWT.create()
            .withIssuer("http://localhost")
            .withAudience("fictional-drug-ref")
            .withSubject(sub)
            .withClaim("scope", scope)
            .sign(Algorithm.HMAC256(secret))
}
