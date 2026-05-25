package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.common

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.AppJson
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.ProblemDetails
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.ProblemTypes
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.module
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport.withPostgresConfig
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImageRoutesTest {
    @Test
    fun `GET dosage form image returns resized png`() = testApplication {
        withPostgresConfig()
        application { module(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/images/dosage-forms/tablet?size=S")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Image.PNG, response.contentType()?.withoutParameters())
        assertTrue(response.bodyAsBytes().isNotEmpty())
    }

    @Test
    fun `GET dosage form image rejects unsupported size`() = testApplication {
        withPostgresConfig()
        application { module(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/images/dosage-forms/tablet?size=XL")

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        val problem = AppJson.decodeFromString<ProblemDetails>(response.bodyAsText())
        assertEquals(ProblemTypes.VALIDATION, problem.type)
        assertEquals("size", problem.errors?.first()?.field)
    }

    @Test
    fun `GET drug image returns png for existing drug asset`() = testApplication {
        withPostgresConfig()
        application { module(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/images/drugs/drug_0080")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Image.PNG, response.contentType()?.withoutParameters())
        assertTrue(response.bodyAsBytes().isNotEmpty())
    }

    @Test
    fun `GET drug image returns not found for missing drug asset`() = testApplication {
        withPostgresConfig()
        application { module(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/images/drugs/drug_0001")

        assertEquals(HttpStatusCode.NotFound, response.status)
        val problem = AppJson.decodeFromString<ProblemDetails>(response.bodyAsText())
        assertEquals(ProblemTypes.NOT_FOUND, problem.type)
    }
}
