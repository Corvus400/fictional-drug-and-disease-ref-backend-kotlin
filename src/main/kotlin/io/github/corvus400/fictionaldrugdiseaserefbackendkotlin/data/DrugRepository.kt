package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug

interface DrugRepository {
    suspend fun create(drug: Drug): AppResult<Drug>

    suspend fun update(drug: Drug): AppResult<Drug>

    suspend fun delete(publicId: String): AppResult<Unit>

    suspend fun findByPublicId(publicId: String): AppResult<Drug>

    suspend fun findAll(): AppResult<List<Drug>>
}
