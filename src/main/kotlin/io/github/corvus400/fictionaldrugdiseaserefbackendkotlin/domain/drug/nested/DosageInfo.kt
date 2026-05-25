package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.HepaticSeverity
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RenalSeverity
import kotlinx.serialization.Serializable

/**
 * 用法及び用量情報 — 添付文書 6 項。標準用法と年齢別/腎機能別/肝機能別の用量調整リストを階層化した型。
 *
 * `Drug.dosage` で使用。詳細画面 D7/D8 ブロックのソース。各 List が非空のとき該当タブが表示される。
 * 仕様: linked-bubbling-sun-drug.md `DosageInfo` 節。
 */
@Serializable
data class DosageInfo(
    val standardDosage: String,
    val ageSpecificDosage: List<AgeDosage> = emptyList(),
    val renalAdjustment: List<RenalDose> = emptyList(),
    val hepaticAdjustment: List<HepaticDose> = emptyList(),
)

/**
 * 年齢別用量 — 月齢範囲と、その範囲での用量指示文 1 件分のペア。
 */
@Serializable
data class AgeDosage(
    val range: AgeRange,
    val dose: String,
)

/**
 * 年齢範囲 — 月齢の上下限で表現される対象年齢区間。
 */
@Serializable
data class AgeRange(
    /** 月齢下限。`null` のとき下限なし (新生児を含む全年齢が対象)。 */
    val minAgeMonths: Int?,
    /** 月齢上限。`null` のとき上限なし。 */
    val maxAgeMonths: Int?,
    /** 表示用ラベル (例: "6 歳以上 12 歳未満")。クライアントは数値範囲ではなく label を優先表示する。 */
    val label: String,
)

/**
 * 腎機能別用量 — クレアチニンクリアランス (CrCl) 範囲と、その範囲での用量指示文 1 件分のペア。
 */
@Serializable
data class RenalDose(
    val range: CrClRange,
    val dose: String,
)

/**
 * クレアチニンクリアランス範囲 — CrCl 値の区間と対応する腎機能重症度。
 */
@Serializable
data class CrClRange(
    /** CrCl 下限 (mL/min)。`null` のとき下限なし (透析患者など)。 */
    val minMlPerMin: Int?,
    /** CrCl 上限 (mL/min)。`null` のとき上限なし (正常)。 */
    val maxMlPerMin: Int?,
    val severity: RenalSeverity,
    /** 表示用ラベル (例: "30-59 mL/min、中等度腎機能低下")。クライアントは数値範囲ではなく label を優先表示する。 */
    val label: String,
)

/**
 * 肝機能別用量 — 肝機能重症度区分と、その重症度での用量指示文 1 件分のペア。
 */
@Serializable
data class HepaticDose(
    val severity: HepaticSeverity,
    val dose: String,
)
