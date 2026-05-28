package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.FieldViolation
import io.ktor.http.ContentType
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.utils.io.toByteArray
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

suspend fun ApplicationCall.receivePngUpload(maxUploadBytes: Long): AppResult<ByteArray> {
    var fileBytes: ByteArray? = null
    var fileContentType: ContentType? = null
    receiveMultipart().forEachPart { part ->
        try {
            if (fileBytes == null && part is PartData.FileItem) {
                fileContentType = part.contentType?.withoutParameters()
                fileBytes = part.provider().toByteArray()
            }
        } finally {
            part.dispose()
        }
    }
    val bytes = fileBytes
        ?: return AppResult.Failure(
            DomainError.Validation(listOf(FieldViolation(field = "file", reason = "PNG file is required."))),
        )
    return when {
        fileContentType != ContentType.Image.PNG -> AppResult.Failure(
            DomainError.Validation(
                listOf(FieldViolation(field = "file", reason = "File content type must be image/png."))
            ),
        )
        bytes.size > maxUploadBytes -> AppResult.Failure(
            DomainError.Validation(listOf(FieldViolation(field = "file", reason = "PNG file exceeds maximum size."))),
        )
        !bytes.isPng() -> AppResult.Failure(
            DomainError.Validation(listOf(FieldViolation(field = "file", reason = "File must be a PNG image."))),
        )
        else -> AppResult.Success(bytes)
    }
}

fun writeTempUploadedImage(
    uploadDir: Path,
    drugId: String,
    bytes: ByteArray,
): AppResult<Path> =
    unexpectedFailureAsResult {
        Files.createDirectories(uploadDir)
        val tempPath = Files.createTempFile(uploadDir, "$drugId-", ".tmp")
        Files.write(tempPath, bytes)
        tempPath
    }

fun promoteUploadedImage(
    tempPath: Path,
    imagePath: Path,
): AppResult<Unit> =
    unexpectedFailureAsResult {
        Files.move(tempPath, imagePath, StandardCopyOption.REPLACE_EXISTING)
        Unit
    }

fun deleteUploadedImage(imagePath: Path): AppResult<Unit> =
    unexpectedFailureAsResult {
        Files.deleteIfExists(imagePath)
        Unit
    }

private fun ByteArray.isPng(): Boolean =
    size >= PNG_SIGNATURE.size && PNG_SIGNATURE.indices.all { index -> this[index] == PNG_SIGNATURE[index] }

private val PNG_SIGNATURE = byteArrayOf(
    0x89.toByte(),
    0x50,
    0x4E,
    0x47,
    0x0D,
    0x0A,
    0x1A,
    0x0A,
)
