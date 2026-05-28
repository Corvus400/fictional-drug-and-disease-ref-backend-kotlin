package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.dbQuery
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.db.DiseasesTable
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.db.DrugsTable
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.Disease
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update

class ExposedDiseaseRepository(
    private val database: Database,
    private val databaseDispatcher: CoroutineDispatcher,
) : DiseaseRepository {
    override suspend fun create(disease: Disease): AppResult<Disease> =
        run {
            repeat(PUBLIC_ID_CREATE_MAX_ATTEMPTS) {
                try {
                    return dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                        val publicId = nextDiseasePublicId()
                        val created = disease.copy(id = publicId)
                        DiseasesTable.insert {
                            it[DiseasesTable.publicId] = publicId
                            it[data] = created
                        }
                        AppResult.Success(created)
                    }
                } catch (e: ExposedSQLException) {
                    if (!e.isUniqueViolation()) {
                        return AppResult.Failure(DomainError.Unexpected(e))
                    }
                }
            }
            AppResult.Failure(DomainError.Unexpected(IllegalStateException("Failed to allocate unique disease id.")))
        }

    override suspend fun update(disease: Disease): AppResult<Disease> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                val updatedRows = DiseasesTable.update({ DiseasesTable.publicId eq disease.id }) {
                    it[data] = disease
                    it[updatedAt] = CurrentDateTime
                }
                if (updatedRows == 0) {
                    AppResult.Failure(DomainError.NotFound(resource = "disease", id = disease.id))
                } else {
                    AppResult.Success(disease)
                }
            }
        }

    override suspend fun update(
        disease: Disease,
        expectedUpdatedAt: LocalDateTime,
    ): AppResult<Disease> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                val updatedRows = DiseasesTable.update({
                    (DiseasesTable.publicId eq disease.id) and (DiseasesTable.updatedAt eq expectedUpdatedAt)
                }) {
                    it[data] = disease
                    it[updatedAt] = CurrentDateTime
                }
                if (updatedRows == 0) {
                    if (diseaseExists(disease.id)) {
                        AppResult.Failure(
                            DomainError.PreconditionFailed("ETag does not match current disease version.")
                        )
                    } else {
                        AppResult.Failure(DomainError.NotFound(resource = "disease", id = disease.id))
                    }
                } else {
                    AppResult.Success(disease)
                }
            }
        }

    override suspend fun delete(publicId: String): AppResult<Unit> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                val referencingDrug = DrugsTable
                    .selectAll()
                    .firstOrNull { row -> publicId in row[DrugsTable.data].relatedDiseaseIds }
                if (referencingDrug != null) {
                    return@dbQuery AppResult.Failure(
                        DomainError.Conflict(
                            "disease $publicId is referenced by drug ${referencingDrug[DrugsTable.publicId]}"
                        ),
                    )
                }
                val referencingDisease = DiseasesTable
                    .selectAll()
                    .firstOrNull { row -> publicId in row[DiseasesTable.data].relatedDiseaseIds }
                if (referencingDisease != null) {
                    return@dbQuery AppResult.Failure(
                        DomainError.Conflict(
                            "disease $publicId is referenced by disease ${referencingDisease[DiseasesTable.publicId]}",
                        ),
                    )
                }
                DiseasesTable.deleteWhere { DiseasesTable.publicId eq publicId }
                AppResult.Success(Unit)
            }
        }

    override suspend fun delete(
        publicId: String,
        expectedUpdatedAt: LocalDateTime,
    ): AppResult<Unit> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                val referencingDrug = DrugsTable
                    .selectAll()
                    .firstOrNull { row -> publicId in row[DrugsTable.data].relatedDiseaseIds }
                if (referencingDrug != null) {
                    return@dbQuery AppResult.Failure(
                        DomainError.Conflict(
                            "disease $publicId is referenced by drug ${referencingDrug[DrugsTable.publicId]}"
                        ),
                    )
                }
                val referencingDisease = DiseasesTable
                    .selectAll()
                    .firstOrNull { row -> publicId in row[DiseasesTable.data].relatedDiseaseIds }
                if (referencingDisease != null) {
                    return@dbQuery AppResult.Failure(
                        DomainError.Conflict(
                            "disease $publicId is referenced by disease ${referencingDisease[DiseasesTable.publicId]}",
                        ),
                    )
                }
                val deletedRows = DiseasesTable.deleteWhere {
                    (DiseasesTable.publicId eq publicId) and (DiseasesTable.updatedAt eq expectedUpdatedAt)
                }
                if (deletedRows == 0 && diseaseExists(publicId)) {
                    AppResult.Failure(DomainError.PreconditionFailed("ETag does not match current disease version."))
                } else {
                    AppResult.Success(Unit)
                }
            }
        }

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

    override suspend fun findWithMetaByPublicId(publicId: String): AppResult<EntityWithMeta<Disease>> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                DiseasesTable
                    .selectAll()
                    .where { DiseasesTable.publicId eq publicId }
                    .singleOrNull()
                    ?.let { row ->
                        AppResult.Success(
                            EntityWithMeta(
                                entity = row[DiseasesTable.data],
                                updatedAt = row[DiseasesTable.updatedAt],
                            ),
                        )
                    }
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

    private fun nextDiseasePublicId(): String {
        val existingIds = DiseasesTable
            .selectAll()
            .map { row -> row[DiseasesTable.publicId] }
        return nextPublicId(existingIds = existingIds, prefix = DISEASE_ID_PREFIX)
    }

    private fun diseaseExists(publicId: String): Boolean =
        DiseasesTable
            .selectAll()
            .where { DiseasesTable.publicId eq publicId }
            .singleOrNull() != null

    private companion object {
        const val DISEASE_ID_PREFIX = "disease_"
        const val PUBLIC_ID_CREATE_MAX_ATTEMPTS = 8
    }
}

private fun ExposedSQLException.isUniqueViolation(): Boolean = sqlState == "23505"
