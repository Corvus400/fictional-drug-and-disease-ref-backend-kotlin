package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested

import kotlinx.serialization.Serializable

/**
 * 治療情報: 薬物療法・非薬物療法・急性期プロトコルの 3 区分を統合する治療セクション。
 *
 * `Disease.treatments` で使用。各リストは省略可能で、慢性疾患では `acutePhaseProtocol`
 * を空のまま運用するなど、疾患特性に応じて柔軟に組合せる。
 */
@Serializable
data class TreatmentInfo(
    val pharmacological: List<PharmaTreatment> = emptyList(),
    val nonPharmacological: List<TreatmentSection> = emptyList(),
    val acutePhaseProtocol: List<ProtocolStep> = emptyList(),
)

/**
 * 薬物療法: 薬剤カテゴリ・関連薬 ID・適応・補足の 1 セット。
 *
 * `TreatmentInfo.pharmacological` 要素として使用。
 */
@Serializable
data class PharmaTreatment(
    val drugCategory: String,
    /** 関連薬 ID 配列。drug ドメインの `Drug.id` (`drug_NNNN` 形式) を参照し、相互参照整合性が要求される。 */
    val drugIds: List<String> = emptyList(),
    val indication: String,
    val notes: String,
)

/**
 * 非薬物療法セクション: 見出し・項目リスト・補足テキストの組。
 *
 * `TreatmentInfo.nonPharmacological` 要素として、生活指導・リハビリ等を記載。
 * `description` は項目で表せない補足説明 (任意)。
 */
@Serializable
data class TreatmentSection(
    val heading: String,
    val items: List<String> = emptyList(),
    val description: String? = null,
)

/**
 * 急性期プロトコル手順: 順序付き手順 1 ステップ分のモデル。
 *
 * `TreatmentInfo.acutePhaseProtocol` 要素として、緊急対応のシーケンスを表現。
 */
@Serializable
data class ProtocolStep(
    /** 1 始まりのステップ番号。配列順と独立に明示することで挿入・並べ替えに耐性を持たせる。 */
    val order: Int,
    val action: String,
    /** ステップ完了の目標値や到達条件 (例: "SpO2 ≥ 94%")。任意。 */
    val target: String? = null,
)
