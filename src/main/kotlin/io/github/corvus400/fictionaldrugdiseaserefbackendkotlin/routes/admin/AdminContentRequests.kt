package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.Disease
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.Chronicity
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.Icd10Chapter
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.MedicalDepartment
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested.DiagnosticCriteriaInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested.EpidemiologyInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested.Exam
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested.SeverityInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested.SymptomInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested.TreatmentInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.DosageForm
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RegulatoryClass
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RouteOfAdministration
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.AdverseReactionInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.ClinicalResultSection
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.CompositionInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.DosageInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.IndicationItem
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.InteractionInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.NumberedParagraph
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.OverdoseInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.PackageInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.PharmacokineticsInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.PharmacologyInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.PhysicochemicalInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.PrecautionPopulation
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.nested.Reference
import kotlinx.serialization.Serializable

@Serializable
internal data class AdminDrugContentRequest(
    val genericName: String,
    val brandName: String,
    val brandNameKana: String,
    val atcCode: String,
    val yjCode: String? = null,
    val therapeuticCategoryName: String,
    val regulatoryClass: List<RegulatoryClass>,
    val dosageForm: DosageForm,
    val routeOfAdministration: RouteOfAdministration,
    val composition: CompositionInfo,
    val warning: List<NumberedParagraph> = emptyList(),
    val contraindications: List<NumberedParagraph>,
    val indications: List<IndicationItem>,
    val indicationsRelatedPrecautions: List<NumberedParagraph> = emptyList(),
    val dosage: DosageInfo,
    val dosageRelatedPrecautions: List<NumberedParagraph> = emptyList(),
    val importantPrecautions: List<NumberedParagraph> = emptyList(),
    val precautionsForSpecificPopulations: List<PrecautionPopulation> = emptyList(),
    val interactions: InteractionInfo? = null,
    val adverseReactions: AdverseReactionInfo,
    val effectsOnLabTests: List<NumberedParagraph> = emptyList(),
    val overdose: OverdoseInfo? = null,
    val administrationPrecautions: List<NumberedParagraph> = emptyList(),
    val otherPrecautions: List<NumberedParagraph> = emptyList(),
    val pharmacokinetics: PharmacokineticsInfo? = null,
    val clinicalResults: List<ClinicalResultSection> = emptyList(),
    val pharmacology: PharmacologyInfo? = null,
    val physicochemicalProperties: PhysicochemicalInfo? = null,
    val handlingPrecautions: List<NumberedParagraph> = emptyList(),
    val approvalConditions: List<NumberedParagraph> = emptyList(),
    val packages: List<PackageInfo>,
    val references: List<Reference> = emptyList(),
    val insuranceNotes: List<NumberedParagraph> = emptyList(),
    val manufacturer: String,
    val relatedDiseaseIds: List<String> = emptyList(),
) {
    fun toDrug(id: String, revisedAt: String): Drug =
        Drug(
            id = id,
            genericName = genericName,
            brandName = brandName,
            brandNameKana = brandNameKana,
            atcCode = atcCode,
            yjCode = yjCode,
            therapeuticCategoryName = therapeuticCategoryName,
            regulatoryClass = regulatoryClass,
            dosageForm = dosageForm,
            routeOfAdministration = routeOfAdministration,
            composition = composition,
            warning = warning,
            contraindications = contraindications,
            indications = indications,
            indicationsRelatedPrecautions = indicationsRelatedPrecautions,
            dosage = dosage,
            dosageRelatedPrecautions = dosageRelatedPrecautions,
            importantPrecautions = importantPrecautions,
            precautionsForSpecificPopulations = precautionsForSpecificPopulations,
            interactions = interactions,
            adverseReactions = adverseReactions,
            effectsOnLabTests = effectsOnLabTests,
            overdose = overdose,
            administrationPrecautions = administrationPrecautions,
            otherPrecautions = otherPrecautions,
            pharmacokinetics = pharmacokinetics,
            clinicalResults = clinicalResults,
            pharmacology = pharmacology,
            physicochemicalProperties = physicochemicalProperties,
            handlingPrecautions = handlingPrecautions,
            approvalConditions = approvalConditions,
            packages = packages,
            references = references,
            insuranceNotes = insuranceNotes,
            manufacturer = manufacturer,
            revisedAt = revisedAt,
            relatedDiseaseIds = relatedDiseaseIds,
        )
}

@Serializable
internal data class AdminDiseaseContentRequest(
    val name: String,
    val nameKana: String,
    val nameEnglish: String? = null,
    val icd10Chapter: Icd10Chapter,
    val medicalDepartment: List<MedicalDepartment>,
    val chronicity: Chronicity,
    val infectious: Boolean,
    val synonyms: List<String> = emptyList(),
    val summary: String,
    val epidemiology: EpidemiologyInfo? = null,
    val etiology: String,
    val symptoms: SymptomInfo,
    val diagnosticCriteria: DiagnosticCriteriaInfo,
    val requiredExams: List<Exam> = emptyList(),
    val severityGrading: SeverityInfo? = null,
    val differentialDiagnoses: List<String> = emptyList(),
    val complications: List<String> = emptyList(),
    val treatments: TreatmentInfo,
    val prognosis: String? = null,
    val prevention: List<String> = emptyList(),
    val relatedDrugIds: List<String> = emptyList(),
    val relatedDiseaseIds: List<String> = emptyList(),
) {
    fun toDisease(id: String, revisedAt: String): Disease =
        Disease(
            id = id,
            name = name,
            nameKana = nameKana,
            nameEnglish = nameEnglish,
            icd10Chapter = icd10Chapter,
            medicalDepartment = medicalDepartment,
            chronicity = chronicity,
            infectious = infectious,
            synonyms = synonyms,
            summary = summary,
            epidemiology = epidemiology,
            etiology = etiology,
            symptoms = symptoms,
            diagnosticCriteria = diagnosticCriteria,
            requiredExams = requiredExams,
            severityGrading = severityGrading,
            differentialDiagnoses = differentialDiagnoses,
            complications = complications,
            treatments = treatments,
            prognosis = prognosis,
            prevention = prevention,
            relatedDrugIds = relatedDrugIds,
            relatedDiseaseIds = relatedDiseaseIds,
            revisedAt = revisedAt,
        )
}
