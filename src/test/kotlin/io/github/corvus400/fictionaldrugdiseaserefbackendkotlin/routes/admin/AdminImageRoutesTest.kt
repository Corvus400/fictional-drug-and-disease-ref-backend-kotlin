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
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
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
        Files.deleteIfExists(uploadDir)
    }
}
