package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested

import kotlinx.serialization.Serializable

/**
 * 診断基準情報: 必須項目と補助項目を分けて保持する診断要件モデル。
 *
 * `Disease.diagnosticCriteria` で使用。`required` は確定診断に不可欠な項目、
 * `supporting` は確度を高める補助項目、`notes` は補足条件 (任意)。
 */
@Serializable
data class DiagnosticCriteriaInfo(
    val required: List<String>,
    val supporting: List<String> = emptyList(),
    val notes: String? = null,
)
