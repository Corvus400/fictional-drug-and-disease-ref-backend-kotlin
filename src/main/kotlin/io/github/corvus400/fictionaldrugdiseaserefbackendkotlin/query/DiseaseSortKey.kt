package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.query

enum class DiseaseSortKey {
    REVISED_AT_DESC,
    NAME_KANA_ASC,
    ICD10_CHAPTER_ASC,
    ;

    companion object {
        fun fromQuery(raw: String?): DiseaseSortKey = when (raw) {
            null -> REVISED_AT_DESC
            "-revised_at" -> REVISED_AT_DESC
            "name_kana" -> NAME_KANA_ASC
            "icd10_chapter" -> ICD10_CHAPTER_ASC
            else -> throw IllegalArgumentException("Unknown sort key: $raw")
        }
    }
}
