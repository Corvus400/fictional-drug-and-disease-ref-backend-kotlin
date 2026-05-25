package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.DrugRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AtcEntry
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.CategoriesResponse
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.Icd10ChapterEntry
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.TherapeuticCategoryEntry
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.Icd10Chapter
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.MedicalDepartment
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.DosageForm
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RegulatoryClass
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RouteOfAdministration
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.TherapeuticCategory
import kotlinx.serialization.serializer

class CategoriesQueryService(private val drugRepository: DrugRepository) {
    suspend fun build(): AppResult<CategoriesResponse> =
        when (val result = drugRepository.findAll()) {
            is AppResult.Failure -> result
            is AppResult.Success -> AppResult.Success(buildResponse(result.value))
        }

    private fun buildResponse(drugs: List<Drug>): CategoriesResponse = CategoriesResponse(
        atc = TherapeuticCategory.entries.map { category ->
            AtcEntry(code = category.atcInitial.toString(), label = category.displayName)
        },
        therapeuticCategories = drugs.mapNotNull { drug ->
            TherapeuticCategory.fromDisplayName(displayName = drug.therapeuticCategoryName)?.let { category ->
                TherapeuticCategoryEntry(id = category.categoryId, label = category.displayName)
            }
        }.distinctBy { entry -> entry.id },
        routeOfAdministration = enumSerialNames<RouteOfAdministration>(),
        dosageForm = enumSerialNames<DosageForm>(),
        regulatoryClass = enumSerialNames<RegulatoryClass>(),
        icd10Chapters = buildIcd10Chapters(),
        medicalDepartments = enumSerialNames<MedicalDepartment>(),
    )

    private fun buildIcd10Chapters(): List<Icd10ChapterEntry> = Icd10Chapter.entries.map { chapter ->
        Icd10ChapterEntry(
            roman = chapter.chapterKey,
            code = chapter.codeRange,
            label = ICD10_CHAPTER_LABELS_JA.getValue(key = chapter),
        )
    }

    private inline fun <reified T : Enum<T>> enumSerialNames(): List<String> {
        val descriptor = serializer<T>().descriptor
        return enumValues<T>().map { entry ->
            descriptor.getElementName(index = entry.ordinal)
        }
    }

    companion object {
        private val ICD10_CHAPTER_LABELS_JA: Map<Icd10Chapter, String> = mapOf(
            Icd10Chapter.CHAPTER_I to "感染症および寄生虫症",
            Icd10Chapter.CHAPTER_II to "新生物",
            Icd10Chapter.CHAPTER_III to "血液および造血器の疾患ならびに免疫機構の障害",
            Icd10Chapter.CHAPTER_IV to "内分泌、栄養および代謝疾患",
            Icd10Chapter.CHAPTER_V to "精神および行動の障害",
            Icd10Chapter.CHAPTER_VI to "神経系の疾患",
            Icd10Chapter.CHAPTER_VII to "眼および付属器の疾患",
            Icd10Chapter.CHAPTER_VIII to "耳および乳様突起の疾患",
            Icd10Chapter.CHAPTER_IX to "循環器系の疾患",
            Icd10Chapter.CHAPTER_X to "呼吸器系の疾患",
            Icd10Chapter.CHAPTER_XI to "消化器系の疾患",
            Icd10Chapter.CHAPTER_XII to "皮膚および皮下組織の疾患",
            Icd10Chapter.CHAPTER_XIII to "筋骨格系および結合組織の疾患",
            Icd10Chapter.CHAPTER_XIV to "腎尿路生殖器系の疾患",
            Icd10Chapter.CHAPTER_XV to "妊娠、分娩および産褥",
            Icd10Chapter.CHAPTER_XVI to "周産期に発生した病態",
            Icd10Chapter.CHAPTER_XVII to "先天奇形、変形および染色体異常",
            Icd10Chapter.CHAPTER_XVIII to "症状、徴候および異常臨床所見・異常検査所見で他に分類されないもの",
            Icd10Chapter.CHAPTER_XIX to "損傷、中毒およびその他の外因の影響",
            Icd10Chapter.CHAPTER_XX to "傷病および死亡の外因",
            Icd10Chapter.CHAPTER_XXI to "健康状態に影響を及ぼす要因および保健サービスの利用",
            Icd10Chapter.CHAPTER_XXII to "特殊目的用コード",
        )
    }
}
