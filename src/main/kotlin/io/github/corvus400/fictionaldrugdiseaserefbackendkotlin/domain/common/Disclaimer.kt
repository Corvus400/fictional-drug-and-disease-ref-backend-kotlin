package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common

object Disclaimer {
    const val FULL_JA_EN: String =
        "これは架空の医薬品・疾患データを返すモックサーバーです。\n" +
            "内容は実在の医薬品・疾患・治療法を表すものではなく、\n" +
            "医療判断・診療・自己判断に使用してはなりません。\n" +
            "This server returns FICTIONAL drug and disease data.\n" +
            "DO NOT use for medical decisions or clinical practice."

    const val SHORT: String =
        "FICTIONAL DATA - NOT FOR MEDICAL USE / 架空データ・医療判断には使用不可"
}
