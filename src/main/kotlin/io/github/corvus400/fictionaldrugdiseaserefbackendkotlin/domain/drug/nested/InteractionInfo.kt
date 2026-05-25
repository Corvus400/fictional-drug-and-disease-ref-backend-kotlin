package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested

import kotlinx.serialization.Serializable

/**
 * 相互作用情報 — 添付文書 10 項。併用禁忌と併用注意を 2 つのリストに分類保持する。
 *
 * `Drug.interactions` で使用 (任意、`null` のとき D11 ブロック非表示)。
 * 仕様: linked-bubbling-sun-drug.md `InteractionInfo` 節。
 */
@Serializable
data class InteractionInfo(
    val combinationProhibited: List<InteractionEntry> = emptyList(),
    val combinationCaution: List<InteractionEntry> = emptyList(),
)

/**
 * 相互作用エントリ 1 件 — 相手薬剤と臨床症状・機序の組。
 *
 * `drugId` を指定する場合は実在する内部 Drug の id を参照しダングリング参照を禁止 (Fixture 作成ルール)。
 * カテゴリ表記 (例: "他の NSAIDs") は `displayName` のみ指定して `drugId` を `null` にする。
 */
@Serializable
data class InteractionEntry(
    /** 内部 Drug の id (`drug_NNNN`)。具体薬品参照時に指定。`null` のときカテゴリ表記とみなす。 */
    val drugId: String? = null,
    /** 画面表示名。`drugId` 未指定でも必ず指定する。 */
    val displayName: String,
    val clinicalSymptom: String,
    val mechanism: String,
)
