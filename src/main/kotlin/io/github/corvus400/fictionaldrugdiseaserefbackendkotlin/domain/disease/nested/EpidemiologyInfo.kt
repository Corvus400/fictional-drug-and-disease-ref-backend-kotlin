package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.PrevalenceUnit
import kotlinx.serialization.Serializable

/**
 * 疫学情報: 罹患率・発症年齢・性差・リスク因子をまとめた集約モデル。
 *
 * `Disease.epidemiology` (nullable) として保持。各サブフィールドも nullable で、
 * データが揃わない疾患では一部のみが提供される。
 */
@Serializable
data class EpidemiologyInfo(
    val prevalence: Prevalence? = null,
    val onsetAgeRange: OnsetAgeRange? = null,
    /** 性差分布。`SexDistribution` 経由で男女別罹患比率と臨床補足コメントを保持する。 */
    val sexRatio: SexDistribution? = null,
    val riskFactors: List<String> = emptyList(),
)

/**
 * 罹患率: 母集団あたりの発症割合と単位 (`PrevalenceUnit`) を保持するモデル。
 *
 * `EpidemiologyInfo.prevalence` で使用。`rate` / `denominator` は概数につき nullable、
 * `label` は「人口10万あたり 30〜50 名」のような UI 表示用文字列。
 */
@Serializable
data class Prevalence(
    val rate: Double?,
    val denominator: Int?,
    /** 罹患率の単位区分 (`PrevalenceUnit` enum)。母集団あたりの分母種別を表す。 */
    val unit: PrevalenceUnit = PrevalenceUnit.PER_POPULATION,
    val label: String,
)

/**
 * 発症年齢範囲: 最小〜最大年齢と表示用ラベルを保持するモデル。
 *
 * `EpidemiologyInfo.onsetAgeRange` で使用。新生児や小児期限定など片方の境界が
 * 不定のときは `null` を許容、`label` で人間可読な表記を提供する。
 */
@Serializable
data class OnsetAgeRange(
    val minAgeYears: Int?,
    val maxAgeYears: Int?,
    val label: String,
)

/**
 * 性差分布: 男女別罹患比率 (整数比) と補足コメントを保持するモデル。
 *
 * `EpidemiologyInfo.sexRatio` で使用。`maleRatio` : `femaleRatio` は合計 100 になるとは
 * 限らない概比 (例: 1 : 3)、`note` は「中年女性に多い」等の臨床補足。
 */
@Serializable
data class SexDistribution(
    val maleRatio: Int,
    val femaleRatio: Int,
    val note: String? = null,
)
