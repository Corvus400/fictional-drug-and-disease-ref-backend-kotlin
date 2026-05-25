package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ICD-10 章コード: WHO ICD-10 第 I〜XXII 章を表す enum (22 値)。
 *
 * `Disease.icd10Chapter` フィールドおよび `/diseases?icd10_chapter=<key>` クエリで使用。
 * SerialName は `chapter_i` 〜 `chapter_xxii`、`chapterKey` プロパティで `I` 〜 `XXII` を
 * 取得する。`/diseases` クエリの逆引きは `fromSerialName` を用いる (OpenAPI description は
 * snake_case 表記で例示しているため)。
 */
@Serializable
enum class Icd10Chapter {
    /** 感染症および寄生虫症 */
    @SerialName("chapter_i")
    CHAPTER_I,

    /** 新生物 */
    @SerialName("chapter_ii")
    CHAPTER_II,

    /** 血液および造血器の疾患ならびに免疫機構の障害 */
    @SerialName("chapter_iii")
    CHAPTER_III,

    /** 内分泌、栄養および代謝疾患 */
    @SerialName("chapter_iv")
    CHAPTER_IV,

    /** 精神および行動の障害 */
    @SerialName("chapter_v")
    CHAPTER_V,

    /** 神経系の疾患 */
    @SerialName("chapter_vi")
    CHAPTER_VI,

    /** 眼および付属器の疾患 */
    @SerialName("chapter_vii")
    CHAPTER_VII,

    /** 耳および乳様突起の疾患 */
    @SerialName("chapter_viii")
    CHAPTER_VIII,

    /** 循環器系の疾患 */
    @SerialName("chapter_ix")
    CHAPTER_IX,

    /** 呼吸器系の疾患 */
    @SerialName("chapter_x")
    CHAPTER_X,

    /** 消化器系の疾患 */
    @SerialName("chapter_xi")
    CHAPTER_XI,

    /** 皮膚および皮下組織の疾患 */
    @SerialName("chapter_xii")
    CHAPTER_XII,

    /** 筋骨格系および結合組織の疾患 */
    @SerialName("chapter_xiii")
    CHAPTER_XIII,

    /** 腎尿路生殖器系の疾患 */
    @SerialName("chapter_xiv")
    CHAPTER_XIV,

    /** 妊娠、分娩および産褥 */
    @SerialName("chapter_xv")
    CHAPTER_XV,

    /** 周産期に発生した病態 */
    @SerialName("chapter_xvi")
    CHAPTER_XVI,

    /** 先天奇形、変形および染色体異常 */
    @SerialName("chapter_xvii")
    CHAPTER_XVII,

    /** 症状、徴候および異常臨床所見・異常検査所見で他に分類されないもの */
    @SerialName("chapter_xviii")
    CHAPTER_XVIII,

    /** 損傷、中毒およびその他の外因の影響 */
    @SerialName("chapter_xix")
    CHAPTER_XIX,

    /** 傷病および死亡の外因 */
    @SerialName("chapter_xx")
    CHAPTER_XX,

    /** 健康状態に影響を及ぼす要因および保健サービスの利用 */
    @SerialName("chapter_xxi")
    CHAPTER_XXI,

    /** 特殊目的用コード */
    @SerialName("chapter_xxii")
    CHAPTER_XXII,
    ;

    /**
     * `/diseases?icd10_chapter=<value>` クエリフィルタで用いる英語 snake_case (`@SerialName` 値)。
     * 列挙子の宣言順序が [descriptor] の要素順と一致するため、新しい章を追加しても同期漏れが起きない。
     */
    val serialName: String
        get() = serializer().descriptor.getElementName(index = ordinal)

    /**
     * `/diseases?icd10_chapter=<key>` クエリフィルタで用いるローマ数字キー (`I`, `II`, ..., `XXII`)。
     * 列挙子名 (`CHAPTER_XXX`) から接頭辞を落とすだけなので、新しい章を追加したときに同期漏れが起きない。
     */
    val chapterKey: String
        get() = name.removePrefix(prefix = "CHAPTER_")

    /**
     * ICD-10 章別の標準コード範囲。`/categories` の code とキーワード検索対象で共有する。
     */
    val codeRange: String
        get() = when (this) {
            CHAPTER_I -> "A00-B99"
            CHAPTER_II -> "C00-D48"
            CHAPTER_III -> "D50-D89"
            CHAPTER_IV -> "E00-E90"
            CHAPTER_V -> "F00-F99"
            CHAPTER_VI -> "G00-G99"
            CHAPTER_VII -> "H00-H59"
            CHAPTER_VIII -> "H60-H95"
            CHAPTER_IX -> "I00-I99"
            CHAPTER_X -> "J00-J99"
            CHAPTER_XI -> "K00-K93"
            CHAPTER_XII -> "L00-L99"
            CHAPTER_XIII -> "M00-M99"
            CHAPTER_XIV -> "N00-N99"
            CHAPTER_XV -> "O00-O99"
            CHAPTER_XVI -> "P00-P96"
            CHAPTER_XVII -> "Q00-Q99"
            CHAPTER_XVIII -> "R00-R99"
            CHAPTER_XIX -> "S00-T98"
            CHAPTER_XX -> "V01-Y98"
            CHAPTER_XXI -> "Z00-Z99"
            CHAPTER_XXII -> "U00-U85"
        }

    companion object {
        /**
         * `@SerialName` 値 (snake_case `chapter_i` 〜 `chapter_xxii`) から列挙子を逆引きする。
         * `/diseases?icd10_chapter=<key>` の OpenAPI 仕様 (`Icd10Chapter.serialName` 例示) に追従するため、
         * `chapterKey` (ローマ数字) ではなく `serialName` で受理する用途で使用する。未定義キーは `null`。
         */
        fun fromSerialName(serialName: String): Icd10Chapter? =
            entries.firstOrNull { it.serialName == serialName }
    }
}
