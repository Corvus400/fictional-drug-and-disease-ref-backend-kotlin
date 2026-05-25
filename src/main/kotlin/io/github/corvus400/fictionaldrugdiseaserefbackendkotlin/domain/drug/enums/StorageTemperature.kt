package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 保管温度区分 — 医薬品包装の保管温度を表す Enum (全 3 種: 室温/冷所 (2-8 ℃)/冷凍 (-20 ℃ 以下))。
 *
 * `StorageCondition.temperature` で使用。
 * 仕様: linked-bubbling-sun-drug.md `PackageInfo` 節。
 */
@Serializable
enum class StorageTemperature {
    /** 室温 */
    @SerialName("room_temperature")
    ROOM_TEMPERATURE,

    /** 冷所 */
    @SerialName("cold")
    COLD,

    /** 冷凍 */
    @SerialName("frozen")
    FROZEN,
    ;

    /**
     * 保存温度区分の英語 snake_case 表記 (`@SerialName` 値)。
     * 列挙子の宣言順序が [descriptor] の要素順と一致するため、新しい区分を追加しても同期漏れが起きない。
     */
    val serialName: String
        get() = serializer().descriptor.getElementName(index = ordinal)
}
