package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.Disclaimer
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.Chronicity
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.Icd10Chapter
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.MedicalDepartment
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested.DiagnosticCriteriaInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested.EpidemiologyInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested.Exam
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested.SeverityInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested.SymptomInfo
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.nested.TreatmentInfo
import kotlinx.serialization.Serializable

/**
 * 疾患エンティティ: 単一疾患の全フィールドを保持するルートモデル。
 *
 * `/diseases/{id}` 詳細レスポンス本体および fixture の最小単位。分類軸
 * (`icd10Chapter` / `chronicity` / `infectious` / `medicalDepartment`) と詳細セクション
 * (`symptoms` / `diagnosticCriteria` / `treatments` 等) を内包する。
 * 仕様: linked-bubbling-sun-disease.md `共通フィールド` 節。
 */
@Serializable
data class Disease(
    val id: String,
    val name: String,
    val nameKana: String,
    val nameEnglish: String? = null,
    val icd10Chapter: Icd10Chapter,
    val medicalDepartment: List<MedicalDepartment>,
    val chronicity: Chronicity,
    /** 感染性疾患か (`true` のとき他者への伝播能を持つ。`/diseases?infectious=true` のフィルタ条件)。 */
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
    /** 最終改訂日 (`YYYY-MM-DD` 形式の固定文字列)。`IsoDateFormatter` が生成する ISO 8601 日付。 */
    val revisedAt: String,
    val disclaimer: String = Disclaimer.SHORT,
)
