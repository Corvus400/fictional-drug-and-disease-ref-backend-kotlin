package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.PrecautionPopulationCategory
import kotlinx.serialization.Serializable

/**
 * 特定背景患者注意 1 件 — 添付文書 9 項の 1 小区分とその注意文 (常体 1〜3 文の Markdown)。
 *
 * `Drug.precautionsForSpecificPopulations` (`List<PrecautionPopulation>`) で使用。詳細画面 D10 ブロック (折り畳み)。
 * 仕様: linked-bubbling-sun-drug.md `PrecautionPopulation` 節。
 */
@Serializable
data class PrecautionPopulation(
    val category: PrecautionPopulationCategory,
    val note: String,
)
