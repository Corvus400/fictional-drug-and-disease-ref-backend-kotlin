package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.dbQuery
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.db.DiseasesTable
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.db.DrugsTable
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.Disease
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DiseaseRepositoryTest {
    private val repository = ExposedDiseaseRepository(
        database = PostgresTestSupport.database,
        databaseDispatcher = PostgresTestSupport.databaseDispatcher,
    )
    private val drugRepository = ExposedDrugRepository(
        database = PostgresTestSupport.database,
        databaseDispatcher = PostgresTestSupport.databaseDispatcher,
    )

    @Test
    fun `findAll returns all seeded diseases`() = runBlocking {
        val result = assertIs<AppResult.Success<List<*>>>(repository.findAll())
        assertEquals(80, result.value.size)
    }

    @Test
    fun `findByPublicId returns nested JSONB document`() = runBlocking {
        val result = assertIs<AppResult.Success<*>>(repository.findByPublicId("disease_0001"))
        val disease = assertIs<Disease>(result.value)
        assertEquals("disease_0001", disease.id)
        assertTrue(disease.symptoms.mainSymptoms.isNotEmpty())
        assertTrue(disease.requiredExams.isNotEmpty())
    }

    @Test
    fun `findByPublicId returns NotFound for missing disease`() = runBlocking {
        val result = assertIs<AppResult.Failure>(repository.findByPublicId("disease_9999"))
        assertEquals(DomainError.NotFound(resource = "disease", id = "disease_9999"), result.error)
    }

    @Test
    fun `create persists a new disease with a server assigned public id`() = runBlocking {
        val source = assertIs<AppResult.Success<Disease>>(repository.findByPublicId("disease_0001")).value
        val draft = source.copy(
            id = "",
            name = "テスト作成用疾患",
            nameKana = "テストサクセイヨウシッカン",
            relatedDrugIds = emptyList(),
            relatedDiseaseIds = emptyList(),
        )

        val created = assertIs<AppResult.Success<Disease>>(repository.create(draft)).value
        try {
            assertTrue(created.id.matches(Regex("""disease_\d{4}""")))
            assertEquals("テスト作成用疾患", created.name)

            val found = assertIs<AppResult.Success<Disease>>(repository.findByPublicId(created.id)).value
            assertEquals(created, found)
        } finally {
            dbQuery(
                database = PostgresTestSupport.database,
                databaseDispatcher = PostgresTestSupport.databaseDispatcher
            ) {
                DiseasesTable.deleteWhere { DiseasesTable.publicId eq created.id }
            }
        }
    }

    @Test
    fun `update replaces a persisted disease document`() = runBlocking {
        val source = assertIs<AppResult.Success<Disease>>(repository.findByPublicId("disease_0001")).value
        val created = assertIs<AppResult.Success<Disease>>(
            repository.create(
                source.copy(
                    id = "",
                    name = "テスト更新前疾患",
                    nameKana = "テストコウシンマエシッカン",
                    relatedDrugIds = emptyList(),
                    relatedDiseaseIds = emptyList(),
                ),
            ),
        ).value
        try {
            val replacement = created.copy(
                name = "テスト更新後疾患",
                nameKana = "テストコウシンゴシッカン",
            )

            val updated = assertIs<AppResult.Success<Disease>>(repository.update(replacement)).value
            assertEquals(replacement, updated)

            val found = assertIs<AppResult.Success<Disease>>(repository.findByPublicId(created.id)).value
            assertEquals(replacement, found)
        } finally {
            dbQuery(
                database = PostgresTestSupport.database,
                databaseDispatcher = PostgresTestSupport.databaseDispatcher
            ) {
                DiseasesTable.deleteWhere { DiseasesTable.publicId eq created.id }
            }
        }
    }

    @Test
    fun `delete removes a persisted disease and is idempotent for missing ids`() = runBlocking {
        val source = assertIs<AppResult.Success<Disease>>(repository.findByPublicId("disease_0001")).value
        val created = assertIs<AppResult.Success<Disease>>(
            repository.create(
                source.copy(
                    id = "",
                    name = "テスト削除用疾患",
                    nameKana = "テストサクジョヨウシッカン",
                    relatedDrugIds = emptyList(),
                    relatedDiseaseIds = emptyList(),
                ),
            ),
        ).value
        try {
            assertIs<AppResult.Success<Unit>>(repository.delete(created.id))
            assertEquals(
                DomainError.NotFound(resource = "disease", id = created.id),
                assertIs<AppResult.Failure>(repository.findByPublicId(created.id)).error,
            )
            assertIs<AppResult.Success<Unit>>(repository.delete(created.id))
        } finally {
            dbQuery(
                database = PostgresTestSupport.database,
                databaseDispatcher = PostgresTestSupport.databaseDispatcher
            ) {
                DiseasesTable.deleteWhere { DiseasesTable.publicId eq created.id }
            }
        }
        Unit
    }

    @Test
    fun `delete returns Conflict when another drug references the disease`() = runBlocking {
        val diseaseSource = assertIs<AppResult.Success<Disease>>(repository.findByPublicId("disease_0001")).value
        val drugSource = assertIs<AppResult.Success<Drug>>(drugRepository.findByPublicId("drug_0001")).value
        val createdDisease = assertIs<AppResult.Success<Disease>>(
            repository.create(
                diseaseSource.copy(
                    id = "",
                    name = "テスト被参照疾患",
                    nameKana = "テストヒサンショウシッカン",
                    relatedDrugIds = emptyList(),
                    relatedDiseaseIds = emptyList(),
                ),
            ),
        ).value
        val createdDrug = assertIs<AppResult.Success<Drug>>(
            drugRepository.create(
                drugSource.copy(
                    id = "",
                    genericName = "テスト参照元一般名",
                    brandName = "テスト参照元ブランド名",
                    brandNameKana = "テストサンショウモトブランドメイ",
                    relatedDiseaseIds = listOf(createdDisease.id),
                ),
            ),
        ).value
        try {
            val result = assertIs<AppResult.Failure>(repository.delete(createdDisease.id))
            assertIs<DomainError.Conflict>(result.error)
            assertIs<AppResult.Success<Disease>>(repository.findByPublicId(createdDisease.id))
        } finally {
            dbQuery(
                database = PostgresTestSupport.database,
                databaseDispatcher = PostgresTestSupport.databaseDispatcher
            ) {
                DrugsTable.deleteWhere { DrugsTable.publicId eq createdDrug.id }
                DiseasesTable.deleteWhere { DiseasesTable.publicId eq createdDisease.id }
            }
        }
        Unit
    }

    @Test
    fun `delete returns Conflict when another disease references the disease`() = runBlocking {
        val diseaseSource = assertIs<AppResult.Success<Disease>>(repository.findByPublicId("disease_0001")).value
        val referenced = assertIs<AppResult.Success<Disease>>(
            repository.create(
                diseaseSource.copy(
                    id = "",
                    name = "テスト被参照疾患2",
                    nameKana = "テストヒサンショウシッカンツー",
                    relatedDrugIds = emptyList(),
                    relatedDiseaseIds = emptyList(),
                ),
            ),
        ).value
        val referencing = assertIs<AppResult.Success<Disease>>(
            repository.create(
                diseaseSource.copy(
                    id = "",
                    name = "テスト参照元疾患2",
                    nameKana = "テストサンショウモトシッカンツー",
                    relatedDrugIds = emptyList(),
                    relatedDiseaseIds = listOf(referenced.id),
                ),
            ),
        ).value
        try {
            val result = assertIs<AppResult.Failure>(repository.delete(referenced.id))
            assertIs<DomainError.Conflict>(result.error)
            assertIs<AppResult.Success<Disease>>(repository.findByPublicId(referenced.id))
        } finally {
            dbQuery(
                database = PostgresTestSupport.database,
                databaseDispatcher = PostgresTestSupport.databaseDispatcher
            ) {
                DiseasesTable.deleteWhere { DiseasesTable.publicId eq referencing.id }
                DiseasesTable.deleteWhere { DiseasesTable.publicId eq referenced.id }
            }
        }
        Unit
    }
}
