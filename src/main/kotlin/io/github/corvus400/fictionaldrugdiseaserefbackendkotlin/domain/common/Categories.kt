package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common

import kotlinx.serialization.Serializable

@Serializable
data class AtcEntry(
    val code: String,
    val label: String,
)

@Serializable
data class TherapeuticCategoryEntry(
    val id: String,
    val label: String,
)

@Serializable
data class Icd10ChapterEntry(
    val roman: String,
    val code: String,
    val label: String,
)

@Serializable
data class CategoriesResponse(
    val atc: List<AtcEntry>,
    val therapeuticCategories: List<TherapeuticCategoryEntry>,
    val routeOfAdministration: List<String>,
    val dosageForm: List<String>,
    val regulatoryClass: List<String>,
    val icd10Chapters: List<Icd10ChapterEntry>,
    val medicalDepartments: List<String>,
)
