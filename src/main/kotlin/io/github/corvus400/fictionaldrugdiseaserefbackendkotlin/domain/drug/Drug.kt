package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.Disclaimer
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.DosageForm
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RegulatoryClass
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RouteOfAdministration
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.AdverseReactionInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.ClinicalResultSection
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.CompositionInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.DosageInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.IndicationItem
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.InteractionInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.NumberedParagraph
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.OverdoseInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.PackageInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.PharmacokineticsInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.PharmacologyInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.PhysicochemicalInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.PrecautionPopulation
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.Reference
import kotlinx.serialization.Serializable

/**
 * 医薬品エンティティ — 添付文書「新記載要領 26 項目 (令和5年改正)」に準拠した架空医薬品 1 件分のフルモデル。
 *
 * 詳細画面 (D1〜D19 ブロック) のソース。`regulatoryClass` / `dosageForm` / `routeOfAdministration` / ATC 第 1 階層を
 * 分類軸として、UI ブロック発火条件と一覧フィルタを決定する。
 * 仕様: linked-bubbling-sun-drug.md `共通フィールド` 節。
 */
@Serializable
data class Drug(
    val id: String,
    val genericName: String,
    val brandName: String,
    val brandNameKana: String,
    /** 解剖治療化学分類コード (ATC) — WHO の薬剤分類体系。第 1 階層は 14 群 (A-V) を表す英字 1 文字。 */
    val atcCode: String,
    /** 日本標準商品分類番号 (YJ コード) — 厚労省の医薬品コード体系。架空モデルでは 12 桁の数字文字列。 */
    val yjCode: String? = null,
    val therapeuticCategoryName: String,
    val regulatoryClass: List<RegulatoryClass>,
    val dosageForm: DosageForm,
    val routeOfAdministration: RouteOfAdministration,
    val composition: CompositionInfo,
    val warning: List<NumberedParagraph> = emptyList(),
    val contraindications: List<NumberedParagraph>,
    val indications: List<IndicationItem>,
    val indicationsRelatedPrecautions: List<NumberedParagraph> = emptyList(),
    val dosage: DosageInfo,
    val dosageRelatedPrecautions: List<NumberedParagraph> = emptyList(),
    val importantPrecautions: List<NumberedParagraph> = emptyList(),
    val precautionsForSpecificPopulations: List<PrecautionPopulation> = emptyList(),
    val interactions: InteractionInfo? = null,
    val adverseReactions: AdverseReactionInfo,
    val effectsOnLabTests: List<NumberedParagraph> = emptyList(),
    val overdose: OverdoseInfo? = null,
    val administrationPrecautions: List<NumberedParagraph> = emptyList(),
    val otherPrecautions: List<NumberedParagraph> = emptyList(),
    val pharmacokinetics: PharmacokineticsInfo? = null,
    val clinicalResults: List<ClinicalResultSection> = emptyList(),
    val pharmacology: PharmacologyInfo? = null,
    val physicochemicalProperties: PhysicochemicalInfo? = null,
    val handlingPrecautions: List<NumberedParagraph> = emptyList(),
    val approvalConditions: List<NumberedParagraph> = emptyList(),
    val packages: List<PackageInfo>,
    val references: List<Reference> = emptyList(),
    val insuranceNotes: List<NumberedParagraph> = emptyList(),
    val manufacturer: String,
    /** 添付文書改訂年月 — `YYYY-MM-DD` 形式 (日は `01` 固定)。例 `"2024-03-01"` = 2024 年 3 月改訂。 */
    val revisedAt: String,
    /** 関連疾患 ID リスト — `disease_NNNN` 形式。ダングリング参照禁止 (仕様: linked-bubbling-sun.md 共通モデル節)。 */
    val relatedDiseaseIds: List<String> = emptyList(),
    val imageUrl: String = buildDrugImageUrl(id, dosageForm),
    val disclaimer: String = Disclaimer.SHORT,
)
