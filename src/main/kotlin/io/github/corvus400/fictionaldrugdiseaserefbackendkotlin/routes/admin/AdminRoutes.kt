package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.AppJson
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.ImageStorageConfig
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.parseEtag
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.respondResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.DiseaseRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.DrugRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.FieldViolation
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.Disease
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.buildDrugImageUrl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.nio.file.Files
import java.time.LocalDate

@Serializable
data class WhoAmIResponse(
    val subject: String,
    val scopes: List<String>,
)

@Suppress("CyclomaticComplexMethod")
fun Route.adminRoutes(
    drugRepository: DrugRepository,
    diseaseRepository: DiseaseRepository,
    imageStorageConfig: ImageStorageConfig,
) {
    get("/whoami") {
        val principal = checkNotNull(call.principal<JWTPrincipal>())
        val scopes = principal.payload.getClaim("scope")
            .asString()
            ?.split(" ")
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        call.respond(
            WhoAmIResponse(
                subject = principal.payload.getClaim("sub").asString(),
                scopes = scopes,
            ),
        )
    }
    post("/drugs") {
        when (val request = call.receiveAdminContent<AdminDrugContentRequest>()) {
            is AppResult.Failure -> call.respondResult(request)
            is AppResult.Success -> {
                val drug = request.value.toDrug(
                    id = "",
                    revisedAt = LocalDate.now().toString(),
                )
                call.respondResult(
                    drugRepository.create(drug),
                    successStatus = HttpStatusCode.Created,
                )
            }
        }
    }
    post("/diseases") {
        when (val request = call.receiveAdminContent<AdminDiseaseContentRequest>()) {
            is AppResult.Failure -> call.respondResult(request)
            is AppResult.Success -> {
                val disease = request.value.toDisease(
                    id = "",
                    revisedAt = LocalDate.now().toString(),
                )
                call.respondResult(
                    diseaseRepository.create(disease),
                    successStatus = HttpStatusCode.Created,
                )
            }
        }
    }
    put("/diseases/{id}") {
        val id = when (val parsedId = call.requireDiseaseId()) {
            is AppResult.Failure -> return@put call.respondResult(parsedId)
            is AppResult.Success -> parsedId.value
        }
        val expectedUpdatedAt = when (val ifMatch = call.requireIfMatch()) {
            is AppResult.Failure -> return@put call.respondResult(ifMatch)
            is AppResult.Success -> ifMatch.value
        }
        when (val current = diseaseRepository.findByPublicId(id)) {
            is AppResult.Failure -> call.respondResult(current)
            is AppResult.Success -> {
                when (val request = call.receiveAdminContent<AdminDiseaseContentRequest>()) {
                    is AppResult.Failure -> call.respondResult(request)
                    is AppResult.Success -> {
                        val disease = request.value.toDisease(
                            id = id,
                            revisedAt = currentRevisionDate(),
                        )
                        call.respondResult(
                            diseaseRepository.update(disease, expectedUpdatedAt),
                        )
                    }
                }
            }
        }
    }
    patch("/diseases/{id}") {
        val id = when (val parsedId = call.requireDiseaseId()) {
            is AppResult.Failure -> return@patch call.respondResult(parsedId)
            is AppResult.Success -> parsedId.value
        }
        when (val contentType = call.requireMergePatchContentType()) {
            is AppResult.Failure -> return@patch call.respondResult(contentType)
            is AppResult.Success -> Unit
        }
        val expectedUpdatedAt = when (val ifMatch = call.requireIfMatch()) {
            is AppResult.Failure -> return@patch call.respondResult(ifMatch)
            is AppResult.Success -> ifMatch.value
        }
        when (val current = diseaseRepository.findByPublicId(id)) {
            is AppResult.Failure -> call.respondResult(current)
            is AppResult.Success -> {
                val patch = AppJson.parseToJsonElement(call.receiveText()).jsonObject.withoutServerManagedFields()
                val currentJson = AppJson.parseToJsonElement(AppJson.encodeToString(current.value)).jsonObject
                val patchedJson = mergePatch(currentJson, patch)
                when (
                    val patched = decodeAdminContent<Disease>(patchedJson).mapSuccess {
                        it.copy(id = id, revisedAt = currentRevisionDate())
                    }
                ) {
                    is AppResult.Failure -> call.respondResult(patched)
                    is AppResult.Success -> {
                        call.respondResult(
                            diseaseRepository.update(patched.value, expectedUpdatedAt),
                        )
                    }
                }
            }
        }
    }
    put("/drugs/{id}") {
        val id = when (val parsedId = call.requireDrugId()) {
            is AppResult.Failure -> return@put call.respondResult(parsedId)
            is AppResult.Success -> parsedId.value
        }
        val expectedUpdatedAt = when (val ifMatch = call.requireIfMatch()) {
            is AppResult.Failure -> return@put call.respondResult(ifMatch)
            is AppResult.Success -> ifMatch.value
        }
        when (val current = drugRepository.findByPublicId(id)) {
            is AppResult.Failure -> call.respondResult(current)
            is AppResult.Success -> {
                when (val request = call.receiveAdminContent<AdminDrugContentRequest>()) {
                    is AppResult.Failure -> call.respondResult(request)
                    is AppResult.Success -> {
                        val drug = request.value.toDrug(
                            id = id,
                            revisedAt = currentRevisionDate(),
                            hasUploadedDrugImage = hasUploadedDrugImage(id, imageStorageConfig),
                        )
                        call.respondResult(
                            drugRepository.update(drug, expectedUpdatedAt)
                        )
                    }
                }
            }
        }
    }
    patch("/drugs/{id}") {
        val id = when (val parsedId = call.requireDrugId()) {
            is AppResult.Failure -> return@patch call.respondResult(parsedId)
            is AppResult.Success -> parsedId.value
        }
        when (val contentType = call.requireMergePatchContentType()) {
            is AppResult.Failure -> return@patch call.respondResult(contentType)
            is AppResult.Success -> Unit
        }
        val expectedUpdatedAt = when (val ifMatch = call.requireIfMatch()) {
            is AppResult.Failure -> return@patch call.respondResult(ifMatch)
            is AppResult.Success -> ifMatch.value
        }
        when (val current = drugRepository.findByPublicId(id)) {
            is AppResult.Failure -> call.respondResult(current)
            is AppResult.Success -> {
                val patch = AppJson.parseToJsonElement(call.receiveText()).jsonObject.withoutServerManagedFields()
                val currentJson = AppJson.parseToJsonElement(AppJson.encodeToString(current.value)).jsonObject
                val patchedJson = mergePatch(currentJson, patch)
                when (
                    val patched = decodeAdminContent<Drug>(patchedJson).mapSuccess {
                        it.copy(
                            id = id,
                            revisedAt = currentRevisionDate(),
                            imageUrl = buildDrugImageUrl(
                                drugId = id,
                                dosageForm = it.dosageForm,
                                hasUploadedDrugImage = hasUploadedDrugImage(id, imageStorageConfig),
                            ),
                        )
                    }
                ) {
                    is AppResult.Failure -> call.respondResult(patched)
                    is AppResult.Success -> {
                        call.respondResult(
                            drugRepository.update(patched.value, expectedUpdatedAt)
                        )
                    }
                }
            }
        }
    }
    post("/drugs/{id}/image") {
        val id = when (val parsedId = call.requireDrugId()) {
            is AppResult.Failure -> return@post call.respondResult(parsedId)
            is AppResult.Success -> parsedId.value
        }
        val expectedUpdatedAt = when (val ifMatch = call.requireIfMatch()) {
            is AppResult.Failure -> return@post call.respondResult(ifMatch)
            is AppResult.Success -> ifMatch.value
        }
        when (val current = drugRepository.findByPublicId(id)) {
            is AppResult.Failure -> call.respondResult(current)
            is AppResult.Success -> {
                when (
                    val tempImage = call.receivePngUploadTempFile(
                        uploadDir = imageStorageConfig.uploadDir,
                        drugId = id,
                        maxUploadBytes = imageStorageConfig.maxUploadBytes,
                    )
                ) {
                    is AppResult.Failure -> call.respondResult(tempImage)
                    is AppResult.Success -> {
                        val imagePath = imageStorageConfig.uploadDir.resolve("$id.png").normalize()
                        when (val replacement = replaceUploadedImage(tempImage.value, imagePath)) {
                            is AppResult.Failure -> call.respondResult(replacement)
                            is AppResult.Success -> {
                                val updatedDrug = current.value.copy(
                                    revisedAt = currentRevisionDate(),
                                    imageUrl = "/v1/images/drugs/$id?size=Original",
                                )
                                when (val update = drugRepository.update(updatedDrug, expectedUpdatedAt)) {
                                    is AppResult.Failure -> {
                                        when (
                                            val rollback = rollbackUploadedImageReplacement(
                                                imagePath = imagePath,
                                                replacement = replacement.value,
                                            )
                                        ) {
                                            is AppResult.Failure -> call.respondResult(rollback)
                                            is AppResult.Success -> call.respondResult(update)
                                        }
                                    }
                                    is AppResult.Success -> {
                                        when (val commit = commitUploadedImageReplacement(replacement.value)) {
                                            is AppResult.Failure -> call.respondResult(commit)
                                            is AppResult.Success -> call.respondResult(update)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    delete("/drugs/{id}") {
        val id = when (val parsedId = call.requireDrugId()) {
            is AppResult.Failure -> return@delete call.respondResult(parsedId)
            is AppResult.Success -> parsedId.value
        }
        val expectedUpdatedAt = when (val ifMatch = call.requireIfMatch()) {
            is AppResult.Failure -> return@delete call.respondResult(ifMatch)
            is AppResult.Success -> ifMatch.value
        }
        when (val result = drugRepository.delete(id, expectedUpdatedAt)) {
            is AppResult.Failure -> call.respondResult(result)
            is AppResult.Success -> {
                when (val deleteImage = deleteUploadedImage(imageStorageConfig.uploadDir.resolve("$id.png"))) {
                    is AppResult.Failure -> call.respondResult(deleteImage)
                    is AppResult.Success -> call.respond(HttpStatusCode.NoContent)
                }
            }
        }
    }
    delete("/diseases/{id}") {
        val id = when (val parsedId = call.requireDiseaseId()) {
            is AppResult.Failure -> return@delete call.respondResult(parsedId)
            is AppResult.Success -> parsedId.value
        }
        val expectedUpdatedAt = when (val ifMatch = call.requireIfMatch()) {
            is AppResult.Failure -> return@delete call.respondResult(ifMatch)
            is AppResult.Success -> ifMatch.value
        }
        when (val result = diseaseRepository.delete(id, expectedUpdatedAt)) {
            is AppResult.Failure -> call.respondResult(result)
            is AppResult.Success -> call.respond(HttpStatusCode.NoContent)
        }
    }
}

private val serverManagedJsonFields = setOf(
    "id",
    "revised_at",
    "image_url",
    "disclaimer",
    "created_at",
    "updated_at",
)

private fun JsonObject.withoutServerManagedFields(): JsonObject =
    JsonObject(filterKeys { key -> key !in serverManagedJsonFields })

private fun mergePatch(
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

private suspend inline fun <reified T : Any> ApplicationCall.receiveAdminContent(): AppResult<T> =
    validationFailureAsResult(field = "body", fallbackReason = "Invalid request body") { receive<T>() }

private fun ApplicationCall.requireIfMatch(): AppResult<LocalDateTime> {
    val raw = request.headers[HttpHeaders.IfMatch]
        ?: return AppResult.Failure(DomainError.PreconditionFailed("If-Match header is required."))
    val parsed = parseEtag(raw)
        ?: return AppResult.Failure(DomainError.PreconditionFailed("If-Match header is invalid."))
    return AppResult.Success(parsed)
}

private fun ApplicationCall.requireMergePatchContentType(): AppResult<Unit> {
    val expected = ContentType("application", "merge-patch+json")
    val actual = request.contentType().withoutParameters()
    return if (actual == expected) {
        AppResult.Success(Unit)
    } else {
        AppResult.Failure(
            DomainError.UnsupportedMediaType("PATCH requests must use application/merge-patch+json."),
        )
    }
}

private fun ApplicationCall.requireDrugId(): AppResult<String> =
    requirePathId(expectedPrefix = "drug")

private fun ApplicationCall.requireDiseaseId(): AppResult<String> =
    requirePathId(expectedPrefix = "disease")

private fun ApplicationCall.requirePathId(expectedPrefix: String): AppResult<String> {
    val id = parameters["id"]
        ?: return AppResult.Failure(
            DomainError.Validation(listOf(FieldViolation(field = "id", reason = "Path id is required."))),
        )
    val pattern = Regex("""$expectedPrefix\_\d{4}""")
    return if (pattern.matches(id)) {
        AppResult.Success(id)
    } else {
        AppResult.Failure(
            DomainError.Validation(listOf(FieldViolation(field = "id", reason = "Invalid $expectedPrefix id: $id"))),
        )
    }
}

private fun currentRevisionDate(): String = LocalDate.now().toString()

private fun hasUploadedDrugImage(
    id: String,
    imageStorageConfig: ImageStorageConfig,
): Boolean =
    Files.isRegularFile(imageStorageConfig.uploadDir.resolve("$id.png").normalize())

private suspend inline fun <reified T : Any> decodeAdminContent(jsonObject: JsonObject): AppResult<T> =
    validationFailureAsResult(field = "body", fallbackReason = "Invalid request body") {
        AppJson.decodeFromString<T>(AppJson.encodeToString(jsonObject))
    }

private inline fun <T : Any, R : Any> AppResult<T>.mapSuccess(transform: (T) -> R): AppResult<R> =
    when (this) {
        is AppResult.Failure -> this
        is AppResult.Success -> AppResult.Success(transform(value))
    }
