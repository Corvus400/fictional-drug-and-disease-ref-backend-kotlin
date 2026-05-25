package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.AppJson
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.config.respondResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainException
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.FieldViolation
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.ProblemDetails
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.ProblemTypes
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.support.PostgresTestSupport.withPostgresConfig
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class StatusPagesTest {
    @Test
    fun `unmatched route returns problem json 404`() = testApplication {
        withPostgresConfig()
        application { module() }
        val response = client.get("/definitely-not-a-route")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals(ContentType("application", "problem+json"), response.contentType()?.withoutParameters())
        val problem = AppJson.decodeFromString<ProblemDetails>(response.bodyAsText())
        assertEquals(ProblemTypes.NOT_FOUND, problem.type)
        assertEquals(404, problem.status)
    }

    @Test
    fun `thrown DomainException renders problem json`() = testApplication {
        withPostgresConfig()
        application {
            module()
            routing {
                get("/__test/not-found") {
                    throw DomainException(DomainError.NotFound("drug", "drug_9999"))
                }
            }
        }
        val response = client.get("/__test/not-found")
        assertEquals(HttpStatusCode.NotFound, response.status)
        val problem = AppJson.decodeFromString<ProblemDetails>(response.bodyAsText())
        assertEquals(ProblemTypes.NOT_FOUND, problem.type)
        assertEquals("drug drug_9999", problem.detail)
    }

    @Test
    fun `respondResult failure renders problem json`() = testApplication {
        withPostgresConfig()
        application {
            module()
            routing {
                get("/__test/validation") {
                    call.respondResult<String>(
                        AppResult.Failure(
                            DomainError.Validation(
                                listOf(FieldViolation("dosage_form", "Unknown dosage_form: xyz")),
                            ),
                        ),
                    )
                }
            }
        }
        val response = client.get("/__test/validation")
        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        val problem = AppJson.decodeFromString<ProblemDetails>(response.bodyAsText())
        assertEquals(ProblemTypes.VALIDATION, problem.type)
        assertEquals("dosage_form", problem.errors?.first()?.field)
    }

    @Test
    fun `unexpected exception does not leak cause message`() = testApplication {
        withPostgresConfig()
        application {
            module()
            routing {
                get("/__test/boom") {
                    throw IllegalStateException("SECRET internal detail")
                }
            }
        }
        val response = client.get("/__test/boom")
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val body = response.bodyAsText()
        assertFalse(body.contains("SECRET"))
        val problem = AppJson.decodeFromString<ProblemDetails>(body)
        assertEquals(ProblemTypes.INTERNAL, problem.type)
        assertEquals("Internal server error", problem.title)
        assertNull(problem.detail)
    }
}
