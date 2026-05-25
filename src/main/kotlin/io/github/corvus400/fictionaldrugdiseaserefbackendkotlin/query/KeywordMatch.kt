package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

enum class KeywordMatch {
    PREFIX,
    PARTIAL,
    ;

    companion object {
        /**
         * `keyword_match` クエリパラメータを `KeywordMatch` に解釈する。
         *
         * `null` / 空文字は既定値 `PARTIAL` にフォールバックする (`/drugs` 検索 API 既定挙動)。
         * 不正値 (例: `prefxxx`) のバリデーションエラー化は別 Issue (#91) で扱う想定で、
         * 現状は安全側に既定値へフォールバックする。
         */
        fun fromQuery(value: String?): KeywordMatch = when (value) {
            null, "" -> PARTIAL
            "prefix" -> PREFIX
            "partial" -> PARTIAL
            else -> PARTIAL
        }
    }
}
