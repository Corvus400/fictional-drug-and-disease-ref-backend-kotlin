package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums

enum class TherapeuticCategory(
    val displayName: String,
    val atcInitial: Char,
    val categoryId: String,
) {
    ALIMENTARY_METABOLISM("消化器系および代謝", 'A', "alimentary_metabolism"),
    BLOOD_BLOOD_FORMING_ORGANS("血液および造血器", 'B', "blood"),
    CARDIOVASCULAR_SYSTEM("循環器系", 'C', "cardiovascular"),
    DERMATOLOGICAL("皮膚科用", 'D', "dermatologicals"),
    GENITO_URINARY_SYSTEM_AND_SEX_HORMONES(
        displayName = "泌尿生殖器系およびホルモン製剤",
        atcInitial = 'G',
        categoryId = "genito_urinary_hormones",
    ),
    SYSTEMIC_HORMONAL_PREPARATIONS("全身性ホルモン製剤", 'H', "systemic_hormones"),
    ANTI_INFECTIVES_FOR_SYSTEMIC_USE("感染症治療薬", 'J', "antiinfectives"),
    ANTINEOPLASTIC_IMMUNOMODULATING(
        displayName = "抗腫瘍剤および免疫調節剤",
        atcInitial = 'L',
        categoryId = "antineoplastic_immunomodulators",
    ),
    MUSCULO_SKELETAL_SYSTEM("筋骨格系", 'M', "musculo_skeletal"),
    NERVOUS_SYSTEM("神経系", 'N', "nervous"),
    ANTIPARASITIC_PRODUCTS("抗寄生虫剤", 'P', "antiparasitic"),
    RESPIRATORY_SYSTEM("呼吸器系", 'R', "respiratory"),
    SENSORY_ORGANS("感覚器", 'S', "sensory"),
    VARIOUS("その他", 'V', "various"),
    ;

    companion object {
        fun fromQueryOrThrow(raw: String): TherapeuticCategory =
            runCatching { valueOf(value = raw) }
                .getOrElse { throw IllegalArgumentException("Unknown therapeutic_category: $raw") }

        fun fromAtcInitial(initial: Char): TherapeuticCategory? =
            entries.firstOrNull { category -> category.atcInitial == initial }

        fun fromDisplayName(displayName: String): TherapeuticCategory? =
            entries.firstOrNull { category -> category.displayName == displayName }
    }
}
