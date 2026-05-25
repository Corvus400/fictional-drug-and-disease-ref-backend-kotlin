package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.DosageForm
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RegulatoryClass
import kotlinx.serialization.Serializable

/**
 * 医薬品一覧サマリ — `/drugs` 一覧で 1 件分のカード表示に必要な最小フィールドのみ保持する軽量モデル。
 *
 * `Drug.toSummary()` 拡張関数で `Drug` から派生。詳細画面で必要な完全なフィールド集合は `Drug` を参照。
 */
@Serializable
data class DrugSummary(
    val id: String,
    val brandName: String,
    val genericName: String,
    val therapeuticCategoryName: String,
    val regulatoryClass: List<RegulatoryClass>,
    val dosageForm: DosageForm,
    val brandNameKana: String,
    val atcCode: String,
    val revisedAt: String,
    val imageUrl: String = buildDrugImageUrl(id, dosageForm),
)

fun Drug.toSummary(): DrugSummary =
    DrugSummary(
        id = id,
        brandName = brandName,
        genericName = genericName,
        therapeuticCategoryName = therapeuticCategoryName,
        regulatoryClass = regulatoryClass,
        dosageForm = dosageForm,
        brandNameKana = brandNameKana,
        atcCode = atcCode,
        revisedAt = revisedAt,
        imageUrl = imageUrl,
    )
