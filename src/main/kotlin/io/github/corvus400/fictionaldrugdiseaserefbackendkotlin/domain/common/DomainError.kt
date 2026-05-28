package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common

import kotlinx.serialization.Serializable

/** 全層が参照する単一のドメインエラー型。feature ごとに再定義しない。 */
sealed interface DomainError {
    data class NotFound(val resource: String, val id: String) : DomainError
    data class Validation(val violations: List<FieldViolation>) : DomainError
    data class Conflict(val detail: String) : DomainError
    data class PreconditionFailed(val detail: String) : DomainError
    data object Unauthorized : DomainError
    data object Forbidden : DomainError
    data class Unexpected(val cause: Throwable) : DomainError
}

/** problem+json の errors[] にそのまま載るため @Serializable。 */
@Serializable
data class FieldViolation(val field: String, val reason: String)
