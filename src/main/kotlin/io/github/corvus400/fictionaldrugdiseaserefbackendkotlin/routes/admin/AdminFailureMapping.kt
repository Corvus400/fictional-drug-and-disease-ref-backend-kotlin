package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.FieldViolation
import kotlinx.coroutines.CancellationException

internal suspend inline fun <T> validationFailureAsResult(
    field: String,
    fallbackReason: String,
    crossinline block: suspend () -> T,
): AppResult<T> =
    runCatching { block() }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { error ->
            if (error is CancellationException) throw error
            AppResult.Failure(
                DomainError.Validation(
                    listOf(FieldViolation(field = field, reason = error.message ?: fallbackReason)),
                ),
            )
        },
    )

internal inline fun <T> unexpectedFailureAsResult(block: () -> T): AppResult<T> =
    runCatching { block() }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { error ->
            if (error is CancellationException) throw error
            AppResult.Failure(DomainError.Unexpected(error))
        },
    )
