package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested

import kotlinx.serialization.Serializable

/**
 * 過量投与情報 — 添付文書 13 項。過量投与時の臨床症状と処置 (対症療法等) のペア。
 *
 * `Drug.overdose` で使用 (任意)。詳細画面 D15 補助情報折り畳みの一部。
 * 仕様: linked-bubbling-sun-drug.md `OverdoseInfo` 節。
 */
@Serializable
data class OverdoseInfo(
    val symptoms: String,
    val management: String,
)
