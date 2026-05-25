package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DrugRepositoryTest {
    private val repository = ExposedDrugRepository(
        database = PostgresTestSupport.database,
        databaseDispatcher = PostgresTestSupport.databaseDispatcher,
    )

    @Test
    fun `findAll returns all seeded drugs`() = runBlocking {
        val result = assertIs<AppResult.Success<List<*>>>(repository.findAll())
        assertEquals(120, result.value.size)
    }

    @Test
    fun `findByPublicId returns nested JSONB document`() = runBlocking {
        val result = assertIs<AppResult.Success<*>>(repository.findByPublicId("drug_0001"))
        val drug = assertIs<io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug>(result.value)
        assertEquals("drug_0001", drug.id)
        assertTrue(drug.composition.activeIngredient.isNotBlank())
        assertTrue(
            drug.adverseReactions.serious.isNotEmpty() || drug.adverseReactions.other.frequencyUnknown.isNotEmpty()
        )
    }

    @Test
    fun `findByPublicId returns NotFound for missing drug`() = runBlocking {
        val result = assertIs<AppResult.Failure>(repository.findByPublicId("drug_9999"))
        assertEquals(DomainError.NotFound(resource = "drug", id = "drug_9999"), result.error)
    }
}
