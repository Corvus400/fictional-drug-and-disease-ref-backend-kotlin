package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class AdminFailureMappingTest {
    @Test
    fun `validation failure mapping rethrows cancellation`() = runTest {
        assertFailsWith<CancellationException> {
            validationFailureAsResult(field = "body", fallbackReason = "Invalid request body") {
                throw CancellationException("cancelled")
            }
        }
    }

    @Test
    fun `unexpected failure mapping rethrows cancellation`() {
        assertFailsWith<CancellationException> {
            unexpectedFailureAsResult {
                throw CancellationException("cancelled")
            }
        }
    }

    @Test
    fun `validation failure mapping converts ordinary failures`() = runTest {
        val result = validationFailureAsResult(field = "body", fallbackReason = "Invalid request body") {
            error("bad body")
        }

        val failure = assertIs<AppResult.Failure>(result)
        assertIs<DomainError.Validation>(failure.error)
    }

    @Test
    fun `unexpected failure mapping converts ordinary failures`() {
        val result = unexpectedFailureAsResult {
            error("filesystem failed")
        }

        val failure = assertIs<AppResult.Failure>(result)
        assertIs<DomainError.Unexpected>(failure.error)
    }
}
