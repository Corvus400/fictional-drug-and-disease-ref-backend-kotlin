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

class DrugRepositoryTest {
    private val repository = ExposedDrugRepository(
        database = PostgresTestSupport.database,
        databaseDispatcher = PostgresTestSupport.databaseDispatcher,
    )
    private val diseaseRepository = ExposedDiseaseRepository(
        database = PostgresTestSupport.database,
        databaseDispatcher = PostgresTestSupport.databaseDispatcher,
    )

    @Test
    fun `findAll returns all seeded drugs`() = runBlocking {
        val result = assertIs<AppResult.Success<List<*>>>(repository.findAll())
        assertEquals(120, result.value.size)
    }

    @Test
    fun `findByPublicId returns nested JSONB document`() = runBlocking {
        val result = assertIs<AppResult.Success<*>>(repository.findByPublicId("drug_0001"))
        val drug = assertIs<io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug>(result.value)
        assertEquals("drug_0001", drug.id)
        assertTrue(drug.composition.activeIngredient.isNotBlank())
        assertTrue(
            drug.adverseReactions.serious.isNotEmpty() || drug.adverseReactions.other.frequencyUnknown.isNotEmpty()
        )
    }

    @Test
    fun `findByPublicId returns NotFound for missing drug`() = runBlocking {
        val result = assertIs<AppResult.Failure>(repository.findByPublicId("drug_9999"))
        assertEquals(DomainError.NotFound(resource = "drug", id = "drug_9999"), result.error)
    }

    @Test
    fun `create persists a new drug with a server assigned public id`() = runBlocking {
        val source = assertIs<AppResult.Success<Drug>>(repository.findByPublicId("drug_0001")).value
        val draft = source.copy(
            id = "",
            genericName = "テスト作成用一般名",
            brandName = "テスト作成用ブランド名",
            brandNameKana = "テストサクセイヨウブランドメイ",
            relatedDiseaseIds = emptyList(),
        )

        val created = assertIs<AppResult.Success<Drug>>(repository.create(draft)).value
        try {
            assertTrue(created.id.matches(Regex("""drug_\d{4}""")))
            assertEquals("テスト作成用一般名", created.genericName)

            val found = assertIs<AppResult.Success<Drug>>(repository.findByPublicId(created.id)).value
            assertEquals(created, found)
        } finally {
            dbQuery(
                database = PostgresTestSupport.database,
                databaseDispatcher = PostgresTestSupport.databaseDispatcher
            ) {
                DrugsTable.deleteWhere { DrugsTable.publicId eq created.id }
            }
        }
    }

    @Test
    fun `update replaces a persisted drug document`() = runBlocking {
        val source = assertIs<AppResult.Success<Drug>>(repository.findByPublicId("drug_0001")).value
        val created = assertIs<AppResult.Success<Drug>>(
            repository.create(
                source.copy(
                    id = "",
                    genericName = "テスト更新前一般名",
                    brandName = "テスト更新前ブランド名",
                    brandNameKana = "テストコウシンマエブランドメイ",
                    relatedDiseaseIds = emptyList(),
                ),
            ),
        ).value
        try {
            val replacement = created.copy(
                genericName = "テスト更新後一般名",
                brandName = "テスト更新後ブランド名",
                brandNameKana = "テストコウシンゴブランドメイ",
            )

            val updated = assertIs<AppResult.Success<Drug>>(repository.update(replacement)).value
            assertEquals(replacement, updated)

            val found = assertIs<AppResult.Success<Drug>>(repository.findByPublicId(created.id)).value
            assertEquals(replacement, found)
        } finally {
            dbQuery(
                database = PostgresTestSupport.database,
                databaseDispatcher = PostgresTestSupport.databaseDispatcher
            ) {
                DrugsTable.deleteWhere { DrugsTable.publicId eq created.id }
            }
        }
    }

    @Test
    fun `delete removes a persisted drug and is idempotent for missing ids`() = runBlocking {
        val source = assertIs<AppResult.Success<Drug>>(repository.findByPublicId("drug_0001")).value
        val created = assertIs<AppResult.Success<Drug>>(
            repository.create(
                source.copy(
                    id = "",
                    genericName = "テスト削除用一般名",
                    brandName = "テスト削除用ブランド名",
                    brandNameKana = "テストサクジョヨウブランドメイ",
                    relatedDiseaseIds = emptyList(),
                ),
            ),
        ).value
        try {
            assertIs<AppResult.Success<Unit>>(repository.delete(created.id))
            assertEquals(
                DomainError.NotFound(resource = "drug", id = created.id),
                assertIs<AppResult.Failure>(repository.findByPublicId(created.id)).error,
            )
            assertIs<AppResult.Success<Unit>>(repository.delete(created.id))
        } finally {
            dbQuery(
                database = PostgresTestSupport.database,
                databaseDispatcher = PostgresTestSupport.databaseDispatcher
            ) {
                DrugsTable.deleteWhere { DrugsTable.publicId eq created.id }
            }
        }
        Unit
    }

    @Test
    fun `delete returns Conflict when another disease references the drug`() = runBlocking {
        val drugSource = assertIs<AppResult.Success<Drug>>(repository.findByPublicId("drug_0001")).value
        val diseaseSource = assertIs<AppResult.Success<Disease>>(
            diseaseRepository.findByPublicId("disease_0001"),
        ).value
        val createdDrug = assertIs<AppResult.Success<Drug>>(
            repository.create(
                drugSource.copy(
                    id = "",
                    genericName = "テスト参照削除用一般名",
                    brandName = "テスト参照削除用ブランド名",
                    brandNameKana = "テストサンショウサクジョヨウブランドメイ",
                    relatedDiseaseIds = emptyList(),
                ),
            ),
        ).value
        val createdDisease = assertIs<AppResult.Success<Disease>>(
            diseaseRepository.create(
                diseaseSource.copy(
                    id = "",
                    name = "テスト参照元疾患",
                    nameKana = "テストサンショウモトシッカン",
                    relatedDrugIds = listOf(createdDrug.id),
                    relatedDiseaseIds = emptyList(),
                ),
            ),
        ).value
        try {
            val result = assertIs<AppResult.Failure>(repository.delete(createdDrug.id))
            assertIs<DomainError.Conflict>(result.error)
            assertIs<AppResult.Success<Drug>>(repository.findByPublicId(createdDrug.id))
        } finally {
            dbQuery(
                database = PostgresTestSupport.database,
                databaseDispatcher = PostgresTestSupport.databaseDispatcher
            ) {
                DiseasesTable.deleteWhere { DiseasesTable.publicId eq createdDisease.id }
                DrugsTable.deleteWhere { DrugsTable.publicId eq createdDrug.id }
            }
        }
        Unit
    }
}
