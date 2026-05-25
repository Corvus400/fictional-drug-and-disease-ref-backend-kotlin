package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.ExposedDrugRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.DrugListResponse
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.DosageForm
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DrugListQueryServiceTest {
    private val service = DrugListQueryService(
        ExposedDrugRepository(
            database = PostgresTestSupport.database,
            databaseDispatcher = PostgresTestSupport.databaseDispatcher,
        ),
    )

    @Test
    fun `default list uses mock revised_at descending order and pagination`() = runBlocking {
        val response = list(query = DrugListQuery())
        assertEquals(120, response.totalCount)
        assertEquals(6, response.totalPages)
        assertEquals(20, response.items.size)
        assertEquals("drug_0080", response.items.first().id)
    }

    @Test
    fun `dosage form filter keeps only matching drugs`() = runBlocking {
        val response = list(query = DrugListQuery(dosageForms = listOf(DosageForm.TABLET)), pageSize = 100)
        assertTrue(response.items.isNotEmpty())
        assertTrue(response.items.all { it.dosageForm == DosageForm.TABLET })
    }

    @Test
    fun `keyword search applies kana normalization`() = runBlocking {
        val response = list(query = DrugListQuery(keyword = "らいしょうねくえ"), pageSize = 100)
        assertTrue(response.items.any { it.id == "drug_0090" })
    }

    private suspend fun list(
        query: DrugListQuery,
        sort: DrugSortKey = DrugSortKey.REVISED_AT_DESC,
        page: Int = 1,
        pageSize: Int = SearchDefaults.DEFAULT_PAGE_SIZE,
    ): DrugListResponse {
        val result = assertIs<AppResult.Success<DrugListResponse>>(
            service.list(query = query, sort = sort, page = page, pageSize = pageSize),
        )
        return result.value
    }
}
