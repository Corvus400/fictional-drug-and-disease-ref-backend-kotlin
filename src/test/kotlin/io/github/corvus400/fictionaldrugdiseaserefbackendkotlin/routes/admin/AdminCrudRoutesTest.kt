package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.AppJson
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.dbQuery
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.ExposedDiseaseRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.ExposedDrugRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.db.DiseasesTable
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.db.DrugsTable
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.disease.Disease
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.moduleWithDatabaseDispatcher
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport.withPostgresConfig
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AdminCrudRoutesTest {
    @Test
    fun `POST admin drugs creates a drug and returns the complete entity`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }
        val repository = ExposedDrugRepository(
            database = PostgresTestSupport.database,
            databaseDispatcher = PostgresTestSupport.databaseDispatcher,
        )
        val source = assertIs<AppResult.Success<Drug>>(
            runBlocking { repository.findByPublicId("drug_0001") },
        ).value
        val draft = source.copy(
            id = "client_supplied_id_is_ignored",
            genericName = "管理API作成用一般名",
            brandName = "管理API作成用ブランド名",
            brandNameKana = "カンリアイピーアイサクセイヨウブランドメイ",
            relatedDiseaseIds = emptyList(),
        )

        val response = client.post("/v1/admin/drugs") {
            bearerAuth(mintToken(scope = "admin"))
            contentType(ContentType.Application.Json)
            setBody(contentOnlyBody(draft))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val created = AppJson.decodeFromString<Drug>(response.bodyAsText())
        try {
            assertTrue(created.id.matches(Regex("""drug_\d{4}""")))
            assertEquals("管理API作成用一般名", created.genericName)
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
    fun `PUT admin drugs replaces a drug and returns the complete entity`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }
        val repository = ExposedDrugRepository(
            database = PostgresTestSupport.database,
            databaseDispatcher = PostgresTestSupport.databaseDispatcher,
        )
        val source = assertIs<AppResult.Success<Drug>>(
            runBlocking { repository.findByPublicId("drug_0001") },
        ).value
        val created = assertIs<AppResult.Success<Drug>>(
            runBlocking {
                repository.create(
                    source.copy(
                        id = "",
                        genericName = "管理API更新前一般名",
                        brandName = "管理API更新前ブランド名",
                        brandNameKana = "カンリアイピーアイコウシンマエブランドメイ",
                        relatedDiseaseIds = emptyList(),
                    ),
                )
            },
        ).value
        try {
            val replacement = created.copy(
                genericName = "管理API更新後一般名",
                brandName = "管理API更新後ブランド名",
                brandNameKana = "カンリアイピーアイコウシンゴブランドメイ",
            )

            val response = client.put("/v1/admin/drugs/${created.id}") {
                bearerAuth(mintToken(scope = "admin"))
                contentType(ContentType.Application.Json)
                setBody(contentOnlyBody(replacement.copy(id = "client_supplied_id_is_ignored")))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val updated = AppJson.decodeFromString<Drug>(response.bodyAsText())
            assertEquals(replacement, updated)
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
    fun `PATCH admin drugs merge patches a drug and preserves omitted fields`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }
        val repository = ExposedDrugRepository(
            database = PostgresTestSupport.database,
            databaseDispatcher = PostgresTestSupport.databaseDispatcher,
        )
        val source = assertIs<AppResult.Success<Drug>>(
            runBlocking { repository.findByPublicId("drug_0001") },
        ).value
        val created = assertIs<AppResult.Success<Drug>>(
            runBlocking {
                repository.create(
                    source.copy(
                        id = "",
                        genericName = "管理APIパッチ前一般名",
                        brandName = "管理APIパッチ前ブランド名",
                        brandNameKana = "カンリアイピーアイパッチマエブランドメイ",
                        relatedDiseaseIds = emptyList(),
                    ),
                )
            },
        ).value
        try {
            val response = client.patch("/v1/admin/drugs/${created.id}") {
                bearerAuth(mintToken(scope = "admin"))
                contentType(ContentType("application", "merge-patch+json"))
                setBody("""{"generic_name":"管理APIパッチ後一般名","id":"client_supplied_id_is_ignored"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val updated = AppJson.decodeFromString<Drug>(response.bodyAsText())
            assertEquals(created.id, updated.id)
            assertEquals("管理APIパッチ後一般名", updated.genericName)
            assertEquals(created.brandName, updated.brandName)
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
    fun `DELETE admin drugs removes a drug and returns no content`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }
        val repository = ExposedDrugRepository(
            database = PostgresTestSupport.database,
            databaseDispatcher = PostgresTestSupport.databaseDispatcher,
        )
        val source = assertIs<AppResult.Success<Drug>>(
            runBlocking { repository.findByPublicId("drug_0001") },
        ).value
        val created = assertIs<AppResult.Success<Drug>>(
            runBlocking {
                repository.create(
                    source.copy(
                        id = "",
                        genericName = "管理API削除用一般名",
                        brandName = "管理API削除用ブランド名",
                        brandNameKana = "カンリアイピーアイサクジョヨウブランドメイ",
                        relatedDiseaseIds = emptyList(),
                    ),
                )
            },
        ).value
        try {
            val response = client.delete("/v1/admin/drugs/${created.id}") {
                bearerAuth(mintToken(scope = "admin"))
            }
            assertEquals(HttpStatusCode.NoContent, response.status)
            assertIs<AppResult.Failure>(runBlocking { repository.findByPublicId(created.id) })
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
    fun `POST admin diseases creates a disease and returns the complete entity`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }
        val repository = ExposedDiseaseRepository(
            database = PostgresTestSupport.database,
            databaseDispatcher = PostgresTestSupport.databaseDispatcher,
        )
        val source = assertIs<AppResult.Success<Disease>>(
            runBlocking { repository.findByPublicId("disease_0001") },
        ).value
        val draft = source.copy(
            id = "client_supplied_id_is_ignored",
            name = "管理API作成用疾患",
            nameKana = "カンリアイピーアイサクセイヨウシッカン",
            relatedDrugIds = emptyList(),
            relatedDiseaseIds = emptyList(),
        )

        val response = client.post("/v1/admin/diseases") {
            bearerAuth(mintToken(scope = "admin"))
            contentType(ContentType.Application.Json)
            setBody(contentOnlyBody(draft))
        }
        assertEquals(HttpStatusCode.Created, response.status)
        val created = AppJson.decodeFromString<Disease>(response.bodyAsText())
        try {
            assertTrue(created.id.matches(Regex("""disease_\d{4}""")))
            assertEquals("管理API作成用疾患", created.name)
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
    fun `PUT admin diseases replaces a disease and returns the complete entity`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }
        val repository = ExposedDiseaseRepository(
            database = PostgresTestSupport.database,
            databaseDispatcher = PostgresTestSupport.databaseDispatcher,
        )
        val source = assertIs<AppResult.Success<Disease>>(
            runBlocking { repository.findByPublicId("disease_0001") },
        ).value
        val created = assertIs<AppResult.Success<Disease>>(
            runBlocking {
                repository.create(
                    source.copy(
                        id = "",
                        name = "管理API更新前疾患",
                        nameKana = "カンリアイピーアイコウシンマエシッカン",
                        relatedDrugIds = emptyList(),
                        relatedDiseaseIds = emptyList(),
                    ),
                )
            },
        ).value
        try {
            val replacement = created.copy(
                name = "管理API更新後疾患",
                nameKana = "カンリアイピーアイコウシンゴシッカン",
            )

            val response = client.put("/v1/admin/diseases/${created.id}") {
                bearerAuth(mintToken(scope = "admin"))
                contentType(ContentType.Application.Json)
                setBody(contentOnlyBody(replacement.copy(id = "client_supplied_id_is_ignored")))
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val updated = AppJson.decodeFromString<Disease>(response.bodyAsText())
            assertEquals(replacement, updated)
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
    fun `PATCH admin diseases merge patches a disease and preserves omitted fields`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }
        val repository = ExposedDiseaseRepository(
            database = PostgresTestSupport.database,
            databaseDispatcher = PostgresTestSupport.databaseDispatcher,
        )
        val source = assertIs<AppResult.Success<Disease>>(
            runBlocking { repository.findByPublicId("disease_0001") },
        ).value
        val created = assertIs<AppResult.Success<Disease>>(
            runBlocking {
                repository.create(
                    source.copy(
                        id = "",
                        name = "管理APIパッチ前疾患",
                        nameKana = "カンリアイピーアイパッチマエシッカン",
                        relatedDrugIds = emptyList(),
                        relatedDiseaseIds = emptyList(),
                    ),
                )
            },
        ).value
        try {
            val response = client.patch("/v1/admin/diseases/${created.id}") {
                bearerAuth(mintToken(scope = "admin"))
                contentType(ContentType("application", "merge-patch+json"))
                setBody("""{"name":"管理APIパッチ後疾患","id":"client_supplied_id_is_ignored"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val updated = AppJson.decodeFromString<Disease>(response.bodyAsText())
            assertEquals(created.id, updated.id)
            assertEquals("管理APIパッチ後疾患", updated.name)
            assertEquals(created.nameKana, updated.nameKana)
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
    fun `DELETE admin diseases removes a disease and returns no content`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }
        val repository = ExposedDiseaseRepository(
            database = PostgresTestSupport.database,
            databaseDispatcher = PostgresTestSupport.databaseDispatcher,
        )
        val source = assertIs<AppResult.Success<Disease>>(
            runBlocking { repository.findByPublicId("disease_0001") },
        ).value
        val created = assertIs<AppResult.Success<Disease>>(
            runBlocking {
                repository.create(
                    source.copy(
                        id = "",
                        name = "管理API削除用疾患",
                        nameKana = "カンリアイピーアイサクジョヨウシッカン",
                        relatedDrugIds = emptyList(),
                        relatedDiseaseIds = emptyList(),
                    ),
                )
            },
        ).value
        try {
            val response = client.delete("/v1/admin/diseases/${created.id}") {
                bearerAuth(mintToken(scope = "admin"))
            }
            assertEquals(HttpStatusCode.NoContent, response.status)
            assertIs<AppResult.Failure>(runBlocking { repository.findByPublicId(created.id) })
        } finally {
            dbQuery(
                database = PostgresTestSupport.database,
                databaseDispatcher = PostgresTestSupport.databaseDispatcher
            ) {
                DiseasesTable.deleteWhere { DiseasesTable.publicId eq created.id }
            }
        }
    }

    private fun mintToken(
        secret: String = "test-secret-please-change",
        scope: String,
        sub: String = "admin-1",
    ): String =
        JWT.create()
            .withIssuer("http://localhost")
            .withAudience("fictional-drug-ref")
            .withSubject(sub)
            .withClaim("scope", scope)
            .sign(Algorithm.HMAC256(secret))

    private fun contentOnlyBody(drug: Drug): String =
        AppJson.encodeToString(
            AppJson.parseToJsonElement(AppJson.encodeToString(drug))
                .jsonObject
                .withoutServerManagedFields(),
        )

    private fun contentOnlyBody(disease: Disease): String =
        AppJson.encodeToString(
            AppJson.parseToJsonElement(AppJson.encodeToString(disease))
                .jsonObject
                .withoutServerManagedFields(),
        )

    private fun JsonObject.withoutServerManagedFields(): JsonObject =
        JsonObject(filterKeys { key -> key !in serverManagedJsonFields })

    private companion object {
        val serverManagedJsonFields = setOf(
            "id",
            "revised_at",
            "image_url",
            "disclaimer",
            "created_at",
            "updated_at",
        )
    }
}
