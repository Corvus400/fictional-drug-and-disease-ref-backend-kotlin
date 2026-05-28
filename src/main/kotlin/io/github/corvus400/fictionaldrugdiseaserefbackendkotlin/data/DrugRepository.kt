package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import kotlinx.datetime.LocalDateTime

interface DrugRepository {
    suspend fun create(drug: Drug): AppResult<Drug>

    suspend fun update(drug: Drug): AppResult<Drug>

    suspend fun update(drug: Drug, expectedUpdatedAt: LocalDateTime): AppResult<Drug>

    suspend fun delete(publicId: String): AppResult<Unit>

    suspend fun delete(publicId: String, expectedUpdatedAt: LocalDateTime): AppResult<Unit>

    suspend fun findByPublicId(publicId: String): AppResult<Drug>

    suspend fun findWithMetaByPublicId(publicId: String): AppResult<EntityWithMeta<Drug>>

    suspend fun findAll(): AppResult<List<Drug>>
}
