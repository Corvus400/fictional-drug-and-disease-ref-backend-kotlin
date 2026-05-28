package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.AppJson
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.FieldViolation
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.Chronicity
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.Icd10Chapter
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.enums.MedicalDepartment
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.DosageForm
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RegulatoryClass
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.enums.RouteOfAdministration
import io.ktor.http.ContentType
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val serverManagedJsonFields = setOf(
    "id",
    "revised_at",
    "image_url",
    "disclaimer",
    "created_at",
    "updated_at",
)

internal fun JsonObject.withoutServerManagedFields(): JsonObject =
    JsonObject(filterKeys { key -> key !in serverManagedJsonFields })

internal fun mergePatch(
    current: JsonObject,
    patch: JsonObject,
): JsonObject {
    val merged = current.toMutableMap()
    patch.forEach { (key, value) ->
        when {
            value is JsonNull -> merged.remove(key)
            value is JsonObject && merged[key] is JsonObject -> merged[key] =
                mergePatch(merged.getValue(key).jsonObject, value)
            else -> merged[key] = value
        }
    }
    return JsonObject(merged)
}

internal suspend inline fun <reified T : Any> ApplicationCall.receiveAdminContent(): AppResult<T> =
    when (val contentType = requireJsonContentType()) {
        is AppResult.Failure -> contentType
        is AppResult.Success -> when (
            val jsonObject = validationFailureAsResult(field = "body", fallbackReason = "Invalid request body") {
                AppJson.parseToJsonElement(receiveText()).jsonObject.withoutServerManagedFields()
            }
        ) {
            is AppResult.Failure -> jsonObject
            is AppResult.Success -> decodeAdminContent<T>(jsonObject.value)
        }
    }

internal suspend inline fun <reified T : Any> decodeAdminContent(jsonObject: JsonObject): AppResult<T> =
    when (val validation = validateAdminJsonObject<T>(jsonObject)) {
        is AppResult.Failure -> validation
        is AppResult.Success -> validationFailureAsResult(field = "body", fallbackReason = "Invalid request body") {
            AppJson.decodeFromJsonElement<T>(jsonObject)
        }
    }

internal inline fun <T : Any, R : Any> AppResult<T>.mapSuccess(transform: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Failure -> this
        is AppResult.Success -> AppResult.Success(transform(value))
    }

private fun ApplicationCall.requireJsonContentType(): AppResult<Unit> =
    if (request.contentType().withoutParameters() == ContentType.Application.Json) {
        AppResult.Success(Unit)
    } else {
        AppResult.Failure(
            DomainError.UnsupportedMediaType("Admin content requests must use application/json."),
        )
    }

private inline fun <reified T : Any> validateAdminJsonObject(jsonObject: JsonObject): AppResult<Unit> =
    when (T::class) {
        AdminDrugContentRequest::class -> validateAdminDrugJsonObject(jsonObject)
        AdminDiseaseContentRequest::class -> validateAdminDiseaseJsonObject(jsonObject)
        else -> AppResult.Success(Unit)
    }

private fun validateAdminDrugJsonObject(jsonObject: JsonObject): AppResult<Unit> =
    firstFailure(
        validateEnumValue(
            jsonObject = jsonObject,
            field = "dosage_form",
            allowedValues = DosageForm.entries.map { it.serialName }.toSet(),
        ),
        validateEnumValue(
            jsonObject = jsonObject,
            field = "route_of_administration",
            allowedValues = RouteOfAdministration.entries.map { it.serialName }.toSet(),
        ),
        validateEnumArray(
            jsonObject = jsonObject,
            field = "regulatory_class",
            allowedValues = RegulatoryClass.entries.map { it.serialName }.toSet(),
        ),
    )

private fun validateAdminDiseaseJsonObject(jsonObject: JsonObject): AppResult<Unit> =
    firstFailure(
        validateEnumValue(
            jsonObject = jsonObject,
            field = "icd10_chapter",
            allowedValues = Icd10Chapter.entries.map { it.serialName }.toSet(),
        ),
        validateEnumArray(
            jsonObject = jsonObject,
            field = "medical_department",
            allowedValues = MedicalDepartment.entries.map { it.serialName }.toSet(),
        ),
        validateEnumValue(
            jsonObject = jsonObject,
            field = "chronicity",
            allowedValues = Chronicity.entries.map { it.serialName }.toSet(),
        ),
    )

private fun firstFailure(vararg results: AppResult<Unit>): AppResult<Unit> =
    results.firstOrNull { it is AppResult.Failure } ?: AppResult.Success(Unit)

private fun validateEnumValue(
    jsonObject: JsonObject,
    field: String,
    allowedValues: Set<String>,
): AppResult<Unit> {
    val value = jsonObject[field] ?: return AppResult.Success(Unit)
    val raw = value.jsonStringOrNull()
    return if (raw != null && raw in allowedValues) {
        AppResult.Success(Unit)
    } else {
        invalidEnumField(field)
    }
}

private fun validateEnumArray(
    jsonObject: JsonObject,
    field: String,
    allowedValues: Set<String>,
): AppResult<Unit> {
    val value = jsonObject[field] ?: return AppResult.Success(Unit)
    val items = value as? JsonArray ?: return invalidEnumField(field)
    return if (items.all { item -> item.jsonStringOrNull() in allowedValues }) {
        AppResult.Success(Unit)
    } else {
        invalidEnumField(field)
    }
}

private fun JsonElement.jsonStringOrNull(): String? =
    runCatching { jsonPrimitive.contentOrNull }.getOrNull()

private fun invalidEnumField(field: String): AppResult.Failure =
    AppResult.Failure(DomainError.Validation(listOf(FieldViolation(field = field, reason = "invalid"))))
