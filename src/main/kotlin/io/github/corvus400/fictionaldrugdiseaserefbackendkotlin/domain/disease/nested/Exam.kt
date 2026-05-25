package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.ExamCategory
import kotlinx.serialization.Serializable

/**
 * 検査項目: 単一検査の名称・カテゴリ・典型所見・参考値を保持するモデル。
 *
 * `Disease.requiredExams` 要素として、当該疾患で実施が想定される検査の一覧化に使用。
 * `category` は `ExamCategory` enum、`referenceRange` は数値検査でのみ提供 (任意)。
 */
@Serializable
data class Exam(
    val name: String,
    val category: ExamCategory,
    val typicalFinding: String,
    val referenceRange: String? = null,
)
