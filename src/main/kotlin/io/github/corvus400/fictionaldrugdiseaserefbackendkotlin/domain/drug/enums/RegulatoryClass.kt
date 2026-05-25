package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 規制区分 — 医薬品の薬事規制上の分類 Enum。詳細画面先頭バッジと警告色強調を決定する分類軸 (全 11 種、複数値可)。
 *
 * `Drug.regulatoryClass` (`List<RegulatoryClass>`) で使用。
 * `RegulatoryClass.POISON.serialName` で `/drugs?regulatory_class=<value>` クエリ値を取得。
 * 仕様: linked-bubbling-sun-drug.md `分類軸` 節。
 */
@Serializable
enum class RegulatoryClass {
    /** 毒薬 */
    @SerialName("poison")
    POISON,

    /** 劇薬 */
    @SerialName("potent")
    POTENT,

    /** 普通薬 */
    @SerialName("ordinary")
    ORDINARY,

    /** 向精神薬第1種 */
    @SerialName("psychotropic_1")
    PSYCHOTROPIC_1,

    /** 向精神薬第2種 */
    @SerialName("psychotropic_2")
    PSYCHOTROPIC_2,

    /** 向精神薬第3種 */
    @SerialName("psychotropic_3")
    PSYCHOTROPIC_3,

    /** 麻薬 */
    @SerialName("narcotic")
    NARCOTIC,

    /** 覚醒剤原料 */
    @SerialName("stimulant_precursor")
    STIMULANT_PRECURSOR,

    /** 生物由来製品 */
    @SerialName("biological")
    BIOLOGICAL,

    /** 特定生物由来製品 */
    @SerialName("specified_biological")
    SPECIFIED_BIOLOGICAL,

    /** 処方箋医薬品 */
    @SerialName("prescription_required")
    PRESCRIPTION_REQUIRED,
    ;

    /**
     * `/drugs?regulatory_class=<value>` クエリフィルタで用いる英語 snake_case 表記 (`@SerialName` 値)。
     * 列挙子の宣言順序が [descriptor] の要素順と一致するため、新しい規制区分を追加しても同期漏れが起きない。
     */
    val serialName: String
        get() = serializer().descriptor.getElementName(index = ordinal)
}
