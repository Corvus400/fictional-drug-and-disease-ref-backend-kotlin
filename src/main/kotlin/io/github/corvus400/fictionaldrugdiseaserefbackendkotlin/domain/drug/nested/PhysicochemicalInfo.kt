package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested

import kotlinx.serialization.Serializable

/**
 * 理化学的知見 — 添付文書 19 項。一般名英語表記・分子式・分子量・性状の Markdown 説明を保持する。
 *
 * `Drug.physicochemicalProperties` で使用 (任意)。詳細画面 D15 補助情報折り畳みの一部。
 * 仕様: linked-bubbling-sun-drug.md `PhysicochemicalInfo` 節。
 */
@Serializable
data class PhysicochemicalInfo(
    val genericNameEnglish: String,
    val molecularFormula: String,
    val molecularWeight: Double? = null,
    val description: String,
)
