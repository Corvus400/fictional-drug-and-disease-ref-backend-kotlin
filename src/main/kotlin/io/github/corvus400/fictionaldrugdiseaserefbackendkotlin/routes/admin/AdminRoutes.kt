package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.AppJson
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.respondResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.DiseaseRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.DrugRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.Disease
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate

@Serializable
data class WhoAmIResponse(
    val subject: String,
    val scopes: List<String>,
)

fun Route.adminRoutes(
    drugRepository: DrugRepository,
    diseaseRepository: DiseaseRepository,
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
        val drug = call.receive<AdminDrugContentRequest>().toDrug(
            id = "",
            revisedAt = LocalDate.now().toString(),
        )
        call.respondResult(drugRepository.create(drug), successStatus = HttpStatusCode.Created)
    }
    post("/diseases") {
        val disease = call.receive<AdminDiseaseContentRequest>().toDisease(
            id = "",
            revisedAt = LocalDate.now().toString(),
        )
        call.respondResult(diseaseRepository.create(disease), successStatus = HttpStatusCode.Created)
    }
    put("/diseases/{id}") {
        val id = checkNotNull(call.parameters["id"])
        when (val current = diseaseRepository.findByPublicId(id)) {
            is AppResult.Failure -> call.respondResult(current)
            is AppResult.Success -> {
                val disease = call.receive<AdminDiseaseContentRequest>().toDisease(
                    id = id,
                    revisedAt = current.value.revisedAt,
                )
                call.respondResult(diseaseRepository.update(disease))
            }
        }
    }
    patch("/diseases/{id}") {
        val id = checkNotNull(call.parameters["id"])
        when (val current = diseaseRepository.findByPublicId(id)) {
            is AppResult.Failure -> call.respondResult(current)
            is AppResult.Success -> {
                val patch = AppJson.parseToJsonElement(call.receiveText()).jsonObject.withoutServerManagedFields()
                val currentJson = AppJson.parseToJsonElement(AppJson.encodeToString(current.value)).jsonObject
                val patchedJson = mergePatch(currentJson, patch)
                val patched = AppJson.decodeFromString<Disease>(AppJson.encodeToString(patchedJson)).copy(id = id)
                call.respondResult(diseaseRepository.update(patched))
            }
        }
    }
    put("/drugs/{id}") {
        val id = checkNotNull(call.parameters["id"])
        when (val current = drugRepository.findByPublicId(id)) {
            is AppResult.Failure -> call.respondResult(current)
            is AppResult.Success -> {
                val drug = call.receive<AdminDrugContentRequest>().toDrug(
                    id = id,
                    revisedAt = current.value.revisedAt,
                )
                call.respondResult(drugRepository.update(drug))
            }
        }
    }
    patch("/drugs/{id}") {
        val id = checkNotNull(call.parameters["id"])
        when (val current = drugRepository.findByPublicId(id)) {
            is AppResult.Failure -> call.respondResult(current)
            is AppResult.Success -> {
                val patch = AppJson.parseToJsonElement(call.receiveText()).jsonObject.withoutServerManagedFields()
                val currentJson = AppJson.parseToJsonElement(AppJson.encodeToString(current.value)).jsonObject
                val patchedJson = mergePatch(currentJson, patch)
                val patched = AppJson.decodeFromString<Drug>(AppJson.encodeToString(patchedJson)).copy(id = id)
                call.respondResult(drugRepository.update(patched))
            }
        }
    }
    delete("/drugs/{id}") {
        val id = checkNotNull(call.parameters["id"])
        when (val result = drugRepository.delete(id)) {
            is AppResult.Failure -> call.respondResult(result)
            is AppResult.Success -> call.respond(HttpStatusCode.NoContent)
        }
    }
    delete("/diseases/{id}") {
        val id = checkNotNull(call.parameters["id"])
        when (val result = diseaseRepository.delete(id)) {
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
