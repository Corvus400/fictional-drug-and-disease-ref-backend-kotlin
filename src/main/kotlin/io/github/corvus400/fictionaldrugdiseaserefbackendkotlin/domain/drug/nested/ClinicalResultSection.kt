package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested

import kotlinx.serialization.Serializable

/**
 * 臨床成績セクション 1 件 — 添付文書 17 項の小見出しと本文 (Markdown 段落) のペア。
 *
 * `Drug.clinicalResults` (`List<ClinicalResultSection>`) で使用。
 * 見出し例: "有効性"、"安全性"、"長期投与試験"。
 * 仕様: linked-bubbling-sun-drug.md `ClinicalResultSection` 節。
 */
@Serializable
data class ClinicalResultSection(
    val heading: String,
    val content: String,
)
