package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested

import kotlinx.serialization.Serializable

/**
 * 薬物動態情報 — 添付文書 16 項。血中濃度・吸収・分布・代謝・排泄の各説明と主要パラメータ表を保持する。
 *
 * `Drug.pharmacokinetics` で使用 (任意)。注射剤では Fixture 作成ルールにより全フィールドが `null` になることはなく、
 * 少なくとも `bloodConcentration` と `metabolism` / `excretion` のいずれかが非 null とする。
 * 仕様: linked-bubbling-sun-drug.md `PharmacokineticsInfo` 節。
 */
@Serializable
data class PharmacokineticsInfo(
    val bloodConcentration: String? = null,
    val absorption: String? = null,
    val distribution: String? = null,
    val metabolism: String? = null,
    val excretion: String? = null,
    val parameters: List<PkParameter> = emptyList(),
)

/**
 * 薬物動態パラメータ 1 件 — パラメータ名と単位込みの値文字列のペア。
 *
 * 例: `PkParameter("Cmax", "4.5 μg/mL")`。架空値の柔軟性を優先し `value` は単位込み文字列で表現する。
 */
@Serializable
data class PkParameter(
    val name: String,
    val value: String,
)
