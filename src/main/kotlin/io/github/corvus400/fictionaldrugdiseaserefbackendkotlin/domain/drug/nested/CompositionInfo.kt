package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.DoseUnit
import kotlinx.serialization.Serializable

/**
 * 組成・性状情報 — 添付文書 3 項。有効成分・添加物・外観・識別コードを保持する。
 *
 * `Drug.composition` で使用。詳細画面 D5 ブロックのソース。
 * 仕様: linked-bubbling-sun-drug.md `CompositionInfo` 節。
 */
@Serializable
data class CompositionInfo(
    val activeIngredient: String,
    val activeIngredientAmount: Dose,
    val inactiveIngredients: List<String> = emptyList(),
    val appearance: String,
    val identificationCode: String? = null,
)

/**
 * 含量量目 — 数値・単位・分母を持つ値オブジェクト (例: "100 mg / 1 錠中" = `Dose(100.0, MG, "1 錠中")`)。
 *
 * `CompositionInfo.activeIngredientAmount` で使用。
 */
@Serializable
data class Dose(
    /** 数値部。型は `Double` (整数値も `100.0` のように表現)。 */
    val amount: Double,
    /** 単位 ([DoseUnit] の enum 値)。 */
    val unit: DoseUnit,
    /** 分母表現 (例: "1 錠中"、"1 mL 中")。`null` のとき絶対量を表す。 */
    val per: String? = null,
)
