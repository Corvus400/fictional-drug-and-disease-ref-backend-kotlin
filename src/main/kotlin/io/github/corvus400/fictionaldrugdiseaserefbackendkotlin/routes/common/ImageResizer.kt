package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.common

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import kotlin.math.max
import kotlin.math.roundToInt

enum class ImageSize {
    S,
    M,
    ORIGINAL,
}

object ImageResizer {
    fun resize(
        originalImage: BufferedImage,
        size: ImageSize,
    ): BufferedImage =
        if (size == ImageSize.S || size == ImageSize.M) {
            val divisor = if (size == ImageSize.S) 8 else 4
            val aspectRatio = originalImage.width.toDouble() / originalImage.height
            val longEdge = max(1, maxOf(originalImage.width, originalImage.height) / divisor)
            val targetWidth = if (originalImage.width >=
                originalImage.height
            ) {
                longEdge
            } else {
                max(1, (longEdge * aspectRatio).roundToInt())
            }
            val targetHeight = if (originalImage.width >=
                originalImage.height
            ) {
                max(1, (longEdge / aspectRatio).roundToInt())
            } else {
                longEdge
            }
            BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB).also { resizedImage ->
                val graphics = resizedImage.createGraphics()
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                graphics.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null)
                graphics.dispose()
            }
        } else {
            originalImage
        }
}
