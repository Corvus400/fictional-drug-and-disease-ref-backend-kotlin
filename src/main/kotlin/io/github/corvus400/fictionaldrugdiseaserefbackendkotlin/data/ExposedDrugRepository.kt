package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.dbQuery
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.db.DiseasesTable
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.db.DrugsTable
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.FieldViolation
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
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
        run {
            repeat(PUBLIC_ID_CREATE_MAX_ATTEMPTS) {
                try {
                    return dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                        val publicId = nextDrugPublicId()
                        val created = drug.copy(id = publicId)
                        validateReferences(created)?.let { error ->
                            return@dbQuery AppResult.Failure(error)
                        }
                        DrugsTable.insert {
                            it[DrugsTable.publicId] = publicId
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
            AppResult.Failure(DomainError.Unexpected(IllegalStateException("Failed to allocate unique drug id.")))
        }

    override suspend fun update(drug: Drug): AppResult<Drug> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                validateReferences(drug)?.let { error ->
                    return@dbQuery AppResult.Failure(error)
                }
                val updatedRows = DrugsTable.update({ DrugsTable.publicId eq drug.id }) {
                    it[data] = drug
                    it[updatedAt] = CurrentDateTime
                }
                if (updatedRows == 0) {
                    AppResult.Failure(DomainError.NotFound(resource = "drug", id = drug.id))
                } else {
                    AppResult.Success(drug)
                }
            }
        }

    override suspend fun update(
        drug: Drug,
        expectedUpdatedAt: LocalDateTime,
    ): AppResult<Drug> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                validateReferences(drug)?.let { error ->
                    return@dbQuery AppResult.Failure(error)
                }
                val updatedRows = DrugsTable.update({
                    (DrugsTable.publicId eq drug.id) and (DrugsTable.updatedAt eq expectedUpdatedAt)
                }) {
                    it[data] = drug
                    it[updatedAt] = CurrentDateTime
                }
                if (updatedRows == 0) {
                    if (drugExists(drug.id)) {
                        AppResult.Failure(DomainError.PreconditionFailed("ETag does not match current drug version."))
                    } else {
                        AppResult.Failure(DomainError.NotFound(resource = "drug", id = drug.id))
                    }
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

    override suspend fun delete(
        publicId: String,
        expectedUpdatedAt: LocalDateTime,
    ): AppResult<Unit> =
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
                val deletedRows = DrugsTable.deleteWhere {
                    (DrugsTable.publicId eq publicId) and (DrugsTable.updatedAt eq expectedUpdatedAt)
                }
                if (deletedRows == 0 && drugExists(publicId)) {
                    AppResult.Failure(DomainError.PreconditionFailed("ETag does not match current drug version."))
                } else {
                    AppResult.Success(Unit)
                }
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

    override suspend fun findWithMetaByPublicId(publicId: String): AppResult<EntityWithMeta<Drug>> =
        queryUnexpectedAsFailure {
            dbQuery(database = database, databaseDispatcher = databaseDispatcher) {
                DrugsTable
                    .selectAll()
                    .where { DrugsTable.publicId eq publicId }
                    .singleOrNull()
                    ?.let { row ->
                        AppResult.Success(
                            EntityWithMeta(
                                entity = row[DrugsTable.data],
                                updatedAt = row[DrugsTable.updatedAt],
                            ),
                        )
                    }
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

    private fun drugExists(publicId: String): Boolean =
        DrugsTable
            .selectAll()
            .where { DrugsTable.publicId eq publicId }
            .singleOrNull() != null

    private fun validateReferences(drug: Drug): DomainError? {
        val violations = mutableListOf<FieldViolation>()
        val validDiseaseIds = drug.relatedDiseaseIds.filter { diseaseId ->
            DISEASE_ID_PATTERN.matches(diseaseId).also { matches ->
                if (!matches) {
                    violations += FieldViolation(
                        field = RELATED_DISEASE_IDS_FIELD,
                        reason = "Invalid $RELATED_DISEASE_IDS_FIELD id: $diseaseId",
                    )
                }
            }
        }
        if (validDiseaseIds.isNotEmpty()) {
            val existingDiseaseIds = DiseasesTable
                .selectAll()
                .where { DiseasesTable.publicId inList validDiseaseIds }
                .map { row -> row[DiseasesTable.publicId] }
                .toSet()
            validDiseaseIds
                .filterNot { diseaseId -> diseaseId in existingDiseaseIds }
                .forEach { diseaseId ->
                    violations += FieldViolation(
                        field = RELATED_DISEASE_IDS_FIELD,
                        reason = "Unknown $RELATED_DISEASE_IDS_FIELD id: $diseaseId",
                    )
                }
        }
        return violations
            .takeIf { it.isNotEmpty() }
            ?.let(DomainError::Validation)
    }

    private companion object {
        const val DRUG_ID_PREFIX = "drug_"
        const val RELATED_DISEASE_IDS_FIELD = "related_disease_ids"
        const val PUBLIC_ID_CREATE_MAX_ATTEMPTS = 8
        val DISEASE_ID_PATTERN = Regex("""disease\_\d{4}""")
    }
}

private fun ExposedSQLException.isUniqueViolation(): Boolean = sqlState == "23505"
