package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.Chronicity
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.ExamCategory
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.Icd10Chapter
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.MedicalDepartment
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.OnsetPattern

data class DiseaseListQuery(
    val icd10Chapters: List<Icd10Chapter> = emptyList(),
    val departments: List<MedicalDepartment> = emptyList(),
    val chronicities: List<Chronicity> = emptyList(),
    val infectious: Boolean? = null,
    val keyword: String? = null,
    val keywordMatch: KeywordMatch = KeywordMatch.PARTIAL,
    val keywordTarget: DiseaseKeywordTarget = DiseaseKeywordTarget.NAME,
    val symptomKeyword: String? = null,
    val onsetPatterns: List<OnsetPattern> = emptyList(),
    val examCategories: List<ExamCategory> = emptyList(),
    val hasPharmacologicalTreatment: Boolean? = null,
    val hasSeverityGrading: Boolean? = null,
)
