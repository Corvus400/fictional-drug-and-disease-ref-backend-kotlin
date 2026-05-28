package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.dbQuery
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.db.DiseasesTable
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.db.DrugsTable
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class ExposedDrugRepository(
    private val database: Database,
    private val databaseDispatcher: CoroutineDispatcher,
) : DrugRepository {
    override suspend fun create(drug: Drug): AppResult<Drug> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                val publicId = nextDrugPublicId()
                val created = drug.copy(id = publicId)
                DrugsTable.insert {
                    it[DrugsTable.publicId] = publicId
                    it[data] = created
                }
                AppResult.Success(created)
            }
        }

    override suspend fun update(drug: Drug): AppResult<Drug> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                val updatedRows = DrugsTable.update({ DrugsTable.publicId eq drug.id }) {
                    it[data] = drug
                }
                if (updatedRows == 0) {
                    AppResult.Failure(DomainError.NotFound(resource = "drug", id = drug.id))
                } else {
                    AppResult.Success(drug)
                }
            }
        }

    override suspend fun delete(publicId: String): AppResult<Unit> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                val referencingDisease = DiseasesTable
                    .selectAll()
                    .firstOrNull { row -> publicId in row[DiseasesTable.data].relatedDrugIds }
                if (referencingDisease != null) {
                    return@dbQuery AppResult.Failure(
                        DomainError.Conflict(
                            "drug $publicId is referenced by disease ${referencingDisease[DiseasesTable.publicId]}"
                        ),
                    )
                }
                DrugsTable.deleteWhere { DrugsTable.publicId eq publicId }
                AppResult.Success(Unit)
            }
        }

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

    private fun nextDrugPublicId(): String {
        val existingIds = DrugsTable
            .selectAll()
            .map { row -> row[DrugsTable.publicId] }
        return nextPublicId(existingIds = existingIds, prefix = DRUG_ID_PREFIX)
    }

    private companion object {
        const val DRUG_ID_PREFIX = "drug_"
    }
}
