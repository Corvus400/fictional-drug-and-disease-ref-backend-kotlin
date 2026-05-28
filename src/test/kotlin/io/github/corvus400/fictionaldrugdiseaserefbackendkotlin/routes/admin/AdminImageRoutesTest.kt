package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.AppJson
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.dbQuery
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.ExposedDrugRepository
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data.db.DrugsTable
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.ProblemDetails
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.drug.Drug
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.moduleWithDatabaseDispatcher
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport.withPostgresConfig
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AdminImageRoutesTest {
    @Test
    fun `POST admin drug image stores png updates image url and serves uploaded image`() = testApplication {
        val uploadDir = Files.createTempDirectory("drug-image-upload-test")
        withPostgresConfig(imageUploadDir = uploadDir.toString())
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
                        genericName = "管理API画像アップロード一般名",
                        brandName = "管理API画像アップロードブランド名",
                        brandNameKana = "カンリアイピーアイガゾウアップロードブランドメイ",
                        relatedDiseaseIds = emptyList(),
                    ),
                )
            },
        ).value
        try {
            val response = client.post("/v1/admin/drugs/${created.id}/image") {
                bearerAuth(mintToken(scope = "admin"))
                header(HttpHeaders.IfMatch, etagForDrug(created.id))
                setBody(pngMultipartBody())
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val updated = AppJson.decodeFromString<Drug>(response.bodyAsText())
            assertEquals("/v1/images/drugs/${created.id}?size=Original", updated.imageUrl)
            assertEquals(LocalDate.now().toString(), updated.revisedAt)
            assertTrue(Files.isRegularFile(uploadDir.resolve("${created.id}.png")))

            val imageResponse = client.get("/v1/images/drugs/${created.id}")
            assertEquals(HttpStatusCode.OK, imageResponse.status)
            assertEquals(ContentType.Image.PNG, imageResponse.contentType()?.withoutParameters())

            val resizedImageResponse = client.get("/v1/images/drugs/${created.id}?size=S")
            assertEquals(HttpStatusCode.OK, resizedImageResponse.status)
            assertEquals(ContentType.Image.PNG, resizedImageResponse.contentType()?.withoutParameters())
            assertTrue(resizedImageResponse.bodyAsBytes().isNotEmpty())
        } finally {
            cleanupDrug(created.id, uploadDir)
        }
    }

    @Test
    fun `POST admin drug image returns not found for unknown drug`() = testApplication {
        val uploadDir = Files.createTempDirectory("drug-image-upload-test")
        withPostgresConfig(imageUploadDir = uploadDir.toString())
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.post("/v1/admin/drugs/drug_9999/image") {
            bearerAuth(mintToken(scope = "admin"))
            header(HttpHeaders.IfMatch, "\"2024-01-01T00:00:00\"")
            setBody(pngMultipartBody())
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        val problem = AppJson.decodeFromString<ProblemDetails>(response.bodyAsText())
        assertEquals("Resource not found", problem.title)
        assertTrue(Files.notExists(uploadDir.resolve("drug_9999.png")))
        Files.deleteIfExists(uploadDir)
    }

    @Test
    fun `POST admin drug image rejects non png upload`() = testApplication {
        val uploadDir = Files.createTempDirectory("drug-image-upload-test")
        withPostgresConfig(imageUploadDir = uploadDir.toString())
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
                        genericName = "管理API画像検証一般名",
                        brandName = "管理API画像検証ブランド名",
                        brandNameKana = "カンリアイピーアイガゾウケンショウブランドメイ",
                        relatedDiseaseIds = emptyList(),
                    ),
                )
            },
        ).value
        try {
            val response = client.post("/v1/admin/drugs/${created.id}/image") {
                bearerAuth(mintToken(scope = "admin"))
                header(HttpHeaders.IfMatch, etagForDrug(created.id))
                setBody(textMultipartBody())
            }

            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            val problem = AppJson.decodeFromString<ProblemDetails>(response.bodyAsText())
            assertEquals("file", problem.errors?.single()?.field)
            assertTrue(Files.notExists(uploadDir.resolve("${created.id}.png")))
        } finally {
            cleanupDrug(created.id, uploadDir)
        }
    }

    @Test
    fun `POST admin drug image rejects oversized upload`() = testApplication {
        val uploadDir = Files.createTempDirectory("drug-image-upload-test")
        withPostgresConfig(imageUploadDir = uploadDir.toString(), imageUploadMaxBytes = 4)
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
                        genericName = "管理API画像サイズ検証一般名",
                        brandName = "管理API画像サイズ検証ブランド名",
                        brandNameKana = "カンリアイピーアイガゾウサイズケンショウブランドメイ",
                        relatedDiseaseIds = emptyList(),
                    ),
                )
            },
        ).value
        try {
            val response = client.post("/v1/admin/drugs/${created.id}/image") {
                bearerAuth(mintToken(scope = "admin"))
                header(HttpHeaders.IfMatch, etagForDrug(created.id))
                setBody(pngMultipartBody())
            }

            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            val problem = AppJson.decodeFromString<ProblemDetails>(response.bodyAsText())
            assertEquals("file", problem.errors?.single()?.field)
            assertTrue(Files.notExists(uploadDir.resolve("${created.id}.png")))
        } finally {
            cleanupDrug(created.id, uploadDir)
        }
    }

    @Test
    fun `admin image upload streams multipart content to temp file instead of buffering whole file`() {
        val source = Files.readString(
            Path.of(
                "src/main/kotlin/io/github/corvus400/fictionaldrugdiseaserefbackendkotlin/routes/admin/" +
                    "AdminImageUploads.kt",
            ),
        )

        assertFalse(source.contains("toByteArray()"))
        assertTrue(source.contains("formFieldLimit"))
    }

    @Test
    fun `DELETE admin drugs removes uploaded image file`() = testApplication {
        val uploadDir = Files.createTempDirectory("drug-image-delete-test")
        withPostgresConfig(imageUploadDir = uploadDir.toString())
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
                        genericName = "管理API画像削除一般名",
                        brandName = "管理API画像削除ブランド名",
                        brandNameKana = "カンリアイピーアイガゾウサクジョブランドメイ",
                        relatedDiseaseIds = emptyList(),
                    ),
                )
            },
        ).value
        try {
            val uploadResponse = client.post("/v1/admin/drugs/${created.id}/image") {
                bearerAuth(mintToken(scope = "admin"))
                header(HttpHeaders.IfMatch, etagForDrug(created.id))
                setBody(pngMultipartBody())
            }
            assertEquals(HttpStatusCode.OK, uploadResponse.status)
            assertTrue(Files.isRegularFile(uploadDir.resolve("${created.id}.png")))

            val deleteResponse = client.delete("/v1/admin/drugs/${created.id}") {
                bearerAuth(mintToken(scope = "admin"))
                header(HttpHeaders.IfMatch, etagForDrug(created.id))
            }

            assertEquals(HttpStatusCode.NoContent, deleteResponse.status)
            assertTrue(Files.notExists(uploadDir.resolve("${created.id}.png")))
        } finally {
            cleanupDrug(created.id, uploadDir)
        }
    }

    @Test
    fun `POST admin drug image with stale etag preserves existing uploaded image`() = testApplication {
        val uploadDir = Files.createTempDirectory("drug-image-stale-etag-test")
        withPostgresConfig(imageUploadDir = uploadDir.toString())
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
                        genericName = "管理API画像競合一般名",
                        brandName = "管理API画像競合ブランド名",
                        brandNameKana = "カンリアイピーアイガゾウキョウゴウブランドメイ",
                        relatedDiseaseIds = emptyList(),
                    ),
                )
            },
        ).value
        try {
            val staleEtag = etagForDrug(created.id)
            val uploadResponse = client.post("/v1/admin/drugs/${created.id}/image") {
                bearerAuth(mintToken(scope = "admin"))
                header(HttpHeaders.IfMatch, staleEtag)
                setBody(pngMultipartBody())
            }
            assertEquals(HttpStatusCode.OK, uploadResponse.status)
            val imagePath = uploadDir.resolve("${created.id}.png")
            val originalBytes = Files.readAllBytes(imagePath)

            val staleResponse = client.post("/v1/admin/drugs/${created.id}/image") {
                bearerAuth(mintToken(scope = "admin"))
                header(HttpHeaders.IfMatch, staleEtag)
                setBody(pngMultipartBody())
            }

            assertEquals(HttpStatusCode.PreconditionFailed, staleResponse.status)
            assertTrue(Files.isRegularFile(imagePath))
            assertEquals(originalBytes.toList(), Files.readAllBytes(imagePath).toList())
        } finally {
            cleanupDrug(created.id, uploadDir)
        }
    }

    @Test
    fun `POST admin drug image keeps database unchanged when final image promotion fails`() = testApplication {
        val uploadDir = Files.createTempDirectory("drug-image-promote-failure-test")
        withPostgresConfig(imageUploadDir = uploadDir.toString())
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
                        genericName = "管理API画像昇格失敗一般名",
                        brandName = "管理API画像昇格失敗ブランド名",
                        brandNameKana = "カンリアイピーアイガゾウショウカクシッパイブランドメイ",
                        relatedDiseaseIds = emptyList(),
                    ),
                )
            },
        ).value
        val originalEtag = etagForDrug(created.id)
        val targetDirectory = uploadDir.resolve("${created.id}.png")
        Files.createDirectory(targetDirectory)
        Files.writeString(targetDirectory.resolve("blocker"), "not replaceable by a file")
        try {
            val response = client.post("/v1/admin/drugs/${created.id}/image") {
                bearerAuth(mintToken(scope = "admin"))
                header(HttpHeaders.IfMatch, originalEtag)
                setBody(pngMultipartBody())
            }

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            val found = assertIs<AppResult.Success<Drug>>(runBlocking { repository.findByPublicId(created.id) }).value
            assertEquals(created.imageUrl, found.imageUrl)
            assertEquals(created.revisedAt, found.revisedAt)
            assertEquals(originalEtag, etagForDrug(created.id))
        } finally {
            deleteDirectoryIfExists(targetDirectory)
            cleanupDrug(created.id, uploadDir)
        }
    }

    @Test
    fun `PUT admin drug with uploaded image keeps drug image url`() = testApplication {
        val uploadDir = Files.createTempDirectory("drug-image-put-test")
        withPostgresConfig(imageUploadDir = uploadDir.toString())
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
                        genericName = "管理API画像URL一般名",
                        brandName = "管理API画像URLブランド名",
                        brandNameKana = "カンリアイピーアイガゾウユーアールエルブランドメイ",
                        relatedDiseaseIds = emptyList(),
                    ),
                )
            },
        ).value
        try {
            val uploadResponse = client.post("/v1/admin/drugs/${created.id}/image") {
                bearerAuth(mintToken(scope = "admin"))
                header(HttpHeaders.IfMatch, etagForDrug(created.id))
                setBody(pngMultipartBody())
            }
            assertEquals(HttpStatusCode.OK, uploadResponse.status)

            val response = client.put("/v1/admin/drugs/${created.id}") {
                bearerAuth(mintToken(scope = "admin"))
                header(HttpHeaders.IfMatch, etagForDrug(created.id))
                contentType(ContentType.Application.Json)
                setBody(
                    contentOnlyBody(
                        created.copy(
                            genericName = "管理API画像URL更新後一般名",
                            revisedAt = LocalDate.now().toString(),
                        ),
                    ),
                )
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val updated = AppJson.decodeFromString<Drug>(response.bodyAsText())
            assertEquals("/v1/images/drugs/${created.id}?size=Original", updated.imageUrl)
        } finally {
            cleanupDrug(created.id, uploadDir)
        }
    }

    @Test
    fun `POST admin drug image rejects malformed path id before file write`() = testApplication {
        val uploadDir = Files.createTempDirectory("drug-image-id-validation-test")
        withPostgresConfig(imageUploadDir = uploadDir.toString())
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }

        val response = client.post("/v1/admin/drugs/not_a_drug_id/image") {
            bearerAuth(mintToken(scope = "admin"))
            header(HttpHeaders.IfMatch, "\"2024-01-01T00:00:00\"")
            setBody(pngMultipartBody())
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        val problem = AppJson.decodeFromString<ProblemDetails>(response.bodyAsText())
        assertEquals("id", problem.errors?.single()?.field)
        assertTrue(Files.notExists(uploadDir.resolve("not_a_drug_id.png")))
        Files.deleteIfExists(uploadDir)
    }

    @Test
    fun `repository create retries public id collisions under concurrent drug creates`() = testApplication {
        withPostgresConfig()
        application { moduleWithDatabaseDispatcher(databaseDispatcher = PostgresTestSupport.databaseDispatcher) }
        val repository = ExposedDrugRepository(
            database = PostgresTestSupport.database,
            databaseDispatcher = PostgresTestSupport.databaseDispatcher,
        )
        val source = assertIs<AppResult.Success<Drug>>(
            runBlocking { repository.findByPublicId("drug_0001") },
        ).value

        val created = runBlocking {
            (1..8).map { index ->
                async {
                    repository.create(
                        source.copy(
                            id = "",
                            genericName = "管理API並行作成一般名$index",
                            brandName = "管理API並行作成ブランド名$index",
                            brandNameKana = "カンリアイピーアイヘイコウサクセイブランドメイ$index",
                            relatedDiseaseIds = emptyList(),
                        ),
                    )
                }
            }.awaitAll().map { result -> assertIs<AppResult.Success<Drug>>(result).value }
        }
        try {
            assertEquals(created.size, created.map { it.id }.toSet().size)
        } finally {
            cleanupDrugs(created.map { it.id })
        }
    }

    private suspend fun ApplicationTestBuilder.etagForDrug(id: String): String =
        checkNotNull(client.get("/v1/drugs/$id").headers[HttpHeaders.ETag])

    private fun pngMultipartBody(): MultiPartFormDataContent =
        MultiPartFormDataContent(
            formData {
                append(
                    key = "file",
                    value = pngBytes(),
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Image.PNG.toString())
                        append(HttpHeaders.ContentDisposition, "filename=\"drug.png\"")
                    },
                )
            },
        )

    private fun textMultipartBody(): MultiPartFormDataContent =
        MultiPartFormDataContent(
            formData {
                append(
                    key = "file",
                    value = "not a png".encodeToByteArray(),
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Image.PNG.toString())
                        append(HttpHeaders.ContentDisposition, "filename=\"drug.txt\"")
                    },
                )
            },
        )

    private fun pngBytes(): ByteArray =
        checkNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream("images/dosage_form/tablet.png"))
            .use { it.readBytes() }

    private fun contentOnlyBody(drug: Drug): String =
        AppJson.encodeToString(
            AppJson.parseToJsonElement(AppJson.encodeToString(drug))
                .jsonObject
                .withoutServerManagedFields(),
        )

    private fun JsonObject.withoutServerManagedFields(): JsonObject =
        JsonObject(filterKeys { key -> key !in serverManagedJsonFields })

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

    private fun cleanupDrug(
        id: String,
        uploadDir: Path,
    ) {
        runBlocking {
            dbQuery(
                database = PostgresTestSupport.database,
                databaseDispatcher = PostgresTestSupport.databaseDispatcher
            ) {
                DrugsTable.deleteWhere { DrugsTable.publicId eq id }
            }
        }
        Files.deleteIfExists(uploadDir.resolve("$id.png"))
        deleteDirectoryIfExists(uploadDir)
    }

    private fun deleteDirectoryIfExists(path: Path) {
        if (!Files.isDirectory(path)) return
        Files.list(path).use { children ->
            children.forEach { child -> Files.deleteIfExists(child) }
        }
        Files.deleteIfExists(path)
    }

    private fun cleanupDrugs(ids: List<String>) {
        runBlocking {
            dbQuery(
                database = PostgresTestSupport.database,
                databaseDispatcher = PostgresTestSupport.databaseDispatcher
            ) {
                ids.forEach { id ->
                    DrugsTable.deleteWhere { DrugsTable.publicId eq id }
                }
            }
        }
    }

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
