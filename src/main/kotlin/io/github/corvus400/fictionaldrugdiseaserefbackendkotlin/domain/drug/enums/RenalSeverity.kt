package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 腎機能障害重症度 — クレアチニンクリアランス (CrCl) に基づく腎機能区分 Enum (全 5 種)。
 *
 * `CrClRange.severity` で使用。区分閾値は 正常 ≥90 / 軽度低下 60-89 / 中等度低下 30-59 / 重度低下 15-29 / 末期 <15 (mL/min)。
 * 仕様: linked-bubbling-sun-drug.md `DosageInfo` 節。
 */
@Serializable
enum class RenalSeverity {
    /** 正常 */
    @SerialName("normal")
    NORMAL,

    /** 軽度低下 */
    @SerialName("mild")
    MILD,

    /** 中等度低下 */
    @SerialName("moderate")
    MODERATE,

    /** 重度低下 */
    @SerialName("severe")
    SEVERE,

    /** 末期 */
    @SerialName("end_stage")
    END_STAGE,
    ;

    /**
     * 腎機能障害重症度区分の英語 snake_case 表記 (`@SerialName` 値)。
     * 列挙子の宣言順序が [descriptor] の要素順と一致するため、新しい区分を追加しても同期漏れが起きない。
     */
    val serialName: String
        get() = serializer().descriptor.getElementName(index = ordinal)
}
