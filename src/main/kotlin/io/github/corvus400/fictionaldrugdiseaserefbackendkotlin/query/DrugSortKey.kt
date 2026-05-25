package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

enum class DrugSortKey {
    REVISED_AT_DESC,
    BRAND_NAME_KANA_ASC,
    ATC_CODE_ASC,
    THERAPEUTIC_CATEGORY_NAME_ASC,
    ;

    companion object {
        fun fromQuery(raw: String?): DrugSortKey = when (raw) {
            null -> REVISED_AT_DESC
            "-revised_at" -> REVISED_AT_DESC
            "brand_name_kana" -> BRAND_NAME_KANA_ASC
            "atc_code" -> ATC_CODE_ASC
            "therapeutic_category_name" -> THERAPEUTIC_CATEGORY_NAME_ASC
            else -> throw IllegalArgumentException("Unknown sort key: $raw")
        }
    }
}
