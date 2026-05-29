package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.ADMIN_SECURITY_SCHEME
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.ADMIN_SPEC_NAME
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
import io.github.smiley4.ktoropenapi.config.RequestConfig
import io.github.smiley4.ktoropenapi.config.ResponsesConfig
import io.github.smiley4.ktoropenapi.config.RouteConfig
import io.github.smiley4.ktoropenapi.delete
import io.github.smiley4.ktoropenapi.get
import io.github.smiley4.ktoropenapi.patch
import io.github.smiley4.ktoropenapi.post
import io.github.smiley4.ktoropenapi.put
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonObject
import java.nio.file.Files
import java.time.LocalDate

@Serializable
data class WhoAmIResponse(
    val subject: String,
    val scopes: List<String>,
)

private val whoAmIDocs: RouteConfig.() -> Unit = {
    summary = "管理 API トークンの principal を確認する"
    description = "JWT principal の subject と scope を返す。CMS が管理トークンの有効性確認に使う。"
    tags("Admin")
    specName = ADMIN_SPEC_NAME
    securitySchemeNames(ADMIN_SECURITY_SCHEME)
    response {
        code(HttpStatusCode.OK) {
            description = "JWT principal 情報"
            body<WhoAmIResponse>()
        }
        code(HttpStatusCode.Unauthorized) {
            description = "JWT が無効または欠落している"
        }
    }
}

private fun RequestConfig.drugIdPath() {
    pathParameter<String>("id") { description = "医薬品 ID (`drug_NNNN` 形式)" }
}

private fun RequestConfig.diseaseIdPath() {
    pathParameter<String>("id") { description = "疾病 ID (`disease_NNNN` 形式)" }
}

private fun RequestConfig.ifMatchHeader() {
    headerParameter<String>("If-Match") {
        description = "対象リソースの現在の ETag。楽観ロック (楽観的並行制御) のため更新系で必須。"
        required = true
    }
}

private fun ResponsesConfig.validationError() {
    code(HttpStatusCode.BadRequest) { description = "リクエスト検証エラー (problem+json)" }
}

private fun ResponsesConfig.authErrors() {
    code(HttpStatusCode.Unauthorized) { description = "JWT が無効または欠落している" }
    code(HttpStatusCode.Forbidden) { description = "admin scope が不足している" }
}

private fun ResponsesConfig.notFoundError() {
    code(HttpStatusCode.NotFound) { description = "指定 id のリソースが存在しない" }
}

private fun ResponsesConfig.preconditionError() {
    code(HttpStatusCode.PreconditionFailed) { description = "If-Match の欠落/不一致 (楽観ロック失敗)" }
}

private fun ResponsesConfig.mergePatchMediaError() {
    code(HttpStatusCode.UnsupportedMediaType) {
        description = "Content-Type が `application/merge-patch+json` でない"
    }
}

private val drugCreateDocs: RouteConfig.() -> Unit = {
    summary = "医薬品を新規作成する"
    description = "医薬品を新規作成する。id はサーバ側で採番する。"
    tags("Admin")
    specName = ADMIN_SPEC_NAME
    securitySchemeNames(ADMIN_SECURITY_SCHEME)
    request { body<AdminDrugContentRequest> { description = "作成する医薬品の内容" } }
    response {
        code(HttpStatusCode.Created) {
            description = "作成された医薬品"
            body<Drug>()
        }
        validationError()
        authErrors()
    }
}

private val diseaseCreateDocs: RouteConfig.() -> Unit = {
    summary = "疾病を新規作成する"
    description = "疾病を新規作成する。id はサーバ側で採番する。"
    tags("Admin")
    specName = ADMIN_SPEC_NAME
    securitySchemeNames(ADMIN_SECURITY_SCHEME)
    request { body<AdminDiseaseContentRequest> { description = "作成する疾病の内容" } }
    response {
        code(HttpStatusCode.Created) {
            description = "作成された疾病"
            body<Disease>()
        }
        validationError()
        authErrors()
    }
}

private val drugUpdateDocs: RouteConfig.() -> Unit = {
    summary = "医薬品を全体更新する"
    description = "医薬品を全フィールド置換で更新する。If-Match による楽観ロック必須。"
    tags("Admin")
    specName = ADMIN_SPEC_NAME
    securitySchemeNames(ADMIN_SECURITY_SCHEME)
    request {
        drugIdPath()
        ifMatchHeader()
        body<AdminDrugContentRequest> { description = "置換後の医薬品の内容" }
    }
    response {
        code(HttpStatusCode.OK) {
            description = "更新された医薬品"
            body<Drug>()
        }
        validationError()
        authErrors()
        notFoundError()
        preconditionError()
    }
}

private val diseaseUpdateDocs: RouteConfig.() -> Unit = {
    summary = "疾病を全体更新する"
    description = "疾病を全フィールド置換で更新する。If-Match による楽観ロック必須。"
    tags("Admin")
    specName = ADMIN_SPEC_NAME
    securitySchemeNames(ADMIN_SECURITY_SCHEME)
    request {
        diseaseIdPath()
        ifMatchHeader()
        body<AdminDiseaseContentRequest> { description = "置換後の疾病の内容" }
    }
    response {
        code(HttpStatusCode.OK) {
            description = "更新された疾病"
            body<Disease>()
        }
        validationError()
        authErrors()
        notFoundError()
        preconditionError()
    }
}

private val drugPatchDocs: RouteConfig.() -> Unit = {
    summary = "医薬品を部分更新する"
    description = "JSON Merge Patch (RFC 7386) で医薬品を部分更新する。" +
        "Content-Type は `application/merge-patch+json`。If-Match による楽観ロック必須。"
    tags("Admin")
    specName = ADMIN_SPEC_NAME
    securitySchemeNames(ADMIN_SECURITY_SCHEME)
    request {
        drugIdPath()
        ifMatchHeader()
        body<AdminDrugContentRequest> { description = "更新したいフィールドのみを含む merge-patch ドキュメント" }
    }
    response {
        code(HttpStatusCode.OK) {
            description = "更新された医薬品"
            body<Drug>()
        }
        validationError()
        authErrors()
        notFoundError()
        preconditionError()
        mergePatchMediaError()
    }
}

private val diseasePatchDocs: RouteConfig.() -> Unit = {
    summary = "疾病を部分更新する"
    description = "JSON Merge Patch (RFC 7386) で疾病を部分更新する。" +
        "Content-Type は `application/merge-patch+json`。If-Match による楽観ロック必須。"
    tags("Admin")
    specName = ADMIN_SPEC_NAME
    securitySchemeNames(ADMIN_SECURITY_SCHEME)
    request {
        diseaseIdPath()
        ifMatchHeader()
        body<AdminDiseaseContentRequest> { description = "更新したいフィールドのみを含む merge-patch ドキュメント" }
    }
    response {
        code(HttpStatusCode.OK) {
            description = "更新された疾病"
            body<Disease>()
        }
        validationError()
        authErrors()
        notFoundError()
        preconditionError()
        mergePatchMediaError()
    }
}

private val drugImageUploadDocs: RouteConfig.() -> Unit = {
    summary = "医薬品画像をアップロードする"
    description = "医薬品の画像 (PNG のみ) を multipart で差し替える。" +
        "フォームフィールド名は `file`。If-Match による楽観ロック必須。"
    tags("Admin")
    specName = ADMIN_SPEC_NAME
    securitySchemeNames(ADMIN_SECURITY_SCHEME)
    request {
        drugIdPath()
        ifMatchHeader()
        body<ByteArray> {
            description = "PNG 画像 (multipart/form-data, フィールド名 `file`)"
            mediaTypes(ContentType.MultiPart.FormData)
        }
    }
    response {
        code(HttpStatusCode.OK) {
            description = "画像を更新した医薬品"
            body<Drug>()
        }
        validationError()
        authErrors()
        notFoundError()
        preconditionError()
        code(HttpStatusCode.UnsupportedMediaType) { description = "PNG 以外の画像形式" }
    }
}

private val drugDeleteDocs: RouteConfig.() -> Unit = {
    summary = "医薬品を削除する"
    description = "医薬品を削除する。If-Match による楽観ロック必須。"
    tags("Admin")
    specName = ADMIN_SPEC_NAME
    securitySchemeNames(ADMIN_SECURITY_SCHEME)
    request {
        drugIdPath()
        ifMatchHeader()
    }
    response {
        code(HttpStatusCode.NoContent) { description = "削除完了" }
        authErrors()
        notFoundError()
        preconditionError()
    }
}

private val diseaseDeleteDocs: RouteConfig.() -> Unit = {
    summary = "疾病を削除する"
    description = "疾病を削除する。If-Match による楽観ロック必須。"
    tags("Admin")
    specName = ADMIN_SPEC_NAME
    securitySchemeNames(ADMIN_SECURITY_SCHEME)
    request {
        diseaseIdPath()
        ifMatchHeader()
    }
    response {
        code(HttpStatusCode.NoContent) { description = "削除完了" }
        authErrors()
        notFoundError()
        preconditionError()
    }
}

@Suppress("CyclomaticComplexMethod")
fun Route.adminRoutes(
    drugRepository: DrugRepository,
    diseaseRepository: DiseaseRepository,
    imageStorageConfig: ImageStorageConfig,
) {
    get("/whoami", whoAmIDocs) {
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
    post("/drugs", drugCreateDocs) {
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
    post("/diseases", diseaseCreateDocs) {
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
    put("/diseases/{id}", diseaseUpdateDocs) {
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
    patch("/diseases/{id}", diseasePatchDocs) {
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
    put("/drugs/{id}", drugUpdateDocs) {
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
    patch("/drugs/{id}", drugPatchDocs) {
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
    post("/drugs/{id}/image", drugImageUploadDocs) {
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
    delete("/drugs/{id}", drugDeleteDocs) {
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
    delete("/diseases/{id}", diseaseDeleteDocs) {
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
