package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.DiseaseRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.Disease
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.DiseaseListResponse
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.toSummary
import kotlin.math.ceil

class DiseaseListQueryService(private val repository: DiseaseRepository) {
    suspend fun list(
        query: DiseaseListQuery,
        sort: DiseaseSortKey,
        page: Int,
        pageSize: Int,
    ): AppResult<DiseaseListResponse> =
        when (val result = repository.findAll()) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val filtered = applyDiseaseListFilters(diseases = result.value, query = query)
                val keywordFiltered = DiseaseSearchService.applyKeyword(
                    items = filtered,
                    keyword = query.keyword,
                    match = query.keywordMatch,
                    target = query.keywordTarget,
                )
                val additionallyFiltered = DiseaseSearchService.applyAdditionalFilters(
                    items = keywordFiltered,
                    symptomKeyword = query.symptomKeyword,
                    onsetPatterns = query.onsetPatterns,
                    examCategories = query.examCategories,
                    hasPharmacologicalTreatment = query.hasPharmacologicalTreatment,
                    hasSeverityGrading = query.hasSeverityGrading,
                )
                val sorted = DiseaseSearchService.applySort(items = additionallyFiltered, sort = sort)
                AppResult.Success(toResponse(diseases = sorted, page = page, pageSize = pageSize))
            }
        }

    private fun applyDiseaseListFilters(diseases: List<Disease>, query: DiseaseListQuery): List<Disease> {
        var result = diseases
        if (query.icd10Chapters.isNotEmpty()) {
            result = result.filter { it.icd10Chapter in query.icd10Chapters }
        }
        if (query.departments.isNotEmpty()) {
            result = result.filter { disease -> disease.medicalDepartment.any { it in query.departments } }
        }
        if (query.chronicities.isNotEmpty()) {
            result = result.filter { it.chronicity in query.chronicities }
        }
        val infectious = query.infectious
        if (infectious != null) {
            result = result.filter { it.infectious == infectious }
        }
        return result
    }

    private fun toResponse(diseases: List<Disease>, page: Int, pageSize: Int): DiseaseListResponse {
        val totalCount = diseases.size
        val totalPages = if (totalCount == 0) 0 else ceil(totalCount.toDouble() / pageSize.toDouble()).toInt()
        val startIndex = (page - 1) * pageSize
        val items = if (startIndex >= totalCount) {
            emptyList()
        } else {
            diseases.drop(n = startIndex).take(n = pageSize).map { disease -> disease.toSummary() }
        }
        return DiseaseListResponse(
            items = items,
            page = page,
            pageSize = pageSize,
            totalPages = totalPages,
            totalCount = totalCount,
        )
    }
}
