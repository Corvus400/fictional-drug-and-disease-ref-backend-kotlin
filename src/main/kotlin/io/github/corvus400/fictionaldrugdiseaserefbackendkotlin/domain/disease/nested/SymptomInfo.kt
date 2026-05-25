package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.OnsetPattern
import kotlinx.serialization.Serializable

/**
 * 症状情報: 主症状・関連症状・発症パターンをまとめた症状記述モデル。
 *
 * `Disease.symptoms` で使用。`mainSymptoms` は 1 件以上の主訴、
 * `associatedSymptoms` は併発しうる関連症状、`onsetPattern` は急性 / 亜急性等の発症形態。
 */
@Serializable
data class SymptomInfo(
    val mainSymptoms: List<String>,
    val associatedSymptoms: List<String> = emptyList(),
    val onsetPattern: OnsetPattern? = null,
)
