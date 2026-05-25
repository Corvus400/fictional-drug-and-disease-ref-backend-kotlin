package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested

import kotlinx.serialization.Serializable

/**
 * 薬効薬理情報 — 添付文書 18 項。作用機序 (mechanism) と薬効 (effect) の Markdown 段落のペア。
 *
 * `Drug.pharmacology` で使用 (任意)。詳細画面 D15 補助情報折り畳みの一部。
 * 仕様: linked-bubbling-sun-drug.md `PharmacologyInfo` 節。
 */
@Serializable
data class PharmacologyInfo(
    val mechanism: String,
    val effect: String,
)
