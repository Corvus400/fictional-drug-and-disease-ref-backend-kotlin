package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import io.ktor.server.application.Application
import java.nio.file.Path

private const val DEFAULT_MAX_UPLOAD_BYTES = 5L * 1024L * 1024L

data class ImageStorageConfig(
    val uploadDir: Path,
    val maxUploadBytes: Long,
)

fun Application.loadImageStorageConfig(): ImageStorageConfig =
    ImageStorageConfig(
        uploadDir = Path.of(resolveConfig("IMAGE_UPLOAD_DIR", "images.uploadDir", default = "var/uploads/drugs")),
        maxUploadBytes = resolveConfig(
            "IMAGE_UPLOAD_MAX_BYTES",
            "images.uploadMaxBytes",
            default = DEFAULT_MAX_UPLOAD_BYTES.toString(),
        ).toLong(),
    )
