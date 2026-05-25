package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.DrugRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.DrugListResponse
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.toSummary
import kotlin.math.ceil

class DrugListQueryService(private val repository: DrugRepository) {
    suspend fun list(
        query: DrugListQuery,
        sort: DrugSortKey,
        page: Int,
        pageSize: Int,
    ): AppResult<DrugListResponse> =
        when (val result = repository.findAll()) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val filtered = applyFilters(drugs = result.value, query = query)
                val sorted = DrugSearchService.applySort(items = filtered, sort = sort)
                AppResult.Success(
                    toResponse(
                        drugs = sorted,
                        page = page,
                        pageSize = pageSize,
                    ),
                )
            }
        }

    private fun applyFilters(drugs: List<Drug>, query: DrugListQuery): List<Drug> {
        var filtered = drugs
        val atcPrefix = query.atcPrefix
        if (atcPrefix != null) {
            filtered = filtered.filter { drug ->
                drug.atcCode.startsWith(prefix = atcPrefix)
            }
        }
        if (query.regulatoryClasses.isNotEmpty()) {
            filtered = filtered.filter { drug ->
                drug.regulatoryClass.any { it in query.regulatoryClasses }
            }
        }
        if (query.routes.isNotEmpty()) {
            filtered = filtered.filter { drug ->
                drug.routeOfAdministration in query.routes
            }
        }
        if (query.dosageForms.isNotEmpty()) {
            filtered = filtered.filter { drug ->
                drug.dosageForm in query.dosageForms
            }
        }
        val therapeuticCategory = query.therapeuticCategory
        if (therapeuticCategory != null) {
            filtered = filtered.filter { drug ->
                drug.therapeuticCategoryName == therapeuticCategory.displayName
            }
        }
        val keyword = query.keyword
        if (!keyword.isNullOrBlank()) {
            filtered = DrugSearchService.applyKeyword(
                items = filtered,
                keyword = keyword,
                match = query.keywordMatch,
                target = query.keywordTarget,
            )
        }
        val adverseReactionKeyword = query.adverseReactionKeyword
        val precautionCategories = query.precautionCategories
        if (!adverseReactionKeyword.isNullOrBlank() || precautionCategories.isNotEmpty()) {
            filtered = DrugSearchService.applyAdditionalFilters(
                items = filtered,
                adverseReactionKeyword = adverseReactionKeyword,
                precautionCategories = precautionCategories,
            )
        }
        return filtered
    }

    private fun toResponse(drugs: List<Drug>, page: Int, pageSize: Int): DrugListResponse {
        val totalCount = drugs.size
        val totalPages = if (totalCount == 0) 0 else ceil(totalCount.toDouble() / pageSize.toDouble()).toInt()
        val startIndex = (page - 1) * pageSize
        val items = if (startIndex >= totalCount) {
            emptyList()
        } else {
            drugs.drop(n = startIndex).take(n = pageSize).map { drug -> drug.toSummary() }
        }
        return DrugListResponse(
            items = items,
            page = page,
            pageSize = pageSize,
            totalPages = totalPages,
            totalCount = totalCount,
        )
    }
}
