package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.routes.admin

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.FieldViolation
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException

internal suspend inline fun <T> validationFailureAsResult(
    field: String,
    fallbackReason: String,
    crossinline block: suspend () -> T,
): AppResult<T> =
    runCatching { block() }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { error ->
            if (error is CancellationException) throw error
            val violation = validationViolation(
                error = error,
                fallbackField = field,
                fallbackReason = fallbackReason,
            )
            AppResult.Failure(
                DomainError.Validation(
                    listOf(violation),
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

@OptIn(ExperimentalSerializationApi::class)
private fun validationViolation(
    error: Throwable,
    fallbackField: String,
    fallbackReason: String,
): FieldViolation {
    val missingFieldException = error.firstCause<MissingFieldException>()
    if (missingFieldException != null) {
        val missingField = missingFieldException.missingFieldNames().firstOrNull()?.toWireFieldName()
        if (missingField != null) {
            return FieldViolation(field = missingField, reason = "required")
        }
    }

    val jsonPathField = error.causeSequence()
        .mapNotNull { cause -> cause.message?.let(::fieldFromJsonPath) }
        .firstOrNull()
    return FieldViolation(
        field = jsonPathField ?: fallbackField,
        reason = if (jsonPathField == null) error.message ?: fallbackReason else "invalid",
    )
}

@OptIn(ExperimentalSerializationApi::class)
private fun MissingFieldException.missingFieldNames(): List<String> = missingFields

private fun fieldFromJsonPath(message: String): String? {
    val match = Regex("""path: \$\.([A-Za-z0-9_]+)""").find(message)
    return match?.groupValues?.getOrNull(1)?.toWireFieldName()
}

private fun String.toWireFieldName(): String =
    if ('_' in this) {
        this
    } else {
        replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()
    }

private inline fun <reified T : Throwable> Throwable.firstCause(): T? =
    causeSequence().firstOrNull { it is T } as T?

private fun Throwable.causeSequence(): Sequence<Throwable> =
    generateSequence(this) { current -> current.cause }
