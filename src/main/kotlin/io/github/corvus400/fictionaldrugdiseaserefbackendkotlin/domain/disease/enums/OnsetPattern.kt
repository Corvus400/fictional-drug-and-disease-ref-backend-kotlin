package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 発症パターン区分: `SymptomInfo.onsetPattern` で用いる 5 値の enum。
 *
 * 急性 / 亜急性 / 慢性 / 間欠性 / 再発性 を区別。`@SerialName` は英語 snake_case で
 * クライアント側 DTO とシリアライズ互換。
 */
@Serializable
enum class OnsetPattern {
    /** 急性発症 */
    @SerialName("acute")
    ACUTE,

    /** 亜急性発症 */
    @SerialName("subacute")
    SUBACUTE,

    /** 慢性経過 */
    @SerialName("chronic")
    CHRONIC,

    /** 間欠性 */
    @SerialName("intermittent")
    INTERMITTENT,

    /** 再発性 */
    @SerialName("relapsing")
    RELAPSING,
    ;

    /**
     * JSON encoding 時に用いる英語 snake_case (`@SerialName` 値)。
     * 列挙子の宣言順序が [descriptor] の要素順と一致するため、新しい発症パターンを追加しても同期漏れが起きない。
     */
    val serialName: String
        get() = serializer().descriptor.getElementName(index = ordinal)

    companion object {
        /**
         * `/diseases?onset_pattern=...` クエリ値 ([raw]) を enum 定数名 (例: `ACUTE`) として解決する。
         *
         * 未知の値は [IllegalArgumentException] を投げ、Route 層で 400 + `INVALID_ONSET_PATTERN`
         * の `ErrorResponse` に変換される。`runCatching {} .getOrNull()` 等での握りつぶし禁止
         * (基本方針 10: エラー可視性)。
         */
        fun fromQueryOrThrow(raw: String): OnsetPattern =
            runCatching { valueOf(value = raw) }
                .getOrElse { throw IllegalArgumentException("Unknown onset_pattern: $raw") }
    }
}
