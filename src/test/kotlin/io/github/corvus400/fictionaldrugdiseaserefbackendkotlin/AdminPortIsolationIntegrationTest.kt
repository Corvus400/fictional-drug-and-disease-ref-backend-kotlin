package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin

import com.auth0.jwt.JWT
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.AppJson
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.ProblemDetails
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.ProblemTypes
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminPortIsolationIntegrationTest {
    @Test
    fun `public connector hides admin whoami as canonical not found`() {
        val publicPort = freePort()
        val adminPort = freePort()
        val server = createConfiguredServer(
            args = testServerArgs(publicPort = publicPort, adminPort = adminPort),
        )

        try {
            server.start(wait = false)

            val response = httpClient.send(
                request("http://127.0.0.1:$publicPort/v1/admin/whoami"),
                HttpResponse.BodyHandlers.ofString(),
            )
            val missingResponse = httpClient.send(
                request("http://127.0.0.1:$publicPort/v1/__nope__"),
                HttpResponse.BodyHandlers.ofString(),
            )
            val problem = AppJson.decodeFromString<ProblemDetails>(response.body())
            val missingProblem = AppJson.decodeFromString<ProblemDetails>(missingResponse.body())

            assertEquals(404, response.statusCode())
            assertEquals(
                "application/problem+json",
                response.headers().firstValue("content-type").get().substringBefore(";")
            )
            assertEquals(
                "application/problem+json",
                missingResponse.headers().firstValue("content-type").get().substringBefore(";")
            )
            assertEquals(ProblemTypes.NOT_FOUND, problem.type)
            assertEquals(missingProblem.type, problem.type)
            assertEquals("Resource not found", problem.title)
            assertEquals(missingProblem.title, problem.title)
            assertEquals(404, problem.status)
            assertEquals(missingProblem.status, problem.status)
            assertEquals("No route matched /v1/admin/whoami", problem.detail)
            assertEquals("/v1/admin/whoami", problem.instance)
            assertEquals("No route matched /v1/__nope__", missingProblem.detail)
            assertEquals("/v1/__nope__", missingProblem.instance)
            assertEquals(false, response.headers().firstValue("www-authenticate").isPresent)
        } finally {
            server.stop()
        }
    }

    @Test
    fun `admin connector reaches whoami authentication challenge`() {
        val publicPort = freePort()
        val adminPort = freePort()
        val server = createConfiguredServer(
            args = testServerArgs(publicPort = publicPort, adminPort = adminPort),
        )

        try {
            server.start(wait = false)

            val response = httpClient.send(
                request("http://127.0.0.1:$adminPort/v1/admin/whoami"),
                HttpResponse.BodyHandlers.ofString(),
            )

            assertEquals(401, response.statusCode())
            assertEquals(
                "application/problem+json",
                response.headers().firstValue("content-type").get().substringBefore(";")
            )
        } finally {
            server.stop()
        }
    }

    @Test
    fun `admin connector mints admin token`() {
        val publicPort = freePort()
        val adminPort = freePort()
        val server = createConfiguredServer(
            args = testServerArgs(publicPort = publicPort, adminPort = adminPort),
        )

        try {
            server.start(wait = false)

            val response = httpClient.send(
                post("http://127.0.0.1:$adminPort/v1/admin/token"),
                HttpResponse.BodyHandlers.ofString(),
            )
            val body = AppJson.parseToJsonElement(response.body()).jsonObject
            val token = body.getValue("access_token").jsonPrimitive.content
            val decoded = JWT.decode(token)

            assertEquals(200, response.statusCode())
            assertEquals(token, body.getValue("token").jsonPrimitive.content)
            assertEquals("Bearer", body.getValue("token_type").jsonPrimitive.content)
            assertEquals("admin", decoded.getClaim("scope").asString())
            assertEquals("local-admin", decoded.subject)
        } finally {
            server.stop()
        }
    }

    @Test
    fun `admin connector answers admin write cors preflight`() {
        val publicPort = freePort()
        val adminPort = freePort()
        val origin = "http://localhost:5173"
        val server = createConfiguredServer(
            args = testServerArgs(publicPort = publicPort, adminPort = adminPort, corsOrigin = origin),
        )

        try {
            server.start(wait = false)

            val response = httpClient.send(
                options(
                    url = "http://127.0.0.1:$adminPort/v1/admin/token",
                    origin = origin,
                    requestedMethod = "POST",
                ),
                HttpResponse.BodyHandlers.ofString(),
            )

            assertEquals(200, response.statusCode())
            assertEquals(origin, response.headers().firstValue("access-control-allow-origin").get())
            val allowMethods = response.headers().firstValue("access-control-allow-methods").get().uppercase()
            assertEquals(true, allowMethods.contains("PUT"), allowMethods)
            assertEquals(true, allowMethods.contains("PATCH"), allowMethods)
            assertEquals(true, allowMethods.contains("DELETE"), allowMethods)
            assertEquals(true, allowMethods.contains("OPTIONS"), allowMethods)
        } finally {
            server.stop()
        }
    }

    @Test
    fun `public read route answers authorized cors preflight and exposes etag`() {
        val publicPort = freePort()
        val adminPort = freePort()
        val origin = "http://localhost:5173"
        val server = createConfiguredServer(
            args = testServerArgs(publicPort = publicPort, adminPort = adminPort, corsOrigin = origin),
        )

        try {
            server.start(wait = false)

            val preflight = httpClient.send(
                options(
                    url = "http://127.0.0.1:$publicPort/v1/drugs",
                    origin = origin,
                    requestedMethod = "GET",
                ),
                HttpResponse.BodyHandlers.ofString(),
            )
            val getResponse = httpClient.send(
                request("http://127.0.0.1:$publicPort/v1/drugs", origin = origin),
                HttpResponse.BodyHandlers.ofString(),
            )

            assertEquals(200, preflight.statusCode())
            assertEquals(origin, preflight.headers().firstValue("access-control-allow-origin").get())
            assertEquals(200, getResponse.statusCode())
            assertEquals(origin, getResponse.headers().firstValue("access-control-allow-origin").get())
            assertEquals("ETag", getResponse.headers().firstValue("access-control-expose-headers").get())
        } finally {
            server.stop()
        }
    }

    @Test
    fun `public connector hides admin options without cors headers`() {
        val publicPort = freePort()
        val adminPort = freePort()
        val origin = "http://localhost:5173"
        val server = createConfiguredServer(
            args = testServerArgs(publicPort = publicPort, adminPort = adminPort, corsOrigin = origin),
        )

        try {
            server.start(wait = false)

            val response = httpClient.send(
                options(
                    url = "http://127.0.0.1:$publicPort/v1/admin/token",
                    origin = origin,
                    requestedMethod = "POST",
                ),
                HttpResponse.BodyHandlers.ofString(),
            )

            assertEquals(404, response.statusCode())
            assertEquals(false, response.headers().firstValue("access-control-allow-origin").isPresent)
        } finally {
            server.stop()
        }
    }

    private fun testServerArgs(
        publicPort: Int,
        adminPort: Int,
        corsOrigin: String = "",
    ): Array<String> = arrayOf(
        "-P:app.environment=test",
        "-P:ktor.deployment.port=$publicPort",
        "-P:ktor.deployment.host=127.0.0.1",
        "-P:security.adminHost=127.0.0.1",
        "-P:security.adminPort=$adminPort",
        "-P:database.url=${PostgresTestSupport.databaseConfig.url}",
        "-P:database.user=${PostgresTestSupport.databaseConfig.user}",
        "-P:database.password=${PostgresTestSupport.databaseConfig.password}",
        "-P:database.maxPoolSize=${PostgresTestSupport.databaseConfig.maxPoolSize}",
        "-P:security.jwtSecret=test-secret-please-change",
        "-P:security.jwtIssuer=http://localhost",
        "-P:security.jwtAudience=fictional-drug-ref",
        "-P:security.jwtRealm=fictional-drug-ref",
        "-P:security.rateLimitLimit=1000",
        "-P:security.rateLimitRefillSeconds=60",
        "-P:security.corsAllowedOrigins=$corsOrigin",
        "-P:security.adminCorsAllowedOrigins=$corsOrigin",
        "-P:security.adminTokenTtlSeconds=3600",
        "-P:observability.serviceName=drug-disease-api-test",
        "-P:observability.logLevel=INFO",
        "-P:observability.metricsAllowedCidrs=127.0.0.1/32,::1/128",
    )

    private fun request(
        url: String,
        origin: String? = null,
    ): HttpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .apply {
                if (origin != null) {
                    header("Origin", origin)
                }
            }
            .GET()
            .build()

    private fun post(url: String): HttpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build()

    private fun options(
        url: String,
        origin: String,
        requestedMethod: String,
    ): HttpRequest =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Origin", origin)
            .header("Access-Control-Request-Method", requestedMethod)
            .header("Access-Control-Request-Headers", "Authorization, Content-Type")
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .build()

    private fun freePort(): Int = ServerSocket(0).use { it.localPort }

    private companion object {
        val httpClient: HttpClient = HttpClient.newHttpClient()
    }
}
