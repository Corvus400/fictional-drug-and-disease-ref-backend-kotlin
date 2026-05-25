package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.DosageForm

private val drugImageOverrideIds = setOf("drug_0080", "drug_0089")

fun buildDrugImageUrl(
    drugId: String,
    dosageForm: DosageForm,
): String =
    if (drugId in drugImageOverrideIds) {
        "/v1/images/drugs/$drugId?size=Original"
    } else {
        "/v1/images/dosage-forms/${dosageForm.serialName}?size=Original"
    }
