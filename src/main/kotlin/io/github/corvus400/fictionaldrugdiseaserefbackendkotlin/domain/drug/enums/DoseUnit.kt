package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用量単位 — 医薬品の含量や用量を表す単位 Enum (全 10 種)。
 *
 * `Dose.unit` で使用。質量系 (mg/g/μg)、容量系 (mL/L)、生物学的活性系 (IU/mEq/mol/mmol)、比率 (%) を含む。
 * 仕様: linked-bubbling-sun-drug.md `CompositionInfo` 節。
 */
@Serializable
enum class DoseUnit {
    /** ミリグラム */
    @SerialName("mg")
    MG,

    /** グラム */
    @SerialName("g")
    G,

    /** マイクログラム */
    @SerialName("microgram")
    MICROGRAM,

    /** ミリリットル */
    @SerialName("ml")
    ML,

    /** リットル */
    @SerialName("l")
    L,

    /** 国際単位 */
    @SerialName("iu")
    IU,

    /** ミリ当量 */
    @SerialName("meq")
    MEQ,

    /** モル */
    @SerialName("mol")
    MOL,

    /** ミリモル */
    @SerialName("mmol")
    MMOL,

    /** パーセント */
    @SerialName("percent")
    PERCENT,
    ;

    /**
     * `dose.unit` ネストフィールドで用いる英語 snake_case 表記 (`@SerialName` 値)。
     * 列挙子の宣言順序が [descriptor] の要素順と一致するため、新しい単位を追加しても同期漏れが起きない。
     */
    val serialName: String
        get() = serializer().descriptor.getElementName(index = ordinal)
}
