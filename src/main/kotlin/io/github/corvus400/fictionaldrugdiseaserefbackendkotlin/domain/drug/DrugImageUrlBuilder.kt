package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.DosageForm

fun buildDrugImageUrl(
    drugId: String,
    dosageForm: DosageForm,
    hasUploadedDrugImage: Boolean = false,
): String =
    if (hasUploadedDrugImage || hasBundledDrugImage(drugId)) {
        "/v1/images/drugs/$drugId?size=Original"
    } else {
        "/v1/images/dosage-forms/${dosageForm.serialName}?size=Original"
    }

private fun hasBundledDrugImage(drugId: String): Boolean =
    Thread.currentThread().contextClassLoader.getResource("images/drug/$drugId.png") != null
