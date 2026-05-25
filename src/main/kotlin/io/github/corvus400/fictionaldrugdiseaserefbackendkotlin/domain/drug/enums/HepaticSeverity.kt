package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 肝機能障害重症度 — 用法用量の肝機能別調整に用いる重症度区分 Enum (全 3 種: 軽度/中等度/重度)。
 *
 * `HepaticDose.severity` で使用。詳細画面 D8 用法詳細タブの肝機能別行を切り替える。
 * 仕様: linked-bubbling-sun-drug.md `DosageInfo` 節。
 */
@Serializable
enum class HepaticSeverity {
    /** 軽度 */
    @SerialName("mild")
    MILD,

    /** 中等度 */
    @SerialName("moderate")
    MODERATE,

    /** 重度 */
    @SerialName("severe")
    SEVERE,
    ;

    /**
     * 肝機能障害重症度区分の英語 snake_case 表記 (`@SerialName` 値)。
     * 列挙子の宣言順序が [descriptor] の要素順と一致するため、新しい区分を追加しても同期漏れが起きない。
     */
    val serialName: String
        get() = serializer().descriptor.getElementName(index = ordinal)
}
