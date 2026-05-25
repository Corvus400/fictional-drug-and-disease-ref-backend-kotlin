package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 診療科区分: `Disease.medicalDepartment` で使用する 16 値の enum。
 *
 * 内科 / 循環器内科 / ... / 感染症科。`/diseases?department=<value>` クエリでフィルタ可能。
 * `fromSerialName` で SerialName からの逆引きも提供する。
 */
@Serializable
enum class MedicalDepartment {
    /** 内科 */
    @SerialName("internal_medicine")
    INTERNAL_MEDICINE,

    /** 循環器内科 */
    @SerialName("cardiology")
    CARDIOLOGY,

    /** 消化器内科 */
    @SerialName("gastroenterology")
    GASTROENTEROLOGY,

    /** 内分泌代謝科 */
    @SerialName("endocrinology")
    ENDOCRINOLOGY,

    /** 神経内科 */
    @SerialName("neurology")
    NEUROLOGY,

    /** 精神科 */
    @SerialName("psychiatry")
    PSYCHIATRY,

    /** 外科 */
    @SerialName("surgery")
    SURGERY,

    /** 整形外科 */
    @SerialName("orthopedics")
    ORTHOPEDICS,

    /** 皮膚科 */
    @SerialName("dermatology")
    DERMATOLOGY,

    /** 眼科 */
    @SerialName("ophthalmology")
    OPHTHALMOLOGY,

    /** 耳鼻咽喉科 */
    @SerialName("otolaryngology")
    OTOLARYNGOLOGY,

    /** 泌尿器科 */
    @SerialName("urology")
    UROLOGY,

    /** 婦人科 */
    @SerialName("gynecology")
    GYNECOLOGY,

    /** 小児科 */
    @SerialName("pediatrics")
    PEDIATRICS,

    /** 救急科 */
    @SerialName("emergency")
    EMERGENCY,

    /** 感染症科 */
    @SerialName("infectious_disease")
    INFECTIOUS_DISEASE,
    ;

    /**
     * `/diseases?department=<value>` クエリフィルタで用いる英語 snake_case (`@SerialName` 値)。
     * 列挙子の宣言順序が [descriptor] の要素順と一致するため、新しい診療科を追加しても同期漏れが起きない。
     */
    val serialName: String
        get() = serializer().descriptor.getElementName(index = ordinal)

    companion object {
        /**
         * 英語 snake_case (`@SerialName` 値) から列挙子を逆引きする。未定義キーは `null`。
         */
        fun fromSerialName(key: String): MedicalDepartment? = entries.firstOrNull { it.serialName == key }
    }
}
