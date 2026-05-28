package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.common

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.ApiTag
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.ImageStorageConfig
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.DrugRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainException
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.FieldViolation
import io.github.smiley4.ktoropenapi.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

fun Route.dosageFormImageRoutes() {
    get("/images/dosage-forms/{form}", {
        summary = "剤形画像を取得する"
        description = "13 種類の剤形 PNG 画像を返す。?size=S|M|Original でリサイズ対応。"
        tags(ApiTag.DRUG.tagName)
        request {
            pathParameter<String>("form") {
                description = "剤形名 (例: tablet, capsule)"
            }
            queryParameter<String>("size") {
                description = "S|M|Original (省略時 Original)"
                required = false
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "指定 form の PNG 画像"
                body<ByteArray> {
                    mediaTypes(ContentType.Image.PNG)
                }
            }
            code(HttpStatusCode.NotFound) {
                description = "指定 form の画像が存在しない"
            }
            code(HttpStatusCode.UnprocessableEntity) {
                description = "size に S/M/Original 以外を指定"
            }
        }
    }) {
        val form = checkNotNull(call.parameters["form"])
        val size = call.parseImageSize()
        val bytes = loadImageBytes(resourcePath = "images/dosage_form/$form.png", size = size)
            ?: throw DomainException(DomainError.NotFound(resource = "dosage-form-image", id = form))
        call.respondBytes(bytes, ContentType.Image.PNG)
    }
}

fun Route.drugImageRoutes(
    repository: DrugRepository,
    imageStorageConfig: ImageStorageConfig,
) {
    get("/images/drugs/{drugId}", {
        summary = "医薬品画像を取得する"
        description = "指定 drugId のアップロード画像、同梱固有画像、剤形既定画像の順で PNG 画像を返す。"
        tags(ApiTag.DRUG.tagName)
        request {
            pathParameter<String>("drugId") {
                description = "医薬品 ID (例: drug_0080, drug_0089)"
            }
            queryParameter<String>("size") {
                description = "S|M|Original (省略時 Original)"
                required = false
            }
        }
        response {
            code(HttpStatusCode.OK) {
                description = "指定 drugId の PNG 画像"
                body<ByteArray> {
                    mediaTypes(ContentType.Image.PNG)
                }
            }
            code(HttpStatusCode.NotFound) {
                description = "指定 drugId の画像が存在しない"
            }
            code(HttpStatusCode.UnprocessableEntity) {
                description = "size に S/M/Original 以外を指定"
            }
        }
    }) {
        val drugId = checkNotNull(call.parameters["drugId"])
        val size = call.parseImageSize()
        val drug = when (val result = repository.findByPublicId(drugId)) {
            is AppResult.Failure -> throw DomainException(result.error)
            is AppResult.Success -> result.value
        }
        val uploadedPath = imageStorageConfig.uploadDir.resolve("$drugId.png").normalize()
        val bytes = loadImageBytes(filePath = uploadedPath, resourcePath = "images/drug/$drugId.png", size = size)
            ?: loadImageBytes(resourcePath = "images/dosage_form/${drug.dosageForm.serialName}.png", size = size)
            ?: throw DomainException(DomainError.NotFound(resource = "drug-image", id = drugId))
        call.respondBytes(bytes, ContentType.Image.PNG)
    }
}

private fun ApplicationCall.parseImageSize(): ImageSize =
    when (val raw = request.queryParameters["size"]) {
        "S" -> ImageSize.S
        "M" -> ImageSize.M
        null, "Original" -> ImageSize.ORIGINAL
        else -> throw DomainException(
            DomainError.Validation(
                listOf(FieldViolation(field = "size", reason = "Unknown image size: $raw")),
            ),
        )
    }

private fun loadImageBytes(
    filePath: Path? = null,
    resourcePath: String,
    size: ImageSize,
): ByteArray? {
    val originalBytes = filePath
        ?.takeIf { Files.isRegularFile(it) }
        ?.let { Files.readAllBytes(it) }
        ?: Thread.currentThread().contextClassLoader
            .getResourceAsStream(resourcePath)
            ?.use { it.readBytes() }
        ?: return null
    if (size == ImageSize.ORIGINAL) return originalBytes

    val originalImage = ImageIO.read(ByteArrayInputStream(originalBytes)) ?: return null
    val resizedImage = ImageResizer.resize(originalImage, size)
    return ByteArrayOutputStream().use { output ->
        ImageIO.write(resizedImage, "PNG", output)
        output.toByteArray()
    }
}
