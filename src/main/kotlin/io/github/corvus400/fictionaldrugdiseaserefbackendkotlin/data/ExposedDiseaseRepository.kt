package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.dbQuery
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.db.DiseasesTable
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.Disease
import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll

class ExposedDiseaseRepository(
    private val database: Database,
    private val databaseDispatcher: CoroutineDispatcher,
) : DiseaseRepository {
    override suspend fun findByPublicId(publicId: String): AppResult<Disease> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                DiseasesTable
                    .selectAll()
                    .where { DiseasesTable.publicId eq publicId }
                    .singleOrNull()
                    ?.get(DiseasesTable.data)
                    ?.let { AppResult.Success(it) }
                    ?: AppResult.Failure(DomainError.NotFound(resource = "disease", id = publicId))
            }
        }

    override suspend fun findAll(): AppResult<List<Disease>> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                AppResult.Success(
                    DiseasesTable
                        .selectAll()
                        .map { row -> row[DiseasesTable.data] },
                )
            }
        }
}
