package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 副作用頻度帯 — 添付文書 11 項「その他の副作用」の頻度区分 Enum (全 4 種)。
 *
 * `AdverseReaction.frequency` で使用。詳細画面 D13 ブロック (頻度別展開) の見出し決定軸。
 * 仕様: linked-bubbling-sun-drug.md `AdverseReactionInfo` 節。
 */
@Serializable
enum class FrequencyBand {
    /** 5% 以上 */
    @SerialName("over_5_percent")
    OVER_5_PERCENT,

    /** 1〜5% */
    @SerialName("between_1_and_5_percent")
    BETWEEN_1_AND_5_PERCENT,

    /** 1% 未満 */
    @SerialName("under_1_percent")
    UNDER_1_PERCENT,

    /** 頻度不明 */
    @SerialName("unknown")
    UNKNOWN,
    ;

    /**
     * 副作用頻度区分の英語 snake_case 表記 (`@SerialName` 値)。
     * 列挙子の宣言順序が [descriptor] の要素順と一致するため、新しい区分を追加しても同期漏れが起きない。
     */
    val serialName: String
        get() = serializer().descriptor.getElementName(index = ordinal)
}
