package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

@Suppress("unused", "RedundantSuppression")
enum class ApiTag(
    val tagName: String,
    val description: String,
) {
    DRUG("Drug", "医薬品リファレンスAPI (架空データ)"),
    DISEASE("Disease", "疾患リファレンスAPI (架空データ)"),
    CATEGORIES("Categories", "カテゴリメタデータAPI (架空データ)"),
    SYSTEM("System", "ヘルスチェック等システムAPI"),
}
