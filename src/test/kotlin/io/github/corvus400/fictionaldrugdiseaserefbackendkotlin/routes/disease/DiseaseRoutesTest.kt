package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.disease

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.AppJson
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.ProblemDetails
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.ProblemTypes
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.Disease
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.DiseaseListResponse
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.moduleWithDatabaseDispatcher
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport.withPostgresConfig
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class DiseaseRoutesTest {
    @Test
    fun `GET diseases returns seeded default page envelope`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/diseases")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = AppJson.decodeFromString<DiseaseListResponse>(response.bodyAsText())
        assertEquals(80, body.totalCount)
        assertEquals(1, body.page)
        assertEquals(20, body.pageSize)
        assertEquals(20, body.items.size)
    }

    @Test
    fun `GET diseases rejects invalid boolean filter as problem json`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/diseases?infectious=maybe")

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals(ContentType("application", "problem+json"), response.contentType()?.withoutParameters())
        val problem = AppJson.decodeFromString<ProblemDetails>(response.bodyAsText())
        assertEquals(ProblemTypes.VALIDATION, problem.type)
        assertEquals("infectious", problem.errors?.first()?.field)
    }

    @Test
    fun `GET disease detail returns public id`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/diseases/disease_0001")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = AppJson.decodeFromString<Disease>(response.bodyAsText())
        assertEquals("disease_0001", body.id)
    }

    @Test
    fun `GET missing disease detail returns not found problem`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/diseases/disease_9999")

        assertEquals(HttpStatusCode.NotFound, response.status)
        val problem = AppJson.decodeFromString<ProblemDetails>(response.bodyAsText())
        assertEquals(ProblemTypes.NOT_FOUND, problem.type)
    }
}
