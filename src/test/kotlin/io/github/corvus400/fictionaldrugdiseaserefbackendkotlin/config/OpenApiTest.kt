package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.module
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport.withPostgresConfig
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenApiTest {
    @Test
    fun `openapi json contains v1 business paths and no mock admin contract`() = testApplication {
        withPostgresConfig()
        application { module(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/openapi.json")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        val paths = Json.parseToJsonElement(body).jsonObject.getValue("paths").jsonObject.keys
        assertTrue("/v1/drugs" in paths)
        assertTrue("/v1/drugs/{id}" in paths)
        assertTrue("/v1/diseases" in paths)
        assertTrue("/v1/diseases/{id}" in paths)
        assertTrue("/v1/categories" in paths)
        assertTrue("/v1/images/dosage-forms/{form}" in paths)
        assertTrue("/v1/images/drugs/{drugId}" in paths)
        assertFalse(body.contains("/__admin"))
        assertFalse(body.contains("X-Mock-Scenario"))
    }
}
