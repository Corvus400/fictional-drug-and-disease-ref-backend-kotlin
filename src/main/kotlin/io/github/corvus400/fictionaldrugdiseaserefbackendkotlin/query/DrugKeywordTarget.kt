package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

enum class DrugKeywordTarget {
    GENERIC,
    BRAND,
    BOTH,
    ALL,
    ;

    companion object {
        /**
         * `keyword_target` クエリパラメータを `DrugKeywordTarget` に解釈する。
         *
         * `null` / 空文字は既定値 `BOTH` にフォールバックする (`/drugs` 検索 API 既定挙動)。
         * 不正値のバリデーションエラー化は別 Issue (#91) で扱う想定で、現状は安全側に
         * 既定値へフォールバックする。
         */
        fun fromQuery(value: String?): DrugKeywordTarget = when (value) {
            null, "" -> BOTH
            "generic" -> GENERIC
            "brand" -> BRAND
            "both" -> BOTH
            "all" -> ALL
            else -> BOTH
        }
    }
}
