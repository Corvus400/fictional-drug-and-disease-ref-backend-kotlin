package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.drug

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.FieldViolation
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.DosageForm
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.PrecautionPopulationCategory
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RegulatoryClass
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RouteOfAdministration
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.TherapeuticCategory
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.DrugKeywordTarget
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.DrugListQuery
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.DrugSortKey
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.KeywordMatch
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.SearchDefaults
import io.ktor.server.application.ApplicationCall

data class DrugListParams(
    val query: DrugListQuery,
    val sort: DrugSortKey,
    val page: Int,
    val pageSize: Int,
)

fun ApplicationCall.parseDrugListParams(): AppResult<DrugListParams> {
    val violations = mutableListOf<FieldViolation>()
    val page = request.queryParameters["page"]?.toIntOrNull() ?: 1
    val pageSize = (request.queryParameters["page_size"]?.toIntOrNull() ?: SearchDefaults.DEFAULT_PAGE_SIZE)
        .coerceAtMost(maximumValue = SearchDefaults.MAX_PAGE_SIZE)
    val query = DrugListQuery(
        atcPrefix = request.queryParameters["category_atc"],
        regulatoryClasses = request.queryParameters.enumValues(
            field = "regulatory_class",
            violations = violations,
            matcher = { raw -> RegulatoryClass.entries.firstOrNull { it.serialName == raw } },
        ),
        routes = request.queryParameters.enumValues(
            field = "route",
            violations = violations,
            matcher = { raw -> RouteOfAdministration.entries.firstOrNull { it.serialName == raw } },
        ),
        dosageForms = request.queryParameters.enumValues(
            field = "dosage_form",
            violations = violations,
            matcher = { raw -> DosageForm.entries.firstOrNull { it.serialName == raw } },
        ),
        therapeuticCategory = request.queryParameters.optionalThrowingEnum(
            field = "therapeutic_category",
            violations = violations,
            resolver = TherapeuticCategory::fromQueryOrThrow,
        ),
        keyword = request.queryParameters["keyword"],
        keywordMatch = KeywordMatch.fromQuery(request.queryParameters["keyword_match"]),
        keywordTarget = DrugKeywordTarget.fromQuery(request.queryParameters["keyword_target"]),
        adverseReactionKeyword = request.queryParameters["adverse_reaction_keyword"],
        precautionCategories = request.queryParameters.throwingEnumValues(
            field = "precaution_category",
            violations = violations,
            resolver = PrecautionPopulationCategory::fromQueryOrThrow,
        ),
    )
    val sort = request.queryParameters.optionalThrowingEnum(
        field = "sort",
        violations = violations,
        resolver = DrugSortKey::fromQuery,
    ) ?: DrugSortKey.fromQuery(null)

    return if (violations.isEmpty()) {
        AppResult.Success(DrugListParams(query = query, sort = sort, page = page, pageSize = pageSize))
    } else {
        AppResult.Failure(DomainError.Validation(violations))
    }
}

private fun <T> io.ktor.http.Parameters.enumValues(
    field: String,
    violations: MutableList<FieldViolation>,
    matcher: (String) -> T?,
): List<T> =
    getAll(name = field).orEmpty().mapNotNull { raw ->
        matcher(raw) ?: run {
            violations += FieldViolation(field = field, reason = "Unknown $field: $raw")
            null
        }
    }

private fun <T> io.ktor.http.Parameters.throwingEnumValues(
    field: String,
    violations: MutableList<FieldViolation>,
    resolver: (String) -> T,
): List<T> =
    getAll(name = field).orEmpty().mapNotNull { raw ->
        runCatching { resolver(raw) }
            .onFailure { error ->
                violations += FieldViolation(field = field, reason = error.message ?: "Unknown $field: $raw")
            }
            .getOrNull()
    }

private fun <T> io.ktor.http.Parameters.optionalThrowingEnum(
    field: String,
    violations: MutableList<FieldViolation>,
    resolver: (String) -> T,
): T? {
    val raw = this[field] ?: return null
    return runCatching { resolver(raw) }
        .onFailure { error ->
            violations += FieldViolation(field = field, reason = error.message ?: "Unknown $field: $raw")
        }
        .getOrNull()
}
