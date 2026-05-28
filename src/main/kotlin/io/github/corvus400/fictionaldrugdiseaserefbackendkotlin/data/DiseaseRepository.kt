package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.Disease
import kotlinx.datetime.LocalDateTime

interface DiseaseRepository {
    suspend fun create(disease: Disease): AppResult<Disease>

    suspend fun update(disease: Disease): AppResult<Disease>

    suspend fun update(disease: Disease, expectedUpdatedAt: LocalDateTime): AppResult<Disease>

    suspend fun delete(publicId: String): AppResult<Unit>

    suspend fun delete(publicId: String, expectedUpdatedAt: LocalDateTime): AppResult<Unit>

    suspend fun findByPublicId(publicId: String): AppResult<Disease>

    suspend fun findWithMetaByPublicId(publicId: String): AppResult<EntityWithMeta<Disease>>

    suspend fun findAll(): AppResult<List<Disease>>
}
