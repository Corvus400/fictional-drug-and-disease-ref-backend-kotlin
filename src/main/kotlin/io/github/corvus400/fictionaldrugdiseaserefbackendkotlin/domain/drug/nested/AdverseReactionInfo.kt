package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.FrequencyBand
import kotlinx.serialization.Serializable

/**
 * 副作用情報 — 添付文書 11 項。重大な副作用 (独立リスト) と「その他の副作用」(頻度別) を分離保持する。
 *
 * `Drug.adverseReactions` で使用。詳細画面 D12 (重大) と D13 (頻度別) のソース。
 * 仕様: linked-bubbling-sun-drug.md `AdverseReactionInfo` 節。
 */
@Serializable
data class AdverseReactionInfo(
    val serious: List<AdverseReaction> = emptyList(),
    val other: AdverseReactionByFrequency,
)

/**
 * 重大な副作用 1 件 — 名称・頻度帯・症状・初期症状・対応の組。詳細画面では警告色で強調表示される。
 */
@Serializable
data class AdverseReaction(
    val name: String,
    val frequency: FrequencyBand,
    val symptom: String,
    val initialSigns: String,
    val countermeasure: String,
)

/**
 * 頻度別副作用 — 「その他の副作用」を発現頻度帯ごとの副作用名リストに分類した型。
 *
 * JSON キーは `over5Percent` 等の camelCase (`>=5%` 等の記号は JSON 識別子として非推奨のため使わない、仕様書注記)。
 */
@Serializable
data class AdverseReactionByFrequency(
    /** 発現頻度 5% 以上の副作用名リスト。各要素は短い副作用名 (Markdown 非対象、1〜10 字程度)。 */
    val over5Percent: List<String> = emptyList(),
    /** 発現頻度 1〜5% の副作用名リスト。 */
    val between1And5Percent: List<String> = emptyList(),
    /** 発現頻度 1% 未満の副作用名リスト。 */
    val under1Percent: List<String> = emptyList(),
    /** 発現頻度不明の副作用名リスト。 */
    val frequencyUnknown: List<String> = emptyList(),
)

/**
 * `adverseReactions.serious[].name` と `adverseReactions.other.*` の全副作用名テキストを
 * 1 つの `Sequence<String>` として返す。`adverseReactionKeyword` の部分一致対象を集約する用途。
 */
fun AdverseReactionInfo.allMatchableTexts(): Sequence<String> = sequence {
    serious.forEach { yield(it.name) }
    yieldAll(other.over5Percent)
    yieldAll(other.between1And5Percent)
    yieldAll(other.under1Percent)
    yieldAll(other.frequencyUnknown)
}
