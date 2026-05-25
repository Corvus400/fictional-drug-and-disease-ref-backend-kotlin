package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested

import kotlinx.serialization.Serializable

/**
 * 番号付き段落 — 警告/禁忌/重要な基本的注意/取扱い上の注意など、番号付きで列挙される全フィールドで共通使用される汎用型。
 *
 * フィールドごとに添付文書項番が確定しているため、`order` / `subOrder` はフィールド内ローカルな連番として扱う。
 * クライアントは `"{親項番}.${order}."` (例: warning なら `"1.1."`) または `subOrder` 非 null 時 `"{親項番}.${order}.${subOrder}."`
 * の形でレンダリングするため、`content` 内に番号を書かない。
 * 仕様: linked-bubbling-sun-drug.md `NumberedParagraph` 節。
 */
@Serializable
data class NumberedParagraph(
    /** 表示順 (フィールド内ローカルな 1, 2, 3 ...)。 */
    val order: Int,
    /** サブ番号 (例: "1.1." の 2 階層目)。`null` のときフラット表示。 */
    val subOrder: Int? = null,
    val content: String,
)
