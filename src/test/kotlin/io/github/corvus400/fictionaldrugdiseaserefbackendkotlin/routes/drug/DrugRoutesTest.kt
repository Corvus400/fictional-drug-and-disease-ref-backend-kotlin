package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.drug

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.AppJson
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.ProblemDetails
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.ProblemTypes
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.DrugListResponse
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

class DrugRoutesTest {
    @Test
    fun `GET drugs returns seeded default page envelope`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/drugs")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = AppJson.decodeFromString<DrugListResponse>(response.bodyAsText())
        assertEquals(120, body.totalCount)
        assertEquals(1, body.page)
        assertEquals(20, body.pageSize)
        assertEquals(20, body.items.size)
    }

    @Test
    fun `GET drugs supports sort and page_size query`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/drugs?sort=brand_name_kana&page_size=5")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = AppJson.decodeFromString<DrugListResponse>(response.bodyAsText())
        assertEquals(5, body.items.size)
    }

    @Test
    fun `GET drugs rejects unknown regulatory class as problem json`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/drugs?regulatory_class=__bogus__")

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals(ContentType("application", "problem+json"), response.contentType()?.withoutParameters())
        val problem = AppJson.decodeFromString<ProblemDetails>(response.bodyAsText())
        assertEquals(ProblemTypes.VALIDATION, problem.type)
        assertEquals("regulatory_class", problem.errors?.first()?.field)
    }

    @Test
    fun `GET drug detail returns public id`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/drugs/drug_0001")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = AppJson.decodeFromString<Drug>(response.bodyAsText())
        assertEquals("drug_0001", body.id)
    }

    @Test
    fun `GET missing drug detail returns not found problem`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/drugs/drug_9999")

        assertEquals(HttpStatusCode.NotFound, response.status)
        val problem = AppJson.decodeFromString<ProblemDetails>(response.bodyAsText())
        assertEquals(ProblemTypes.NOT_FOUND, problem.type)
    }
}
