package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.disease

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.FieldViolation
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.Chronicity
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.ExamCategory
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.Icd10Chapter
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.MedicalDepartment
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.OnsetPattern
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.DiseaseKeywordTarget
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.DiseaseListQuery
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.DiseaseSortKey
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.KeywordMatch
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query.SearchDefaults
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall

data class DiseaseListParams(
    val query: DiseaseListQuery,
    val sort: DiseaseSortKey,
    val page: Int,
    val pageSize: Int,
)

fun ApplicationCall.parseDiseaseListParams(): AppResult<DiseaseListParams> {
    val violations = mutableListOf<FieldViolation>()
    val page = request.queryParameters["page"]?.toIntOrNull() ?: 1
    val pageSize = (request.queryParameters["page_size"]?.toIntOrNull() ?: SearchDefaults.DEFAULT_PAGE_SIZE)
        .coerceAtMost(maximumValue = SearchDefaults.MAX_PAGE_SIZE)
    val query = DiseaseListQuery(
        icd10Chapters = request.queryParameters.enumValues(
            field = "icd10_chapter",
            violations = violations,
            matcher = Icd10Chapter::fromSerialName,
        ),
        departments = request.queryParameters.enumValues(
            field = "department",
            violations = violations,
            matcher = MedicalDepartment::fromSerialName,
        ),
        chronicities = request.queryParameters.enumValues(
            field = "chronicity",
            violations = violations,
            matcher = { raw -> Chronicity.entries.firstOrNull { it.serialName == raw } },
        ),
        infectious = request.queryParameters.booleanValue(field = "infectious", violations = violations),
        keyword = request.queryParameters["keyword"],
        keywordMatch = KeywordMatch.fromQuery(request.queryParameters["keyword_match"]),
        keywordTarget = DiseaseKeywordTarget.fromQuery(request.queryParameters["keyword_target"]),
        symptomKeyword = request.queryParameters["symptom_keyword"],
        onsetPatterns = request.queryParameters.throwingEnumValues(
            field = "onset_pattern",
            violations = violations,
            resolver = OnsetPattern::fromQueryOrThrow,
        ),
        examCategories = request.queryParameters.throwingEnumValues(
            field = "exam_category",
            violations = violations,
            resolver = ExamCategory::fromQueryOrThrow,
        ),
        hasPharmacologicalTreatment = request.queryParameters.booleanValue(
            field = "has_pharmacological_treatment",
            violations = violations,
        ),
        hasSeverityGrading = request.queryParameters.booleanValue(
            field = "has_severity_grading",
            violations = violations,
        ),
    )
    val sort = request.queryParameters.optionalThrowingEnum(
        field = "sort",
        violations = violations,
        resolver = DiseaseSortKey::fromQuery,
    ) ?: DiseaseSortKey.fromQuery(null)

    return if (violations.isEmpty()) {
        AppResult.Success(DiseaseListParams(query = query, sort = sort, page = page, pageSize = pageSize))
    } else {
        AppResult.Failure(DomainError.Validation(violations))
    }
}

private fun Parameters.booleanValue(
    field: String,
    violations: MutableList<FieldViolation>,
): Boolean? {
    val raw = this[field] ?: return null
    val parsed = raw.toBooleanStrictOrNull()
    if (parsed == null) {
        violations += FieldViolation(field = field, reason = "Invalid boolean $field: $raw")
    }
    return parsed
}

private fun <T> Parameters.enumValues(
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

private fun <T> Parameters.throwingEnumValues(
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

private fun <T> Parameters.optionalThrowingEnum(
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
