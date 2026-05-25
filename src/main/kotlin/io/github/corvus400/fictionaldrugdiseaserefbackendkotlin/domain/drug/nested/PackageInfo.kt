package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.StorageTemperature
import kotlinx.serialization.Serializable

/**
 * 包装情報 1 件 — 添付文書 22 項。1 包装単位のサイズ表記・保管条件・有効期限 (月数) の組。
 *
 * `Drug.packages` (`List<PackageInfo>`、最低 1 要素) で使用。詳細画面 D16 ブロックの一部。
 * 仕様: linked-bubbling-sun-drug.md `PackageInfo` 節。
 */
@Serializable
data class PackageInfo(
    /** 包装サイズ表記 (Markdown 非対象、自由記述)。例: "100 錠 (10 錠 × 10 PTP)"。 */
    val size: String,
    val storageCondition: StorageCondition,
    /** 有効期限 (月数)。例: `36` は製造日から 36 ヶ月。 */
    val expirationMonths: Int,
)

/**
 * 保管条件 — 温度区分・遮光要否・防湿要否の組合せと、特殊条件 (任意) のテキスト。
 *
 * `PackageInfo.storageCondition` で使用。
 */
@Serializable
data class StorageCondition(
    val temperature: StorageTemperature,
    /** 遮光保管要否 (`true` のとき遮光保管が必要)。 */
    val lightProtection: Boolean,
    /** 防湿保管要否 (`true` のとき防湿保管が必要)。 */
    val moistureProtection: Boolean,
    /** 温度・遮光・防湿で表現できない特殊保管条件 (例: "凍結を避ける")。 */
    val additionalNote: String? = null,
)
