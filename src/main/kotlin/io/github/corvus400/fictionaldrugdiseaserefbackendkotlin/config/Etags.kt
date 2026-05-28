package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config

import kotlinx.datetime.LocalDateTime

fun etagFor(updatedAt: LocalDateTime): String = "\"${updatedAt}\""

fun parseEtag(value: String): LocalDateTime? =
    value
        .removePrefix("\"")
        .removeSuffix("\"")
        .let { raw -> runCatching { LocalDateTime.parse(raw) }.getOrNull() }
