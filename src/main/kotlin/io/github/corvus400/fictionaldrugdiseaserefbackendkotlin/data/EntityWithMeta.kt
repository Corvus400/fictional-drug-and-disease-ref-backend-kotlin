package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import kotlinx.datetime.LocalDateTime

data class EntityWithMeta<T : Any>(
    val entity: T,
    val updatedAt: LocalDateTime,
)
