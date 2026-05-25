package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 慢性度区分: 疾患の経過パターンを表す enum (4 値)。
 *
 * `Disease.chronicity` フィールドおよび `/diseases?chronicity=<value>` クエリで使用。
 * `@SerialName` は英語 snake_case (`acute` / `subacute` / `chronic` / `relapsing`) で
 * クライアント側 DTO とシリアライズ互換。
 */
@Serializable
enum class Chronicity {
    /** 急性 */
    @SerialName("acute")
    ACUTE,

    /** 亜急性 */
    @SerialName("subacute")
    SUBACUTE,

    /** 慢性 */
    @SerialName("chronic")
    CHRONIC,

    /** 再発性 */
    @SerialName("relapsing")
    RELAPSING,
    ;

    /**
     * `/diseases?chronicity=<value>` クエリフィルタで用いる英語 snake_case (`@SerialName` 値)。
     * 列挙子の宣言順序が [descriptor] の要素順と一致するため、新しい慢性度を追加しても同期漏れが起きない。
     */
    val serialName: String
        get() = serializer().descriptor.getElementName(index = ordinal)
}
