package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.Disease
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DiseaseRepositoryTest {
    private val repository = ExposedDiseaseRepository(PostgresTestSupport.database)

    @Test
    fun `findAll returns all seeded diseases`() = runBlocking {
        val result = assertIs<AppResult.Success<List<*>>>(repository.findAll())
        assertEquals(80, result.value.size)
    }

    @Test
    fun `findByPublicId returns nested JSONB document`() = runBlocking {
        val result = assertIs<AppResult.Success<*>>(repository.findByPublicId("disease_0001"))
        val disease = assertIs<Disease>(result.value)
        assertEquals("disease_0001", disease.id)
        assertTrue(disease.symptoms.mainSymptoms.isNotEmpty())
        assertTrue(disease.requiredExams.isNotEmpty())
    }

    @Test
    fun `findByPublicId returns NotFound for missing disease`() = runBlocking {
        val result = assertIs<AppResult.Failure>(repository.findByPublicId("disease_9999"))
        assertEquals(DomainError.NotFound(resource = "disease", id = "disease_9999"), result.error)
    }
}
