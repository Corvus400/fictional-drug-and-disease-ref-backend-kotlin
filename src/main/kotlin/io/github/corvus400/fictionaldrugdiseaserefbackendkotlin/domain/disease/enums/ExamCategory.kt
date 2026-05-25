package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 検査カテゴリ: `Exam.category` で用いる検査種別の enum (5 値)。
 *
 * 血液 / 画像 / 生理 / 病理 / 問診 を区別。`@SerialName` は英語 snake_case で
 * クライアント側 DTO とシリアライズ互換。
 */
@Serializable
enum class ExamCategory {
    /** 血液検査 */
    @SerialName("blood_test")
    BLOOD_TEST,

    /** 画像検査 */
    @SerialName("imaging")
    IMAGING,

    /** 生理検査 */
    @SerialName("physiological")
    PHYSIOLOGICAL,

    /** 病理検査 */
    @SerialName("pathology")
    PATHOLOGY,

    /** 問診 */
    @SerialName("interview")
    INTERVIEW,
    ;

    /**
     * JSON encoding 時に用いる英語 snake_case (`@SerialName` 値)。
     * 列挙子の宣言順序が [descriptor] の要素順と一致するため、新しい検査カテゴリを追加しても同期漏れが起きない。
     */
    val serialName: String
        get() = serializer().descriptor.getElementName(index = ordinal)

    companion object {
        /**
         * `/diseases?exam_category=...` クエリ値 ([raw]) を enum 定数名 (例: `IMAGING`) として解決する。
         *
         * 未知の値は [IllegalArgumentException] を投げ、Route 層で 400 + `INVALID_EXAM_CATEGORY`
         * の `ErrorResponse` に変換される。`runCatching {} .getOrNull()` 等での握りつぶし禁止
         * (基本方針 10: エラー可視性)。
         */
        fun fromQueryOrThrow(raw: String): ExamCategory =
            runCatching { valueOf(value = raw) }
                .getOrElse { throw IllegalArgumentException("Unknown exam_category: $raw") }
    }
}
