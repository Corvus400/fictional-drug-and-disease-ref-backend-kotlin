package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested

import kotlinx.serialization.Serializable

/**
 * 主要文献 1 件 — 添付文書 23 項。架空の文献記述 (citation) と出典誌名 (任意の source) のペア。
 *
 * `Drug.references` (`List<Reference>`) で使用。詳細画面 D17 ブロックの一部。
 * 仕様: linked-bubbling-sun-drug.md `Reference` 節。
 */
@Serializable
data class Reference(
    val citation: String,
    val source: String? = null,
)
