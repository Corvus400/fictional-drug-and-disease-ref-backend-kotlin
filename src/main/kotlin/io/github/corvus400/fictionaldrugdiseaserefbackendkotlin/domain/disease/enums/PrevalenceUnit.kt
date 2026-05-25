package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 罹患率の単位区分: `Prevalence.unit` で用いる母集団分母種別の enum (3 値)。
 *
 * 人口対 / 患者対 / 出生対 を区別。`@SerialName` は英語 snake_case で
 * クライアント側 DTO とシリアライズ互換。
 */
@Serializable
enum class PrevalenceUnit {
    /** 人口対 */
    @SerialName("per_population")
    PER_POPULATION,

    /** 患者対 */
    @SerialName("per_patient")
    PER_PATIENT,

    /** 出生対 */
    @SerialName("per_birth")
    PER_BIRTH,
    ;

    /**
     * JSON encoding 時に用いる英語 snake_case (`@SerialName` 値)。
     * 列挙子の宣言順序が [descriptor] の要素順と一致するため、新しい有病率単位を追加しても同期漏れが起きない。
     */
    val serialName: String
        get() = serializer().descriptor.getElementName(index = ordinal)
}
