package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.Disease
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.ExamCategory
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.OnsetPattern

object DiseaseSearchService {
    /**
     * `keyword` を `target` に対応するフィールド集合に適用してフィルタする。
     *
     * 比較前に NFKC とかな正規化を行い、大文字小文字を区別せずに照合する。
     */
    fun applyKeyword(
        items: List<Disease>,
        keyword: String?,
        match: KeywordMatch,
        target: DiseaseKeywordTarget,
    ): List<Disease> {
        if (keyword.isNullOrBlank()) return items
        val tokens = keyword.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return items
        return items.filter { disease ->
            matchesAllTokens(
                fields = fieldsFor(target = target, disease = disease),
                tokens = tokens,
                match = match,
            )
        }
    }

    /**
     * 複数トークンを `fields` に対して評価する: per-token は OR across fields、トークン間は AND。
     * `target=SYNONYMS` のような list 系フィールドでも、要素跨ぎでトークンが満たされれば true。
     */
    private fun matchesAllTokens(
        fields: List<String>,
        tokens: List<String>,
        match: KeywordMatch,
    ): Boolean =
        tokens.all { token ->
            fields.any { field ->
                matches(field = field, keyword = token, match = match)
            }
        }

    /**
     * `target` に対応する検索対象フィールドの集合を返す。
     * `match=PARTIAL` / `match=PREFIX` どちらの評価でも共有される。
     */
    private fun fieldsFor(
        target: DiseaseKeywordTarget,
        disease: Disease,
    ): List<String> =
        when (target) {
            DiseaseKeywordTarget.NAME -> listOf(disease.name, disease.nameKana)
            DiseaseKeywordTarget.NAME_ENGLISH -> listOfNotNull(disease.nameEnglish)
            DiseaseKeywordTarget.SYNONYMS -> disease.synonyms
            DiseaseKeywordTarget.ALL -> listOfNotNull(
                disease.name,
                disease.nameKana,
                disease.nameEnglish,
                disease.icd10Chapter.serialName,
                disease.icd10Chapter.chapterKey,
                disease.icd10Chapter.codeRange,
            ) + disease.synonyms + disease.symptoms.mainSymptoms
        }

    /**
     * 単一フィールドに対するキーワード一致判定。
     * `match=PARTIAL` は contains、`match=PREFIX` は startsWith で評価する。
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
     * `sort` キーに従って `items` を並び替える純関数。
     * 既定キー `REVISED_AT_DESC` は最終改訂日 (`revisedAt`) の降順で並び替え、
     * 同 `revisedAt` 衝突時は `id` 降順で tie-break する (#118 と対称な API 契約)。
     * 他キーは後続サイクルで triangulation により段階的に拡張する。
     */
    fun applySort(
        items: List<Disease>,
        sort: DiseaseSortKey,
    ): List<Disease> = when (sort) {
        DiseaseSortKey.REVISED_AT_DESC ->
            items.sortedWith(
                compareByDescending<Disease> { it.revisedAt }
                    .thenByDescending { it.id },
            )
        DiseaseSortKey.NAME_KANA_ASC -> items.sortedBy { it.nameKana }
        DiseaseSortKey.ICD10_CHAPTER_ASC -> items.sortedBy { it.icd10Chapter.ordinal }
    }

    /**
     * 追加フィルタ — `keyword` / `sort` 後段に適用される副次的な絞り込み群。
     *
     * 本フェーズ (Phase 13-18) までで `symptomKeyword` / `onsetPatterns` /
     * `examCategories` / `hasPharmacologicalTreatment` / `hasSeverityGrading` を受け付ける。`examCategories` はクエリ層で 0/1 個に
     * 解決される単値フィルタ前提だが、本層では `DrugSearchService.applyAdditionalFilters`
     * の `precautionCategories` と同じく集合扱いにして将来の複数値拡張に備える。
     * 引数無指定時は素通しする純関数。
     */
    fun applyAdditionalFilters(
        items: List<Disease>,
        symptomKeyword: String? = null,
        onsetPatterns: List<OnsetPattern> = emptyList(),
        examCategories: List<ExamCategory> = emptyList(),
        hasPharmacologicalTreatment: Boolean? = null,
        hasSeverityGrading: Boolean? = null,
    ): List<Disease> {
        var result = items
        if (!symptomKeyword.isNullOrBlank()) {
            result = result.filter { disease ->
                disease.symptoms.mainSymptoms.any { it.contains(symptomKeyword) }
            }
        }
        if (onsetPatterns.isNotEmpty()) {
            result = result.filter { disease ->
                disease.symptoms.onsetPattern in onsetPatterns
            }
        }
        if (examCategories.isNotEmpty()) {
            result = result.filter { disease ->
                disease.requiredExams.any { it.category in examCategories }
            }
        }
        when (hasPharmacologicalTreatment) {
            true ->
                result = result.filter { disease ->
                    disease.treatments.pharmacological.isNotEmpty()
                }
            false ->
                result = result.filter { disease ->
                    disease.treatments.pharmacological.isEmpty()
                }
            null -> Unit
        }
        when (hasSeverityGrading) {
            true ->
                result = result.filter { disease ->
                    disease.severityGrading != null
                }
            false ->
                result = result.filter { disease ->
                    disease.severityGrading == null
                }
            null -> Unit
        }
        return result
    }
}
