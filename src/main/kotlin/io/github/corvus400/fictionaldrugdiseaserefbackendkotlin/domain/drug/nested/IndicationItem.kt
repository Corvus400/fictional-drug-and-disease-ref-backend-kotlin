package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested

import kotlinx.serialization.Serializable

/**
 * 効能又は効果 1 件 — 添付文書 4 項。クライアント側で `1)` `2)` 形式 (半角閉じ括弧) にレンダリングされる前提。
 *
 * `Drug.indications` (`List<IndicationItem>`、最低 1 要素) で使用。
 * 仕様: linked-bubbling-sun-drug.md `IndicationItem` 節。
 */
@Serializable
data class IndicationItem(
    /** 表示順 (1, 2, 3 ...)。クライアントは `"${order})"` 形式でレンダリングする。 */
    val order: Int,
    val content: String,
)
