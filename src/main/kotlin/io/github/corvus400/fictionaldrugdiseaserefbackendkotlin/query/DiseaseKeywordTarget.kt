package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

/**
 * キーワード検索対象: `/diseases` 検索 API でキーワードを照合するフィールドを表す enum。
 *
 * `name` / `name_english` / `synonyms` / `all` の値。クエリパラメータ `keyword_target` の値と
 * 1 対 1 対応し、`fromQuery` で逆引きする。Drug 側 `DrugKeywordTarget` と仕様統一。
 */
enum class DiseaseKeywordTarget {
    NAME,
    NAME_ENGLISH,
    SYNONYMS,
    ALL,
    ;

    companion object {
        /**
         * `keyword_target` クエリパラメータを `DiseaseKeywordTarget` に解釈する。
         *
         * `null` / 空文字は既定値 `NAME` にフォールバックする (`/diseases` 検索 API 既定挙動)。
         * 受理値は lower-case 厳密一致 (`name` / `name_english` / `synonyms` / `all`)。不正値 (例: `NAME`
         * など大文字、`title`) のバリデーションエラー化は別 Issue (#91) で扱う想定で、現状は
         * 安全側に既定値へフォールバックする。Drug 側 PR #286 (`DrugKeywordTarget.fromQuery`) と
         * 仕様統一。
         */
        fun fromQuery(value: String?): DiseaseKeywordTarget = when (value) {
            null, "" -> NAME
            "name" -> NAME
            "name_english" -> NAME_ENGLISH
            "synonyms" -> SYNONYMS
            "all" -> ALL
            else -> NAME
        }
    }
}
