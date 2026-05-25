package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.data

import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.AppResult
import io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common.DomainError
import kotlinx.coroutines.CancellationException
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

internal suspend fun <T> queryUnexpectedAsFailure(block: suspend () -> AppResult<T>): AppResult<T> =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ExposedSQLException) {
        AppResult.Failure(DomainError.Unexpected(e))
    }
