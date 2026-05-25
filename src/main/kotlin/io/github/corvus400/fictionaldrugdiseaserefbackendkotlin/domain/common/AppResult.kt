package io.github.corvus400.fictionaldrugdiseaserefbackendkotlin.domain.common

/** 層境界は throw でなく success-or-error の値で渡す。 */
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: DomainError) : AppResult<Nothing>
}

/** throw した方が読みやすい場合の DomainError 運搬例外。StatusPages がレンダリングする。 */
class DomainException(val error: DomainError) : Exception()
