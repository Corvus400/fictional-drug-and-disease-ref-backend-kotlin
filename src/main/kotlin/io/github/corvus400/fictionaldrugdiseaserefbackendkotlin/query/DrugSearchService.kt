package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.PrecautionPopulationCategory
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.allMatchableTexts

/**
 * 医薬品の検索ロジックを提供する純関数オブジェクト。
 *
 * 副作用なし・シナリオ非依存。シナリオ管理機構や Fixture 解決 API には依存せず、
 * 呼び出し側 (route 層) が解決済みの `items` を渡す。本オブジェクトは
 * クエリパラメータに従って絞り込みのみを行う。
 */
object DrugSearchService {
    /**
     * `keyword` が null/空白なら `items` を素通しする (no-op)。
     * 検索ロジック本体は後続フェーズで triangulation により段階的に追加する。
     */
    fun applyKeyword(
        items: List<Drug>,
        keyword: String?,
        match: KeywordMatch,
        target: DrugKeywordTarget,
    ): List<Drug> {
        if (keyword.isNullOrBlank()) {
            return items
        }
        return filterByKeyword(items = items, keyword = keyword, match = match, target = target)
    }

    private fun filterByKeyword(
        items: List<Drug>,
        keyword: String,
        match: KeywordMatch,
        target: DrugKeywordTarget,
    ): List<Drug> {
        val tokens = keyword.split(Regex("\\s+")).filter { it.isNotBlank() }
        return items.filter { drug ->
            val fields = fieldsFor(target = target, drug = drug)
            tokens.all { token ->
                fields.any { field ->
                    matches(field = field, keyword = token, match = match)
                }
            }
        }
    }

    /**
     * 検索対象 (`target`) に応じて、`drug` のうちキーワード照合に用いるフィールド集合を返す。
     * いずれか 1 フィールドが一致すれば該当扱い (OR 結合)。`BOTH` は generic / brand 系を全て含む。
     */
    private fun fieldsFor(
        target: DrugKeywordTarget,
        drug: Drug,
    ): List<String> = when (target) {
        DrugKeywordTarget.GENERIC -> listOf(drug.genericName)
        DrugKeywordTarget.BRAND -> listOf(drug.brandName, drug.brandNameKana)
        DrugKeywordTarget.BOTH -> listOf(drug.genericName, drug.brandName, drug.brandNameKana)
        DrugKeywordTarget.ALL -> listOfNotNull(
            drug.genericName,
            drug.brandName,
            drug.brandNameKana,
            drug.atcCode,
            drug.yjCode,
        )
    }

    /**
     * 単一フィールドに対するキーワード一致判定。`match` の差し替えで PREFIX 対応を後続 triangulation で
     * 段階導入できるよう抽出している。
     */
    private fun matches(
        field: String,
        keyword: String,
        match: KeywordMatch,
    ): Boolean {
        val normalizedField = searchNormalize(value = field)
        val normalizedKeyword = searchNormalize(value = keyword)
        return when (match) {
            KeywordMatch.PARTIAL -> normalizedField.contains(other = normalizedKeyword, ignoreCase = true)
            KeywordMatch.PREFIX -> normalizedField.startsWith(prefix = normalizedKeyword, ignoreCase = true)
        }
    }

    /**
     * `sort` キーに従って `items` を並び替えた新しいリストを返す純関数。
     * 副作用なし・シナリオ非依存。後続 triangulation で sort key 対応を段階的に拡張する。
     *
     * `revisedAt` が同値の場合、API 契約として `id` 降順で tie-break する (`drug_NNNN` の数値が
     * 大きいほど新しい fixture という運用と一致させ、`-revised_at` の「新しい順」を貫徹するため)。
     */
    fun applySort(
        items: List<Drug>,
        sort: DrugSortKey,
    ): List<Drug> = when (sort) {
        DrugSortKey.REVISED_AT_DESC -> items.sortedWith(
            compareByDescending<Drug> { it.revisedAt }.thenByDescending { it.id },
        )
        DrugSortKey.BRAND_NAME_KANA_ASC -> items.sortedBy { it.brandNameKana }
        DrugSortKey.ATC_CODE_ASC -> items.sortedBy { it.atcCode }
        DrugSortKey.THERAPEUTIC_CATEGORY_NAME_ASC -> items.sortedBy { it.therapeuticCategoryName }
    }

    /**
     * 追加フィルタ — `keyword` / `sort` 後段に適用される副次的な絞り込み群。
     *
     * `adverseReactionKeyword` が非 null/非 blank の場合、`adverseReactions.serious[].name`
     * および `adverseReactions.other.over5Percent[]` / `between1And5Percent[]` /
     * `under1Percent[]` / `frequencyUnknown[]` を 1 語の部分一致で絞り込む。複数語 AND は
     * 本フェーズではスコープ外 (YAGNI)。
     *
     * `precautionCategories` が非空の場合、`precautionsForSpecificPopulations[].category`
     * のいずれかが指定カテゴリ集合に含まれる医薬品のみを残す (OR 結合)。クエリ層で 0/1 個に
     * 解決される単値フィルタ前提だが、本層では集合扱いにして将来の複数値拡張に備える。
     *
     * いずれの引数もデフォルト無指定で素通しになる純関数。
     */
    fun applyAdditionalFilters(
        items: List<Drug>,
        adverseReactionKeyword: String? = null,
        precautionCategories: List<PrecautionPopulationCategory> = emptyList(),
    ): List<Drug> {
        var result = items
        if (!adverseReactionKeyword.isNullOrBlank()) {
            result = result.filter { drug ->
                drug.adverseReactions.allMatchableTexts().any { it.contains(adverseReactionKeyword) }
            }
        }
        if (precautionCategories.isNotEmpty()) {
            result = result.filter { drug ->
                drug.precautionsForSpecificPopulations.any { it.category in precautionCategories }
            }
        }
        return result
    }
}
