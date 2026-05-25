package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested

import kotlinx.serialization.Serializable

/**
 * 重症度評価情報: 採用する評価尺度名 (`gradingSystem`) と段階定義 (`grades`) の組。
 *
 * `Disease.severityGrading` (nullable) で使用。重症度評価が定義されない疾患では `null`。
 */
@Serializable
data class SeverityInfo(
    val gradingSystem: String,
    val grades: List<Grade>,
)

/**
 * 重症度ランク: 評価尺度上の 1 段階の名称・基準・推奨対応をまとめたモデル。
 *
 * `SeverityInfo.grades` 要素として使用。`recommendedAction` は当該段階で推奨される
 * 医療行為や生活指導を記載する。
 */
@Serializable
data class Grade(
    val label: String,
    val criteria: String,
    val recommendedAction: String,
)
