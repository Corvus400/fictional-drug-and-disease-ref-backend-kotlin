package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.ktor.http.ContentType
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveMultipart
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

suspend fun ApplicationCall.receivePngUploadTempFile(
    uploadDir: Path,
    drugId: String,
    maxUploadBytes: Long,
): AppResult<Path> =
    validationFailureAsResult(
        field = "file",
        fallbackReason = "PNG upload could not be read.",
    ) {
        Files.createDirectories(uploadDir)
        var file: UploadedTempImage? = null
        receiveMultipart(formFieldLimit = MULTIPART_FIELD_LIMIT_BYTES).forEachPart { part ->
            try {
                if (file == null && part is PartData.FileItem) {
                    val tempPath = Files.createTempFile(uploadDir, "$drugId-", ".tmp")
                    val exceedsMaxBytes = part.provider()
                        .toInputStream()
                        .use { input -> writeUploadWithinLimit(input, tempPath, maxUploadBytes) }
                    file = UploadedTempImage(
                        path = tempPath,
                        contentType = part.contentType?.withoutParameters(),
                        exceedsMaxBytes = exceedsMaxBytes,
                    )
                }
            } finally {
                part.dispose()
            }
        }
        val uploaded = file ?: throw ImageValidationException("PNG file is required.")
        when {
            uploaded.contentType != ContentType.Image.PNG -> {
                Files.deleteIfExists(uploaded.path)
                throw ImageValidationException("File content type must be image/png.")
            }
            uploaded.exceedsMaxBytes -> {
                Files.deleteIfExists(uploaded.path)
                throw ImageValidationException("PNG file exceeds maximum size.")
            }
            !uploaded.path.isPng() -> {
                Files.deleteIfExists(uploaded.path)
                throw ImageValidationException("File must be a PNG image.")
            }
            else -> uploaded.path
        }
    }

data class UploadedImageReplacement(val backupPath: Path?)

fun replaceUploadedImage(
    tempPath: Path,
    imagePath: Path,
): AppResult<UploadedImageReplacement> =
    unexpectedFailureAsResult {
        val parent = checkNotNull(imagePath.parent) { "Image path must have a parent directory." }
        Files.createDirectories(parent)
        if (Files.exists(imagePath) && !Files.isRegularFile(imagePath)) {
            Files.deleteIfExists(tempPath)
            error("Image path is not a regular file: $imagePath")
        }
        val backupPath = if (Files.isRegularFile(imagePath)) {
            Files.createTempFile(parent, "${imagePath.fileName}.", ".bak").also { backup ->
                Files.move(imagePath, backup, StandardCopyOption.REPLACE_EXISTING)
            }
        } else {
            null
        }
        try {
            Files.move(tempPath, imagePath, StandardCopyOption.REPLACE_EXISTING)
            UploadedImageReplacement(backupPath)
        } catch (error: java.io.IOException) {
            if (backupPath != null && Files.isRegularFile(backupPath)) {
                Files.move(backupPath, imagePath, StandardCopyOption.REPLACE_EXISTING)
            }
            Files.deleteIfExists(tempPath)
            throw error
        }
    }

fun rollbackUploadedImageReplacement(
    imagePath: Path,
    replacement: UploadedImageReplacement,
): AppResult<Unit> =
    unexpectedFailureAsResult {
        Files.deleteIfExists(imagePath)
        replacement.backupPath
            ?.takeIf { Files.isRegularFile(it) }
            ?.let { backupPath -> Files.move(backupPath, imagePath, StandardCopyOption.REPLACE_EXISTING) }
        Unit
    }

fun commitUploadedImageReplacement(replacement: UploadedImageReplacement): AppResult<Unit> =
    unexpectedFailureAsResult {
        replacement.backupPath?.let(Files::deleteIfExists)
        Unit
    }

fun deleteUploadedImage(imagePath: Path): AppResult<Unit> =
    unexpectedFailureAsResult {
        Files.deleteIfExists(imagePath)
        Unit
    }

private fun ByteArray.isPng(): Boolean =
    size >= PNG_SIGNATURE.size && PNG_SIGNATURE.indices.all { index -> this[index] == PNG_SIGNATURE[index] }

private fun Path.isPng(): Boolean {
    if (Files.size(this) < PNG_SIGNATURE.size) return false
    val header = ByteArray(PNG_SIGNATURE.size)
    Files.newInputStream(this).use { input ->
        if (input.read(header) != PNG_SIGNATURE.size) return false
    }
    return header.isPng()
}

private data class UploadedTempImage(
    val path: Path,
    val contentType: ContentType?,
    val exceedsMaxBytes: Boolean,
)

private class ImageValidationException(message: String) : IllegalArgumentException(message)

private fun writeUploadWithinLimit(
    input: java.io.InputStream,
    tempPath: Path,
    maxUploadBytes: Long,
): Boolean {
    var totalBytes = 0L
    Files.newOutputStream(tempPath).use { output ->
        val buffer = ByteArray(UPLOAD_COPY_BUFFER_BYTES)
        while (true) {
            val readBytes = input.read(buffer)
            if (readBytes == -1) return false
            totalBytes += readBytes
            if (totalBytes > maxUploadBytes) return true
            output.write(buffer, 0, readBytes)
        }
    }
}

private const val UPLOAD_COPY_BUFFER_BYTES = 8192
private const val MULTIPART_FIELD_LIMIT_BYTES = 64L * 1024L * 1024L

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
