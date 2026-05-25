package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 投与経路 — 医薬品の投与経路を表す Enum。用法用量 UI の表現形式と適用上の注意の要否を決定する分類軸 (全 8 種)。
 *
 * `Drug.routeOfAdministration` で使用。`RouteOfAdministration.ORAL.serialName` で `/drugs?route=<value>` クエリ値を取得。
 * 注: 剤形側の `DosageForm.INJECTION_FORM` (`"injection_form"`) とは serialName が異なるため衝突しない。
 * 仕様: linked-bubbling-sun-drug.md `分類軸` 節。
 */
@Serializable
enum class RouteOfAdministration {
    /** 内服 */
    @SerialName("oral")
    ORAL,

    /** 外用 */
    @SerialName("topical")
    TOPICAL,

    /** 注射 */
    @SerialName("injection_route")
    INJECTION_ROUTE,

    /** 吸入 */
    @SerialName("inhalation")
    INHALATION,

    /** 坐剤 */
    @SerialName("rectal")
    RECTAL,

    /** 点眼 */
    @SerialName("ophthalmic")
    OPHTHALMIC,

    /** 点鼻 */
    @SerialName("nasal")
    NASAL,

    /** 貼付 */
    @SerialName("transdermal")
    TRANSDERMAL,
    ;

    /**
     * `/drugs?route=<value>` クエリフィルタで用いる英語 snake_case 表記 (`@SerialName` 値)。
     * 列挙子の宣言順序が [descriptor] の要素順と一致するため、新しい投与経路を追加しても同期漏れが起きない。
     */
    val serialName: String
        get() = serializer().descriptor.getElementName(index = ordinal)
}
