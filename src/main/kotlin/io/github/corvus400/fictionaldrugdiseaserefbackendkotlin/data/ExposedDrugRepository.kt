package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.dbQuery
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.db.DrugsTable
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll

class ExposedDrugRepository(
    private val database: Database,
    private val databaseDispatcher: CoroutineDispatcher,
) : DrugRepository {
    override suspend fun findByPublicId(publicId: String): AppResult<Drug> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                DrugsTable
                    .selectAll()
                    .where { DrugsTable.publicId eq publicId }
                    .singleOrNull()
                    ?.get(DrugsTable.data)
                    ?.let { AppResult.Success(it) }
                    ?: AppResult.Failure(DomainError.NotFound(resource = "drug", id = publicId))
            }
        }

    override suspend fun findAll(): AppResult<List<Drug>> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                AppResult.Success(
                    DrugsTable
                        .selectAll()
                        .map { row -> row[DrugsTable.data] },
                )
            }
        }
}
