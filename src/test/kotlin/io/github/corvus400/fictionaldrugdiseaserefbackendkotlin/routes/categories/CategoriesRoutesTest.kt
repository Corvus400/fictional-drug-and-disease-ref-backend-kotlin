package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.categories

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.AppJson
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.CategoriesResponse
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.moduleWithDatabaseDispatcher
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport.withPostgresConfig
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoriesRoutesTest {
    @Test
    fun `GET categories returns seven non-empty category groups`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.get("/v1/categories")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = AppJson.decodeFromString<CategoriesResponse>(response.bodyAsText())
        assertTrue(body.atc.isNotEmpty())
        assertTrue(body.therapeuticCategories.isNotEmpty())
        assertTrue(body.routeOfAdministration.isNotEmpty())
        assertTrue(body.dosageForm.isNotEmpty())
        assertTrue(body.regulatoryClass.isNotEmpty())
        assertTrue(body.icd10Chapters.isNotEmpty())
        assertTrue(body.medicalDepartments.isNotEmpty())
    }
}
