package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.ExposedDiseaseRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.DiseaseListResponse
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.Icd10Chapter
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DiseaseListQueryServiceTest {
    private val service = DiseaseListQueryService(ExposedDiseaseRepository(PostgresTestSupport.database))

    @Test
    fun `default list uses mock revised_at descending order and pagination`() = runBlocking {
        val response = list(query = DiseaseListQuery())
        assertEquals(80, response.totalCount)
        assertEquals(4, response.totalPages)
        assertEquals(20, response.items.size)
        assertEquals("disease_0000", response.items.first().id)
    }

    @Test
    fun `icd10 and infectious filters are applied before pagination`() = runBlocking {
        val response = list(
            query = DiseaseListQuery(
                icd10Chapters = listOf(Icd10Chapter.CHAPTER_I),
                infectious = true,
            ),
            pageSize = 100,
        )
        assertTrue(response.items.isNotEmpty())
        assertTrue(response.items.all { it.icd10Chapter == Icd10Chapter.CHAPTER_I && it.infectious })
    }

    @Test
    fun `keyword search applies kana normalization`() = runBlocking {
        val response = list(query = DiseaseListQuery(keyword = "らめるくい"), pageSize = 100)
        assertTrue(response.items.any { it.id == "disease_0001" })
    }

    private suspend fun list(
        query: DiseaseListQuery,
        sort: DiseaseSortKey = DiseaseSortKey.REVISED_AT_DESC,
        page: Int = 1,
        pageSize: Int = SearchDefaults.DEFAULT_PAGE_SIZE,
    ): DiseaseListResponse {
        val result = assertIs<AppResult.Success<DiseaseListResponse>>(
            service.list(query = query, sort = sort, page = page, pageSize = pageSize),
        )
        return result.value
    }
}
